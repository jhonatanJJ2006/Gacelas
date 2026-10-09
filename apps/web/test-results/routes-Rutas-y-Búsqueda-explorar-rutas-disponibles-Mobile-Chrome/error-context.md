# Instructions

- Following Playwright test failed.
- Explain why, be concise, respect Playwright best practices.
- Provide a snippet of code with the fix, if possible.

# Test info

- Name: routes.spec.ts >> Rutas y Búsqueda >> explorar rutas disponibles
- Location: e2e/routes.spec.ts:21:3

# Error details

```
TimeoutError: locator.click: Timeout 15000ms exceeded.
Call log:
  - waiting for getByRole('link', { name: /rutas/i }).first()

```

# Test source

```ts
  1  | import { test, expect } from '@playwright/test'
  2  |
  3  | test.describe('Rutas y Búsqueda', () => {
  4  |   test.beforeEach(async ({ page }) => {
  5  |     await page.goto('/inicio-sesion')
  6  |     await page.fill('input[type="email"]', 'test@example.com')
  7  |     await page.fill('input[type="password"]', 'password123')
  8  |     await page.getByRole('button', { name: /iniciar sesión/i }).click()
  9  |     await expect(page).toHaveURL(/\/inicio/, { timeout: 10000 })
  10 |     await page.waitForLoadState('networkidle')
  11 |   })
  12 |
  13 |   test('buscar rutas', async ({ page }) => {
  14 |     await page.getByRole('link', { name: /rutas/i }).first().click()
  15 |     await page.getByPlaceholder(/buscar rutas/i).click()
  16 |
  17 |     await expect(page).toHaveURL(/\/rutas\/buscar/)
  18 |     await expect(page.getByPlaceholder(/a dónde quieres correr/i)).toBeVisible()
  19 |   })
  20 |
  21 |   test('explorar rutas disponibles', async ({ page }) => {
> 22 |     await page.getByRole('link', { name: /rutas/i }).first().click()
     |                                                              ^ TimeoutError: locator.click: Timeout 15000ms exceeded.
  23 |
  24 |     await expect(page.getByText(/parque jipiro/i)).toBeVisible()
  25 |     await expect(page.getByText(/río zamora/i)).toBeVisible()
  26 |   })
  27 | })
  28 |
```
