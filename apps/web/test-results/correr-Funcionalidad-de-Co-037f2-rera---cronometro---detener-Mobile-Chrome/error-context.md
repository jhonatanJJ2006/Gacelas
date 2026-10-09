# Instructions

- Following Playwright test failed.
- Explain why, be concise, respect Playwright best practices.
- Provide a snippet of code with the fix, if possible.

# Test info

- Name: correr.spec.ts >> Funcionalidad de Correr >> iniciar carrera -> cronometro -> detener
- Location: e2e/correr.spec.ts:13:3

# Error details

```
TimeoutError: locator.click: Timeout 15000ms exceeded.
Call log:
  - waiting for getByRole('link', { name: /empezar a correr/i })

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
  11 |   })
  12 |
  13 |   test('iniciar carrera -> cronometro -> detener', async ({ page }) => {
> 14 |     await page.getByRole('link', { name: /empezar a correr/i }).click()
     |                                                                 ^ TimeoutError: locator.click: Timeout 15000ms exceeded.
  15 |
  16 |     await expect(page.getByText(/listo para correr/i)).toBeVisible()
  17 |
  18 |     await page
  19 |       .getByRole('button')
  20 |       .filter({ has: page.locator('text=play_arrow') })
  21 |       .click()
  22 |
  23 |     await expect(page).toHaveURL(/\/correr\/en-curso/)
  24 |
  25 |     await page.click('text=Cronómetro')
  26 |
  27 |     await expect(page).toHaveURL(/\/correr\/cronometro/)
  28 |     await expect(page.getByText(/cronómetro profesional/i)).toBeVisible()
  29 |
  30 |     await page.getByRole('button', { name: /pause/i }).click()
  31 |     await expect(page.getByText(/play_arrow/)).toBeVisible()
  32 |
  33 |     await page.getByRole('button', { name: /play_arrow/i }).click()
  34 |     await expect(page.getByText(/pause/)).toBeVisible()
  35 |
  36 |     const stopButton = page.locator('text=Fin').first()
  37 |     await stopButton.click()
  38 |
  39 |     await expect(page).toHaveURL(/\/correr\/resumen/)
  40 |   })
  41 | })
  42 |
```
