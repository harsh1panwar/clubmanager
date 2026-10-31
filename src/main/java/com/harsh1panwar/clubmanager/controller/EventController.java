package com.harsh1panwar.clubmanager.controller;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.harsh1panwar.clubmanager.dto.AttendeeResponse;
import com.harsh1panwar.clubmanager.dto.CheckInRequest;
import com.harsh1panwar.clubmanager.dto.EventRequest;
import com.harsh1panwar.clubmanager.dto.EventResponse;
import com.harsh1panwar.clubmanager.dto.LiveQrResponse;
import com.harsh1panwar.clubmanager.dto.RegistrationResponse;
import com.harsh1panwar.clubmanager.security.TotpService;
import com.harsh1panwar.clubmanager.service.CertificateService;
import com.harsh1panwar.clubmanager.service.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;
    private final TotpService totpService;
    private final CertificateService certificateService;

    // ---------- Organizer ----------

    @PostMapping
    public ResponseEntity<EventResponse> createEvent(@Valid @RequestBody EventRequest request,
                                                     @AuthenticationPrincipal String email) {
        return ResponseEntity.status(HttpStatus.CREATED).body(eventService.createEvent(request, email));
    }

    @GetMapping("/mine")
    public ResponseEntity<List<EventResponse>> getMyEvents(@AuthenticationPrincipal String email) {
        return ResponseEntity.ok(eventService.getMyEvents(email));
    }

    /** Current rotating token as JSON (token + seconds until it rotates). */
    @GetMapping("/{id}/live-qr")
    public ResponseEntity<LiveQrResponse> getLiveQr(@PathVariable Long id,
                                                    @AuthenticationPrincipal String email) {
        eventService.assertOrganizerOwns(id, email);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(new LiveQrResponse(totpService.generateToken(id),
                        totpService.secondsUntilRotation(), TotpService.WINDOW_SECONDS));
    }

    /** Current rotating token rendered as a PNG QR code for the organizer's screen. */
    @GetMapping("/{id}/live-qr-image")
    public ResponseEntity<byte[]> getLiveQrImage(@PathVariable Long id,
                                                 @AuthenticationPrincipal String email) throws Exception {
        eventService.assertOrganizerOwns(id, email);

        BitMatrix matrix = new MultiFormatWriter().encode(
                totpService.generateToken(id), BarcodeFormat.QR_CODE, 360, 360,
                Map.of(EncodeHintType.MARGIN, 1));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(matrix, "PNG", out);

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header("X-Expires-In", String.valueOf(totpService.secondsUntilRotation()))
                .contentType(MediaType.IMAGE_PNG)
                .body(out.toByteArray());
    }

    @GetMapping("/{id}/attendees")
    public ResponseEntity<List<AttendeeResponse>> getAttendees(@PathVariable Long id,
                                                               @AuthenticationPrincipal String email) {
        return ResponseEntity.ok(eventService.getAttendees(id, email));
    }

    // ---------- Everyone (authenticated) ----------

    @GetMapping
    public ResponseEntity<List<EventResponse>> getAllEvents() {
        return ResponseEntity.ok(eventService.getAllEvents());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventResponse> getEventById(@PathVariable Long id) {
        return ResponseEntity.ok(eventService.getEventById(id));
    }

    // ---------- Attendee ----------

    @PostMapping("/{id}/register")
    public ResponseEntity<RegistrationResponse> registerForEvent(@PathVariable Long id,
                                                                 @AuthenticationPrincipal String email) {
        return ResponseEntity.status(HttpStatus.CREATED).body(eventService.registerForEvent(id, email));
    }

    @PostMapping("/{id}/check-in")
    public ResponseEntity<RegistrationResponse> checkIn(@PathVariable Long id,
                                                        @Valid @RequestBody CheckInRequest request,
                                                        @AuthenticationPrincipal String email) {
        return ResponseEntity.ok(eventService.checkIn(id, email, request.getToken()));
    }

    @GetMapping("/{id}/certificate")
    public ResponseEntity<byte[]> downloadCertificate(@PathVariable Long id,
                                                      @AuthenticationPrincipal String email) {
        byte[] pdf = certificateService.generateCertificate(id, email);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("certificate-event-" + id + ".pdf").build().toString())
                .body(pdf);
    }
}
