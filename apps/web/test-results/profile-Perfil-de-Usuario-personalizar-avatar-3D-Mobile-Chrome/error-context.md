# Instructions

- Following Playwright test failed.
- Explain why, be concise, respect Playwright best practices.
- Provide a snippet of code with the fix, if possible.

# Test info

- Name: profile.spec.ts >> Perfil de Usuario >> personalizar avatar 3D
- Location: e2e/profile.spec.ts:29:3

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
  3  | test.describe('Perfil de Usuario', () => {
  4  |   test.beforeEach(async ({ page }) => {
  5  |     await page.goto('/inicio-sesion')
  6  |     await page.fill('input[type="email"]', 'test@example.com')
  7  |     await page.fill('input[type="password"]', 'password123')
  8  |     await page.getByRole('button', { name: /iniciar sesión/i }).click()
  9  |     await expect(page).toHaveURL(/\/inicio/, { timeout: 10000 })
  10 |     await page.waitForLoadState('networkidle')
  11 |   })
  12 |
  13 |   test('editar perfil', async ({ page }) => {
  14 |     await page
  15 |       .getByRole('link', { name: /perfil/i })
  16 |       .first()
  17 |       .click()
  18 |     await page.getByRole('link', { name: /edit/i }).first().click()
  19 |
  20 |     await expect(page).toHaveURL(/\/perfil\/editar/)
  21 |     await expect(page.getByText(/editar perfil/i)).toBeVisible()
  22 |
  23 |     await page.fill('input#nombre', 'Nuevo Nombre')
  24 |     await page.getByRole('button', { name: /guardar/i }).click()
  25 |
  26 |     await expect(page).toHaveURL(/\/perfil/)
  27 |   })
  28 |
  29 |   test('personalizar avatar 3D', async ({ page }) => {
  30 |     await page
  31 |       .getByRole('link', { name: /perfil/i })
  32 |       .first()
> 33 |       .click()
     |        ^ TimeoutError: locator.click: Timeout 15000ms exceeded.
  34 |     await page.getByText(/personalizar avatar/i).click()
  35 |
  36 |     await expect(page).toHaveURL(/\/perfil\/avatar/)
  37 |     await expect(page.getByText(/personalizar avatar/i)).toBeVisible()
  38 |
  39 |     const canvas = page.locator('canvas')
  40 |     await expect(canvas).toBeVisible({ timeout: 10000 })
  41 |   })
  42 | })
  43 |
```
