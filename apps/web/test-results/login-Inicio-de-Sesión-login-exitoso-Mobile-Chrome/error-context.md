# Instructions

- Following Playwright test failed.
- Explain why, be concise, respect Playwright best practices.
- Provide a snippet of code with the fix, if possible.

# Test info

- Name: login.spec.ts >> Inicio de Sesión >> login exitoso
- Location: e2e/login.spec.ts:4:3

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
  3  | test.describe('Inicio de Sesión', () => {
  4  |   test('login exitoso', async ({ page }) => {
  5  |     await page.goto('/inicio-sesion')
  6  | 
  7  |     await expect(page.getByText(/bienvenido de vuelta/i)).toBeVisible()
  8  | 
  9  |     await page.fill('input[type="email"]', 'test@example.com')
  10 |     await page.fill('input[type="password"]', 'password123')
  11 | 
  12 |     await page.getByRole('button', { name: /iniciar sesión/i }).click()
  13 | 
  14 |     await expect(page).toHaveURL(/\/inicio/, { timeout: 10000 })
  15 |     await page.waitForLoadState('networkidle')
> 16 |     await page.waitForSelector('nav', { timeout: 10000 })
     |                ^ TimeoutError: page.waitForSelector: Timeout 10000ms exceeded.
  17 |     await expect(page.locator('header')).toBeVisible()
  18 |   })
  19 | 
  20 |   test('navegar a recuperar contraseña', async ({ page }) => {
  21 |     await page.goto('/inicio-sesion')
  22 | 
  23 |     await page.getByText(/olvidaste tu contraseña/i).click()
  24 | 
  25 |     await expect(page).toHaveURL(/recuperar-contrasena/)
  26 |     await expect(page.getByText(/recuperar contraseña/i)).toBeVisible()
  27 |   })
  28 | })
  29 | 
```