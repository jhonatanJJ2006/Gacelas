# Instructions

- Following Playwright test failed.
- Explain why, be concise, respect Playwright best practices.
- Provide a snippet of code with the fix, if possible.

# Test info

- Name: correr.spec.ts >> Funcionalidad de Correr >> iniciar carrera -> cronometro -> detener
- Location: e2e/correr.spec.ts:14:3

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
  3  | test.describe('Funcionalidad de Correr', () => {
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
  14 |   test('iniciar carrera -> cronometro -> detener', async ({ page }) => {
  15 |     await page.getByRole('link', { name: /empezar a correr/i }).click()
  16 | 
  17 |     await expect(page.getByText(/listo para correr/i)).toBeVisible()
  18 | 
  19 |     await page.locator('button').filter({ hasText: 'play_arrow' }).click()
  20 | 
  21 |     await expect(page).toHaveURL(/\/correr\/en-curso/)
  22 | 
  23 |     await page.click('text=Cronómetro')
  24 | 
  25 |     await expect(page).toHaveURL(/\/correr\/cronometro/)
  26 |     await expect(page.getByText(/cronómetro profesional/i)).toBeVisible()
  27 | 
  28 |     await page.getByRole('button', { name: /pause/i }).click()
  29 |     await expect(page.getByText(/play_arrow/)).toBeVisible()
  30 | 
  31 |     await page.getByRole('button', { name: /play_arrow/i }).click()
  32 |     await expect(page.getByText(/pause/)).toBeVisible()
  33 | 
  34 |     const stopButton = page.locator('text=Fin').first()
  35 |     await stopButton.click()
  36 | 
  37 |     await expect(page).toHaveURL(/\/correr\/resumen/)
  38 |   })
  39 | })
  40 | 
```