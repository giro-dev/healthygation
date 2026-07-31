package dev.agiro.healthygation.graphrag.domain;

import dev.agiro.healthygation.graphrag.api.AskRequest;
import dev.agiro.healthygation.graphrag.api.AskResponse;
import dev.agiro.healthygation.graphrag.config.GraphRagProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.embedding.EmbeddingModel;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class GraphRagServiceTest {

    private final EmbeddingModel embeddingModel = Mockito.mock(EmbeddingModel.class);
    private final ChatClient chatClient = Mockito.mock(ChatClient.class, Mockito.RETURNS_DEEP_STUBS);
    private final Neo4jGraphStore graphStore = Mockito.mock(Neo4jGraphStore.class);
    private final GraphRagProperties properties = Mockito.mock(GraphRagProperties.class);

    private GraphRagService service;

    @BeforeEach
    void setUp() {
        when(properties.getSimilarityTopK()).thenReturn(2);
        service = new GraphRagService(embeddingModel, chatClient, graphStore, properties);
    }

    @Test
    void askReturnsNoContextWhenNoSimilarNodes() {
        when(embeddingModel.embed(anyString())).thenReturn(new float[]{0.1f, 0.2f});
        when(graphStore.findSimilarNodes(anyList(), anyInt())).thenReturn(List.of());

        AskResponse response = service.ask(new AskRequest("Any question"));

        assertEquals("No relevant context found in the graph.", response.answer());
        assertTrue(response.context().isEmpty());
    }

    @Test
    void askReturnsAnswerFromContext() {
        when(embeddingModel.embed(anyString())).thenReturn(new float[]{0.1f, 0.2f});
        when(graphStore.findSimilarNodes(anyList(), anyInt())).thenReturn(List.of(
                Map.of("fhirId", "Condition/1", "text", "Hypertension")
        ));
        when(graphStore.expandOneHop(anySet())).thenReturn(List.of(
                Map.of("conceptId", "Condition/1",
                        "conceptText", "Hypertension",
                        "patientId", "Patient/1",
                        "gender", "male",
                        "birthYear", 1980,
                        "relationship", "HAS_CONDITION")
        ));
        when(chatClient.prompt().user(anyString()).call().content()).thenReturn("Patients with hypertension.");

        AskResponse response = service.ask(new AskRequest("Which patients have hypertension?"));

        assertEquals("Patients with hypertension.", response.answer());
        assertTrue(response.context().contains("Patient: Patient/1"));
    }
}
