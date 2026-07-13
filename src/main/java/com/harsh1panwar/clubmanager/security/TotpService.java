package com.harsh1panwar.clubmanager.security;

import org.springframework.stereotype.Service;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
public class TotpService {

    private static final String SECRET = "clubmanager-totp-secret-key-2026";
    private static final int WINDOW_SECONDS = 30;

    // Organizer ke screen pe dikhane ke liye token generate karo
    public String generateToken(Long eventId) {
        long timeWindow = System.currentTimeMillis() / 1000 / WINDOW_SECONDS;
        return computeHmac(eventId, timeWindow);
    }

    // Attendee ka scanned token validate karo
    // Current + previous window check karo (scan delay ke liye)
    public boolean validateToken(Long eventId, String scannedToken) {
        long timeWindow = System.currentTimeMillis() / 1000 / WINDOW_SECONDS;
        return computeHmac(eventId, timeWindow).equals(scannedToken)
                || computeHmac(eventId, timeWindow - 1).equals(scannedToken);
    }

    private String computeHmac(Long eventId, long timeWindow) {
        try {
            String data = eventId + ":" + timeWindow;
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(
                    SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(keySpec);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("TOTP generation failed", e);
        }
    }
}