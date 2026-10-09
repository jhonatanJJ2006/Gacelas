# Instructions

- Following Playwright test failed.
- Explain why, be concise, respect Playwright best practices.
- Provide a snippet of code with the fix, if possible.

# Test info

- Name: navigation.spec.ts >> Navegación >> navegación a eventos
- Location: e2e/navigation.spec.ts:31:3

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
  3  | test.describe('Navegación', () => {
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
  14 |   test('navegación por bottom nav', async ({ page }) => {
  15 |     await page.locator('nav a[href="/rutas"]').click()
  16 |     await expect(page).toHaveURL(/\/rutas/)
  17 |     await expect(page.getByText(/rutas de loja/i)).toBeVisible()
  18 | 
  19 |     await page.locator('nav a[href="/comunidad"]').click()
  20 |     await expect(page).toHaveURL(/\/comunidad/)
  21 |     await expect(page.getByText(/comunidad/i)).toBeVisible()
  22 | 
  23 |     await page.locator('nav a[href="/perfil"]').click()
  24 |     await expect(page).toHaveURL(/\/perfil/)
  25 |     await expect(page.getByText(/jhonatan runner/i)).toBeVisible()
  26 | 
  27 |     await page.locator('nav a[href="/inicio"]').click()
  28 |     await expect(page).toHaveURL(/\/inicio/)
  29 |   })
  30 | 
  31 |   test('navegación a eventos', async ({ page }) => {
  32 |     await page.locator('nav a[href="/comunidad"]').click()
  33 |     await page.getByText(/eventos/i).first().click()
  34 | 
  35 |     await expect(page).toHaveURL(/\/comunidad\/eventos/)
  36 |     await expect(page.getByText(/carrera parque jipiro/i)).toBeVisible()
  37 |   })
  38 | 
  39 |   test('detalle de evento', async ({ page }) => {
  40 |     await page.goto('/comunidad/eventos')
  41 |     await page.getByText(/carrera parque jipiro/i).first().click()
  42 | 
  43 |     await expect(page).toHaveURL(/\/comunidad\/eventos\//)
  44 |     await expect(page.getByText(/inscribir/i)).toBeVisible()
  45 |   })
  46 | })
  47 | 
```