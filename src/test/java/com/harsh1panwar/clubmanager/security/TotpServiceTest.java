package com.harsh1panwar.clubmanager.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Plain unit tests — no Spring context or database needed. */
class TotpServiceTest {

    private final TotpService totp = new TotpService("test-secret");

    @Test
    void freshTokenIsAccepted() {
        String token = totp.generateToken(42L);
        assertTrue(totp.validateToken(42L, token));
    }

    @Test
    void tokenForOneEventIsRejectedForAnother() {
        String token = totp.generateToken(42L);
        assertFalse(totp.validateToken(43L, token));
    }

    @Test
    void garbageAndBlankTokensAreRejected() {
        assertFalse(totp.validateToken(42L, "not-a-real-token"));
        assertFalse(totp.validateToken(42L, ""));
        assertFalse(totp.validateToken(42L, null));
    }

    @Test
    void tokenFromDifferentSecretIsRejected() {
        String forged = new TotpService("other-secret").generateToken(42L);
        assertFalse(totp.validateToken(42L, forged));
    }

    @Test
    void secondsUntilRotationIsWithinWindow() {
        long s = totp.secondsUntilRotation();
        assertTrue(s >= 1 && s <= TotpService.WINDOW_SECONDS);
    }
}
