package dev.agiro.healthygation.graphrag.domain;

import dev.agiro.healthygation.graphrag.api.AskRequest;
import dev.agiro.healthygation.graphrag.api.AskResponse;
import dev.agiro.healthygation.graphrag.config.GraphRagProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class GraphRagService {

    private static final Logger log = LoggerFactory.getLogger(GraphRagService.class);

    private final EmbeddingModel embeddingModel;
    private final ChatClient chatClient;
    private final Neo4jGraphStore graphStore;
    private final GraphRagProperties properties;

    public GraphRagService(EmbeddingModel embeddingModel, ChatClient chatClient, Neo4jGraphStore graphStore, GraphRagProperties properties) {
        this.embeddingModel = embeddingModel;
        this.chatClient = chatClient;
        this.graphStore = graphStore;
        this.properties = properties;
    }

    public AskResponse ask(AskRequest request) {
        float[] questionVector = embeddingModel.embed(request.question());
        List<Float> embedding = new ArrayList<>(questionVector.length);
        for (float value : questionVector) {
            embedding.add(value);
        }

        var similarNodes = graphStore.findSimilarNodes(embedding, properties.getSimilarityTopK());
        if (similarNodes.isEmpty()) {
            return new AskResponse("No relevant context found in the graph.", "", null);
        }

        Set<String> conceptIds = similarNodes.stream()
                .map(n -> (String) n.get("fhirId"))
                .collect(Collectors.toSet());
        var expansions = graphStore.expandOneHop(conceptIds);

        String context = buildContext(expansions);

        String prompt = """
                You are a clinical data analyst. Use only the provided patient cohort context to answer.
                Do not invent facts. If the context does not support an answer, say so.

                Context:
                %s

                Question: %s
                """.formatted(context, request.question());

        String answer = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        return new AskResponse(answer, context, null);
    }

    private String buildContext(List<Map<String, Object>> rows) {
        Map<String, List<String>> byPatient = new HashMap<>();
        for (var row : rows) {
            String patientId = (String) row.get("patientId");
            String relationship = String.valueOf(row.get("relationship"));
            String conceptText = String.valueOf(row.get("conceptText"));
            byPatient.computeIfAbsent(patientId, k -> new ArrayList<>())
                    .add(relationship + " " + conceptText);
        }
        StringBuilder sb = new StringBuilder();
        byPatient.forEach((patient, concepts) -> {
            sb.append("Patient: ").append(patient).append("\n");
            for (String c : concepts) {
                sb.append("  - ").append(c).append("\n");
            }
        });
        return sb.toString();
    }
}
