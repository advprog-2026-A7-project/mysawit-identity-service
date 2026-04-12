package com.mysawit.identity.config;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;

import static org.junit.jupiter.api.Assertions.*;

class RabbitMQConfigTest {

    private final RabbitMQConfig config = new RabbitMQConfig();

    @Test
    void userExchangeCreated() {
        TopicExchange exchange = config.userExchange();
        assertEquals("user.exchange", exchange.getName());
    }

    @Test
    void userRegisteredQueueCreated() {
        Queue queue = config.userRegisteredQueue();
        assertEquals("user.registered.queue", queue.getName());
        assertTrue(queue.isDurable());
    }

    @Test
    void userRegisteredBindingCreated() {
        Queue queue = config.userRegisteredQueue();
        TopicExchange exchange = config.userExchange();
        Binding binding = config.userRegisteredBinding(queue, exchange);

        assertEquals("user.registered.queue", binding.getDestination());
        assertEquals("user.exchange", binding.getExchange());
        assertEquals("user.registered", binding.getRoutingKey());
    }

    @Test
    void jsonMessageConverterCreated() {
        MessageConverter converter = config.jsonMessageConverter();
        assertInstanceOf(Jackson2JsonMessageConverter.class, converter);
    }
}
