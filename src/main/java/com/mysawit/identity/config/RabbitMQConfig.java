package com.mysawit.identity.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "user.exchange";
    public static final String QUEUE_NAME = "user.registered.queue";
    public static final String ROUTING_KEY = "user.registered";

    public static final String USER_ASSIGNED_QUEUE = "user.assigned.queue";
    public static final String USER_ASSIGNED_ROUTING_KEY = "user.assigned";

    @Bean
    public TopicExchange userExchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public Queue userRegisteredQueue() {
        return new Queue(QUEUE_NAME, true);
    }

    @Bean
    public Binding userRegisteredBinding(Queue userRegisteredQueue, TopicExchange userExchange) {
        return BindingBuilder.bind(userRegisteredQueue).to(userExchange).with(ROUTING_KEY);
    }

    @Bean
    public Queue userAssignedQueue() {
        return new Queue(USER_ASSIGNED_QUEUE, true);
    }

    @Bean
    public Binding userAssignedBinding(Queue userAssignedQueue, TopicExchange userExchange) {
        return BindingBuilder.bind(userAssignedQueue).to(userExchange).with(USER_ASSIGNED_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
