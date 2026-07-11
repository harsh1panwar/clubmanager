package com.harsh1panwar.clubmanager.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class EventResponse {
    private Long id;
    private String title;
    private String description;
    private String location;
    private LocalDateTime eventDate;
    private Integer capacity;
    private Integer registeredCount;
    private String clubName;
    private String organizerName;
    private LocalDateTime createdAt;
}