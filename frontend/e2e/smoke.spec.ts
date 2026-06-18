import { test, expect } from '@playwright/test';
import { login, watch } from './_helpers';

/**
 * Low-maintenance navigation smoke test for the Stella SPA.
 *
 * Logs in and navigates the main screens via the sidebar while watching the network and console.
 * Fails if any API call returns 4xx/5xx or the page logs an error — catching front/back drift
 * without brittle text/markup assertions. Data correctness is covered by the *-crud round-trip
 * specs; this one guards navigation and contract health.
 */

const NAV_ROUTES = ['/people', '/users', '/categories', '/locations', '/main-items', '/photo-upload', '/dashboard'];

test('logs in and navigates the main screens with no failed API calls or console errors', async ({ page }) => {
  const apiFailures: string[] = [];
  const consoleErrors: string[] = [];
  watch(page, apiFailures, consoleErrors);

  await login(page);

  for (const route of NAV_ROUTES) {
    await page.locator(`a[href$="${route}"]`).first().click();
    await page.waitForURL(`**${route}`, { timeout: 15_000 });
    await page.waitForLoadState('networkidle', { timeout: 15_000 }).catch(() => {});
  }

  expect(apiFailures, `Failed API calls detected:\n${apiFailures.join('\n')}`).toEqual([]);
  expect(consoleErrors, `Console errors detected:\n${consoleErrors.join('\n')}`).toEqual([]);
});
