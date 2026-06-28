package br.com.stella.api.config;

import com.rabbitmq.client.ConnectionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.testcontainers.rabbitmq.RabbitMQContainer;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class EmbeddingRabbitConfigurationIntegrationTest {

    @Test
    void shouldRouteRejectedMessageToDeadLetterQueueOnRealBroker() throws Exception {
        try (RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:4.1-alpine")) {
            rabbit.start();
            EmbeddingMessagingProperties properties = properties();
            Declarables topology = new EmbeddingRabbitConfiguration().embeddingMessagingTopology(properties);
            ConnectionFactory connectionFactory = new ConnectionFactory();
            connectionFactory.setUri(rabbit.getAmqpUrl());

            try (var connection = connectionFactory.newConnection(); var channel = connection.createChannel()) {
                for (DirectExchange exchange : topology.getDeclarablesByType(DirectExchange.class)) {
                    channel.exchangeDeclare(exchange.getName(), exchange.getType(), exchange.isDurable(),
                            exchange.isAutoDelete(), exchange.getArguments());
                }
                for (Queue queue : topology.getDeclarablesByType(Queue.class)) {
                    channel.queueDeclare(queue.getName(), queue.isDurable(), queue.isExclusive(),
                            queue.isAutoDelete(), queue.getArguments());
                }
                for (Binding binding : topology.getDeclarablesByType(Binding.class)) {
                    channel.queueBind(binding.getDestination(), binding.getExchange(),
                            binding.getRoutingKey(), binding.getArguments());
                }

                channel.basicPublish(properties.exchange(), properties.routingKey(), null,
                        "event".getBytes(StandardCharsets.UTF_8));
                var delivery = channel.basicGet(properties.queue(), false);
                assertThat(delivery).isNotNull();
                channel.basicReject(delivery.getEnvelope().getDeliveryTag(), false);

                var deadline = java.time.Instant.now().plus(Duration.ofSeconds(10));
                com.rabbitmq.client.GetResponse deadLetter = null;
                while (deadLetter == null && java.time.Instant.now().isBefore(deadline)) {
                    deadLetter = channel.basicGet(properties.deadLetterQueue(), true);
                    if (deadLetter == null) {
                        Thread.sleep(100);
                    }
                }
                assertThat(deadLetter).isNotNull();
                assertThat(new String(deadLetter.getBody(), StandardCharsets.UTF_8)).isEqualTo("event");
            }
        }
    }

    private EmbeddingMessagingProperties properties() {
        return new EmbeddingMessagingProperties(true, null, null, null, null, null, null, 0, 0, 0);
    }
}
