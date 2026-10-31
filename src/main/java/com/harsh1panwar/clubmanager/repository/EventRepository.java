package com.harsh1panwar.clubmanager.repository;

import com.harsh1panwar.clubmanager.entity.Event;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {

    /** All events with club + organizer fetched in a single JOIN query (fixes the N+1 problem). */
    @EntityGraph(attributePaths = {"club", "organizer"})
    List<Event> findAllByOrderByEventDateAsc();

    @EntityGraph(attributePaths = {"club", "organizer"})
    List<Event> findByOrganizerIdOrderByEventDateAsc(Long organizerId);

    @EntityGraph(attributePaths = {"club", "organizer"})
    Optional<Event> findWithDetailsById(Long id);

    List<Event> findByClubId(Long clubId);
}
