import { expect, test, registrarse } from './ayuda'

test('ruta inexistente muestra 404 en espanol con enlace a inicio', async ({ page }) => {
  await page.goto('/ruta-que-no-existe-xyz')
  await expect(page.getByRole('heading', { level: 1, name: /no encontrada/i })).toBeVisible()
  await expect(page.getByRole('link', { name: 'Volver al inicio' })).toHaveAttribute(
    'href',
    '/inicio'
  )
})

test('resumen sin carrera muestra estado vacio', async ({ page }) => {
  await registrarse(page)
  await page.goto('/correr/resumen')
  await expect(page.getByText('No hay datos de carrera para guardar')).toBeVisible()
})
