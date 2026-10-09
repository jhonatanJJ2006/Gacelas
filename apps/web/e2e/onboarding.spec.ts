import { test, expect } from '@playwright/test'

test.describe('Onboarding y Registro', () => {
  test('flujo de bienvenida -> registro -> permisos -> inicio', async ({ page }) => {
    await page.goto('/')

    await expect(page).toHaveTitle(/Gacelas/i)

    await expect(page.getByText(/bienvenid/i)).toBeVisible()

    await page.getByText(/crear cuenta|registr/i).click()

    await expect(page.getByText(/únete a la manada|crea tu cuenta/i)).toBeVisible()

    await page.fill('input[type="text"]', 'Juan Corredor')
    await page.fill('input[type="email"]', 'juan@example.com')
    await page.fill('input[type="password"]', 'password123')

    await page.getByRole('button', { name: /crear cuenta/i }).click()

    await expect(page).toHaveURL(/permisos-ubicacion|inicio/)
  })
})
