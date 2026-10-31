package com.harsh1panwar.clubmanager.service;

import com.harsh1panwar.clubmanager.entity.Event;
import com.harsh1panwar.clubmanager.entity.Registration;
import com.harsh1panwar.clubmanager.entity.User;
import com.harsh1panwar.clubmanager.exception.ApiException;
import com.harsh1panwar.clubmanager.repository.RegistrationRepository;
import com.harsh1panwar.clubmanager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Builds a participation certificate PDF on demand (nothing is stored on disk),
 * and only for attendees whose attendance was verified through the QR check-in.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CertificateService {

    private static final Color NAVY = new Color(30, 58, 138);
    private static final Color GOLD = new Color(180, 140, 50);
    private static final Color DARK = new Color(33, 37, 41);
    private static final Color MUTED = new Color(108, 117, 125);

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.ENGLISH);

    private final RegistrationRepository registrationRepository;
    private final UserRepository userRepository;

    public byte[] generateCertificate(Long eventId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> ApiException.notFound("User not found"));

        Registration registration = registrationRepository
                .findWithDetailsByUserIdAndEventId(user.getId(), eventId)
                .orElseThrow(() -> ApiException.forbidden("You are not registered for this event"));

        if (!registration.isAttended()) {
            throw ApiException.forbidden("Certificate is available only after your attendance is marked");
        }

        try {
            return render(registration);
        } catch (IOException e) {
            throw new IllegalStateException("Could not generate certificate", e);
        }
    }

    private byte[] render(Registration registration) throws IOException {
        Event event = registration.getEvent();
        String attendeeName = registration.getUser().getName();
        String certificateId = String.format("CM-%d-%05d", event.getId(), registration.getId());

        PDFont bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        PDFont regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        PDFont italic = new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE);
        PDFont serifBold = new PDType1Font(Standard14Fonts.FontName.TIMES_BOLD);

        // Landscape A4
        PDRectangle size = new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth());
        float width = size.getWidth();
        float height = size.getHeight();

        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(size);
            doc.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                // Double border
                cs.setStrokingColor(NAVY);
                cs.setLineWidth(6);
                cs.addRect(24, 24, width - 48, height - 48);
                cs.stroke();
                cs.setStrokingColor(GOLD);
                cs.setLineWidth(1.5f);
                cs.addRect(36, 36, width - 72, height - 72);
                cs.stroke();

                float maxText = width - 160;

                centered(cs, bold, 30, NAVY, "CERTIFICATE OF PARTICIPATION", width, height - 120, maxText);

                // Gold rule under the heading
                cs.setStrokingColor(GOLD);
                cs.setLineWidth(2);
                cs.moveTo(width / 2 - 120, height - 140);
                cs.lineTo(width / 2 + 120, height - 140);
                cs.stroke();

                centered(cs, italic, 16, MUTED, "This is to certify that", width, height - 190, maxText);
                centered(cs, serifBold, 36, DARK, attendeeName, width, height - 245, maxText);
                centered(cs, italic, 16, MUTED, "has successfully attended", width, height - 290, maxText);
                centered(cs, bold, 24, NAVY, event.getTitle(), width, height - 335, maxText);

                String details = "organized by " + event.getClub().getName()
                        + " on " + event.getEventDate().format(DATE)
                        + " at " + event.getLocation();
                centered(cs, regular, 14, DARK, details, width, height - 375, maxText);

                // Footer
                String verified = registration.getAttendedAt() == null
                        ? "Attendance verified via rotating QR check-in"
                        : "Attendance verified via rotating QR check-in on "
                          + registration.getAttendedAt().format(DATE_TIME);
                text(cs, regular, 10, MUTED, verified, 60, 60);
                String idLine = "Certificate ID: " + certificateId;
                float idWidth = textWidth(regular, 10, safe(regular, idLine));
                text(cs, regular, 10, MUTED, idLine, width - 60 - idWidth, 60);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        }
    }

    /** Draws text centred horizontally, shrinking the font until it fits maxWidth. */
    private void centered(PDPageContentStream cs, PDFont font, float fontSize, Color color,
                          String raw, float pageWidth, float y, float maxWidth) throws IOException {
        String text = safe(font, raw);
        float size = fontSize;
        while (size > 10 && textWidth(font, size, text) > maxWidth) {
            size -= 1;
        }
        float x = (pageWidth - textWidth(font, size, text)) / 2;
        text(cs, font, size, color, text, x, y);
    }

    private void text(PDPageContentStream cs, PDFont font, float size, Color color,
                      String raw, float x, float y) throws IOException {
        cs.beginText();
        cs.setFont(font, size);
        cs.setNonStrokingColor(color);
        cs.newLineAtOffset(x, y);
        cs.showText(safe(font, raw));
        cs.endText();
    }

    private float textWidth(PDFont font, float size, String text) throws IOException {
        return font.getStringWidth(text) / 1000 * size;
    }

    /** Standard PDF fonts only cover Latin characters; replace anything they cannot encode. */
    private String safe(PDFont font, String text) {
        if (text == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        text.codePoints().forEach(cp -> {
            String ch = new String(Character.toChars(cp));
            try {
                font.encode(ch);
                sb.append(ch);
            } catch (IOException | IllegalArgumentException e) {
                sb.append('?');
            }
        });
        return sb.toString();
    }
}
