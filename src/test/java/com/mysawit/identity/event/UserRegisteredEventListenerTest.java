package com.mysawit.identity.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.mockito.Mockito.*;

class UserRegisteredEventListenerTest {

    private RabbitTemplate rabbitTemplate;
    private UserRegisteredEventListener listener;

    @BeforeEach
    void setUp() {
        rabbitTemplate = mock(RabbitTemplate.class);
        listener = new UserRegisteredEventListener(rabbitTemplate);
    }

    @Test
    void handleUserRegisteredEventPublishesToRabbit() {
        UserRegisteredEvent event = new UserRegisteredEvent("user-1", "user@mail.com", "BURUH");

        listener.handleUserRegisteredEvent(event);

        verify(rabbitTemplate).convertAndSend("user.exchange", "user.registered", event);
    }

    @Test
    void handleUserRegisteredEventCatchesException() {
        UserRegisteredEvent event = new UserRegisteredEvent("user-1", "user@mail.com", "BURUH");
        doThrow(new RuntimeException("connection failed"))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

        listener.handleUserRegisteredEvent(event);

        verify(rabbitTemplate).convertAndSend("user.exchange", "user.registered", event);
    }
}
