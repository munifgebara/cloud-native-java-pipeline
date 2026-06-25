package br.com.munif.common.owner;

import java.util.Optional;

public final class OwnerContext {

    private static final ThreadLocal<OwnerIdentity> CURRENT = new ThreadLocal<>();

    private OwnerContext() {
    }

    public static void set(OwnerIdentity identity) {
        CURRENT.set(identity);
    }

    public static Optional<OwnerIdentity> current() {
        return Optional.ofNullable(CURRENT.get());
    }

    public static OwnerIdentity currentRequired() {
        return current().orElseThrow(() -> new IllegalStateException("Authenticated owner context is required."));
    }

    public static String currentEmailOrNull() {
        return current().map(OwnerIdentity::email).orElse(null);
    }

    public static String currentIssuerOrNull() {
        return current().map(OwnerIdentity::issuer).orElse(null);
    }

    public static boolean isUnrestricted() {
        return current().isEmpty();
    }

    public static void clear() {
        CURRENT.remove();
    }
}
