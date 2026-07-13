package com.harsh1panwar.clubmanager.service;

import com.harsh1panwar.clubmanager.dto.EventRequest;
import com.harsh1panwar.clubmanager.dto.EventResponse;
import com.harsh1panwar.clubmanager.dto.RegistrationResponse;
import com.harsh1panwar.clubmanager.entity.*;
import com.harsh1panwar.clubmanager.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class EventService {

    private final EventRepository eventRepository;
    private final ClubRepository clubRepository;
    private final UserRepository userRepository;
    private final RegistrationRepository registrationRepository;

    // Organizer event create kare
    public EventResponse createEvent(EventRequest request, String organizerEmail) {

        User organizer = userRepository.findByEmail(organizerEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Club club = clubRepository.findById(request.getClubId())
                .orElseThrow(() -> new RuntimeException("Club not found"));

        // Sirf club ka owner event create kar sakta hai
        if (!club.getOwner().getId().equals(organizer.getId())) {
            throw new RuntimeException("Only club owner can create events");
        }

        Event event = Event.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .location(request.getLocation())
                .eventDate(request.getEventDate())
                .capacity(request.getCapacity())
                .club(club)
                .organizer(organizer)
                .build();

        Event saved = eventRepository.save(event);
        return mapToResponse(saved);
    }

    // Sabhi events dekhna
    public List<EventResponse> getAllEvents() {
        return eventRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // Ek event ki details
    public EventResponse getEventById(Long id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found"));
        return mapToResponse(event);
    }

    // Attendee event mein register kare
    public RegistrationResponse registerForEvent(Long eventId, String userEmail) {

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        // Already registered?
        if (registrationRepository.existsByUserIdAndEventId(user.getId(), eventId)) {
            throw new RuntimeException("Already registered for this event");
        }

        // Capacity check
        int currentCount = registrationRepository.countByEventId(eventId);
        if (currentCount >= event.getCapacity()) {
            throw new RuntimeException("Event is full");
        }

        Registration registration = Registration.builder()
                .user(user)
                .event(event)
                .build();

        Registration saved = registrationRepository.save(registration);

        return RegistrationResponse.builder()
                .id(saved.getId())
                .eventTitle(event.getTitle())
                .eventLocation(event.getLocation())
                .eventDate(event.getEventDate())
                .registeredAt(saved.getRegisteredAt())
                .attended(saved.isAttended())
                .build();
    }

    // Entity → DTO convert karna
    private EventResponse mapToResponse(Event event) {
        return EventResponse.builder()
                .id(event.getId())
                .title(event.getTitle())
                .description(event.getDescription())
                .location(event.getLocation())
                .eventDate(event.getEventDate())
                .capacity(event.getCapacity())
                .registeredCount(registrationRepository.countByEventId(event.getId()))
                .clubName(event.getClub().getName())
                .organizerName(event.getOrganizer().getName())
                .createdAt(event.getCreatedAt())
                .build();
    }
}