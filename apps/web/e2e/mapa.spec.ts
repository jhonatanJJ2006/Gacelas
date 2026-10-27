import type { Page } from '@playwright/test'
import { expect, test, prepararGps, registrarse, responderCompartirEnVivo } from './ayuda'

/**
 * Pantallas de mapa: "Reportar incidente" va al borde derecho (no centrado) y hay una flecha
 * de retroceso arriba a la izquierda. Con QA_CAPTURAS=<carpeta> guarda capturas.
 */
const CARPETA = process.env.QA_CAPTURAS

async function capturar(page: Page, nombre: string) {
  if (CARPETA) await page.screenshot({ path: `${CARPETA}/web-${nombre}.png` })
}

async function reportarAlLado(page: Page) {
  const b = page.getByTestId('reportar-incidente')
  await expect(b).toBeVisible()
  await expect(b).toHaveAccessibleName('Reportar incidente')
  const caja = (await b.boundingBox())!
  const ancho = page.viewportSize()!.width
  // Borde derecho: su centro queda en el ultimo cuarto de la pantalla.
  expect(caja.x + caja.width / 2).toBeGreaterThan(ancho * 0.75)
}

test.describe('Pantallas de mapa en 390x844', () => {
  test.use({ viewport: { width: 390, height: 844 } })
  test.beforeEach(async ({ page, context }) => {
    await prepararGps(context)
    await registrarse(page)
  })

  test('carrera: reportar al lado y flecha atras sin perder la carrera', async ({ page }) => {
    await page.getByRole('link', { name: /empezar a correr/i }).click()
    await page.getByRole('button', { name: 'Comenzar carrera' }).click()
    await page.getByRole('button', { name: 'Aceptar y empezar' }).click()
    await expect(page).toHaveURL(/\/correr\/en-curso/)
    await responderCompartirEnVivo(page)
    await page.waitForTimeout(500)
    await capturar(page, 'carrera')
    await reportarAlLado(page)
    const volver = page.getByTestId('volver-mapa')
    const caja = (await volver.boundingBox())!
    expect(caja.x).toBeLessThan(80)
    expect(caja.y).toBeLessThan(80)
    await volver.click()
    await expect(page).toHaveURL(/\/inicio$/)
    // La carrera sigue activa: Correr lleva de vuelta a la carrera en curso.
    await page.getByRole('link', { name: 'Correr', exact: true }).click()
    await expect(page).toHaveURL(/\/correr\/en-curso/)
    await responderCompartirEnVivo(page)
  })

  test('mapa en vivo: reportar al lado y volver atras', async ({ page }) => {
    await page.goto('/comunidad/mapa')
    await page.waitForTimeout(500)
    await capturar(page, 'mapa-vivo')
    await reportarAlLado(page)
    await page.getByRole('link', { name: 'Volver' }).click()
    await expect(page).toHaveURL(/\/comunidad\/en-vivo$/)
  })
})
