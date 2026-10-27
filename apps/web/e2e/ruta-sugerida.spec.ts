import { expect, registrarse, test } from './ayuda'

test.describe('Ruta sugerida para hoy', () => {
  test('muestra la ruta segura que elige el servidor', async ({ page, supabase }) => {
    await registrarse(page)
    const tarjeta = page.getByTestId('ruta-sugerida')
    await expect(tarjeta.getByText('Parque Lineal La Tebaida', { exact: true })).toBeVisible()
    await expect(tarjeta.getByTestId('ruta-sugerida-segura')).toContainText(
      'Zona segura a esta hora'
    )
    expect(supabase.peticiones).toContain('POST /rest/v1/rpc/ruta_sugerida_hoy')
    await tarjeta.evaluate((e) => e.scrollIntoView({ block: 'center' }))
    await tarjeta.screenshot({ path: 'test-results/ruta-sugerida-web.png' })
    await page.screenshot({ path: 'test-results/ruta-sugerida-web-inicio.png', fullPage: true })
  })

  test('sin ruta segura disponible muestra el motivo', async ({ page, supabase }) => {
    supabase.rutaSugerida = {
      estado: 'sin_ruta',
      dia: '2026-10-26',
      motivo: 'Ya corriste o te sugerimos todas las rutas seguras en los últimos 3 días.',
    }
    await registrarse(page)
    await expect(page.getByTestId('ruta-sugerida-vacia')).toContainText('últimos 3 días')
    await page.setViewportSize({ width: 412, height: 2400 })
    await page
      .getByTestId('ruta-sugerida')
      .screenshot({ path: 'test-results/ruta-sugerida-web-vacia.png' })
  })
})
