package com.industry.aichat.service.graph;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GraphEntity {

    private String type;

    private String name;

    public String key() {
        return type + ":" + name;
    }
}
