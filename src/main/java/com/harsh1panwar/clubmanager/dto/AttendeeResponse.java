package com.harsh1panwar.clubmanager.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/** One row of an organizer's attendance sheet. */
@Data
@Builder
public class AttendeeResponse {
    private Long registrationId;
    private String name;
    private String email;
    private LocalDateTime registeredAt;
    private boolean attended;
    private LocalDateTime attendedAt;
}
