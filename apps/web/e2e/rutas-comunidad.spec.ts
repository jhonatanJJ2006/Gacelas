import { expect, test, prepararGps, registrarse } from './ayuda'

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
    await expect(page.getByRole('button', { name: 'Quitar Río Zamora de guardadas' })).toBeVisible()
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

    // Estilo Stitch: nivel, desnivel, compartir (repost) y comentario desde la hoja con vista previa.
    await expect(maria.getByTestId('insignia-nivel')).toBeVisible()
    await expect(maria.getByTestId('desnivel-post')).toContainText('+42')
    await maria.getByRole('button', { name: 'Compartir en mi perfil' }).click()
    await expect(maria.getByRole('button', { name: 'Dejar de compartir' })).toContainText('1')
    await maria.getByRole('button', { name: 'Comentar', exact: true }).click()
    const hoja = page.getByRole('dialog', { name: 'Comentar: Carrera matutina' })
    await hoja.getByLabel('Comentario').fill('Gran ritmo')
    await hoja.getByRole('button', { name: 'Publicar' }).click()
    await page.reload()
    await expect(maria.getByTestId('vista-comentario')).toContainText('Gran ritmo')
    await expect(maria.getByRole('button', { name: 'Dejar de compartir' })).toBeVisible()

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
