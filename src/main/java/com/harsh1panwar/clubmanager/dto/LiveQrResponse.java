package com.harsh1panwar.clubmanager.dto;

/**
 * Current rotating check-in token for an event.
 *
 * @param token            HMAC token encoded in the QR code
 * @param expiresInSeconds seconds until the token rotates
 * @param windowSeconds    length of one rotation window
 */
public record LiveQrResponse(String token, long expiresInSeconds, int windowSeconds) {
}
