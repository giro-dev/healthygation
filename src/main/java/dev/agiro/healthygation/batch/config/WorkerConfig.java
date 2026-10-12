package dev.agiro.healthygation.batch.config;

import dev.agiro.healthygation.batch.model.BatchModel.CommandOutcome;
import dev.agiro.healthygation.batch.model.BatchModel.ExternalCallResultRow;
import dev.agiro.healthygation.batch.worker.CommandOutcomeWriter;
import dev.agiro.healthygation.batch.worker.CommandProcessor;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.batch.integration.chunk.RemoteChunkingWorkerBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.amqp.dsl.Amqp;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.dsl.IntegrationFlow;

/**
 * Workers del STEP 2 (tots els pods): escolten step2.requests, executen processor + writer
 * del chunk i responen a la cua del manager indicada al header replyKey. Pots arrencar tantes instàncies com vulguis.
 * (El step 1 ja no passa per aquí: va per request-reply cap a B.)
 */
@Configuration
public class WorkerConfig {

    @Bean DirectChannel step2WorkerIn()  { return new DirectChannel(); }
    @Bean DirectChannel step2WorkerOut() { return new DirectChannel(); }

    @Bean
    IntegrationFlow step2WorkerInbound(ConnectionFactory cf,
                                       @Value("${healthygation.batch.worker.prefetch:2}") int prefetch) {
        // Cada missatge és un chunk sencer: amb prefetch baix un worker només acumula
        // uns pocs chunks sense confirmar i la resta es reparteixen entre els altres pods.
        return IntegrationFlow.from(Amqp.inboundAdapter(cf, RemoteChunkingQueuesConfig.STEP2_REQUESTS)
                        .configureContainer(c -> c.prefetchCount(prefetch)))
                .channel(step2WorkerIn())
                .get();
    }

    @Bean
    IntegrationFlow step2WorkerOutbound(AmqpTemplate amqp) {
        return IntegrationFlow.from(step2WorkerOut())
                .handle(Amqp.outboundAdapter(amqp)
                        .exchangeName(RemoteChunkingQueuesConfig.EXCHANGE)
                        // la resposta torna al pod manager que ha enviat el chunk
                        .routingKeyExpression("headers['" + RemoteChunkingQueuesConfig.REPLY_KEY_HEADER + "']"))
                .get();
    }

    @Bean
    IntegrationFlow step2WorkerHandler(CommandProcessor processor,
                                       CommandOutcomeWriter writer) {
        return new RemoteChunkingWorkerBuilder<ExternalCallResultRow, CommandOutcome>()
                .itemProcessor(processor)
                .itemWriter(writer)
                .inputChannel(step2WorkerIn())
                .outputChannel(step2WorkerOut())
                .build();
    }
}
