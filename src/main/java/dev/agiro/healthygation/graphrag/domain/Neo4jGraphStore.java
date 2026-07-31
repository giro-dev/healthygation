package dev.agiro.healthygation.graphrag.domain;

import dev.agiro.healthygation.graphrag.config.GraphRagProperties;
import jakarta.annotation.PostConstruct;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Repository
public class Neo4jGraphStore {

    private static final Set<String> ALLOWED_LABELS = Set.of("Patient", "Condition", "Medication", "Observation", "Embedding");
    private static final Set<String> ALLOWED_RELATIONSHIPS = Set.of("HAS_CONDITION", "TAKES", "HAS_OBSERVATION");

    private final Driver driver;
    private final GraphRagProperties properties;

    public Neo4jGraphStore(Driver driver, GraphRagProperties properties) {
        this.driver = driver;
        this.properties = properties;
    }

    @PostConstruct
    public void createSchema() {
        try (Session session = driver.session()) {
            session.run("CREATE CONSTRAINT fhirIdConstraint IF NOT EXISTS FOR (n:Embedding) REQUIRE n.fhirId IS UNIQUE");
            session.run("""
                    CREATE VECTOR INDEX embeddingIndex IF NOT EXISTS FOR (n:Embedding)
                    ON (n.embedding)
                    OPTIONS {indexConfig: {`vector.dimensions`: $dims, `vector.similarity_function`: 'cosine'}}
                    """,
                    Map.of("dims", properties.getEmbeddingDimensions()));
        }
    }

    public void savePatients(List<GraphNode> patients) {
        if (patients.isEmpty()) {
            return;
        }
        List<Map<String, Object>> params = patients.stream().map(this::toPatientParam).toList();
        try (Session session = driver.session()) {
            session.run("""
                    UNWIND $patients AS n
                    MERGE (p:Patient {fhirId: n.fhirId})
                    ON CREATE SET p.text = n.text, p += n.props
                    """,
                    Map.of("patients", params));
        }
    }

    public void saveConcepts(List<GraphNode> concepts) {
        if (concepts.isEmpty()) {
            return;
        }
        Map<String, List<Map<String, Object>>> byLabel = new HashMap<>();
        for (GraphNode concept : concepts) {
            byLabel.computeIfAbsent(concept.primaryLabel(), k -> new ArrayList<>()).add(toConceptParam(concept));
        }
        for (Map.Entry<String, List<Map<String, Object>>> entry : byLabel.entrySet()) {
            String label = validateLabel(entry.getKey());
            try (Session session = driver.session()) {
                String cypher = """
                        UNWIND $nodes AS n
                        MERGE (c:%s:Embedding {fhirId: n.fhirId})
                        ON CREATE SET c.text = n.text, c.embedding = n.embedding, c += n.props
                        """.formatted(label);
                session.run(cypher, Map.of("nodes", entry.getValue()));
            }
        }
    }

    public void saveRelationships(List<Relationship> relationships, String relationshipType, String targetLabel) {
        if (relationships.isEmpty()) {
            return;
        }
        if (!ALLOWED_RELATIONSHIPS.contains(relationshipType)) {
            throw new IllegalArgumentException("Unsupported relationship type: " + relationshipType);
        }
        validateLabel(targetLabel);
        List<Map<String, Object>> params = relationships.stream()
                .map(r -> Map.<String, Object>of("fromId", r.fromId(), "toId", r.toId()))
                .toList();
        try (Session session = driver.session()) {
            String cypher = """
                    UNWIND $relations AS r
                    MATCH (a:Patient {fhirId: r.fromId}), (b:%s:Embedding {fhirId: r.toId})
                    MERGE (a)-[:%s]->(b)
                    """.formatted(targetLabel, relationshipType);
            session.run(cypher, Map.of("relations", params));
        }
    }

    public List<Map<String, Object>> findSimilarNodes(List<Float> embedding, int k) {
        if (embedding == null || embedding.isEmpty()) {
            return List.of();
        }
        try (Session session = driver.session()) {
            var result = session.run("""
                    CALL db.index.vector.queryNodes('embeddingIndex', $k, $embedding) YIELD node, score
                    RETURN node.fhirId AS fhirId, node.text AS text, labels(node) AS labels, score
                    """,
                    Map.of("k", k, "embedding", embedding));
            List<Map<String, Object>> nodes = new ArrayList<>();
            result.forEachRemaining(record -> {
                Map<String, Object> map = new HashMap<>();
                map.put("fhirId", record.get("fhirId").asString());
                map.put("text", record.get("text").asString());
                map.put("labels", record.get("labels").asList(value -> value.asString()));
                map.put("score", record.get("score").asDouble());
                nodes.add(map);
            });
            return nodes;
        }
    }

    public List<Map<String, Object>> expandOneHop(Set<String> fhirIds) {
        if (fhirIds == null || fhirIds.isEmpty()) {
            return List.of();
        }
        try (Session session = driver.session()) {
            var result = session.run("""
                    UNWIND $ids AS id
                    MATCH (p:Patient)-[r]->(c:Embedding {fhirId: id})
                    RETURN c.fhirId AS conceptId, c.text AS conceptText, p.fhirId AS patientId,
                           p.gender AS gender, p.birthYear AS birthYear, type(r) AS relationship
                    """,
                    Map.of("ids", new ArrayList<>(fhirIds)));
            List<Map<String, Object>> rows = new ArrayList<>();
            result.forEachRemaining(record -> {
                Map<String, Object> map = new HashMap<>();
                map.put("conceptId", record.get("conceptId").asString());
                map.put("conceptText", record.get("conceptText").asString());
                map.put("patientId", record.get("patientId").asString());
                map.put("gender", record.get("gender").asString(null));
                map.put("birthYear", record.get("birthYear").asObject());
                map.put("relationship", record.get("relationship").asString());
                rows.add(map);
            });
            return rows;
        }
    }

    private Map<String, Object> toPatientParam(GraphNode node) {
        Map<String, Object> map = new HashMap<>();
        map.put("fhirId", node.fhirId());
        map.put("text", node.text());
        map.put("props", node.properties() == null ? Map.of() : node.properties());
        return map;
    }

    private Map<String, Object> toConceptParam(GraphNode node) {
        Map<String, Object> map = new HashMap<>();
        map.put("fhirId", node.fhirId());
        map.put("text", node.text());
        map.put("embedding", node.embedding());
        map.put("props", node.properties() == null ? Map.of() : node.properties());
        return map;
    }

    private String validateLabel(String label) {
        if (!ALLOWED_LABELS.contains(label)) {
            throw new IllegalArgumentException("Unsupported label: " + label);
        }
        return label;
    }
}
