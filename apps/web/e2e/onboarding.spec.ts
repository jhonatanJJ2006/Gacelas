import { test, expect } from '@playwright/test'

test.describe('Onboarding y Registro', () => {
  test('flujo de bienvenida -> registro -> permisos -> inicio', async ({ page }) => {
    await page.goto('/bienvenida')

    await expect(page).toHaveTitle(/Gacelas/i)

    await expect(page.getByRole('heading', { name: /corre con la manada de loja/i })).toBeVisible()

    await page.getByRole('button', { name: /crear cuenta/i }).click()

    await expect(page).toHaveURL(/\/registro/)
    await expect(page.getByRole('heading', { name: /únete a la manada/i })).toBeVisible()

    await page.fill('input[type="text"]', 'Juan Corredor')
    await page.fill('input[type="email"]', 'juan@example.com')
    await page.fill('input[type="password"]', 'password123')

    await page.getByRole('button', { name: /crear cuenta/i }).click()

    await expect(page).toHaveURL(/permisos-ubicacion|inicio/)
  })
})
