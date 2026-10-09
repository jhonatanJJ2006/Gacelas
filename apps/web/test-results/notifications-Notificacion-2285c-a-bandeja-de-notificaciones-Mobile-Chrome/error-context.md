# Instructions

- Following Playwright test failed.
- Explain why, be concise, respect Playwright best practices.
- Provide a snippet of code with the fix, if possible.

# Test info

- Name: notifications.spec.ts >> Notificaciones >> acceder a bandeja de notificaciones
- Location: e2e/notifications.spec.ts:13:3

# Error details

```
TimeoutError: locator.click: Timeout 15000ms exceeded.
Call log:
  - waiting for getByLabel(/notificaciones/i)

```

# Test source

```ts
  1  | import { test, expect } from '@playwright/test'
  2  |
  3  | test.describe('Notificaciones', () => {
  4  |   test.beforeEach(async ({ page }) => {
  5  |     await page.goto('/inicio-sesion')
  6  |     await page.fill('input[type="email"]', 'test@example.com')
  7  |     await page.fill('input[type="password"]', 'password123')
  8  |     await page.getByRole('button', { name: /iniciar sesión/i }).click()
  9  |     await expect(page).toHaveURL(/\/inicio/, { timeout: 10000 })
  10 |     await page.waitForLoadState('networkidle')
  11 |   })
  12 |
  13 |   test('acceder a bandeja de notificaciones', async ({ page }) => {
> 14 |     await page.getByLabel(/notificaciones/i).click()
     |                                              ^ TimeoutError: locator.click: Timeout 15000ms exceeded.
  15 |
  16 |     await expect(page).toHaveURL(/\/notificaciones/)
  17 |   })
  18 | })
  19 |
```
