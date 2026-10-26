import { expect, test, registrarse } from './ayuda'

test.describe('Grupos y comunidades', () => {
  test('registrar un grupo de running y unirse a otro', async ({ page, supabase }) => {
    await registrarse(page)
    await page.goto('/comunidad/clubes')
    await expect(page.getByRole('heading', { name: 'Grupos y comunidades' })).toBeVisible()

    // Unirse a un grupo existente.
    await page.getByPlaceholder('Buscar por nombre, lugar o ciudad').fill('jipiro')
    await page.getByTestId('tarjeta-club').filter({ hasText: 'Gacelas del Jipiro' }).click()
    await expect(page.getByTestId('datos-grupo')).toContainText('Mar y Sáb · 06:00')
    await page.getByTestId('accion-club').click()
    await expect(page.getByTestId('miembros-club')).toContainText('2 miembros')
    await expect(page.getByTestId('accion-club')).toHaveText('Salir del grupo')

    // Registrar un grupo propio.
    await page.goto('/comunidad/clubes')
    await page.getByTestId('crear-club').click()
    await expect(page.getByRole('heading', { name: 'Registrar grupo' })).toBeVisible()
    await page.getByLabel('Nombre').fill('Trail Villonaco')
    await page.getByLabel('Descripción').fill('Subidas al Villonaco')
    await page.getByLabel('Punto de encuentro').fill('Redondel de la Argelia')
    await page.getByRole('button', { name: 'Jue' }).click()
    await page.getByLabel('Hora').fill('05:30')
    await page.getByLabel('Ritmo').fill('6:00')
    await page.getByLabel('Nivel').selectOption('avanzado')
    await page.getByLabel('Cupo').fill('15')
    await page.getByTestId('guardar-club').click()

    await expect(page.getByRole('heading', { name: 'Trail Villonaco' })).toBeVisible()
    await expect(page.getByTestId('lista-miembros')).toContainText('Fundador')
    await expect(page.getByTestId('datos-grupo')).toContainText('6:00 min/km')
    await expect(page.getByTestId('miembros-club')).toContainText('1/15')
    const creado = supabase.tablas.clubs.find((c) => c.name === 'Trail Villonaco')
    expect(creado).toMatchObject({
      kind: 'grupo',
      training_days: [4],
      level: 'avanzado',
      max_members: 15,
    })

    // Nombre repetido: lo avisa antes de enviar.
    await page.goto('/comunidad/clubes/nuevo?tipo=grupo')
    await page.getByLabel('Nombre').fill('trail  villonaco')
    await page.getByTestId('guardar-club').click()
    await expect(page.getByRole('alert')).toHaveText(/Ya existe un grupo/)
  })
})
