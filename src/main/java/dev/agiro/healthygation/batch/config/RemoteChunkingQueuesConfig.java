package dev.agiro.healthygation.batch.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cues del remote chunking del STEP 2 (el step 1 ja no en fa servir: va per request-reply cap a B).
 */
@Configuration
public class RemoteChunkingQueuesConfig {

    public static final String EXCHANGE = "remote-chunking.exchange";

    public static final String STEP2_REQUESTS = "step2.requests";
    public static final String STEP2_REPLIES = "step2.replies";

    @Bean
    DirectExchange remoteChunkingExchange() {
        return new DirectExchange(EXCHANGE);
    }

    @Bean Queue step2Requests() { return new Queue(STEP2_REQUESTS, true); }
    @Bean Queue step2Replies()  { return new Queue(STEP2_REPLIES, true); }

    @Bean Binding step2RequestsBinding() {
        return BindingBuilder.bind(step2Requests()).to(remoteChunkingExchange()).with(STEP2_REQUESTS);
    }

    @Bean Binding step2RepliesBinding() {
        return BindingBuilder.bind(step2Replies()).to(remoteChunkingExchange()).with(STEP2_REPLIES);
    }
}
