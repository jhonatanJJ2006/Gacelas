import { test, expect } from '@playwright/test'

test.describe('Rutas y Búsqueda', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/inicio-sesion')
    await page.fill('input[type="email"]', 'test@example.com')
    await page.fill('input[type="password"]', 'password123')
    await page.getByRole('button', { name: /iniciar sesión/i }).click()
    await expect(page).toHaveURL(/\/inicio/, { timeout: 10000 })
    await page.waitForLoadState('networkidle')
    await page.waitForSelector('nav', { timeout: 10000 })
  })

  test('buscar rutas', async ({ page }) => {
    await page.locator('nav a[href="/rutas"]').click()
    await page.getByPlaceholder(/buscar rutas/i).click()

    await expect(page).toHaveURL(/\/rutas\/buscar/)
    await expect(page.getByPlaceholder(/a dónde quieres correr/i)).toBeVisible()
  })

  test('explorar rutas disponibles', async ({ page }) => {
    await page.locator('nav a[href="/rutas"]').click()

    await expect(page).toHaveURL(/\/rutas$/)
    await expect(page.getByRole('heading', { name: /rutas de loja/i })).toBeVisible()
    await expect(page.getByText('Parque Jipiro', { exact: true })).toBeVisible()
    await expect(page.getByText('Río Zamora', { exact: true })).toBeVisible()
  })
})
