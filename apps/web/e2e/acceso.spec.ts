import { expect, test, registrarse } from './ayuda'

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

test.describe('bienvenida sin scroll', () => {
  for (const [w, h] of [
    [1024, 545],
    [1440, 900],
    [390, 844],
    [360, 640],
  ]) {
    test(`cabe en ${w}x${h} y muestra el corredor`, async ({ page }) => {
      await page.setViewportSize({ width: w, height: h })
      await page.goto('/bienvenida')
      await expect(page.getByTestId('bienvenida-corredor')).toBeVisible()
      await expect(page.getByRole('img', { name: 'Corredor de Gacelas' }).first()).toBeVisible()
      const { sh, ih } = await page.evaluate(() => ({
        sh: document.scrollingElement!.scrollHeight,
        ih: window.innerHeight,
      }))
      expect(sh).toBeLessThanOrEqual(ih)
    })
  }
})
