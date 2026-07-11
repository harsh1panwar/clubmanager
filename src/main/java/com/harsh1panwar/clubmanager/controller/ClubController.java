package com.harsh1panwar.clubmanager.controller;

import com.harsh1panwar.clubmanager.entity.Club;
import com.harsh1panwar.clubmanager.entity.User;
import com.harsh1panwar.clubmanager.repository.ClubRepository;
import com.harsh1panwar.clubmanager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clubs")
@RequiredArgsConstructor
public class ClubController {

    private final ClubRepository clubRepository;
    private final UserRepository userRepository;

    @PostMapping
    public ResponseEntity<Club> createClub(
            @RequestBody Club club,
            @AuthenticationPrincipal String email) {

        User owner = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        club.setOwner(owner);
        Club saved = clubRepository.save(club);
        return ResponseEntity.ok(saved);
    }

    @GetMapping
    public ResponseEntity<List<Club>> getAllClubs() {
        return ResponseEntity.ok(clubRepository.findAll());
    }
}