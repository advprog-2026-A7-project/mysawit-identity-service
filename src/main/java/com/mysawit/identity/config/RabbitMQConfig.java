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

    public static final String USER_ASSIGNMENT_ASSIGNED_QUEUE = "user.assignment.assigned.queue";
    public static final String USER_ASSIGNMENT_ASSIGNED_ROUTING_KEY = "user.assignment.assigned";

    public static final String USER_ASSIGNMENT_REASSIGNED_QUEUE = "user.assignment.reassigned.queue";
    public static final String USER_ASSIGNMENT_REASSIGNED_ROUTING_KEY = "user.assignment.reassigned";

    public static final String USER_ASSIGNMENT_UNASSIGNED_QUEUE = "user.assignment.unassigned.queue";
    public static final String USER_ASSIGNMENT_UNASSIGNED_ROUTING_KEY = "user.assignment.unassigned";

    public static final String USER_DELETED_QUEUE = "user.deleted.queue";
    public static final String USER_DELETED_ROUTING_KEY = "user.deleted";

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
    public Queue userAssignmentAssignedQueue() {
        return new Queue(USER_ASSIGNMENT_ASSIGNED_QUEUE, true);
    }

    @Bean
    public Binding userAssignmentAssignedBinding(Queue userAssignmentAssignedQueue, TopicExchange userExchange) {
        return BindingBuilder.bind(userAssignmentAssignedQueue).to(userExchange).with(USER_ASSIGNMENT_ASSIGNED_ROUTING_KEY);
    }

    @Bean
    public Queue userAssignmentReassignedQueue() {
        return new Queue(USER_ASSIGNMENT_REASSIGNED_QUEUE, true);
    }

    @Bean
    public Binding userAssignmentReassignedBinding(Queue userAssignmentReassignedQueue, TopicExchange userExchange) {
        return BindingBuilder.bind(userAssignmentReassignedQueue).to(userExchange).with(USER_ASSIGNMENT_REASSIGNED_ROUTING_KEY);
    }

    @Bean
    public Queue userAssignmentUnassignedQueue() {
        return new Queue(USER_ASSIGNMENT_UNASSIGNED_QUEUE, true);
    }

    @Bean
    public Binding userAssignmentUnassignedBinding(Queue userAssignmentUnassignedQueue, TopicExchange userExchange) {
        return BindingBuilder.bind(userAssignmentUnassignedQueue).to(userExchange).with(USER_ASSIGNMENT_UNASSIGNED_ROUTING_KEY);
    }

    @Bean
    public Queue userDeletedQueue() {
        return new Queue(USER_DELETED_QUEUE, true);
    }

    @Bean
    public Binding userDeletedBinding(Queue userDeletedQueue, TopicExchange userExchange) {
        return BindingBuilder.bind(userDeletedQueue).to(userExchange).with(USER_DELETED_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
