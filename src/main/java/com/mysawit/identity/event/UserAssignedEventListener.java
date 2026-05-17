package com.mysawit.identity.event;

import com.mysawit.identity.config.RabbitMQConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class UserAssignedEventListener {

    private static final Logger log = LoggerFactory.getLogger(UserAssignedEventListener.class);

    private final RabbitTemplate rabbitTemplate;

    public UserAssignedEventListener(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUserAssignedEvent(UserAssignedEvent event) {
        String routingKey = routingKeyFor(event.getAction());
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE_NAME,
                    routingKey,
                    event
            );
            log.info("Published {} event userId={} mandorId={} action={}",
                    routingKey, event.getUserId(), event.getMandorId(), event.getAction());
        } catch (Exception e) {
            log.error("Failed to publish {} event for userId={}",
                    routingKey, event.getUserId(), e);
        }
    }

    private String routingKeyFor(UserAssignedEvent.AssignmentAction action) {
        return switch (action) {
            case ASSIGNED -> RabbitMQConfig.USER_ASSIGNMENT_ASSIGNED_ROUTING_KEY;
            case REASSIGNED -> RabbitMQConfig.USER_ASSIGNMENT_REASSIGNED_ROUTING_KEY;
            case UNASSIGNED -> RabbitMQConfig.USER_ASSIGNMENT_UNASSIGNED_ROUTING_KEY;
        };
    }
}
