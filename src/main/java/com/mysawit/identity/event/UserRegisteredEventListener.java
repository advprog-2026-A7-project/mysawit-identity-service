package com.mysawit.identity.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class UserRegisteredEventListener {

    private static final Logger log = LoggerFactory.getLogger(UserRegisteredEventListener.class);
    private static final String EXCHANGE = "user.exchange";
    private static final String ROUTING_KEY = "user.registered";

    private final RabbitTemplate rabbitTemplate;

    public UserRegisteredEventListener(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUserRegisteredEvent(UserRegisteredEvent event) {
        try {
            rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY, event);
            log.info("Published user.registered event for user: {}", event.getUserId());
        } catch (Exception e) {
            log.error("Failed to publish wallet init event for user: {}", event.getUserId(), e);
        }
    }
}
