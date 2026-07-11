package com.harsh1panwar.clubmanager.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class RegistrationResponse {
    private Long id;
    private String eventTitle;
    private String eventLocation;
    private LocalDateTime eventDate;
    private LocalDateTime registeredAt;
    private boolean attended;
}