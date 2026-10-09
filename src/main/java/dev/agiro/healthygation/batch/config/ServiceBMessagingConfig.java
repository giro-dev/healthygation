package dev.agiro.healthygation.batch.config;

import java.util.List;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Infraestructura RabbitMQ compartida entre A i B per al request-reply del step 1.
 * No cal cua de respostes: l'outbound gateway fa servir direct reply-to.
 *
 * El micro B ha de tenir aquesta mateixa configuració i les mateixes classes del model
 * (idealment en un mòdul compartit). Si prefereixes JSON, canvia el MessageConverter.
 */
@Configuration
public class ServiceBMessagingConfig {

    public static final String EXCHANGE = "service-b.exchange";
    public static final String B_REQUESTS = "b.requests";

    @Bean
    DirectExchange serviceBExchange() {
        return new DirectExchange(EXCHANGE);
    }

    @Bean
    Queue bRequests() {
        return new Queue(B_REQUESTS, true);
    }

    @Bean
    Binding bRequestsBinding() {
        return BindingBuilder.bind(bRequests()).to(serviceBExchange()).with(B_REQUESTS);
    }

    /** Serialització Java amb llista de classes permeses (els records són Serializable). */
    @Bean
    MessageConverter batchMessageConverter() {
        SimpleMessageConverter converter = new SimpleMessageConverter();
        converter.setAllowedListPatterns(List.of(
                "dev.agiro.healthygation.batch.model.*", "java.lang.*", "java.util.*"));
        return converter;
    }
}
