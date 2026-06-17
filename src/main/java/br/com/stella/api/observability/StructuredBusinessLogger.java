package br.com.stella.api.observability;

import org.slf4j.Logger;
import org.slf4j.MDC;
import org.slf4j.event.Level;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class StructuredBusinessLogger {

    private static final Set<String> SENSITIVE_KEY_FRAGMENTS = Set.of(
            "authorization",
            "base64",
            "password",
            "secret",
            "token"
    );

    private StructuredBusinessLogger() {
    }

    public static Map<String, Object> fields(Object... entries) {
        if (entries.length % 2 != 0) {
            throw new IllegalArgumentException("Structured fields must be provided in key/value pairs.");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < entries.length; i += 2) {
            Object key = entries[i];
            if (!(key instanceof String stringKey)) {
                throw new IllegalArgumentException("Structured field key must be a String.");
            }
            Object value = entries[i + 1];
            if (value != null) {
                result.put(stringKey, value);
            }
        }
        return result;
    }

    public static void info(Logger logger, String category, String action, Map<String, ?> fields) {
        log(logger, Level.INFO, category, action, fields, null);
    }

    public static void warn(Logger logger, String category, String action, Map<String, ?> fields) {
        log(logger, Level.WARN, category, action, fields, null);
    }

    public static void error(Logger logger, String category, String action, Map<String, ?> fields, Throwable throwable) {
        log(logger, Level.ERROR, category, action, fields, throwable);
    }

    private static void log(
            Logger logger,
            Level level,
            String category,
            String action,
            Map<String, ?> fields,
            Throwable throwable
    ) {
        Map<String, String> previousContext = MDC.getCopyOfContextMap();
        try {
            MDC.put("event_category", category);
            MDC.put("event_action", action);
            if (fields != null) {
                fields.forEach(StructuredBusinessLogger::putSafeField);
            }

            String message = "business_event category={} action={}";
            switch (level) {
                case ERROR -> logger.error(message, category, action, throwable);
                case WARN -> logger.warn(message, category, action);
                default -> logger.info(message, category, action);
            }
        } finally {
            if (previousContext == null) {
                MDC.clear();
            } else {
                MDC.setContextMap(previousContext);
            }
        }
    }

    private static void putSafeField(String key, Object value) {
        if (key == null || value == null || isSensitive(key)) {
            return;
        }
        MDC.put(key, String.valueOf(value));
    }

    private static boolean isSensitive(String key) {
        String normalized = key.toLowerCase(Locale.ROOT);
        return SENSITIVE_KEY_FRAGMENTS.stream().anyMatch(normalized::contains);
    }
}
