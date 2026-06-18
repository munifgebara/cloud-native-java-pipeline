import { test, expect, Page } from '@playwright/test';

/**
 * Low-maintenance browser smoke test for the Stella SPA.
 *
 * Instead of asserting on specific UI text/markup (which rots on every UI tweak), it logs in and
 * navigates the main screens via the sidebar while watching the network and the console. It fails
 * if any API call returns 4xx/5xx or if the page logs an error — which is exactly how front/back
 * contract drift surfaces. The only UI coupling is the login form (stable ids) and the route
 * hrefs (derived from routerLink), not visible text.
 */

const USERNAME = process.env.STELLA_E2E_USERNAME || 'admin';
const PASSWORD = process.env.STELLA_E2E_PASSWORD || 'admin123';

// Sidebar destinations (Angular routes; the rendered link href ends with these).
const NAV_ROUTES = ['/people', '/users', '/categories', '/locations', '/main-items', '/photo-upload', '/dashboard'];

function watch(page: Page, apiFailures: string[], consoleErrors: string[]) {
  page.on('response', (res) => {
    const url = res.url();
    if (url.includes('/api/') && res.status() >= 400) {
      apiFailures.push(`${res.status()} ${res.request().method()} ${url}`);
    }
  });
  page.on('console', (msg) => {
    if (msg.type() === 'error') consoleErrors.push(msg.text());
  });
  page.on('pageerror', (err) => consoleErrors.push(err.message));
}

test('logs in and navigates the main screens with no failed API calls or console errors', async ({ page }) => {
  const apiFailures: string[] = [];
  const consoleErrors: string[] = [];
  watch(page, apiFailures, consoleErrors);

  // Load the SPA (server forwards /app to the Angular app; routing then happens client-side).
  await page.goto('/app');

  // Log in through the form; the token is persisted in localStorage.
  await page.locator('#username').waitFor({ timeout: 20_000 });
  await page.locator('#username').fill(USERNAME);
  // PrimeNG p-password wraps the actual <input>; target it directly.
  await page.locator('#password input, input[type="password"]').first().fill(PASSWORD);
  await page.locator('p-button button, button[type="submit"]').first().click();
  await page.waitForURL('**/dashboard', { timeout: 20_000 });

  // Navigate each main screen via the sidebar (client-side routing) and let its API calls settle.
  for (const route of NAV_ROUTES) {
    await page.locator(`a[href$="${route}"]`).first().click();
    await page.waitForURL(`**${route}`, { timeout: 15_000 });
    await page.waitForLoadState('networkidle', { timeout: 15_000 }).catch(() => {});
  }

  expect(apiFailures, `Failed API calls detected:\n${apiFailures.join('\n')}`).toEqual([]);
  expect(consoleErrors, `Console errors detected:\n${consoleErrors.join('\n')}`).toEqual([]);
});
