package com.industry.aichat.service.graph;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GraphRelation {

    private GraphEntity source;

    private String type;

    private GraphEntity target;
}
