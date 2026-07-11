package com.harsh1panwar.clubmanager.repository;

import com.harsh1panwar.clubmanager.entity.Registration;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface RegistrationRepository extends JpaRepository<Registration, Long> {
    boolean existsByUserIdAndEventId(Long userId, Long eventId);
    Optional<Registration> findByUserIdAndEventId(Long userId, Long eventId);
    List<Registration> findByEventId(Long eventId);
    int countByEventId(Long eventId);
}