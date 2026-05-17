package com.mysawit.identity.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.Instant;

import static org.mockito.Mockito.*;

class UserDeletedEventListenerTest {

    private RabbitTemplate rabbitTemplate;
    private UserDeletedEventListener listener;

    @BeforeEach
    void setUp() {
        rabbitTemplate = mock(RabbitTemplate.class);
        listener = new UserDeletedEventListener(rabbitTemplate);
    }

    @Test
    void handleUserDeletedEventPublishesToRabbit() {
        UserDeletedEvent event = new UserDeletedEvent(
                "user-1", "BURUH", "mandor-1", Instant.now());

        listener.handleUserDeletedEvent(event);

        verify(rabbitTemplate).convertAndSend("user.exchange", "user.deleted", event);
    }

    @Test
    void handleUserDeletedEventPublishesWithoutPreviousMandor() {
        UserDeletedEvent event = new UserDeletedEvent(
                "user-1", "SUPIR", null, Instant.now());

        listener.handleUserDeletedEvent(event);

        verify(rabbitTemplate).convertAndSend("user.exchange", "user.deleted", event);
    }

    @Test
    void handleUserDeletedEventCatchesException() {
        UserDeletedEvent event = new UserDeletedEvent(
                "user-1", "BURUH", "mandor-1", Instant.now());
        doThrow(new RuntimeException("connection failed"))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

        listener.handleUserDeletedEvent(event);

        verify(rabbitTemplate).convertAndSend("user.exchange", "user.deleted", event);
    }
}
