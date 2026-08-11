package com.industry.aichat.service.graph;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class GraphSchemaService {

    private final Neo4jClient neo4jClient;

    @PostConstruct
    public void initSchema() {
        runSafely("CREATE CONSTRAINT industrial_document_file_id IF NOT EXISTS " +
                "FOR (d:IndustrialDocument) REQUIRE d.fileId IS UNIQUE");
        runSafely("CREATE CONSTRAINT industrial_chunk_id IF NOT EXISTS " +
                "FOR (c:IndustrialChunk) REQUIRE c.chunkId IS UNIQUE");
        runSafely("CREATE CONSTRAINT industrial_entity_key IF NOT EXISTS " +
                "FOR (e:IndustrialEntity) REQUIRE (e.type, e.name) IS UNIQUE");
        runSafely("CREATE FULLTEXT INDEX industrial_chunk_text IF NOT EXISTS " +
                "FOR (c:IndustrialChunk) ON EACH [c.text, c.sourceFileName, c.sectionTitle]");
    }

    private void runSafely(String cypher) {
        try {
            neo4jClient.query(cypher).run();
        } catch (Exception e) {
            log.warn("Neo4j schema init skipped: {}", e.getMessage());
        }
    }
}
