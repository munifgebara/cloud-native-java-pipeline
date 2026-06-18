package br.com.stella.api.contract;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Low-maintenance black-box contract test between the bundled front-end (SPA) and the REST API.
 *
 * <p>It derives <b>both sides automatically</b>, so there are no UI selectors, payloads, or
 * hand-maintained endpoint lists to keep in sync:</p>
 * <ol>
 *   <li>The endpoints the front-end <i>calls</i> are extracted from the served bundle
 *       ({@code static/app/main-*.js}, matched by glob so it survives the content hash).</li>
 *   <li>The endpoints the backend <i>exposes</i> are read from the live OpenAPI document
 *       ({@code /v3/api-docs}), which always reflects the actual controllers.</li>
 * </ol>
 *
 * <p>The test fails when the front calls a resource the backend does not expose — exactly the
 * kind of drift that breaks the SPA after an API rename. When the front bundle or the backend
 * changes, the test re-derives both sides; nothing here needs manual updating.</p>
 *
 * <p>The front-end was aligned to the English API (#180), so this test is active and acts as a
 * CI gate against future front/back drift. It requires the front bundle to be present on the
 * classpath ({@code static/app}); run it through {@code mvn verify} (which builds the front) or
 * after a front build.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class FrontendApiContractTest {

    /** Matches the API path literals the SPA can call, e.g. {@code /api/v0/categories} or {@code /api/public/login}. */
    private static final Pattern API_PATH = Pattern.compile("/api/(?:v0|public)/[A-Za-z0-9_-]+");

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @Test
    void frontendApiCallsMustExistInTheBackend() throws Exception {
        Set<String> backend = backendResourcePrefixes();
        Set<String> frontend = frontendApiCalls();

        assertThat(frontend)
                .as("Sanity check: the front-end bundle should reference at least one /api endpoint")
                .isNotEmpty();

        Set<String> missing = new TreeSet<>(frontend);
        missing.removeAll(backend);

        assertThat(missing)
                .as("The front-end calls these API resources, but the backend does not expose them "
                        + "(front/back contract drift). Backend exposes: %s", backend)
                .isEmpty();
    }

    /** Reads {@code /v3/api-docs} and reduces every documented path to its {@code /api/<scope>/<resource>} prefix. */
    private Set<String> backendResourcePrefixes() throws Exception {
        String apiDocs = mockMvc.perform(get("/v3/api-docs"))
                .andReturn().getResponse().getContentAsString();

        JsonNode paths = objectMapper.readTree(apiDocs).path("paths");
        Set<String> prefixes = new LinkedHashSet<>();
        for (Iterator<String> it = paths.fieldNames(); it.hasNext(); ) {
            String path = it.next();
            if (path.startsWith("/api/")) {
                prefixes.add(resourcePrefix(path));
            }
        }
        return prefixes;
    }

    /** Scans the served front-end bundle for the {@code /api/...} resource prefixes it calls. */
    private Set<String> frontendApiCalls() throws IOException {
        Resource[] bundles = new PathMatchingResourcePatternResolver()
                .getResources("classpath:static/app/main-*.js");

        Set<String> calls = new LinkedHashSet<>();
        for (Resource bundle : bundles) {
            String content = new String(bundle.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            Matcher matcher = API_PATH.matcher(content);
            while (matcher.find()) {
                calls.add(resourcePrefix(matcher.group()));
            }
        }
        return calls;
    }

    /** Keeps the first three path segments: {@code /api/v0/categories/{id}} -> {@code /api/v0/categories}. */
    private static String resourcePrefix(String path) {
        String[] segments = Arrays.stream(path.split("/"))
                .filter(s -> !s.isBlank())
                .toArray(String[]::new);
        int keep = Math.min(3, segments.length);
        StringBuilder prefix = new StringBuilder();
        for (int i = 0; i < keep; i++) {
            prefix.append('/').append(segments[i]);
        }
        return prefix.toString();
    }
}
