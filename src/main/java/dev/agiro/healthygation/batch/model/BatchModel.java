package dev.agiro.healthygation.batch.model;

import java.io.Serializable;

/**
 * Tipus que viatgen per RabbitMQ dins dels chunks: han de ser Serializable
 * (o canvia el MessageConverter a JSON i ajusta els tipus permesos).
 */
public final class BatchModel {

    private BatchModel() {}

    /** Item llegit pel reader del step 1. */
    public record SourceItem(Long id, String payload) implements Serializable {}

    /** Resposta del microservei extern, ja processada (step 1 -> taula intermèdia). */
    public record ExternalCallResponse(Long itemId, String status, String payload) implements Serializable {}

    /** Fila llegida pel step 2 de la taula intermèdia. */
    public record ExternalCallResultRow(Long itemId, String status, String payload) implements Serializable {}

    /** Resultat de la comanda multi-pas executada al step 2. */
    public record CommandOutcome(Long itemId, boolean executed, String detail) implements Serializable {}
}
