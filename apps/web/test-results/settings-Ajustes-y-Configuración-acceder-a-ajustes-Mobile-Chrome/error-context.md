# Instructions

- Following Playwright test failed.
- Explain why, be concise, respect Playwright best practices.
- Provide a snippet of code with the fix, if possible.

# Test info

- Name: settings.spec.ts >> Ajustes y Configuración >> acceder a ajustes
- Location: e2e/settings.spec.ts:14:3

# Error details

```
TimeoutError: page.waitForSelector: Timeout 10000ms exceeded.
Call log:
  - waiting for locator('nav') to be visible

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
> 11 |     await page.waitForSelector('nav', { timeout: 10000 })
     |                ^ TimeoutError: page.waitForSelector: Timeout 10000ms exceeded.
  12 |   })
  13 | 
  14 |   test('acceder a ajustes', async ({ page }) => {
  15 |     await page.locator('nav a[href="/perfil"]').click()
  16 |     await page.getByText(/ajustes y preferencias/i).click()
  17 | 
  18 |     await expect(page).toHaveURL(/\/ajustes/)
  19 |     await expect(page.getByText(/ajustes/i)).toBeVisible()
  20 |   })
  21 | 
  22 |   test('configurar privacidad', async ({ page }) => {
  23 |     await page.goto('/ajustes')
  24 |     await page.getByText(/privacidad y ubicación/i).first().click()
  25 | 
  26 |     await expect(page).toHaveURL(/\/ajustes\/privacidad/)
  27 |   })
  28 | 
  29 |   test('configurar notificaciones', async ({ page }) => {
  30 |     await page.goto('/ajustes')
  31 |     await page.getByText(/notificaciones/i).first().click()
  32 | 
  33 |     await expect(page).toHaveURL(/\/ajustes\/notificaciones/)
  34 |     await expect(page.getByRole('heading', { name: /notificaciones/i }).first()).toBeVisible()
  35 | 
  36 |     const toggles = page.locator('input[type="checkbox"]')
  37 |     const count = await toggles.count()
  38 |     expect(count).toBeGreaterThan(0)
  39 |   })
  40 | })
  41 | 
```