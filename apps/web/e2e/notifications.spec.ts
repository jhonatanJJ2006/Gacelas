import { test, expect } from '@playwright/test'

test.describe('Notificaciones', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/inicio-sesion')
    await page.fill('input[type="email"]', 'test@example.com')
    await page.fill('input[type="password"]', 'password123')
    await page.getByRole('button', { name: /iniciar sesión/i }).click()
    await expect(page).toHaveURL(/\/inicio/, { timeout: 10000 })
    await page.waitForLoadState('networkidle')
    await page.waitForSelector('nav', { timeout: 10000 })
  })

  test('acceder a bandeja de notificaciones', async ({ page }) => {
    await page.locator('header a[href="/notificaciones"]').click()

    await expect(page).toHaveURL(/\/notificaciones/)
  })
})
