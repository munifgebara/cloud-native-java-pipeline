package br.com.munif.common.owner;

import java.util.Locale;

public record OwnerIdentity(String email, String issuer) {

    public OwnerIdentity {
        email = normalizeEmail(email);
        issuer = normalizeIssuer(issuer);
        if (email == null || issuer == null) {
            throw new IllegalArgumentException("Owner email and issuer are required.");
        }
    }

    public static String normalizeEmail(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    public static String normalizeIssuer(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
