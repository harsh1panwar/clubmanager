package com.harsh1panwar.clubmanager.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CheckInRequest {

    @NotBlank(message = "QR token is required")
    private String token;
}
