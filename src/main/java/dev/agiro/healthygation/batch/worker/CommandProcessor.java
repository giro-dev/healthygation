package dev.agiro.healthygation.batch.worker;

import dev.agiro.healthygation.batch.model.BatchModel.CommandOutcome;
import dev.agiro.healthygation.batch.model.BatchModel.ExternalCallResultRow;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;

/**
 * Step 2 (worker): segons la resposta desada al step 1, decideix si cal
 * llançar la comanda multi-pas i l'executa.
 */
@Component
public class CommandProcessor implements ItemProcessor<ExternalCallResultRow, CommandOutcome> {

    /** Contracte de la comanda multi-pas (pots delegar a un servei o a un Tasklet). */
    public interface CommandRunner {
        String run(ExternalCallResultRow row);
    }

    private final CommandRunner runner;

    public CommandProcessor(CommandRunner runner) {
        this.runner = runner;
    }

    @Override
    public CommandOutcome process(ExternalCallResultRow row) {
        if (!"ACCEPTED".equals(row.status())) {
            return new CommandOutcome(row.itemId(), false, "skipped: " + row.status());
        }
        return new CommandOutcome(row.itemId(), true, runner.run(row));
    }
}
