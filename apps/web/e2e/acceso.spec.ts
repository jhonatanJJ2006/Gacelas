import { expect, test } from '@playwright/test'
import { registrarse } from './ayuda'

test.describe('Acceso y sesión', () => {
  test('sin sesión, las pantallas protegidas llevan a bienvenida', async ({ page }) => {
    await page.goto('/perfil')
    await expect(page).toHaveURL(/\/bienvenida/)
    await page.getByRole('button', { name: /saltar/i }).click()
    await expect(page.getByText('Bienvenido de vuelta')).toBeVisible()
  })

  test('registro, cierre de sesión e inicio de sesión persistido', async ({ page }) => {
    await registrarse(page)
    await expect(page.getByText(/, Jhona/)).toBeVisible()
    await page.reload()
    await expect(page.getByText(/, Jhona/)).toBeVisible()

    await page.goto('/ajustes')
    await page.getByRole('button', { name: /Cerrar sesión/ }).click()
    await page.getByRole('dialog').getByRole('button', { name: 'Cerrar sesión' }).click()
    await expect(page).toHaveURL(/\/bienvenida/)

    await page.goto('/inicio-sesion')
    await page.getByLabel('Correo electrónico').fill('jhona@utpl.edu.ec')
    await page.getByLabel('Contraseña').fill('equivocada1')
    await page.getByRole('button', { name: /iniciar sesión/i }).click()
    await expect(page.getByRole('alert')).toHaveText(/incorrectos/)
    await page.getByLabel('Contraseña').fill('gacela123')
    await page.getByRole('button', { name: /iniciar sesión/i }).click()
    await expect(page).toHaveURL(/\/inicio$/)
  })

  test('registro valida los campos', async ({ page }) => {
    await page.goto('/registro')
    await page.getByRole('button', { name: /crear cuenta/i }).click()
    await expect(page.getByRole('alert')).toBeVisible()
  })
})
