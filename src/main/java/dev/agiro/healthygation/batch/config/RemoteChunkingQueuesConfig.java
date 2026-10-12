package dev.agiro.healthygation.batch.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cues del remote chunking del STEP 2 (el step 1 ja no en fa servir: va per request-reply cap a B).
 * Requests: cua compartida (competing consumers entre tots els pods). Replies: una cua per pod.
 */
@Configuration
public class RemoteChunkingQueuesConfig {

    public static final String EXCHANGE = "remote-chunking.exchange";

    public static final String STEP2_REQUESTS = "step2.requests";
    /** Prefix de la cua de respostes de cada pod manager (step2.replies.<instanceId>). */
    public static final String STEP2_REPLIES_PREFIX = "step2.replies.";

    /** Header que porta la routing key de resposta del manager que ha enviat el chunk. */
    public static final String REPLY_KEY_HEADER = "replyKey";

    @Bean
    DirectExchange remoteChunkingExchange() {
        return new DirectExchange(EXCHANGE);
    }

    @Bean Queue step2Requests() { return new Queue(STEP2_REQUESTS, true); }

    /**
     * Cua de respostes pròpia d'aquest pod: exclusiva, no durable i auto-delete. Cada pod que
     * actua de manager només rep les respostes dels chunks que ell mateix ha enviat.
     */
    @Bean
    Queue step2InstanceReplies(@Value("${healthygation.batch.instance-id}") String instanceId) {
        return new Queue(STEP2_REPLIES_PREFIX + instanceId, false, true, true);
    }

    @Bean Binding step2RequestsBinding() {
        return BindingBuilder.bind(step2Requests()).to(remoteChunkingExchange()).with(STEP2_REQUESTS);
    }

    @Bean Binding step2InstanceRepliesBinding(Queue step2InstanceReplies) {
        return BindingBuilder.bind(step2InstanceReplies).to(remoteChunkingExchange())
                .with(step2InstanceReplies.getName());
    }
}
