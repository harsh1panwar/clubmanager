package com.harsh1panwar.clubmanager.controller;
import jakarta.servlet.http.HttpServletResponse;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.harsh1panwar.clubmanager.security.TotpService;
import com.harsh1panwar.clubmanager.dto.EventRequest;
import com.harsh1panwar.clubmanager.dto.EventResponse;
import com.harsh1panwar.clubmanager.dto.RegistrationResponse;
import com.harsh1panwar.clubmanager.service.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {
    // Constructor mein TotpService inject karo
    private final TotpService totpService;

    // Organizer ke screen pe live QR token
    @GetMapping("/{id}/live-qr")
    public ResponseEntity<Map<String, String>> getLiveQr(@PathVariable Long id) {
        String token = totpService.generateToken(id);
        return ResponseEntity.ok(Map.of(
                "token", token,
                "expiresInSeconds", String.valueOf(
                        30 - (System.currentTimeMillis() / 1000 % 30))
        ));
    }
    @GetMapping("/{id}/live-qr-image")
    public void getLiveQrImage(@PathVariable Long id, HttpServletResponse response) throws Exception {
        String token = totpService.generateToken(id);

        BitMatrix bitMatrix = new MultiFormatWriter().encode(
                token, BarcodeFormat.QR_CODE, 300, 300);

        response.setContentType("image/png");
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", response.getOutputStream());
    }
    private final EventService eventService;
    // Attendee check-in
    @PostMapping("/{id}/check-in")
    public ResponseEntity<Map<String, String>> checkIn(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal String email) {
        String scannedToken = body.get("token");
        eventService.checkIn(id, email, scannedToken);
        return ResponseEntity.ok(Map.of("message", "Attendance marked successfully"));
    }


    @PostMapping
    public ResponseEntity<EventResponse> createEvent(
            @Valid @RequestBody EventRequest request,
            @AuthenticationPrincipal String email) {
        return ResponseEntity.ok(eventService.createEvent(request, email));
    }

    @GetMapping
    public ResponseEntity<List<EventResponse>> getAllEvents() {
        return ResponseEntity.ok(eventService.getAllEvents());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventResponse> getEventById(@PathVariable Long id) {
        return ResponseEntity.ok(eventService.getEventById(id));
    }

    @PostMapping("/{id}/register")
    public ResponseEntity<RegistrationResponse> registerForEvent(
            @PathVariable Long id,
            @AuthenticationPrincipal String email) {
        return ResponseEntity.ok(eventService.registerForEvent(id, email));
    }
}