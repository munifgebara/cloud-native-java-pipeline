import { test, expect } from '@playwright/test';
import { login, watch, validCpf } from './_helpers';

/**
 * Data round-trip test: creates a person with address fields through the UI, then reopens the
 * record and asserts the fields are populated with what was typed.
 *
 * This is the test that catches front/back JSON data-key mismatches (e.g. cep/zipCode,
 * cidade/city): if the front sends or reads the wrong key, the values would not round-trip and
 * the assertions fail. The navigation-only smoke test could not catch that.
 */
test('person create -> reopen round-trips all fields (catches data-key drift)', async ({ page }) => {
  const apiFailures: string[] = [];
  const consoleErrors: string[] = [];
  watch(page, apiFailures, consoleErrors);

  const suffix = Date.now().toString();
  const data = {
    name: `E2E Person ${suffix}`,
    taxId: validCpf(),
    email: `e2e-${suffix}@example.local`,
    primaryPhone: '11988887777',
    zipCode: '01310100',
    state: 'SP',
    address: 'Av Paulista 1000',
    complement: 'Sala 42',
    neighborhood: 'Bela Vista',
    city: 'Sao Paulo',
  };

  await login(page);

  // Go to People and open the "new" form (pi-plus icon, locale-independent).
  await page.locator('a[href$="/people"]').first().click();
  await page.waitForURL('**/people', { timeout: 15_000 });
  await page.locator('button:has(.pi-plus)').first().click();
  await page.waitForURL('**/people/**', { timeout: 15_000 });

  // Fill the form by field id and save (pi-save icon).
  for (const [id, value] of Object.entries(data)) {
    await page.locator(`#${id}`).fill(value);
  }
  await page.locator('button:has(.pi-save)').click();
  await page.waitForURL(/\/people$/, { timeout: 20_000 });

  // Open the created person's edit form via its row's edit link.
  const row = page.locator('tr', { hasText: data.name });
  await expect(row).toHaveCount(1, { timeout: 15_000 });
  await row.locator('button:has(.pi-pencil)').click(); // edit action (routerLink button)
  await page.waitForURL('**/edit', { timeout: 15_000 });

  // Assert every field round-tripped (front->backend->front with matching JSON keys).
  for (const [id, value] of Object.entries(data)) {
    await expect(page.locator(`#${id}`), `field "${id}" did not round-trip`).toHaveValue(value, {
      timeout: 10_000,
    });
  }

  // Best-effort cleanup: delete the person created by the test (keeps shared/remote env clean).
  const id = page.url().match(/people\/([0-9a-fA-F-]+)\/edit/)?.[1];
  const token = await page.evaluate(() => localStorage.getItem('stella_access_token'));
  if (id && token) {
    await page.request.delete(`/api/v0/people/${id}`, { headers: { Authorization: `Bearer ${token}` } }).catch(() => {});
  }

  expect(apiFailures, `Failed API calls:\n${apiFailures.join('\n')}`).toEqual([]);
  expect(consoleErrors, `Console errors:\n${consoleErrors.join('\n')}`).toEqual([]);
});
