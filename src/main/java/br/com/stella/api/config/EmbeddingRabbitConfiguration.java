package br.com.stella.api.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "stella.messaging.enabled", havingValue = "true")
public class EmbeddingRabbitConfiguration {

    @Bean
    Declarables embeddingMessagingTopology(EmbeddingMessagingProperties properties) {
        DirectExchange exchange = new DirectExchange(properties.exchange(), true, false);
        DirectExchange deadLetterExchange = new DirectExchange(properties.deadLetterExchange(), true, false);
        Queue queue = QueueBuilder.durable(properties.queue())
                .deadLetterExchange(properties.deadLetterExchange())
                .deadLetterRoutingKey(properties.deadLetterRoutingKey())
                .build();
        Queue deadLetterQueue = QueueBuilder.durable(properties.deadLetterQueue()).build();
        Binding binding = BindingBuilder.bind(queue).to(exchange).with(properties.routingKey());
        Binding deadLetterBinding = BindingBuilder.bind(deadLetterQueue)
                .to(deadLetterExchange)
                .with(properties.deadLetterRoutingKey());
        return new Declarables(exchange, deadLetterExchange, queue, deadLetterQueue, binding, deadLetterBinding);
    }
}
