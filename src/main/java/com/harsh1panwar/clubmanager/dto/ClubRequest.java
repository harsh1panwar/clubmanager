package com.harsh1panwar.clubmanager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ClubRequest {

    @NotBlank(message = "Club name is required")
    @Size(max = 255, message = "Club name is too long")
    private String name;

    @Size(max = 500, message = "Description must be under 500 characters")
    private String description;
}
