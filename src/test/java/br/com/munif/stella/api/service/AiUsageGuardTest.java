package br.com.munif.stella.api.service;

import br.com.munif.stella.api.config.AiProperties;
import br.com.munif.stella.api.config.OpenAiLimitsProperties;
import br.com.munif.stella.api.exception.AiUsageLimitException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiUsageGuardTest {

    @Test
    void deveContabilizarUsoAteOLimiteDiario() {
        AiUsageGuard guard = new AiUsageGuard(
                new AiProperties(true),
                new OpenAiLimitsProperties(1, null, null),
                Clock.fixed(Instant.parse("2026-06-12T10:00:00Z"), ZoneOffset.UTC)
        );

        guard.consume(AiOperation.IMAGE_ANALYSIS);

        assertThat(guard.usage(AiOperation.IMAGE_ANALYSIS)).isEqualTo(1);
        assertThatThrownBy(() -> guard.consume(AiOperation.IMAGE_ANALYSIS))
                .isInstanceOf(AiUsageLimitException.class)
                .hasMessage("Daily limit for OpenAI image analysis reached.")
                .extracting("status")
                .isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    void deveBloquearQuandoIaEstaDesabilitada() {
        AiUsageGuard guard = new AiUsageGuard(
                new AiProperties(false),
                new OpenAiLimitsProperties(null, null, null),
                Clock.systemUTC()
        );

        assertThatThrownBy(() -> guard.consume(AiOperation.IMAGE_GENERATION))
                .isInstanceOf(AiUsageLimitException.class)
                .hasMessage("AI features are disabled in this environment.")
                .extracting("status")
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void deveResetarUsoQuandoMudaODia() {
        MutableClock clock = new MutableClock(Instant.parse("2026-06-12T23:59:00Z"));
        AiUsageGuard guard = new AiUsageGuard(
                new AiProperties(true),
                new OpenAiLimitsProperties(null, 1, null),
                clock
        );
        guard.consume(AiOperation.IMAGE_GENERATION);

        clock.instant = Instant.parse("2026-06-13T00:01:00Z");
        guard.consume(AiOperation.IMAGE_GENERATION);

        assertThat(guard.usage(AiOperation.IMAGE_GENERATION)).isEqualTo(1);
    }

    private static class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
