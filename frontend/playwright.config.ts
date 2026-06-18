import { defineConfig } from '@playwright/test';

/**
 * Playwright E2E configuration for the Stella SPA.
 *
 * The tests drive a *running* application (the Spring Boot app serving the built SPA at /app,
 * plus the API, Keycloak, MinIO and Postgres). They do not start the stack themselves — bring it
 * up first (see docs/testing.md). Point at any environment via STELLA_E2E_BASE_URL.
 */
export default defineConfig({
  testDir: './e2e',
  timeout: 60_000,
  expect: { timeout: 10_000 },
  fullyParallel: false,
  // One worker: the suite shares a single backend/Keycloak; serial runs avoid login contention.
  workers: 1,
  // Retries absorb transient cold-start / Keycloak token latency on the first login of a run;
  // a real regression still fails every attempt.
  retries: process.env.CI ? 2 : 1,
  reporter: [['list']],
  use: {
    baseURL: process.env.STELLA_E2E_BASE_URL || 'http://localhost:8080',
    headless: true,
    ignoreHTTPSErrors: true,
    navigationTimeout: 30_000,
    actionTimeout: 15_000,
    screenshot: 'only-on-failure',
    trace: 'retain-on-failure',
  },
  projects: [{ name: 'chromium', use: { browserName: 'chromium' } }],
});
