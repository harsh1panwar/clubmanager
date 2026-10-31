package com.harsh1panwar.clubmanager.controller;

import com.harsh1panwar.clubmanager.dto.RegistrationResponse;
import com.harsh1panwar.clubmanager.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/registrations")
@RequiredArgsConstructor
public class RegistrationController {

    private final EventService eventService;

    /** The logged-in attendee's registrations, newest first, with attendance status. */
    @GetMapping("/me")
    public ResponseEntity<List<RegistrationResponse>> getMyRegistrations(@AuthenticationPrincipal String email) {
        return ResponseEntity.ok(eventService.getMyRegistrations(email));
    }
}
