package dev.agiro.healthygation.batch.worker;

import dev.agiro.healthygation.batch.model.BatchModel.ExternalCallResponse;
import java.util.List;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Step 1 (micro A): update/upsert massiu de totes les respostes de B per al chunk.
 * L'AsyncItemWriter ja ha resolt els Future, així que aquí només arriben respostes completes.
 * Idempotent: si el chunk es reintenta, el segon intent sobreescriu amb el mateix resultat.
 */
@Component
@Profile("manager")
public class ExternalCallResponseBulkWriter implements ItemWriter<ExternalCallResponse> {

    private final JdbcTemplate jdbc;

    public ExternalCallResponseBulkWriter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void write(Chunk<? extends ExternalCallResponse> chunk) {
        List<Object[]> args = chunk.getItems().stream()
                .map(r -> new Object[] {r.itemId(), r.status(), r.payload()})
                .toList();
        jdbc.batchUpdate("""
                INSERT INTO external_call_result (item_id, status, payload)
                VALUES (?, ?, ?)
                ON CONFLICT (item_id) DO UPDATE
                   SET status = EXCLUDED.status, payload = EXCLUDED.payload
                """, args);
    }
}
