package com.industry.aichat.service.graph;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GraphEvidence {

    private String chunkId;

    private Long fileId;

    private Integer chunkIndex;

    private String sourceFileName;

    private String text;

    @Builder.Default
    private List<String> graphPaths = new ArrayList<>();
}
