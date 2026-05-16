package com.mysawit.identity.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserDeletedEvent {

    private String userId;
    private String role;
    private String previousMandorId;
    private Instant occurredAt;
}
