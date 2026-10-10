import { test, expect } from '@playwright/test'

test.describe('Navegación', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/inicio-sesion')
    await page.fill('input[type="email"]', 'test@example.com')
    await page.fill('input[type="password"]', 'password123')
    await page.getByRole('button', { name: /iniciar sesión/i }).click()
    await expect(page).toHaveURL(/\/inicio/, { timeout: 10000 })
    await page.waitForLoadState('networkidle')
    await page.waitForSelector('nav', { timeout: 10000 })
  })

  test('navegación por bottom nav', async ({ page }) => {
    await page.locator('nav a[href="/rutas"]').click()
    await expect(page).toHaveURL(/\/rutas/)
    await expect(page.getByText(/rutas de loja/i)).toBeVisible()

    await page.locator('nav a[href="/comunidad"]').click()
    await expect(page).toHaveURL(/\/comunidad/)
    await expect(page.getByRole('heading', { name: /comunidad/i })).toBeVisible()

    await page.locator('nav a[href="/perfil"]').click()
    await expect(page).toHaveURL(/\/perfil/)
    await expect(page.getByText(/jhonatan runner/i)).toBeVisible()

    await page.locator('nav a[href="/inicio"]').click()
    await expect(page).toHaveURL(/\/inicio/)
  })

  test('navegación a eventos', async ({ page }) => {
    await page.locator('nav a[href="/comunidad"]').click()
    await page.getByRole('button', { name: /^eventos$/i }).click()

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
    await expect(page.getByRole('button', { name: /inscribirme al evento/i })).toBeVisible()
  })
})
