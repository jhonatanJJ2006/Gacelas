import { expect, test } from '@playwright/test'
import { caminar, prepararGps, registrarse } from './ayuda'

test.describe('Correr con GPS simulado', () => {
  test.beforeEach(async ({ page, context }) => {
    await prepararGps(context)
    await registrarse(page)
  })

  test('graba distancia real, pausa, guarda y aparece en Perfil e Inicio', async ({
    page,
    context,
  }) => {
    test.setTimeout(90_000)
    await page.getByRole('link', { name: /empezar a correr/i }).click()
    await expect(page.getByText('GPS Activado')).toBeVisible()
    await page.getByRole('button', { name: 'Comenzar carrera' }).click()
    await expect(page).toHaveURL(/\/correr\/en-curso/)

    await caminar(context, 6, 8)
    await expect(page.getByTestId('distancia')).not.toHaveText('0.00')

    await page.getByRole('button', { name: 'Pausar' }).click()
    await expect(page.getByText('En pausa')).toBeVisible()
    await page.getByRole('button', { name: 'Reanudar' }).click()

    await page.getByRole('button', { name: 'Terminar carrera' }).click()
    await expect(page.getByText('Carrera completada')).toBeVisible()
    await page.getByLabel('Título de la carrera').fill('Prueba e2e')
    await page.getByRole('button', { name: /guardar carrera/i }).click()

    await expect(page).toHaveURL(/\/perfil/)
    await expect(page.getByText('Prueba e2e')).toBeVisible()
    await expect(page.getByTestId('total-carreras')).toHaveText('1')
    await page.reload()
    await expect(page.getByText('Prueba e2e')).toBeVisible()
    await page.goto('/inicio')
    await expect(page.getByTestId('racha')).toContainText('1')
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
