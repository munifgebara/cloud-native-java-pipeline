package br.com.stella.api.service;

import br.com.stella.api.config.AiProperties;
import br.com.stella.api.config.OpenAiLimitsProperties;
import br.com.stella.api.exception.AiUsageLimitException;
import br.com.stella.api.observability.StructuredBusinessLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.Map;

@Service
public class AiUsageGuard {

    private static final Logger log = LoggerFactory.getLogger(AiUsageGuard.class);

    private final AiProperties aiProperties;
    private final OpenAiLimitsProperties limitsProperties;
    private final Clock clock;
    private final Map<AiOperation, Integer> dailyUsage = new EnumMap<>(AiOperation.class);
    private LocalDate usageDate;

    @Autowired
    public AiUsageGuard(AiProperties aiProperties, OpenAiLimitsProperties limitsProperties) {
        this(aiProperties, limitsProperties, Clock.systemDefaultZone());
    }

    AiUsageGuard(AiProperties aiProperties, OpenAiLimitsProperties limitsProperties, Clock clock) {
        this.aiProperties = aiProperties;
        this.limitsProperties = limitsProperties;
        this.clock = clock;
        this.usageDate = LocalDate.now(clock);
    }

    public void assertEnabled(AiOperation operation) {
        if (!aiProperties.enabled()) {
            StructuredBusinessLogger.warn(log, "ai", "usage-blocked", StructuredBusinessLogger.fields(
                    "ai_operation", operation.name(),
                    "block_reason", "disabled"
            ));
            throw new AiUsageLimitException(HttpStatus.FORBIDDEN, "AI features are disabled in this environment.");
        }
    }

    public synchronized void consume(AiOperation operation) {
        assertEnabled(operation);
        resetIfNeeded();

        Integer limit = limitsProperties.limitFor(operation);
        int used = dailyUsage.getOrDefault(operation, 0);
        if (limit != null && used >= limit) {
            StructuredBusinessLogger.warn(log, "ai", "usage-blocked", StructuredBusinessLogger.fields(
                    "ai_operation", operation.name(),
                    "block_reason", "daily-limit",
                    "daily_limit", limit,
                    "daily_usage", used
            ));
            throw new AiUsageLimitException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Daily limit for OpenAI %s reached.".formatted(operation.description())
            );
        }

        dailyUsage.put(operation, used + 1);
    }

    public synchronized int usage(AiOperation operation) {
        resetIfNeeded();
        return dailyUsage.getOrDefault(operation, 0);
    }

    private void resetIfNeeded() {
        LocalDate today = LocalDate.now(clock);
        if (!today.equals(usageDate)) {
            dailyUsage.clear();
            usageDate = today;
        }
    }
}
