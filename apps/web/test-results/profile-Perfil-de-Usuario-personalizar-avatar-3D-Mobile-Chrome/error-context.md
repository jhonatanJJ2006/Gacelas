# Instructions

- Following Playwright test failed.
- Explain why, be concise, respect Playwright best practices.
- Provide a snippet of code with the fix, if possible.

# Test info

- Name: profile.spec.ts >> Perfil de Usuario >> personalizar avatar 3D
- Location: e2e/profile.spec.ts:27:3

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
  3  | test.describe('Perfil de Usuario', () => {
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
  14 |   test('editar perfil', async ({ page }) => {
  15 |     await page.locator('nav a[href="/perfil"]').click()
  16 |     await page.getByRole('link', { name: /edit/i }).first().click()
  17 | 
  18 |     await expect(page).toHaveURL(/\/perfil\/editar/)
  19 |     await expect(page.getByText(/editar perfil/i)).toBeVisible()
  20 | 
  21 |     await page.fill('input#nombre', 'Nuevo Nombre')
  22 |     await page.getByRole('button', { name: /guardar/i }).click()
  23 | 
  24 |     await expect(page).toHaveURL(/\/perfil/)
  25 |   })
  26 | 
  27 |   test('personalizar avatar 3D', async ({ page }) => {
  28 |     await page.locator('nav a[href="/perfil"]').click()
  29 |     await page.getByText(/personalizar avatar/i).click()
  30 | 
  31 |     await expect(page).toHaveURL(/\/perfil\/avatar/)
  32 |     await expect(page.getByText(/personalizar avatar/i)).toBeVisible()
  33 | 
  34 |     const canvas = page.locator('canvas')
  35 |     await expect(canvas).toBeVisible({ timeout: 10000 })
  36 |   })
  37 | })
  38 | 
```