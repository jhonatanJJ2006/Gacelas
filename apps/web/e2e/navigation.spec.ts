import { test, expect } from '@playwright/test'

test.describe('Navegación', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/inicio-sesion')
    await page.fill('input[type="email"]', 'test@example.com')
    await page.fill('input[type="password"]', 'password123')
    await page.getByRole('button', { name: /iniciar sesión/i }).click()
    await expect(page).toHaveURL(/\/inicio/, { timeout: 10000 })
    await page.waitForLoadState('networkidle')
  })

  test('navegación por bottom nav', async ({ page }) => {
    await page.getByRole('link', { name: /rutas/i }).first().click()
    await expect(page).toHaveURL(/\/rutas/)
    await expect(page.getByText(/rutas de loja/i)).toBeVisible()

    await page
      .getByRole('link', { name: /comunidad/i })
      .first()
      .click()
    await expect(page).toHaveURL(/\/comunidad/)
    await expect(page.getByText(/comunidad/i)).toBeVisible()

    await page
      .getByRole('link', { name: /perfil/i })
      .first()
      .click()
    await expect(page).toHaveURL(/\/perfil/)
    await expect(page.getByText(/jhonatan runner/i)).toBeVisible()

    await page
      .getByRole('link', { name: /inicio/i })
      .first()
      .click()
    await expect(page).toHaveURL(/\/inicio/)
  })

  test('navegación a eventos', async ({ page }) => {
    await page
      .getByRole('link', { name: /comunidad/i })
      .first()
      .click()
    await page
      .getByText(/eventos/i)
      .first()
      .click()

    await expect(page).toHaveURL(/\/comunidad\/eventos/)
    await expect(page.getByText(/carrera parque jipiro/i)).toBeVisible()
  })

  test('detalle de evento', async ({ page }) => {
    await page.goto('/comunidad/eventos')
    await page
      .getByText(/carrera parque jipiro/i)
      .first()
      .click()

    await expect(page).toHaveURL(/\/comunidad\/eventos\//)
    await expect(page.getByText(/inscribir/i)).toBeVisible()
  })
})
