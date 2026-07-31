package dev.agiro.healthygation.graphrag.api;

import jakarta.validation.constraints.NotBlank;

public record AskRequest(@NotBlank String question) {
}
