package dev.agiro.healthygation.graphrag.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.agiro.healthygation.graphrag.domain.GraphRagService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GraphRagControllerTest {

    private final GraphRagService graphRagService = Mockito.mock(GraphRagService.class);
    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new GraphRagController(graphRagService)).build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void askReturnsOkForValidRequest() throws Exception {
        when(graphRagService.ask(any(AskRequest.class))).thenReturn(new AskResponse("Answer", "Context", null));

        mockMvc.perform(post("/graph-rag/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AskRequest("What is diabetes?"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("Answer"));
    }

    @Test
    void askReturnsBadRequestForEmptyQuestion() throws Exception {
        mockMvc.perform(post("/graph-rag/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AskRequest(""))))
                .andExpect(status().isBadRequest());
    }
}
