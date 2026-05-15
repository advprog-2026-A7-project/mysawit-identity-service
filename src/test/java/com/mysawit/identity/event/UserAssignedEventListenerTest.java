package com.mysawit.identity.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.Instant;

import static org.mockito.Mockito.*;

class UserAssignedEventListenerTest {

    private RabbitTemplate rabbitTemplate;
    private UserAssignedEventListener listener;

    @BeforeEach
    void setUp() {
        rabbitTemplate = mock(RabbitTemplate.class);
        listener = new UserAssignedEventListener(rabbitTemplate);
    }

    @Test
    void handleUserAssignedEventPublishesToRabbit() {
        UserAssignedEvent event = new UserAssignedEvent(
                "buruh-1", "mandor-1", "Pak Mandor",
                UserAssignedEvent.AssignmentAction.ASSIGNED, Instant.now());

        listener.handleUserAssignedEvent(event);

        verify(rabbitTemplate).convertAndSend("user.exchange", "user.assigned", event);
    }

    @Test
    void handleUserAssignedEventCatchesException() {
        UserAssignedEvent event = new UserAssignedEvent(
                "buruh-1", "mandor-1", "Pak Mandor",
                UserAssignedEvent.AssignmentAction.ASSIGNED, Instant.now());
        doThrow(new RuntimeException("connection failed"))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

        listener.handleUserAssignedEvent(event);

        verify(rabbitTemplate).convertAndSend("user.exchange", "user.assigned", event);
    }
}
