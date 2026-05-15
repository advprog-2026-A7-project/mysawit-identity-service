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
public class UserAssignedEvent {

    private String userId;
    private String mandorId;
    private String mandorName;
    private AssignmentAction action;
    private Instant occurredAt;

    public enum AssignmentAction {
        ASSIGNED,
        UNASSIGNED,
        REASSIGNED
    }
}
