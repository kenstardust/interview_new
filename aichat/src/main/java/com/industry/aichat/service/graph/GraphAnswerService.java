package com.industry.aichat.service.graph;

import com.industry.aichat.model.DocumentChunk;
import com.industry.aichat.service.rag.RetrievalService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GraphAnswerService {

    private static final int GRAPH_EVIDENCE_LIMIT = 8;
    private static final int FALLBACK_CHUNK_LIMIT = 5;
    private static final int MAX_EVIDENCE_LENGTH = 9000;

    private final GraphRetrievalService graphRetrievalService;
    private final RetrievalService retrievalService;

    public GraphAnswerContext retrieveContext(String query, List<Long> fileIds) {
        List<GraphEvidence> graphEvidence = graphRetrievalService.retrieve(query, fileIds, GRAPH_EVIDENCE_LIMIT);
        if (!graphEvidence.isEmpty()) {
            return GraphAnswerContext.builder()
                    .graphEvidence(graphEvidence)
                    .build();
        }

        List<DocumentChunk> fallbackChunks;
        if (fileIds != null && !fileIds.isEmpty()) {
            fallbackChunks = retrievalService.retrieveRelevantChunksInFiles(query, fileIds, FALLBACK_CHUNK_LIMIT);
        } else {
            fallbackChunks = retrievalService.retrieveRelevantChunks(query, FALLBACK_CHUNK_LIMIT);
        }

        return GraphAnswerContext.builder()
                .fallbackChunks(fallbackChunks)
                .build();
    }

    public String buildPrompt(String query, GraphAnswerContext context) {
        String evidence = buildEvidence(context);
        return String.format("""
                你是工业系统智能问答助手，负责回答工业软件、设备、报警、故障诊断、参数配置和操作规程问题。

                约束：
                1. 只能依据“图谱路径”和“原文证据”回答，不得编造设备参数、报警原因、操作步骤。
                2. 如果证据不足，明确回答“知识库中未找到可靠依据”。
                3. 涉及停机、复位、联锁、高压、参数修改等高风险操作时，必须提示由专业人员确认。
                4. 输出必须包含：结论、原因/依据、处理步骤、注意事项、来源。

                图谱路径与原文证据：
                %s

                用户问题：
                %s
                """, evidence, query);
    }

    private String buildEvidence(GraphAnswerContext context) {
        if (context == null || !context.hasEvidence()) {
            return "未找到图谱路径或原文证据。";
        }

        StringBuilder builder = new StringBuilder();
        int currentLength = 0;

        for (GraphEvidence evidence : context.getGraphEvidence()) {
            String block = formatGraphEvidence(evidence);
            if (currentLength + block.length() > MAX_EVIDENCE_LENGTH) {
                break;
            }
            builder.append(block);
            currentLength += block.length();
        }

        for (DocumentChunk chunk : context.getFallbackChunks()) {
            String block = formatChunkEvidence(chunk);
            if (currentLength + block.length() > MAX_EVIDENCE_LENGTH) {
                break;
            }
            builder.append(block);
            currentLength += block.length();
        }

        return builder.toString();
    }

    private String formatGraphEvidence(GraphEvidence evidence) {
        StringBuilder builder = new StringBuilder();
        builder.append("\n[图谱证据 fileId=")
                .append(evidence.getFileId())
                .append(", chunk=")
                .append(evidence.getChunkIndex())
                .append(", source=")
                .append(emptyIfNull(evidence.getSourceFileName()))
                .append("]\n");
        if (!evidence.getGraphPaths().isEmpty()) {
            builder.append("图谱路径：\n");
            for (String path : evidence.getGraphPaths()) {
                builder.append("- ").append(path).append("\n");
            }
        }
        builder.append("原文片段：\n")
                .append(emptyIfNull(evidence.getText()))
                .append("\n");
        return builder.toString();
    }

    private String formatChunkEvidence(DocumentChunk chunk) {
        return "\n[向量证据 fileId=" + chunk.getFileId() +
                ", chunk=" + chunk.getChunkIndex() + "]\n" +
                emptyIfNull(chunk.getContent()) + "\n";
    }

    private String emptyIfNull(String value) {
        return value == null ? "" : value;
    }
}
