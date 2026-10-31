package com.harsh1panwar.clubmanager.service;

import com.harsh1panwar.clubmanager.dto.ClubRequest;
import com.harsh1panwar.clubmanager.dto.ClubResponse;
import com.harsh1panwar.clubmanager.entity.Club;
import com.harsh1panwar.clubmanager.entity.User;
import com.harsh1panwar.clubmanager.exception.ApiException;
import com.harsh1panwar.clubmanager.repository.ClubRepository;
import com.harsh1panwar.clubmanager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubService {

    private final ClubRepository clubRepository;
    private final UserRepository userRepository;

    @Transactional
    public ClubResponse createClub(ClubRequest request, String ownerEmail) {
        User owner = userRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> ApiException.notFound("User not found"));

        Club club = Club.builder()
                .name(request.getName().trim())
                .description(request.getDescription())
                .owner(owner)
                .build();

        return toResponse(clubRepository.save(club));
    }

    public List<ClubResponse> getAllClubs() {
        return clubRepository.findAllByOrderByNameAsc().stream().map(this::toResponse).toList();
    }

    public List<ClubResponse> getMyClubs(String ownerEmail) {
        User owner = userRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> ApiException.notFound("User not found"));
        return clubRepository.findByOwnerIdOrderByNameAsc(owner.getId()).stream().map(this::toResponse).toList();
    }

    private ClubResponse toResponse(Club club) {
        return ClubResponse.builder()
                .id(club.getId())
                .name(club.getName())
                .description(club.getDescription())
                .ownerName(club.getOwner().getName())
                .createdAt(club.getCreatedAt())
                .build();
    }
}
