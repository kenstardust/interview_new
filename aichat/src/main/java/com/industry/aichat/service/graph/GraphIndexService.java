package com.industry.aichat.service.graph;

import com.industry.aichat.model.ChatFile;
import com.industry.aichat.model.DocumentChunk;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GraphIndexService {

    private final Neo4jClient neo4jClient;
    private final GraphExtractionService extractionService;

    public void indexDocumentGraph(ChatFile file, List<DocumentChunk> chunks) {
        if (file == null || chunks == null || chunks.isEmpty()) {
            return;
        }

        createDocumentNode(file);

        int entityCount = 0;
        int relationCount = 0;
        for (DocumentChunk chunk : chunks) {
            createChunkNode(file, chunk);

            GraphExtractionResult result = extractionService.extract(chunk, file);
            for (GraphEntity entity : result.getEntities()) {
                mergeEntity(entity);
                linkChunkToEntity(chunk, entity);
                entityCount++;
            }
            for (GraphRelation relation : result.getRelations()) {
                mergeBusinessRelation(relation, chunk);
                relationCount++;
            }
        }

        log.info("GraphRAG index finished: fileId={}, chunks={}, entities={}, relations={}",
                file.getId(), chunks.size(), entityCount, relationCount);
    }

    public void deleteDocumentGraph(Long fileId) {
        if (fileId == null) {
            return;
        }
        Map<String, Object> params = Map.of("fileId", fileId);
        neo4jClient.query("MATCH ()-[r]-() WHERE r.fileId = $fileId DELETE r")
                .bindAll(params)
                .run();
        neo4jClient.query("""
                MATCH (d:IndustrialDocument {fileId: $fileId})
                OPTIONAL MATCH (d)-[:HAS_CHUNK]->(c:IndustrialChunk)
                DETACH DELETE c, d
                """)
                .bindAll(params)
                .run();
    }

    private void createDocumentNode(ChatFile file) {
        Map<String, Object> params = new HashMap<>();
        params.put("fileId", file.getId());
        params.put("name", emptyIfNull(file.getOriginalfilename()));
        params.put("storageKey", emptyIfNull(file.getStoragekey()));
        params.put("contentType", emptyIfNull(file.getContenttype()));

        neo4jClient.query("""
                MERGE (d:IndustrialDocument {fileId: $fileId})
                SET d.name = $name,
                    d.storageKey = $storageKey,
                    d.contentType = $contentType
                """)
                .bindAll(params)
                .run();
    }

    private void createChunkNode(ChatFile file, DocumentChunk chunk) {
        Map<String, Object> params = new HashMap<>();
        params.put("fileId", file.getId());
        params.put("sourceFileName", emptyIfNull(file.getOriginalfilename()));
        params.put("chunkId", chunk.getChunkId());
        params.put("text", emptyIfNull(chunk.getContent()));
        params.put("chunkIndex", chunk.getChunkIndex() == null ? 0 : chunk.getChunkIndex());
        params.put("tokenCount", chunk.getTokenCount() == null ? 0 : chunk.getTokenCount());

        neo4jClient.query("""
                MATCH (d:IndustrialDocument {fileId: $fileId})
                MERGE (c:IndustrialChunk {chunkId: $chunkId})
                SET c.text = $text,
                    c.fileId = $fileId,
                    c.chunkIndex = $chunkIndex,
                    c.tokenCount = $tokenCount,
                    c.sourceFileName = $sourceFileName
                MERGE (d)-[:HAS_CHUNK]->(c)
                """)
                .bindAll(params)
                .run();
    }

    private void mergeEntity(GraphEntity entity) {
        Map<String, Object> params = Map.of(
                "type", entity.getType(),
                "name", entity.getName()
        );
        neo4jClient.query("""
                MERGE (e:IndustrialEntity {type: $type, name: $name})
                SET e.updatedAt = datetime()
                """)
                .bindAll(params)
                .run();
    }

    private void linkChunkToEntity(DocumentChunk chunk, GraphEntity entity) {
        Map<String, Object> params = Map.of(
                "chunkId", chunk.getChunkId(),
                "type", entity.getType(),
                "name", entity.getName(),
                "fileId", chunk.getFileId()
        );
        neo4jClient.query("""
                MATCH (c:IndustrialChunk {chunkId: $chunkId})
                MATCH (e:IndustrialEntity {type: $type, name: $name})
                MERGE (c)-[r:MENTIONS]->(e)
                SET r.fileId = $fileId
                """)
                .bindAll(params)
                .run();
    }

    private void mergeBusinessRelation(GraphRelation relation, DocumentChunk chunk) {
        String relationType = safeRelationType(relation.getType());
        Map<String, Object> params = Map.of(
                "sourceType", relation.getSource().getType(),
                "sourceName", relation.getSource().getName(),
                "targetType", relation.getTarget().getType(),
                "targetName", relation.getTarget().getName(),
                "chunkId", chunk.getChunkId(),
                "fileId", chunk.getFileId()
        );

        String cypher = """
                MATCH (s:IndustrialEntity {type: $sourceType, name: $sourceName})
                MATCH (t:IndustrialEntity {type: $targetType, name: $targetName})
                MERGE (s)-[r:%s]->(t)
                SET r.chunkId = $chunkId,
                    r.fileId = $fileId
                """.formatted(relationType);

        neo4jClient.query(cypher)
                .bindAll(params)
                .run();
    }

    private String safeRelationType(String relationType) {
        return switch (relationType) {
            case "OCCURS_ON" -> "OCCURS_ON";
            case "HAS_CAUSE" -> "HAS_CAUSE";
            case "RESOLVED_BY" -> "RESOLVED_BY";
            case "CONFIGURES" -> "CONFIGURES";
            case "REQUIRES" -> "REQUIRES";
            case "INDICATES" -> "INDICATES";
            default -> "RELATED_TO";
        };
    }

    private String emptyIfNull(String value) {
        return value == null ? "" : value;
    }
}
