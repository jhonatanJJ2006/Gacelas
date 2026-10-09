import { test, expect } from '@playwright/test'

test.describe('Inicio de Sesión', () => {
  test('login exitoso', async ({ page }) => {
    await page.goto('/inicio-sesion')

    await expect(page.getByText(/bienvenido de vuelta/i)).toBeVisible()

    await page.fill('input[type="email"]', 'test@example.com')
    await page.fill('input[type="password"]', 'password123')

    await page.getByRole('button', { name: /iniciar sesión/i }).click()

    await expect(page).toHaveURL(/\/inicio/, { timeout: 10000 })
    await page.waitForLoadState('networkidle')
    await page.waitForSelector('nav', { timeout: 10000 })
    await expect(page.locator('header')).toBeVisible()
  })

  test('navegar a recuperar contraseña', async ({ page }) => {
    await page.goto('/inicio-sesion')

    await page.getByText(/olvidaste tu contraseña/i).click()

    await expect(page).toHaveURL(/recuperar-contrasena/)
    await expect(page.getByText(/recuperar contraseña/i)).toBeVisible()
  })
})
