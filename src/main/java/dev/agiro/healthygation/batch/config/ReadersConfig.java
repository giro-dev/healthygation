package dev.agiro.healthygation.batch.config;

import dev.agiro.healthygation.batch.model.BatchModel.ExternalCallResultRow;
import dev.agiro.healthygation.batch.model.BatchModel.SourceItem;
import javax.sql.DataSource;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.batch.infrastructure.item.database.JdbcCursorItemReader;
import org.springframework.batch.infrastructure.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.DataClassRowMapper;

/** Readers dels dos steps. Ajusta les queries a les taules reals. */
@Configuration
public class ReadersConfig {

    @Bean
    JdbcCursorItemReader<SourceItem> step1Reader(DataSource ds) {
        return new JdbcCursorItemReaderBuilder<SourceItem>()
                .name("step1Reader")
                .dataSource(ds)
                .sql("SELECT id, payload FROM source_item ORDER BY id")
                .rowMapper(new DataClassRowMapper<>(SourceItem.class))
                .build();
    }

    @Bean
    JdbcCursorItemReader<ExternalCallResultRow> step2Reader(DataSource ds) {
        return new JdbcCursorItemReaderBuilder<ExternalCallResultRow>()
                .name("step2Reader")
                .dataSource(ds)
                .sql("SELECT item_id, status, payload FROM external_call_result ORDER BY item_id")
                .rowMapper(new DataClassRowMapper<>(ExternalCallResultRow.class))
                .build();
    }
}
