package com.harsh1panwar.clubmanager.repository;

import com.harsh1panwar.clubmanager.entity.Registration;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RegistrationRepository extends JpaRepository<Registration, Long> {

    boolean existsByUserIdAndEventId(Long userId, Long eventId);

    Optional<Registration> findByUserIdAndEventId(Long userId, Long eventId);

    /** Same lookup, but loads user + event + club in one query (used for certificates). */
    @EntityGraph(attributePaths = {"user", "event", "event.club"})
    Optional<Registration> findWithDetailsByUserIdAndEventId(Long userId, Long eventId);

    int countByEventId(Long eventId);

    /** Attendance sheet for an event, with the user joined in (avoids N+1). */
    @EntityGraph(attributePaths = {"user"})
    List<Registration> findByEventIdOrderByRegisteredAtAsc(Long eventId);

    /** An attendee's registrations, with event + club joined in. */
    @EntityGraph(attributePaths = {"event", "event.club"})
    List<Registration> findByUserIdOrderByRegisteredAtDesc(Long userId);

    /** Registration counts for many events in ONE grouped query instead of one COUNT per event. */
    @Query("select r.event.id, count(r) from Registration r where r.event.id in :eventIds group by r.event.id")
    List<Object[]> countGroupedByEventIds(@Param("eventIds") Collection<Long> eventIds);
}
