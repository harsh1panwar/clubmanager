package com.harsh1panwar.clubmanager.repository;

import com.harsh1panwar.clubmanager.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {
    List<Event> findByClubId(Long clubId);
    List<Event> findByOrganizerId(Long organizerId);
}
