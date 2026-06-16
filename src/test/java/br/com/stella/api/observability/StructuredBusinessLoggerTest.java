package br.com.stella.api.observability;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class StructuredBusinessLoggerTest {

    @Test
    void deveAdicionarCamposEstruturadosFiltrarSensiveisERestaurarMdc() {
        Logger logger = (Logger) LoggerFactory.getLogger("structured-business-test");
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        MDC.put("request_id", "req-1");

        try {
            StructuredBusinessLogger.info(logger, "inventory", "item-created", StructuredBusinessLogger.fields(
                    "item_id", "123",
                    "success", true,
                    "access_token", "should-not-appear",
                    "image_base64", "should-not-appear"
            ));

            assertThat(appender.list).hasSize(1);
            ILoggingEvent event = appender.list.getFirst();
            assertThat(event.getLevel()).isEqualTo(Level.INFO);
            assertThat(event.getMDCPropertyMap())
                    .containsEntry("request_id", "req-1")
                    .containsEntry("event_category", "inventory")
                    .containsEntry("event_action", "item-created")
                    .containsEntry("item_id", "123")
                    .containsEntry("success", "true")
                    .doesNotContainKeys("access_token", "image_base64");
            assertThat(MDC.getCopyOfContextMap()).containsExactlyEntriesOf(Map.of("request_id", "req-1"));
        } finally {
            logger.detachAppender(appender);
            MDC.clear();
        }
    }
}
