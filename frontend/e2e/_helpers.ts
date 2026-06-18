import { Page, expect } from '@playwright/test';

export const USERNAME = process.env.STELLA_E2E_USERNAME || 'admin';
export const PASSWORD = process.env.STELLA_E2E_PASSWORD || 'admin123';

/** Collects API failures (4xx/5xx) and console/page errors for end-of-test assertions. */
export function watch(page: Page, apiFailures: string[], consoleErrors: string[]) {
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

/** Loads the SPA and logs in via the form (token persists in localStorage). */
export async function login(page: Page) {
  await page.goto('/app');
  const user = page.locator('#username');
  await user.waitFor({ state: 'visible', timeout: 20_000 });
  await user.fill(USERNAME);
  const pass = page.locator('#password input, input[type="password"]').first();
  await pass.waitFor({ state: 'visible', timeout: 10_000 });
  await pass.fill(PASSWORD);
  // Ensure both values are committed before submitting (PrimeNG p-password can lag),
  // otherwise the form may still be invalid and the submit button is a no-op.
  await expect(user).toHaveValue(USERNAME);
  await expect(pass).toHaveValue(PASSWORD);
  await page.locator('p-button button, button[type="submit"]').first().click();
  await page.waitForURL('**/dashboard', { timeout: 30_000 });
}

/** Generates a syntactically valid CPF (11 digits, check digits). */
export function validCpf(): string {
  const n = Array.from({ length: 9 }, () => Math.floor(Math.random() * 10));
  const dv = (base: number[], startWeight: number) => {
    const sum = base.reduce((acc, d, i) => acc + d * (startWeight - i), 0);
    const r = sum % 11;
    return r < 2 ? 0 : 11 - r;
  };
  const d1 = dv(n, 10);
  const d2 = dv([...n, d1], 11);
  return [...n, d1, d2].join('');
}
