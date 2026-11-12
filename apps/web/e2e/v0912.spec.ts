import { expect, test, prepararGps, registrarse, responderCompartirEnVivo } from './ayuda'

test.describe('0.9.12: navegación, SOS y consentimientos', () => {
  test('sin sesión, tras iniciar sesión vuelve a la pantalla pedida', async ({ page }) => {
    await registrarse(page)
    await page.goto('/ajustes')
    await page.getByRole('button', { name: /Cerrar sesión/ }).click()
    await page.getByRole('dialog').getByRole('button', { name: 'Cerrar sesión' }).click()
    await expect(page).toHaveURL(/\/bienvenida/)
    await page.goto('/comunidad/clubes')
    await expect(page).toHaveURL(/\/bienvenida/)
    await page.goto('/inicio-sesion')
    await page.getByLabel('Correo electrónico').fill('jhona@utpl.edu.ec')
    await page.getByLabel('Contraseña', { exact: true }).fill('gacela123')
    await page.getByRole('button', { name: /iniciar sesión/i }).click()
    await expect(page).toHaveURL(/\/comunidad\/clubes$/)
  })

  test('descartar una carrera vuelve a Inicio y el permiso se pide una sola vez', async ({
    page,
    context,
  }) => {
    await prepararGps(context)
    await registrarse(page)
    await page.goto('/correr')
    await page.getByRole('button', { name: 'Comenzar carrera' }).click()
    await page.getByRole('button', { name: 'Aceptar y empezar' }).click()
    await expect(page).toHaveURL(/\/correr\/en-curso/)
    await responderCompartirEnVivo(page)
    await page.getByRole('button', { name: 'Salir de la carrera' }).click()
    await page.getByRole('dialog').getByRole('button', { name: 'Descartar' }).click()
    await expect(page).toHaveURL(/\/inicio$/)
    await expect(page.getByText('Carrera descartada')).toBeVisible()

    // Segunda carrera: ni el permiso de GPS ni el de compartir en vivo se vuelven a preguntar.
    await page.goto('/correr')
    await page.getByRole('button', { name: 'Comenzar carrera' }).click()
    await expect(page).toHaveURL(/\/correr\/en-curso/)
    await expect(page.getByRole('dialog', { name: '¿Compartir tu carrera en vivo?' })).toHaveCount(
      0
    )
  })

  test('SOS: mantener pulsado activa, ofrece 911 y la página pública muestra la alerta', async ({
    page,
    context,
  }) => {
    await prepararGps(context)
    await registrarse(page)
    await page.goto('/ajustes/privacidad')
    await page.getByLabel('Nombre del contacto').fill('Mamá')
    await page.getByLabel('Teléfono del contacto').fill('0991234567')
    await page.getByRole('button', { name: 'Agregar contacto' }).click()
    await expect(page.getByText('+593991234567')).toBeVisible()

    await page.goto('/correr')
    await page.getByRole('button', { name: 'Comenzar carrera' }).click()
    await page.getByRole('button', { name: 'Aceptar y empezar' }).click()
    await page
      .getByRole('dialog', { name: '¿Compartir tu carrera en vivo?' })
      .getByRole('button', { name: 'No, gracias' })
      .click()
    const sos = page.getByTestId('boton-sos')
    // Toque corto: solo ayuda.
    await sos.click()
    await expect(page.getByText('Mantén pulsado 3 s para pedir ayuda')).toBeVisible()
    // Mantener 3 s.
    const caja = (await sos.boundingBox())!
    await page.mouse.move(caja.x + caja.width / 2, caja.y + caja.height / 2)
    await page.mouse.down()
    await page.waitForTimeout(3300)
    await page.mouse.up()
    const hoja = page.getByRole('dialog', { name: 'SOS activo' })
    await expect(hoja).toBeVisible()
    await expect(hoja.getByTestId('llamar-911')).toHaveAttribute('href', 'tel:911')
    await expect(hoja.getByTestId('sos-sms')).toHaveAttribute('href', /^sms:\+593991234567\?body=/)
    await page.screenshot({ path: 'test-results/sos-hoja.png' })

    await page.goto('/sos/5050aaaa-0000-4000-8000-000000000001')
    await expect(page.getByText('Jhona Runner pidió ayuda')).toBeVisible()
    await expect(page.getByRole('link', { name: /Llamar al 911/ })).toBeVisible()
  })
})
