import { expect, test, caminar, prepararGps, registrarse } from './ayuda'

test.describe('Correr con GPS simulado', () => {
  test.beforeEach(async ({ page, context }) => {
    await prepararGps(context)
    await registrarse(page)
  })

  test('graba distancia real, pausa, guarda y aparece en Perfil e Inicio', async ({
    page,
    context,
    supabase,
  }) => {
    test.setTimeout(90_000)
    await page.getByRole('link', { name: /empezar a correr/i }).click()
    await expect(page.getByText('GPS Activado')).toBeVisible()
    await page.getByRole('button', { name: 'Comenzar carrera' }).click()
    await expect(page).toHaveURL(/\/correr\/en-curso/)
    // Los botones de arriba no quedan tapados por el contenido.
    await page
      .getByRole('button', { name: 'Abrir cronómetro' })
      .click({ trial: true, timeout: 5000 })
    await page
      .getByRole('button', { name: 'Salir de la carrera' })
      .click({ trial: true, timeout: 5000 })

    await caminar(context, 6, 8)
    await expect(page.getByTestId('distancia')).not.toHaveText('0.00')

    await page.getByRole('button', { name: 'Pausar' }).click()
    await expect(page.getByText('En pausa', { exact: true })).toBeVisible()
    await page
      .getByRole('dialog', { name: 'Carrera en pausa' })
      .getByRole('button', { name: 'Reanudar' })
      .click()

    await page.getByRole('button', { name: 'Terminar carrera' }).click()
    await page.getByRole('button', { name: 'Terminar y guardar' }).click()
    await expect(page.getByText('Carrera completada')).toBeVisible()
    await expect(page.getByTestId('celebracion-racha')).toContainText('Racha de 1 día')
    await page.getByLabel('Título de la carrera').fill('Prueba e2e')
    await page.getByRole('button', { name: /guardar carrera/i }).click()

    await expect(page).toHaveURL(/\/perfil/)
    await expect.poll(() => supabase.tablas.activities?.length ?? 0).toBe(1)
    expect(supabase.tablas.activity_tracks?.[0].points).not.toEqual([])
    await expect(page.getByText('Prueba e2e')).toBeVisible()
    await expect(page.getByTestId('total-carreras')).toHaveText('1')
    await expect(page.getByTestId('racha-diaria')).toHaveText('1 día')
    await expect(page.getByTestId('mapa-calor')).toBeVisible()
    await expect(page.getByTestId('estadisticas-historicas')).toContainText('1 carreras')
    await expect(page.getByTestId('km-mensual')).toBeVisible()
    await expect(page.getByTestId('records')).toContainText('1K')
    await expect(page.getByTestId('medallero').locator('[data-lograda="true"]')).toHaveCount(1)
    await expect(page.getByTestId('total-seguidores')).toHaveText('0')
    await page.reload()
    await expect(page.getByText('Prueba e2e')).toBeVisible()
    await page.getByRole('link', { name: 'Prueba e2e' }).click()
    await expect(page.getByTestId('detalle-carrera')).toBeVisible()
    await expect(page).toHaveURL(/\/perfil\/carrera\//)
    await page.goto('/inicio')
    await expect(page.getByTestId('racha')).toContainText('1')
    await expect(page.getByTestId('semana-atletica')).toContainText('Meta semanal')
    await expect(page.getByTestId('reto-mes')).toContainText('Termina en')
    await expect(page.getByTestId('amigos-en-ruta')).toBeVisible()
  })

  test('cronómetro: vueltas, intervalos y metrónomo', async ({ page }) => {
    await page.goto('/correr/cronometro')
    await page.getByRole('button', { name: 'Iniciar' }).click()
    await page.getByRole('button', { name: /vuelta/i }).click()
    await expect(page.getByText('Vuelta 1')).toBeVisible()

    await page.getByRole('button', { name: /fartlek/i }).click()
    await page.getByRole('button', { name: 'Menos Rondas' }).click()
    await page.getByRole('button', { name: 'Iniciar intervalos' }).click()
    await expect(page.getByText('Trabajo', { exact: true })).toBeVisible()

    await page.getByRole('tab', { name: /metrónomo/i }).click()
    await page.getByRole('button', { name: '180' }).click()
    await expect(page.getByTestId('bpm')).toHaveText('180')
    await page.getByRole('button', { name: 'Iniciar metrónomo' }).click()
    await expect(page.getByRole('button', { name: 'Detener metrónomo' })).toBeVisible()
    await page.reload()
    await page.getByRole('tab', { name: /metrónomo/i }).click()
    await expect(page.getByTestId('bpm')).toHaveText('180')
  })
})
