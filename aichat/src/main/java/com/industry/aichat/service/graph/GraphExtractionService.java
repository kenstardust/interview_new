package com.industry.aichat.service.graph;

import com.industry.aichat.model.ChatFile;
import com.industry.aichat.model.DocumentChunk;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class GraphExtractionService {

    private static final Pattern ALARM_CODE_PATTERN =
            Pattern.compile("\\b(?:E|F|A|ALM|ERR|ERROR)[- ]?\\d{2,5}\\b", Pattern.CASE_INSENSITIVE);

    private static final Pattern MODEL_PATTERN =
            Pattern.compile("\\b[A-Z]{2,}[-_]?\\d{2,}[A-Z0-9-]*\\b");

    private static final Pattern PARAMETER_PATTERN =
            Pattern.compile("(?:参数|阈值|设定值|寄存器)\\s*[:：]?\\s*[A-Za-z0-9_.-]{1,32}|\\bP\\d{1,4}(?:\\.\\d+)?\\b");

    public GraphExtractionResult extract(DocumentChunk chunk, ChatFile file) {
        String text = safeText(chunk.getContent());
        GraphExtractionResult result = extractQueryEntities(text);

        GraphEntity documentEntity = GraphEntity.builder()
                .type("Document")
                .name(file.getOriginalfilename())
                .build();
        addEntity(result, documentEntity);

        GraphEntity equipment = firstEntity(result, "Equipment");
        GraphEntity alarm = firstEntity(result, "AlarmCode");
        GraphEntity fault = firstEntity(result, "Fault");
        GraphEntity cause = firstEntity(result, "Cause");
        GraphEntity solution = firstEntity(result, "Solution");
        GraphEntity parameter = firstEntity(result, "Parameter");
        GraphEntity safety = firstEntity(result, "SafetyRule");

        if (alarm != null && equipment != null) {
            addRelation(result, alarm, "OCCURS_ON", equipment);
        }
        if (alarm != null && fault != null) {
            addRelation(result, alarm, "INDICATES", fault);
        }
        if (alarm != null && cause != null) {
            addRelation(result, alarm, "HAS_CAUSE", cause);
        }
        if (cause != null && solution != null) {
            addRelation(result, cause, "RESOLVED_BY", solution);
        }
        if (solution != null && safety != null) {
            addRelation(result, solution, "REQUIRES", safety);
        }
        if (parameter != null && equipment != null) {
            addRelation(result, parameter, "CONFIGURES", equipment);
        }

        return result;
    }

    public GraphExtractionResult extractQueryEntities(String text) {
        GraphExtractionResult result = new GraphExtractionResult();
        String content = safeText(text);

        findMatches(ALARM_CODE_PATTERN, content).forEach(code ->
                addEntity(result, GraphEntity.builder().type("AlarmCode").name(normalizeAlarmCode(code)).build()));

        findMatches(MODEL_PATTERN, content).forEach(model ->
                addEntity(result, GraphEntity.builder().type("Equipment").name(model.trim()).build()));

        findMatches(PARAMETER_PATTERN, content).forEach(parameter ->
                addEntity(result, GraphEntity.builder().type("Parameter").name(compact(parameter)).build()));

        for (String sentence : splitSentences(content)) {
            String compactSentence = compact(sentence);
            if (compactSentence.length() < 4) {
                continue;
            }
            if (containsAny(compactSentence, "设备", "变频器", "PLC", "DCS", "SCADA", "HMI", "传感器", "阀门", "电机", "控制器")) {
                addEntity(result, GraphEntity.builder().type("Equipment").name(shortText(compactSentence)).build());
            }
            if (containsAny(compactSentence, "模块", "子系统", "采集", "监控", "网关", "接口")) {
                addEntity(result, GraphEntity.builder().type("SoftwareModule").name(shortText(compactSentence)).build());
            }
            if (containsAny(compactSentence, "故障", "异常", "报警", "过压", "过流", "停机", "超温", "通信中断")) {
                addEntity(result, GraphEntity.builder().type("Fault").name(shortText(compactSentence)).build());
            }
            if (containsAny(compactSentence, "原因", "由于", "可能是", "导致", "因为")) {
                addEntity(result, GraphEntity.builder().type("Cause").name(shortText(compactSentence)).build());
            }
            if (containsAny(compactSentence, "检查", "处理", "解决", "更换", "复位", "调整", "配置", "设置", "确认")) {
                addEntity(result, GraphEntity.builder().type("Solution").name(shortText(compactSentence)).build());
            }
            if (containsAny(compactSentence, "高压", "断电", "联锁", "停机", "专业人员", "安全", "禁止", "确认设备")) {
                addEntity(result, GraphEntity.builder().type("SafetyRule").name(shortText(compactSentence)).build());
            }
        }

        deduplicate(result);
        return result;
    }

    private void addEntity(GraphExtractionResult result, GraphEntity entity) {
        if (entity == null || isBlank(entity.getType()) || isBlank(entity.getName())) {
            return;
        }
        result.getEntities().add(entity);
    }

    private void addRelation(GraphExtractionResult result, GraphEntity source, String type, GraphEntity target) {
        result.getRelations().add(GraphRelation.builder()
                .source(source)
                .type(type)
                .target(target)
                .build());
    }

    private void deduplicate(GraphExtractionResult result) {
        Map<String, GraphEntity> uniqueEntities = new LinkedHashMap<>();
        for (GraphEntity entity : result.getEntities()) {
            uniqueEntities.put(entity.key(), entity);
        }
        result.setEntities(new ArrayList<>(uniqueEntities.values()));

        Map<String, GraphRelation> uniqueRelations = new LinkedHashMap<>();
        for (GraphRelation relation : result.getRelations()) {
            String key = relation.getSource().key() + "-" + relation.getType() + "-" + relation.getTarget().key();
            uniqueRelations.put(key, relation);
        }
        result.setRelations(new ArrayList<>(uniqueRelations.values()));
    }

    private GraphEntity firstEntity(GraphExtractionResult result, String type) {
        return result.getEntities().stream()
                .filter(entity -> type.equals(entity.getType()))
                .findFirst()
                .orElse(null);
    }

    private List<String> findMatches(Pattern pattern, String text) {
        List<String> matches = new ArrayList<>();
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            matches.add(matcher.group());
        }
        return matches;
    }

    private List<String> splitSentences(String text) {
        return List.of(text.split("[。！？!?\\n]+"));
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String normalizeAlarmCode(String code) {
        return code.toUpperCase().replace(" ", "").replace("-", "");
    }

    private String shortText(String text) {
        String compact = compact(text);
        return compact.length() > 80 ? compact.substring(0, 80) : compact;
    }

    private String compact(String text) {
        return safeText(text).replaceAll("\\s+", " ").trim();
    }

    private String safeText(String text) {
        return text == null ? "" : text;
    }

    private boolean isBlank(String text) {
        return text == null || text.trim().isEmpty();
    }
}
