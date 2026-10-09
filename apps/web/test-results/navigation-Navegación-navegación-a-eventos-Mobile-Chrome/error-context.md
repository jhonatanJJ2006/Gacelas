# Instructions

- Following Playwright test failed.
- Explain why, be concise, respect Playwright best practices.
- Provide a snippet of code with the fix, if possible.

# Test info

- Name: navigation.spec.ts >> Navegación >> navegación a eventos
- Location: e2e/navigation.spec.ts:39:3

# Error details

```
TimeoutError: locator.click: Timeout 15000ms exceeded.
Call log:
  - waiting for getByRole('link', { name: /comunidad/i }).first()

```

# Test source

```ts
  1  | import { test, expect } from '@playwright/test'
  2  |
  3  | test.describe('Navegación', () => {
  4  |   test.beforeEach(async ({ page }) => {
  5  |     await page.goto('/inicio-sesion')
  6  |     await page.fill('input[type="email"]', 'test@example.com')
  7  |     await page.fill('input[type="password"]', 'password123')
  8  |     await page.getByRole('button', { name: /iniciar sesión/i }).click()
  9  |     await expect(page).toHaveURL(/\/inicio/, { timeout: 10000 })
  10 |     await page.waitForLoadState('networkidle')
  11 |   })
  12 |
  13 |   test('navegación por bottom nav', async ({ page }) => {
  14 |     await page.getByRole('link', { name: /rutas/i }).first().click()
  15 |     await expect(page).toHaveURL(/\/rutas/)
  16 |     await expect(page.getByText(/rutas de loja/i)).toBeVisible()
  17 |
  18 |     await page
  19 |       .getByRole('link', { name: /comunidad/i })
  20 |       .first()
  21 |       .click()
  22 |     await expect(page).toHaveURL(/\/comunidad/)
  23 |     await expect(page.getByText(/comunidad/i)).toBeVisible()
  24 |
  25 |     await page
  26 |       .getByRole('link', { name: /perfil/i })
  27 |       .first()
  28 |       .click()
  29 |     await expect(page).toHaveURL(/\/perfil/)
  30 |     await expect(page.getByText(/jhonatan runner/i)).toBeVisible()
  31 |
  32 |     await page
  33 |       .getByRole('link', { name: /inicio/i })
  34 |       .first()
  35 |       .click()
  36 |     await expect(page).toHaveURL(/\/inicio/)
  37 |   })
  38 |
  39 |   test('navegación a eventos', async ({ page }) => {
  40 |     await page
  41 |       .getByRole('link', { name: /comunidad/i })
  42 |       .first()
> 43 |       .click()
     |        ^ TimeoutError: locator.click: Timeout 15000ms exceeded.
  44 |     await page
  45 |       .getByText(/eventos/i)
  46 |       .first()
  47 |       .click()
  48 |
  49 |     await expect(page).toHaveURL(/\/comunidad\/eventos/)
  50 |     await expect(page.getByText(/carrera parque jipiro/i)).toBeVisible()
  51 |   })
  52 |
  53 |   test('detalle de evento', async ({ page }) => {
  54 |     await page.goto('/comunidad/eventos')
  55 |     await page
  56 |       .getByText(/carrera parque jipiro/i)
  57 |       .first()
  58 |       .click()
  59 |
  60 |     await expect(page).toHaveURL(/\/comunidad\/eventos\//)
  61 |     await expect(page.getByText(/inscribir/i)).toBeVisible()
  62 |   })
  63 | })
  64 |
```
