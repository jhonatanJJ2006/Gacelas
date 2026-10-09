import { test, expect } from '@playwright/test'

test.describe('Rutas y Búsqueda', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/inicio-sesion')
    await page.fill('input[type="email"]', 'test@example.com')
    await page.fill('input[type="password"]', 'password123')
    await page.getByRole('button', { name: /iniciar sesión/i }).click()
    await expect(page).toHaveURL(/\/inicio/)
  })

  test('buscar rutas', async ({ page }) => {
    await page.getByRole('link', { name: /rutas/i }).first().click()
    await page.getByPlaceholder(/buscar rutas/i).click()

    await expect(page).toHaveURL(/\/rutas\/buscar/)
    await expect(page.getByPlaceholder(/a dónde quieres correr/i)).toBeVisible()
  })

  test('explorar rutas disponibles', async ({ page }) => {
    await page.getByRole('link', { name: /rutas/i }).first().click()

    await expect(page.getByText(/parque jipiro/i)).toBeVisible()
    await expect(page.getByText(/río zamora/i)).toBeVisible()
  })
})
