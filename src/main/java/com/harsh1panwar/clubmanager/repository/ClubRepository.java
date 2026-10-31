package com.harsh1panwar.clubmanager.repository;

import com.harsh1panwar.clubmanager.entity.Club;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClubRepository extends JpaRepository<Club, Long> {

    @EntityGraph(attributePaths = {"owner"})
    List<Club> findAllByOrderByNameAsc();

    @EntityGraph(attributePaths = {"owner"})
    List<Club> findByOwnerIdOrderByNameAsc(Long ownerId);
}
