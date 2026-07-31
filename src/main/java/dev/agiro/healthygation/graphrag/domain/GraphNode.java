package dev.agiro.healthygation.graphrag.domain;

import java.util.List;
import java.util.Map;

public record GraphNode(
        String fhirId,
        String primaryLabel,
        String text,
        List<Float> embedding,
        Map<String, Object> properties
) {
}
