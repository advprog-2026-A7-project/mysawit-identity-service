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
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE_NAME,
                    RabbitMQConfig.USER_ASSIGNED_ROUTING_KEY,
                    event
            );
            log.info("Published user.assigned event userId={} mandorId={} action={}",
                    event.getUserId(), event.getMandorId(), event.getAction());
        } catch (Exception e) {
            log.error("Failed to publish user.assigned event for userId={}",
                    event.getUserId(), e);
        }
    }
}
