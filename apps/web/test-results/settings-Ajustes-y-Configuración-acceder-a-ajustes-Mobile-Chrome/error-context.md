# Instructions

- Following Playwright test failed.
- Explain why, be concise, respect Playwright best practices.
- Provide a snippet of code with the fix, if possible.

# Test info

- Name: settings.spec.ts >> Ajustes y Configuración >> acceder a ajustes
- Location: e2e/settings.spec.ts:13:3

# Error details

```
TimeoutError: locator.click: Timeout 15000ms exceeded.
Call log:
  - waiting for getByRole('link', { name: /perfil/i }).first()

```

# Test source

```ts
  1  | import { test, expect } from '@playwright/test'
  2  |
  3  | test.describe('Ajustes y Configuración', () => {
  4  |   test.beforeEach(async ({ page }) => {
  5  |     await page.goto('/inicio-sesion')
  6  |     await page.fill('input[type="email"]', 'test@example.com')
  7  |     await page.fill('input[type="password"]', 'password123')
  8  |     await page.getByRole('button', { name: /iniciar sesión/i }).click()
  9  |     await expect(page).toHaveURL(/\/inicio/, { timeout: 10000 })
  10 |     await page.waitForLoadState('networkidle')
  11 |   })
  12 |
  13 |   test('acceder a ajustes', async ({ page }) => {
  14 |     await page
  15 |       .getByRole('link', { name: /perfil/i })
  16 |       .first()
> 17 |       .click()
     |        ^ TimeoutError: locator.click: Timeout 15000ms exceeded.
  18 |     await page.getByText(/ajustes y preferencias/i).click()
  19 |
  20 |     await expect(page).toHaveURL(/\/ajustes/)
  21 |     await expect(page.getByText(/ajustes/i)).toBeVisible()
  22 |   })
  23 |
  24 |   test('configurar privacidad', async ({ page }) => {
  25 |     await page.goto('/ajustes')
  26 |     await page
  27 |       .getByText(/privacidad y ubicación/i)
  28 |       .first()
  29 |       .click()
  30 |
  31 |     await expect(page).toHaveURL(/\/ajustes\/privacidad/)
  32 |   })
  33 |
  34 |   test('configurar notificaciones', async ({ page }) => {
  35 |     await page.goto('/ajustes')
  36 |     await page
  37 |       .getByText(/notificaciones/i)
  38 |       .first()
  39 |       .click()
  40 |
  41 |     await expect(page).toHaveURL(/\/ajustes\/notificaciones/)
  42 |     await expect(page.getByText(/notificaciones/i)).toBeVisible()
  43 |
  44 |     const toggles = page.locator('input[type="checkbox"]')
  45 |     const count = await toggles.count()
  46 |     expect(count).toBeGreaterThan(0)
  47 |   })
  48 | })
  49 |
```
