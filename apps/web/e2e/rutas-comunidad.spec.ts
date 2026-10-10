import { expect, test } from '@playwright/test'
import { prepararGps, registrarse } from './ayuda'

test.describe('Rutas y comunidad', () => {
  test.beforeEach(async ({ page, context }) => {
    await prepararGps(context)
    await registrarse(page)
  })

  test('filtra rutas, guarda una y navega', async ({ page }) => {
    await page.goto('/rutas')
    await expect(page.getByRole('article')).toHaveCount(4)
    await page.getByRole('tab', { name: 'Mis Rutas' }).click()
    await expect(page.getByRole('article')).toHaveCount(1)
    await page.getByRole('tab', { name: 'Explorar' }).click()
    await page.getByRole('button', { name: 'Guardar Río Zamora' }).click()
    await page.reload()
    await page.getByRole('tab', { name: 'Guardadas (1)' }).click()
    await expect(page.getByText('Río Zamora')).toBeVisible()

    await page.goto('/rutas/buscar')
    await page.getByRole('button', { name: /Ruta Universitaria Directa/ }).click()
    await page.getByRole('button', { name: /comenzar navegación/i }).click()
    await expect(page.getByText(/siguiente giro en/i)).toBeVisible()
  })

  test('likes y eventos persisten', async ({ page }) => {
    await page.goto('/comunidad')
    const maria = page.getByRole('article').filter({ hasText: 'María López' })
    await maria.getByRole('button', { name: 'Me gusta' }).click()
    await expect(maria.getByRole('button', { name: 'Quitar me gusta' })).toBeVisible()
    await page.reload()
    await expect(maria.getByRole('button', { name: 'Quitar me gusta' })).toBeVisible()

    await page.goto('/comunidad/eventos')
    await page.getByRole('button', { name: 'Crear evento' }).click()
    const d = page.getByRole('dialog', { name: 'Crear evento' })
    await d.getByLabel('Título del evento').fill('Fondo e2e')
    await d.getByLabel('Fecha').fill('2026-12-01')
    await d.getByLabel('Hora').fill('06:30')
    await d.getByLabel('Punto de encuentro').fill('Parque Jipiro')
    await d.getByRole('button', { name: 'Crear' }).click()
    await expect(page.getByText('Fondo e2e')).toBeVisible()
  })
})
