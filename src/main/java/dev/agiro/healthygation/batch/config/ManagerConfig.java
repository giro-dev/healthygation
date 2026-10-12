package dev.agiro.healthygation.batch.config;

import dev.agiro.healthygation.batch.model.BatchModel.CommandOutcome;
import dev.agiro.healthygation.batch.model.BatchModel.ExternalCallResultRow;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.batch.integration.chunk.RemoteChunkingManagerStepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.amqp.dsl.Amqp;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Rol manager. Ja no depèn d'un perfil: tots els pods tenen aquests beans i fa de manager
 * el pod que llança el job (via JobLauncher). Un pod només hauria d'executar un job de
 * remote chunking alhora, perquè comparteix el canal de respostes step2In.
 *
 * Micro A. El step 1 és a Step1AsyncConfig (async + request-reply cap a B).
 * Aquí queda el step 2 amb remote chunking (workers Spring Batch) i el job que encadena tots dos.
 *
 * NOTA: els builders de Spring Batch canvien entre 5.x i 6.x; ajusta imports si cal.
 */
@Configuration
public class ManagerConfig {

    private static final int CHUNK_SIZE = 50;

    // ---------- STEP 2 (remote chunking) ----------

    @Bean DirectChannel step2Out() { return new DirectChannel(); }
    @Bean QueueChannel step2In()   { return new QueueChannel(); }

    @Bean
    IntegrationFlow step2OutboundFlow(AmqpTemplate amqp, Queue step2InstanceReplies) {
        return IntegrationFlow.from(step2Out())
                // els workers responen a la cua d'aquest pod, via la routing key d'aquest header
                .enrichHeaders(h -> h.header(RemoteChunkingQueuesConfig.REPLY_KEY_HEADER,
                        step2InstanceReplies.getName()))
                .handle(Amqp.outboundAdapter(amqp)
                        .exchangeName(RemoteChunkingQueuesConfig.EXCHANGE)
                        .routingKey(RemoteChunkingQueuesConfig.STEP2_REQUESTS))
                .get();
    }

    @Bean
    IntegrationFlow step2InboundFlow(ConnectionFactory cf, Queue step2InstanceReplies) {
        return IntegrationFlow.from(Amqp.inboundAdapter(cf, step2InstanceReplies.getName()))
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
