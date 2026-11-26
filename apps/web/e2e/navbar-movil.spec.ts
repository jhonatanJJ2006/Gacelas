import { expect, test, prepararGps, registrarse } from './ayuda'

test('barra inferior: burbuja en la seccion activa y navegacion entre secciones', async ({
  page,
  context,
}) => {
  await prepararGps(context)
  await registrarse(page)
  const barra = page.getByTestId('nav-barra')
  await expect(barra).toBeVisible()
  await expect(barra.getByRole('link', { name: 'Inicio' })).toHaveAttribute('aria-current', 'page')
  const burbuja = page.locator('.navc-burbuja')
  const centro = async () => {
    const b = (await burbuja.boundingBox())!
    return b.x + b.width / 2
  }
  const xInicio = await centro()
  if (process.env.CAPTURAS)
    await page.screenshot({ path: `${process.env.CAPTURAS}/movil-inicio.png` })
  await barra.getByRole('link', { name: 'Rutas' }).click()
  await expect(page).toHaveURL(/\/rutas$/)
  await expect(barra.getByRole('link', { name: 'Rutas' })).toHaveAttribute('aria-current', 'page')
  await expect.poll(centro).toBeGreaterThan(xInicio + 40)
  await barra.getByRole('link', { name: 'Perfil' }).click()
  await expect(page).toHaveURL(/\/perfil$/)
  await expect(barra.getByRole('link', { name: 'Perfil' })).toHaveAttribute('aria-current', 'page')
  if (process.env.CAPTURAS)
    await page.screenshot({ path: `${process.env.CAPTURAS}/movil-perfil.png` })
  await barra.getByRole('link', { name: 'Correr' }).click()
  await expect(page).toHaveURL(/\/correr$/)
})
