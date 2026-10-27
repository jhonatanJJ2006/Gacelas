import type { Locator, Page } from '@playwright/test'
import { expect, test, registrarse } from './ayuda'

/**
 * Regresion: los modales se cortaban a la mitad en pantallas pequenas.
 * En 360x640 el boton principal de cada modal debe quedar dentro de la ventana.
 * Con QA_CAPTURAS=<carpeta> ademas guarda capturas de cada modal y pantalla.
 */
const CARPETA = process.env.QA_CAPTURAS
const TAMANOS = CARPETA
  ? [
      { width: 360, height: 640 },
      { width: 390, height: 844 },
      { width: 1440, height: 900 },
    ]
  : [{ width: 360, height: 640 }]

async function dentroDeVentana(page: Page, boton: Locator) {
  await boton.scrollIntoViewIfNeeded()
  const caja = await boton.boundingBox()
  const v = page.viewportSize()!
  expect(caja).not.toBeNull()
  expect(caja!.y).toBeGreaterThanOrEqual(0)
  expect(caja!.y + caja!.height).toBeLessThanOrEqual(v.height)
  expect(caja!.x + caja!.width).toBeLessThanOrEqual(v.width)
}

async function capturar(page: Page, nombre: string) {
  if (!CARPETA) return
  const v = page.viewportSize()!
  await page.screenshot({ path: `${CARPETA}/web-${nombre}-${v.width}x${v.height}.png` })
}

const MODALES: {
  nombre: string
  ruta: string
  abrir: string
  dialogo: string | RegExp
  principal: string | RegExp
}[] = [
  {
    nombre: 'crear-evento',
    ruta: '/comunidad/eventos',
    abrir: 'Crear evento',
    dialogo: 'Crear evento',
    principal: 'Crear',
  },
  {
    nombre: 'cerrar-sesion',
    ruta: '/ajustes',
    abrir: 'Cerrar sesión',
    dialogo: '¿Cerrar sesión?',
    principal: 'Cerrar sesión',
  },
  {
    nombre: 'ayuda',
    ruta: '/ajustes',
    abrir: 'Ayuda y soporte',
    dialogo: 'Ayuda y soporte',
    principal: 'Cerrar',
  },
]

const PANTALLAS = [
  '/inicio',
  '/comunidad',
  '/comunidad/eventos',
  '/rutas',
  '/correr',
  '/perfil',
  '/ajustes',
  '/notificaciones',
]

for (const tam of TAMANOS) {
  test.describe(`Modales en ${tam.width}x${tam.height}`, () => {
    test.use({ viewport: tam })
    test.beforeEach(async ({ page }) => registrarse(page))

    for (const m of MODALES) {
      test(`${m.nombre}: el boton principal es visible`, async ({ page }) => {
        await page.goto(m.ruta)
        await page.getByRole('button', { name: m.abrir }).first().click()
        const d = page.getByRole('dialog', { name: m.dialogo })
        await expect(d).toBeVisible()
        const boton = d.getByRole('button', { name: m.principal, exact: true }).last()
        await dentroDeVentana(page, boton)
        await capturar(page, `modal-${m.nombre}`)
      })
    }

    if (CARPETA)
      test('capturas de pantallas', async ({ page }) => {
        for (const ruta of PANTALLAS) {
          await page.goto(ruta)
          await page.waitForTimeout(600)
          await capturar(page, `pantalla${ruta.replace(/\//g, '-')}`)
        }
      })
  })
}
