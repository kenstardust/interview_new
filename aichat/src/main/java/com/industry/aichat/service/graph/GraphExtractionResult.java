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
public class GraphExtractionResult {

    @Builder.Default
    private List<GraphEntity> entities = new ArrayList<>();

    @Builder.Default
    private List<GraphRelation> relations = new ArrayList<>();
}
