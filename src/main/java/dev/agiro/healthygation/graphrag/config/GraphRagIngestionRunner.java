package dev.agiro.healthygation.graphrag.config;

import dev.agiro.healthygation.graphrag.domain.FhirGraphIngester;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("ingest")
public class GraphRagIngestionRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(GraphRagIngestionRunner.class);

    private final FhirGraphIngester fhirGraphIngester;

    public GraphRagIngestionRunner(FhirGraphIngester fhirGraphIngester) {
        this.fhirGraphIngester = fhirGraphIngester;
    }

    @Override
    public void run(String... args) {
        log.info("Starting Graph-RAG data ingestion");
        fhirGraphIngester.ingestAll(100, 1000);
        log.info("Graph-RAG data ingestion complete");
    }
}
