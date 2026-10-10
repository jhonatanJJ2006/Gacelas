import { expect, type BrowserContext, type Page } from '@playwright/test'

export const LOJA = { latitude: -3.9931, longitude: -79.2042, accuracy: 5 }

/** Da permiso de ubicacion y coloca el GPS falso en Loja. */
export async function prepararGps(context: BrowserContext) {
  await context.grantPermissions(['geolocation'])
  await context.setGeolocation(LOJA)
}

/** Crea una cuenta local nueva y deja la sesion abierta en Inicio. */
export async function registrarse(
  page: Page,
  nombre = 'Jhona Runner',
  email = 'jhona@utpl.edu.ec'
) {
  await page.goto('/registro')
  await page.getByLabel('Nombre completo').fill(nombre)
  await page.getByLabel('Correo electrónico').fill(email)
  await page.getByLabel('Contraseña').fill('gacela123')
  await page.getByRole('button', { name: /crear cuenta/i }).click()
  await expect(page.getByText('Activa tu ubicación')).toBeVisible()
  await page.getByRole('button', { name: 'Configurar más tarde' }).click()
  await expect(page).toHaveURL(/\/inicio$/)
}

/** Mueve el GPS falso hacia el norte, un punto cada dos segundos. */
export async function caminar(context: BrowserContext, pasos: number, metrosPorPaso = 10) {
  for (let i = 1; i <= pasos; i++) {
    await context.setGeolocation({
      ...LOJA,
      latitude: LOJA.latitude + (i * metrosPorPaso) / 111_195,
    })
    await new Promise((r) => setTimeout(r, 2100))
  }
}
