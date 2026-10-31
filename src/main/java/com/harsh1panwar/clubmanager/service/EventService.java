package com.harsh1panwar.clubmanager.service;

import com.harsh1panwar.clubmanager.dto.AttendeeResponse;
import com.harsh1panwar.clubmanager.dto.EventRequest;
import com.harsh1panwar.clubmanager.dto.EventResponse;
import com.harsh1panwar.clubmanager.dto.RegistrationResponse;
import com.harsh1panwar.clubmanager.entity.Club;
import com.harsh1panwar.clubmanager.entity.Event;
import com.harsh1panwar.clubmanager.entity.Registration;
import com.harsh1panwar.clubmanager.entity.User;
import com.harsh1panwar.clubmanager.exception.ApiException;
import com.harsh1panwar.clubmanager.repository.ClubRepository;
import com.harsh1panwar.clubmanager.repository.EventRepository;
import com.harsh1panwar.clubmanager.repository.RegistrationRepository;
import com.harsh1panwar.clubmanager.repository.UserRepository;
import com.harsh1panwar.clubmanager.security.TotpService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventService {

    private final EventRepository eventRepository;
    private final ClubRepository clubRepository;
    private final UserRepository userRepository;
    private final RegistrationRepository registrationRepository;
    private final TotpService totpService;

    // ---------- Organizer ----------

    @Transactional
    public EventResponse createEvent(EventRequest request, String organizerEmail) {
        User organizer = findUser(organizerEmail);

        Club club = clubRepository.findById(request.getClubId())
                .orElseThrow(() -> ApiException.notFound("Club not found"));

        if (!club.getOwner().getId().equals(organizer.getId())) {
            throw ApiException.forbidden("Only the club owner can create events for this club");
        }

        Event event = Event.builder()
                .title(request.getTitle().trim())
                .description(request.getDescription())
                .location(request.getLocation().trim())
                .eventDate(request.getEventDate())
                .capacity(request.getCapacity())
                .club(club)
                .organizer(organizer)
                .build();

        Event saved = eventRepository.save(event);
        return toResponse(saved, 0);
    }

    public List<EventResponse> getMyEvents(String organizerEmail) {
        User organizer = findUser(organizerEmail);
        return toResponses(eventRepository.findByOrganizerIdOrderByEventDateAsc(organizer.getId()));
    }

    /** Throws 403 unless the given user organizes this event. Used before showing the live QR. */
    public void assertOrganizerOwns(Long eventId, String organizerEmail) {
        User organizer = findUser(organizerEmail);
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> ApiException.notFound("Event not found"));
        if (!event.getOrganizer().getId().equals(organizer.getId())) {
            throw ApiException.forbidden("You are not the organizer of this event");
        }
    }

    public List<AttendeeResponse> getAttendees(Long eventId, String organizerEmail) {
        assertOrganizerOwns(eventId, organizerEmail);
        return registrationRepository.findByEventIdOrderByRegisteredAtAsc(eventId).stream()
                .map(r -> AttendeeResponse.builder()
                        .registrationId(r.getId())
                        .name(r.getUser().getName())
                        .email(r.getUser().getEmail())
                        .registeredAt(r.getRegisteredAt())
                        .attended(r.isAttended())
                        .attendedAt(r.getAttendedAt())
                        .build())
                .toList();
    }

    // ---------- Everyone ----------

    public List<EventResponse> getAllEvents() {
        return toResponses(eventRepository.findAllByOrderByEventDateAsc());
    }

    public EventResponse getEventById(Long id) {
        Event event = eventRepository.findWithDetailsById(id)
                .orElseThrow(() -> ApiException.notFound("Event not found"));
        return toResponse(event, registrationRepository.countByEventId(id));
    }

    // ---------- Attendee ----------

    @Transactional
    public RegistrationResponse registerForEvent(Long eventId, String userEmail) {
        User user = findUser(userEmail);

        Event event = eventRepository.findWithDetailsById(eventId)
                .orElseThrow(() -> ApiException.notFound("Event not found"));

        if (event.getEventDate().isBefore(LocalDateTime.now().minusHours(12))) {
            throw ApiException.badRequest("Registration is closed: this event is already over");
        }

        if (registrationRepository.existsByUserIdAndEventId(user.getId(), eventId)) {
            throw ApiException.conflict("Already registered for this event");
        }

        if (registrationRepository.countByEventId(eventId) >= event.getCapacity()) {
            throw ApiException.conflict("Event is full");
        }

        Registration saved = registrationRepository.save(
                Registration.builder().user(user).event(event).build());

        return toRegistrationResponse(saved);
    }

    /**
     * Marks attendance for the logged-in attendee. Because the token rotates every
     * 30 seconds and attendance is tied to the authenticated account, a shared
     * screenshot cannot be used to mark someone else present.
     */
    @Transactional
    public RegistrationResponse checkIn(Long eventId, String userEmail, String scannedToken) {
        if (!totpService.validateToken(eventId, scannedToken)) {
            throw ApiException.badRequest("Invalid or expired QR code — scan the live code again");
        }

        User user = findUser(userEmail);

        Registration registration = registrationRepository
                .findWithDetailsByUserIdAndEventId(user.getId(), eventId)
                .orElseThrow(() -> ApiException.forbidden("You are not registered for this event"));

        if (registration.isAttended()) {
            throw ApiException.conflict("Already checked in");
        }

        registration.setAttended(true);
        registration.setAttendedAt(LocalDateTime.now());
        return toRegistrationResponse(registrationRepository.save(registration));
    }

    public List<RegistrationResponse> getMyRegistrations(String userEmail) {
        User user = findUser(userEmail);
        return registrationRepository.findByUserIdOrderByRegisteredAtDesc(user.getId()).stream()
                .map(this::toRegistrationResponse)
                .toList();
    }

    // ---------- Helpers ----------

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> ApiException.notFound("User not found"));
    }

    /** Maps a list of events using one grouped COUNT query for all registration counts. */
    private List<EventResponse> toResponses(List<Event> events) {
        if (events.isEmpty()) {
            return List.of();
        }
        Map<Long, Long> counts = new HashMap<>();
        for (Object[] row : registrationRepository.countGroupedByEventIds(
                events.stream().map(Event::getId).toList())) {
            counts.put((Long) row[0], (Long) row[1]);
        }
        return events.stream()
                .map(e -> toResponse(e, counts.getOrDefault(e.getId(), 0L).intValue()))
                .toList();
    }

    private EventResponse toResponse(Event event, int registeredCount) {
        return EventResponse.builder()
                .id(event.getId())
                .title(event.getTitle())
                .description(event.getDescription())
                .location(event.getLocation())
                .eventDate(event.getEventDate())
                .capacity(event.getCapacity())
                .registeredCount(registeredCount)
                .clubName(event.getClub().getName())
                .organizerName(event.getOrganizer().getName())
                .createdAt(event.getCreatedAt())
                .build();
    }

    private RegistrationResponse toRegistrationResponse(Registration r) {
        Event event = r.getEvent();
        return RegistrationResponse.builder()
                .id(r.getId())
                .eventId(event.getId())
                .eventTitle(event.getTitle())
                .eventLocation(event.getLocation())
                .clubName(event.getClub().getName())
                .eventDate(event.getEventDate())
                .registeredAt(r.getRegisteredAt())
                .attended(r.isAttended())
                .attendedAt(r.getAttendedAt())
                .build();
    }
}
