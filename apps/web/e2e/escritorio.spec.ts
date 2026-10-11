import { expect, test, prepararGps, registrarse } from './ayuda'

const PANTALLAS = [
  '/inicio',
  '/correr',
  '/rutas',
  '/rutas/buscar',
  '/comunidad',
  '/comunidad/eventos',
  '/comunidad/eventos/1',
  '/perfil',
  '/perfil/editar',
  '/perfil/avatar',
  '/ajustes',
  '/ajustes/privacidad',
  '/ajustes/notificaciones',
  '/notificaciones',
]

test.describe('Escritorio', () => {
  test('acceso en dos columnas y sin scroll', async ({ page }) => {
    for (const ruta of ['/bienvenida', '/inicio-sesion', '/registro']) {
      await page.goto(ruta)
      await page.waitForLoadState('networkidle')
      if (ruta !== '/bienvenida')
        await expect(page.getByTestId('panel-corredor-acceso'), ruta).toBeVisible()
      const { sh, ih, sw, iw } = await page.evaluate(() => ({
        sh: document.documentElement.scrollHeight,
        ih: window.innerHeight,
        sw: document.documentElement.scrollWidth,
        iw: window.innerWidth,
      }))
      expect(sw, ruta).toBeLessThanOrEqual(iw)
      expect(sh, ruta).toBeLessThanOrEqual(ih + 1)
    }
  })

  test('barra lateral persistente y sin desborde horizontal', async ({ page, context }) => {
    await prepararGps(context)
    await registrarse(page)
    const lateral = page.getByRole('complementary', { name: 'Navegación principal' })
    for (const ruta of PANTALLAS) {
      await page.goto(ruta)
      await expect(lateral, ruta).toBeVisible()
      const sw = await page.evaluate(() => document.documentElement.scrollWidth)
      expect(sw, ruta).toBeLessThanOrEqual(page.viewportSize()!.width)
      if (process.env.CAPTURAS) {
        await page.waitForTimeout(800)
        await page.screenshot({
          path: `${process.env.CAPTURAS}/${ruta.slice(1).replace(/\//g, '_')}.png`,
        })
      }
    }
    await page.goto('/inicio')
    await lateral.getByRole('link', { name: 'Rutas' }).click()
    await expect(page).toHaveURL(/\/rutas$/)
    await expect(lateral.getByRole('link', { name: 'Rutas' })).toHaveAttribute(
      'aria-current',
      'page'
    )
    // La barra inferior movil no se muestra en escritorio.
    await expect(page.locator('nav.fixed.bottom-0')).toBeHidden()
  })
})
