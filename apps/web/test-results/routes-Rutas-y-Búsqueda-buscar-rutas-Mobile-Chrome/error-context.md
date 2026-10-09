# Instructions

- Following Playwright test failed.
- Explain why, be concise, respect Playwright best practices.
- Provide a snippet of code with the fix, if possible.

# Test info

- Name: routes.spec.ts >> Rutas y Búsqueda >> buscar rutas
- Location: e2e/routes.spec.ts:14:3

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
  3  | test.describe('Rutas y Búsqueda', () => {
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
  14 |   test('buscar rutas', async ({ page }) => {
  15 |     await page.locator('nav a[href="/rutas"]').click()
  16 |     await page.getByPlaceholder(/buscar rutas/i).click()
  17 | 
  18 |     await expect(page).toHaveURL(/\/rutas\/buscar/)
  19 |     await expect(page.getByPlaceholder(/a dónde quieres correr/i)).toBeVisible()
  20 |   })
  21 | 
  22 |   test('explorar rutas disponibles', async ({ page }) => {
  23 |     await page.locator('nav a[href="/rutas"]').click()
  24 | 
  25 |     await expect(page.getByText(/parque jipiro/i)).toBeVisible()
  26 |     await expect(page.getByText(/río zamora/i)).toBeVisible()
  27 |   })
  28 | })
  29 | 
```