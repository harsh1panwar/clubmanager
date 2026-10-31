package com.harsh1panwar.clubmanager.controller;

import com.harsh1panwar.clubmanager.dto.ClubRequest;
import com.harsh1panwar.clubmanager.dto.ClubResponse;
import com.harsh1panwar.clubmanager.service.ClubService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clubs")
@RequiredArgsConstructor
public class ClubController {

    private final ClubService clubService;

    @PostMapping
    public ResponseEntity<ClubResponse> createClub(@Valid @RequestBody ClubRequest request,
                                                   @AuthenticationPrincipal String email) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clubService.createClub(request, email));
    }

    @GetMapping
    public ResponseEntity<List<ClubResponse>> getAllClubs() {
        return ResponseEntity.ok(clubService.getAllClubs());
    }

    @GetMapping("/mine")
    public ResponseEntity<List<ClubResponse>> getMyClubs(@AuthenticationPrincipal String email) {
        return ResponseEntity.ok(clubService.getMyClubs(email));
    }
}
