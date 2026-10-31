package com.harsh1panwar.clubmanager.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * Stateless rotating check-in tokens (same idea as TOTP / Google Authenticator).
 *
 * token = Base64Url( HMAC-SHA256(secret, eventId + ":" + floor(now / WINDOW)) )
 *
 * Nothing is stored in the database: the server recomputes the expected token
 * for the current and previous window and compares. A screenshot of the QR
 * goes stale after at most two windows, and the check-in is always recorded
 * against the account that is logged in on the scanning phone.
 */
@Service
public class TotpService {

    public static final int WINDOW_SECONDS = 30;

    /** Windows accepted besides the current one, to tolerate scan/network delay. */
    private static final int ALLOWED_PAST_WINDOWS = 1;

    private final byte[] secret;

    public TotpService(@Value("${app.totp.secret}") String secret) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
    }

    public String generateToken(Long eventId) {
        return computeHmac(eventId, currentWindow());
    }

    public long secondsUntilRotation() {
        return WINDOW_SECONDS - (System.currentTimeMillis() / 1000 % WINDOW_SECONDS);
    }

    public boolean validateToken(Long eventId, String scannedToken) {
        if (scannedToken == null || scannedToken.isBlank()) {
            return false;
        }
        byte[] scanned = scannedToken.trim().getBytes(StandardCharsets.UTF_8);
        long window = currentWindow();
        for (int i = 0; i <= ALLOWED_PAST_WINDOWS; i++) {
            byte[] expected = computeHmac(eventId, window - i).getBytes(StandardCharsets.UTF_8);
            // constant-time comparison so response timing leaks nothing about the token
            if (MessageDigest.isEqual(expected, scanned)) {
                return true;
            }
        }
        return false;
    }

    private long currentWindow() {
        return System.currentTimeMillis() / 1000 / WINDOW_SECONDS;
    }

    private String computeHmac(Long eventId, long timeWindow) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            byte[] hash = mac.doFinal((eventId + ":" + timeWindow).getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (Exception e) {
            throw new IllegalStateException("TOTP generation failed", e);
        }
    }
}
