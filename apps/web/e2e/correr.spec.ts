import { test, expect } from '@playwright/test'

test.describe('Funcionalidad de Correr', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/inicio-sesion')
    await page.fill('input[type="email"]', 'test@example.com')
    await page.fill('input[type="password"]', 'password123')
    await page.getByRole('button', { name: /iniciar sesión/i }).click()
    await expect(page).toHaveURL(/\/inicio/, { timeout: 10000 })
    await page.waitForLoadState('networkidle')
    await page.waitForSelector('nav', { timeout: 10000 })
  })

  test('iniciar carrera -> cronometro -> detener', async ({ page }) => {
    await page.getByRole('link', { name: /empezar a correr/i }).click()

    await expect(page.getByText(/listo para correr/i)).toBeVisible()

    await page.locator('button').filter({ hasText: 'play_arrow' }).click()

    await expect(page).toHaveURL(/\/correr\/en-curso/)

    await page.click('text=Cronómetro')

    await expect(page).toHaveURL(/\/correr\/cronometro/)
    await expect(page.getByText(/cronómetro profesional/i)).toBeVisible()

    await page.getByRole('button', { name: /pause/i }).click()
    await expect(page.getByText(/play_arrow/)).toBeVisible()

    await page.getByRole('button', { name: /play_arrow/i }).click()
    await expect(page.getByText(/pause/)).toBeVisible()

    const stopButton = page.locator('text=Fin').first()
    await stopButton.click()

    await expect(page).toHaveURL(/\/correr\/resumen/)
  })
})
