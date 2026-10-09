import { test, expect } from '@playwright/test'

test.describe('Ajustes y Configuración', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/inicio-sesion')
    await page.fill('input[type="email"]', 'test@example.com')
    await page.fill('input[type="password"]', 'password123')
    await page.getByRole('button', { name: /iniciar sesión/i }).click()
    await expect(page).toHaveURL(/\/inicio/, { timeout: 10000 })
    await page.waitForLoadState('networkidle')
    await page.waitForSelector('nav', { timeout: 10000 })
  })

  test('acceder a ajustes', async ({ page }) => {
    await page.locator('nav a[href="/perfil"]').click()
    await page.getByText(/ajustes y preferencias/i).click()

    await expect(page).toHaveURL(/\/ajustes/)
    await expect(page.getByText(/ajustes/i)).toBeVisible()
  })

  test('configurar privacidad', async ({ page }) => {
    await page.goto('/ajustes')
    await page.getByText(/privacidad y ubicación/i).first().click()

    await expect(page).toHaveURL(/\/ajustes\/privacidad/)
  })

  test('configurar notificaciones', async ({ page }) => {
    await page.goto('/ajustes')
    await page.getByText(/notificaciones/i).first().click()

    await expect(page).toHaveURL(/\/ajustes\/notificaciones/)
    await expect(page.getByRole('heading', { name: /notificaciones/i }).first()).toBeVisible()

    const toggles = page.locator('input[type="checkbox"]')
    const count = await toggles.count()
    expect(count).toBeGreaterThan(0)
  })
})
