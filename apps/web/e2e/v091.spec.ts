import { expect, test, prepararGps, registrarse } from './ayuda'

// v0.9.1: compartir en vivo sin boton en el mapa (modal una sola vez) y escenario morado en Inicio.
const dir = process.env.GACELAS_SHOTS ?? 'test-results/v091'

test.describe('v0.9.1', () => {
  test.beforeEach(async ({ page, context }) => {
    await page.setViewportSize({ width: 393, height: 873 })
    await prepararGps(context)
    await registrarse(page)
  })

  test('Inicio usa el escenario morado del Figma C2', async ({ page }) => {
    const esc = page.getByTestId('inicio-escenario')
    await expect(esc).toBeVisible()
    const fondo = await esc.evaluate((el) => getComputedStyle(el).backgroundImage)
    expect(fondo).toContain('radial-gradient')
    await page.screenshot({ path: `${dir}/web-inicio-393x873.png` })
  })

  test('el mapa no tiene boton de compartir y el modal sale una sola vez', async ({ page }) => {
    await page.getByRole('link', { name: /empezar a correr/i }).click()
    await page.getByRole('button', { name: 'Comenzar carrera' }).click()
    await page.getByRole('button', { name: 'Aceptar y empezar' }).click()
    await expect(page).toHaveURL(/\/correr\/en-curso/)
    await expect(page.getByTestId('compartir-en-vivo')).toHaveCount(0)
    const modal = page.getByRole('dialog', { name: '¿Compartir tu carrera en vivo?' })
    await modal.waitFor({ state: 'visible', timeout: 5000 })
    {
      await page.screenshot({ path: `${dir}/web-modal-compartir.png` })
      await modal.getByRole('button', { name: 'No, gracias' }).click()
      await expect(modal).toBeHidden()
    }
    await page.getByRole('button', { name: 'Salir de la carrera' }).click()
    await expect(page.getByRole('dialog', { name: '¿Descartar la carrera?' })).toBeVisible()
    await page.screenshot({ path: `${dir}/web-modal-descartar.png` })
    // La respuesta queda en Ajustes y se cambia desde Configuracion > Privacidad.
    await page.goto('/ajustes/privacidad')
    await expect(page.getByText('Compartir en vivo al correr')).toBeVisible()
    await page.screenshot({ path: `${dir}/web-ajustes-privacidad.png`, fullPage: true })
  })
})
