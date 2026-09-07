package com.industry.aichat.service.graph;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GraphRetrievalService {

    private final Neo4jClient neo4jClient;
    private final GraphExtractionService extractionService;

    public List<GraphEvidence> retrieve(String query, List<Long> fileIds, int limit) {
        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }

        GraphExtractionResult queryEntities = extractionService.extractQueryEntities(query);
        List<String> names = queryEntities.getEntities().stream()
                .map(GraphEntity::getName)
                .distinct()
                .collect(Collectors.toList());
        List<String> terms = buildTerms(query, names);

        if (names.isEmpty() && terms.isEmpty()) {
            return List.of();
        }

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("names", names);
        params.put("terms", terms);
        params.put("fileIds", fileIds == null ? List.of() : fileIds);
        params.put("limit", Math.max(1, Math.min(limit, 20)));

        try {
            Collection<Map<String, Object>> rows = neo4jClient.query("""
                    MATCH (c:IndustrialChunk)-[:MENTIONS]->(seed:IndustrialEntity)
                    WHERE (size($fileIds) = 0 OR c.fileId IN $fileIds)
                      AND (
                        seed.name IN $names
                        OR any(term IN $terms WHERE seed.name CONTAINS term OR c.text CONTAINS term)
                      )
                    OPTIONAL MATCH path = (seed)-[*1..2]-(related:IndustrialEntity)
                    RETURN c.chunkId AS chunkId,
                           c.fileId AS fileId,
                           c.chunkIndex AS chunkIndex,
                           c.sourceFileName AS sourceFileName,
                           c.text AS text,
                           collect(DISTINCT seed.type + ':' + seed.name) AS seeds,
                           collect(DISTINCT [n IN nodes(path) WHERE n:IndustrialEntity | n.type + ':' + n.name]) AS graphPaths
                    LIMIT $limit
                    """)
                    .bindAll(params)
                    .fetch()
                    .all();

            return rows.stream()
                    .map(this::toEvidence)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("GraphRAG retrieval skipped: {}", e.getMessage());
            return List.of();
        }
    }

    private GraphEvidence toEvidence(Map<String, Object> row) {
        return GraphEvidence.builder()
                .chunkId(asString(row.get("chunkId")))
                .fileId(asLong(row.get("fileId")))
                .chunkIndex(asInteger(row.get("chunkIndex")))
                .sourceFileName(asString(row.get("sourceFileName")))
                .text(asString(row.get("text")))
                .graphPaths(toPathTexts(row.get("graphPaths"), row.get("seeds")))
                .build();
    }

    private List<String> buildTerms(String query, List<String> names) {
        List<String> terms = new ArrayList<>(names);
        String compactQuery = query.trim();
        if (compactQuery.length() >= 2 && compactQuery.length() <= 80) {
            terms.add(compactQuery);
        }
        for (String token : compactQuery.split("[\\s,，。；;:：]+")) {
            if (token.length() >= 2 && token.length() <= 32) {
                terms.add(token);
            }
        }
        return terms.stream().distinct().collect(Collectors.toList());
    }

    private List<String> toPathTexts(Object rawPaths, Object rawSeeds) {
        List<String> paths = new ArrayList<>();
        if (rawPaths instanceof Iterable<?> outer) {
            for (Object path : outer) {
                if (path instanceof Iterable<?> nodes) {
                    List<String> nodeNames = new ArrayList<>();
                    for (Object node : nodes) {
                        String value = asString(node);
                        if (!value.isBlank()) {
                            nodeNames.add(value);
                        }
                    }
                    if (nodeNames.size() > 1) {
                        paths.add(String.join(" -> ", nodeNames));
                    }
                }
            }
        }
        if (paths.isEmpty() && rawSeeds instanceof Iterable<?> seeds) {
            for (Object seed : seeds) {
                String value = asString(seed);
                if (!value.isBlank()) {
                    paths.add(value);
                }
            }
        }
        return paths.stream().distinct().limit(5).collect(Collectors.toList());
    }

    private String asString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private Long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return value == null ? null : Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer asInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return value == null ? null : Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
