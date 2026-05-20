package com.mysawit.identity.event;

import com.mysawit.identity.config.RabbitMQConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class UserDeletedEventListener {

    private static final Logger log = LoggerFactory.getLogger(UserDeletedEventListener.class);

    private final RabbitTemplate rabbitTemplate;

    public UserDeletedEventListener(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUserDeletedEvent(UserDeletedEvent event) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE_NAME,
                    RabbitMQConfig.USER_DELETED_ROUTING_KEY,
                    event
            );
            log.info("Published user.deleted event userId={} role={} previousMandorId={}",
                    event.getUserId(), event.getRole(), event.getPreviousMandorId());
        } catch (Exception e) {
            log.error("Failed to publish user.deleted event for userId={}",
                    event.getUserId(), e);
        }
    }
}
