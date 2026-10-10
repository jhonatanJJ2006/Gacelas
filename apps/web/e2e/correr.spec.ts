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

    await page.getByRole('button', { name: 'pause' }).click()
    await expect(page.getByRole('button', { name: 'play_arrow' })).toBeVisible()
    await page.getByRole('button', { name: 'play_arrow' }).click()
    await expect(page.getByRole('button', { name: 'pause' })).toBeVisible()

    await page.getByRole('button', { name: 'stop' }).click()
    await expect(page).toHaveURL(/\/correr\/resumen/)
  })

  test('cronómetro ampliado: pausar y reanudar', async ({ page }) => {
    await page.goto('/correr/cronometro')
    await page.getByRole('button', { name: 'pause' }).click()
    await expect(page.getByRole('button', { name: 'play_arrow' })).toBeVisible()
    await page.getByRole('button', { name: 'play_arrow' }).click()
    await expect(page.getByRole('button', { name: 'pause' })).toBeVisible()
    await expect(page.getByRole('button', { name: /fin/i })).toBeVisible()
  })
})
