package dev.agiro.healthygation.batch.config;

import dev.agiro.healthygation.batch.model.BatchModel.CommandOutcome;
import dev.agiro.healthygation.batch.model.BatchModel.ExternalCallResultRow;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.batch.integration.chunk.RemoteChunkingManagerStepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.integration.amqp.dsl.Amqp;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Micro A (perfil "manager"). El step 1 és a Step1AsyncConfig (async + request-reply cap a B).
 * Aquí queda el step 2 amb remote chunking (workers Spring Batch) i el job que encadena tots dos.
 *
 * NOTA: els builders de Spring Batch canvien entre 5.x i 6.x; ajusta imports si cal.
 */
@Configuration
@Profile("manager")
public class ManagerConfig {

    private static final int CHUNK_SIZE = 50;

    // ---------- STEP 2 (remote chunking) ----------

    @Bean DirectChannel step2Out() { return new DirectChannel(); }
    @Bean QueueChannel step2In()   { return new QueueChannel(); }

    @Bean
    IntegrationFlow step2OutboundFlow(AmqpTemplate amqp) {
        return IntegrationFlow.from(step2Out())
                .handle(Amqp.outboundAdapter(amqp)
                        .exchangeName(RemoteChunkingQueuesConfig.EXCHANGE)
                        .routingKey(RemoteChunkingQueuesConfig.STEP2_REQUESTS))
                .get();
    }

    @Bean
    IntegrationFlow step2InboundFlow(ConnectionFactory cf) {
        return IntegrationFlow.from(Amqp.inboundAdapter(cf, RemoteChunkingQueuesConfig.STEP2_REPLIES))
                .channel(step2In())
                .get();
    }

    @Bean
    Step step2(JobRepository jobRepository,
               PlatformTransactionManager tx,
               ItemReader<ExternalCallResultRow> step2Reader) {
        return new RemoteChunkingManagerStepBuilder<ExternalCallResultRow, CommandOutcome>("step2-orchestration", jobRepository)
                .chunk(CHUNK_SIZE)
                .reader(step2Reader)
                .transactionManager(tx)
                .outputChannel(step2Out())
                .inputChannel(step2In())
                .build();
    }

    // ---------- JOB ----------

    @Bean
    Job remoteChunkingJob(JobRepository jobRepository, Step step1, Step step2) {
        return new JobBuilder("remote-chunking-job", jobRepository)
                .start(step1)
                .next(step2)
                .build();
    }
}
