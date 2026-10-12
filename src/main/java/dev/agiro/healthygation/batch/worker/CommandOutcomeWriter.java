package dev.agiro.healthygation.batch.worker;

import dev.agiro.healthygation.batch.model.BatchModel.CommandOutcome;
import java.util.List;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Step 2 (worker): desa el resultat final de cada comanda, en bloc i idempotent. */
@Component
public class CommandOutcomeWriter implements ItemWriter<CommandOutcome> {

    private final JdbcTemplate jdbc;

    public CommandOutcomeWriter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void write(Chunk<? extends CommandOutcome> chunk) {
        List<Object[]> args = chunk.getItems().stream()
                .map(o -> new Object[] {o.itemId(), o.executed(), o.detail()})
                .toList();
        jdbc.batchUpdate("""
                INSERT INTO command_outcome (item_id, executed, detail)
                VALUES (?, ?, ?)
                ON CONFLICT (item_id) DO UPDATE
                   SET executed = EXCLUDED.executed, detail = EXCLUDED.detail
                """, args);
    }
}
