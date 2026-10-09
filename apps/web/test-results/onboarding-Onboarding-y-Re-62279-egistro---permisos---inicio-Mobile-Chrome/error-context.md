# Instructions

- Following Playwright test failed.
- Explain why, be concise, respect Playwright best practices.
- Provide a snippet of code with the fix, if possible.

# Test info

- Name: onboarding.spec.ts >> Onboarding y Registro >> flujo de bienvenida -> registro -> permisos -> inicio
- Location: e2e/onboarding.spec.ts:4:3

# Error details

```
TimeoutError: locator.click: Timeout 15000ms exceeded.
Call log:
  - waiting for getByRole('button', { name: /crear cuenta/i })

```

# Test source

```ts
  1  | import { test, expect } from '@playwright/test'
  2  |
  3  | test.describe('Onboarding y Registro', () => {
  4  |   test('flujo de bienvenida -> registro -> permisos -> inicio', async ({ page }) => {
  5  |     await page.goto('/')
  6  |
  7  |     await expect(page).toHaveTitle(/Gacelas/i)
  8  |
  9  |     await expect(page.getByText(/bienvenid/i)).toBeVisible()
  10 |
> 11 |     await page.getByRole('button', { name: /crear cuenta/i }).click()
     |                                                               ^ TimeoutError: locator.click: Timeout 15000ms exceeded.
  12 |
  13 |     await expect(page.getByText(/únete a la manada|crea tu cuenta/i)).toBeVisible()
  14 |
  15 |     await page.fill('input[type="text"]', 'Juan Corredor')
  16 |     await page.fill('input[type="email"]', 'juan@example.com')
  17 |     await page.fill('input[type="password"]', 'password123')
  18 |
  19 |     await page.getByRole('button', { name: /crear cuenta/i }).click()
  20 |
  21 |     await expect(page).toHaveURL(/permisos-ubicacion|inicio/)
  22 |   })
  23 | })
  24 |
```
