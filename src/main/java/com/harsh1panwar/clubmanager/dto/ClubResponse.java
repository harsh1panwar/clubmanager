package com.harsh1panwar.clubmanager.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/** Club as seen by clients — never exposes the owner's password hash or lazy JPA proxies. */
@Data
@Builder
public class ClubResponse {
    private Long id;
    private String name;
    private String description;
    private String ownerName;
    private LocalDateTime createdAt;
}
