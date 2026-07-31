package dev.agiro.healthygation.graphrag.api;

public record AskResponse(String answer, String context, String cypher) {
}
