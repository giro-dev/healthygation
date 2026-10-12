package dev.agiro.healthygation.batch.worker;

import dev.agiro.healthygation.batch.model.BatchModel.ExternalCallResultRow;
import dev.agiro.healthygation.batch.worker.CommandProcessor.CommandRunner;
import org.springframework.stereotype.Component;

/** Implementació per defecte (placeholder): substitueix-la per la comanda multi-pas real. */
@Component
public class DefaultCommandRunner implements CommandRunner {

    @Override
    public String run(ExternalCallResultRow row) {
        return "executed item " + row.itemId();
    }
}
