package dev.agiro.healthygation.graphrag.api;

import dev.agiro.healthygation.graphrag.domain.GraphRagService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/graph-rag")
public class GraphRagController {

    private final GraphRagService graphRagService;

    public GraphRagController(GraphRagService graphRagService) {
        this.graphRagService = graphRagService;
    }

    @PostMapping("/ask")
    public AskResponse ask(@Valid @RequestBody AskRequest request) {
        return graphRagService.ask(request);
    }
}
