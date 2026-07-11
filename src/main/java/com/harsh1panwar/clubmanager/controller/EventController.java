package com.harsh1panwar.clubmanager.controller;

import com.harsh1panwar.clubmanager.dto.EventRequest;
import com.harsh1panwar.clubmanager.dto.EventResponse;
import com.harsh1panwar.clubmanager.dto.RegistrationResponse;
import com.harsh1panwar.clubmanager.service.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @PostMapping
    public ResponseEntity<EventResponse> createEvent(
            @Valid @RequestBody EventRequest request,
            @AuthenticationPrincipal String email) {
        return ResponseEntity.ok(eventService.createEvent(request, email));
    }

    @GetMapping
    public ResponseEntity<List<EventResponse>> getAllEvents() {
        return ResponseEntity.ok(eventService.getAllEvents());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventResponse> getEventById(@PathVariable Long id) {
        return ResponseEntity.ok(eventService.getEventById(id));
    }

    @PostMapping("/{id}/register")
    public ResponseEntity<RegistrationResponse> registerForEvent(
            @PathVariable Long id,
            @AuthenticationPrincipal String email) {
        return ResponseEntity.ok(eventService.registerForEvent(id, email));
    }
}