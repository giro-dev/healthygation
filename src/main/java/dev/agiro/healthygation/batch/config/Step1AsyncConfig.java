package dev.agiro.healthygation.batch.config;

import dev.agiro.healthygation.batch.model.BatchModel.ExternalCallResponse;
import dev.agiro.healthygation.batch.model.BatchModel.SourceItem;
import java.util.concurrent.Future;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.batch.integration.async.AsyncItemProcessor;
import org.springframework.batch.integration.async.AsyncItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.amqp.dsl.Amqp;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;
import dev.agiro.healthygation.batch.worker.ExternalCallResponseBulkWriter;

/**
 * STEP 1 al micro A: reader, processor i writer viuen tots a A.
 * El processor fa un request-reply per RabbitMQ cap al micro B (que NO és Spring Batch)
 * i, gràcies a AsyncItemProcessor, els items d'un chunk viatgen a B en paral·lel.
 * AsyncItemWriter espera tots els Future i el writer fa un bulk upsert amb les respostes.
 *
 * NOTA: comprova els imports/paquets de Spring Batch contra la versió de Spring Boot 4.1.
 */
@Configuration
@IntegrationComponentScan(basePackageClasses = Step1AsyncConfig.class)
public class Step1AsyncConfig {

    private static final int CHUNK_SIZE = 50;
    private static final String TO_B_CHANNEL = "toB";

    /** Façana de request-reply cap a B. El timeout ha de cobrir la latència real de B. */
    @MessagingGateway(defaultRequestChannel = TO_B_CHANNEL, defaultReplyTimeout = "120000")
    public interface BGateway {
        ExternalCallResponse call(SourceItem item);
    }

    @Bean
    IntegrationFlow toBFlow(AmqpTemplate amqp) {
        return IntegrationFlow.from(TO_B_CHANNEL)
                .handle(Amqp.outboundGateway(amqp)
                        .exchangeName(ServiceBMessagingConfig.EXCHANGE)
                        .routingKey(ServiceBMessagingConfig.B_REQUESTS))
                .get();
    }

    /** Fils = nombre màxim de crides en vol cap a B. Ajusta'l a la capacitat de B. */
    @Bean
    TaskExecutor step1Executor() {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(16);
        ex.setMaxPoolSize(16);
        ex.setThreadNamePrefix("step1-b-");
        return ex;
    }

    @Bean
    AsyncItemProcessor<SourceItem, ExternalCallResponse> step1AsyncProcessor(BGateway gateway,
                                                                             TaskExecutor step1Executor) {
        AsyncItemProcessor<SourceItem, ExternalCallResponse> p = new AsyncItemProcessor<>();
        p.setDelegate(gateway::call);
        p.setTaskExecutor(step1Executor);
        return p;
    }

    @Bean
    AsyncItemWriter<ExternalCallResponse> step1AsyncWriter(ExternalCallResponseBulkWriter bulkWriter) {
        AsyncItemWriter<ExternalCallResponse> w = new AsyncItemWriter<>();
        w.setDelegate(bulkWriter);
        return w;
    }

    @Bean
    Step step1(JobRepository jobRepository,
               PlatformTransactionManager tx,
               ItemReader<SourceItem> step1Reader,
               AsyncItemProcessor<SourceItem, ExternalCallResponse> step1AsyncProcessor,
               AsyncItemWriter<ExternalCallResponse> step1AsyncWriter) {
        return new StepBuilder("step1-remote-call", jobRepository)
                .<SourceItem, Future<ExternalCallResponse>>chunk(CHUNK_SIZE, tx)
                .reader(step1Reader)
                .processor(step1AsyncProcessor)
                .writer(step1AsyncWriter)
                .build();
    }
}
