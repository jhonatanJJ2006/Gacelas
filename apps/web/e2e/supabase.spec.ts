import { expect, test, registrarse } from './ayuda'
import { URL_FAKE } from './fakeSupabase'

test.describe('Conexión con Supabase (servidor simulado)', () => {
  test('la cuenta y los ajustes se guardan en el servidor y vuelven en otro navegador', async ({
    page,
    supabase,
  }) => {
    await registrarse(page, 'Jhona Runner', 'jhona@utpl.edu.ec')
    expect(supabase.usuarios.map((u) => u.email)).toEqual(['jhona@utpl.edu.ec'])
    expect(supabase.peticiones).toContain('POST /auth/v1/signup')

    await page.goto('/rutas')
    await page
      .getByRole('button', { name: /^Guardar /i })
      .first()
      .click()
    await expect.poll(() => supabase.tablas.saved_routes?.length ?? 0).toBe(1)

    // Sin datos locales: todo debe volver desde el servidor al iniciar sesion.
    await page.evaluate(() => localStorage.clear())
    await page.goto('/inicio-sesion')
    await page.getByLabel('Correo electrónico').fill('jhona@utpl.edu.ec')
    await page.getByLabel('Contraseña', { exact: true }).fill('gacela123')
    await page.getByRole('button', { name: /iniciar sesión/i }).click()
    await expect(page).toHaveURL(/\/inicio$/)
    expect(supabase.peticiones.some((p) => p.startsWith('GET /rest/v1/saved_routes'))).toBe(true)
  })

  test('Continuar con Google redirige al proveedor de Supabase', async ({ page }) => {
    let destino = ''
    await page.route(`${URL_FAKE}/auth/v1/authorize**`, async (route) => {
      destino = route.request().url()
      await route.fulfill({ status: 200, contentType: 'text/html', body: '<p>Google</p>' })
    })
    await page.goto('/bienvenida')
    await page.getByRole('button', { name: /continuar con google/i }).click()
    await expect.poll(() => destino).toContain('provider=google')
    expect(decodeURIComponent(destino)).toContain('/inicio')
  })

  test('si Google no está activo en el servidor lo explica', async ({ page, supabase }) => {
    supabase.googleActivo = false
    await page.goto('/inicio-sesion')
    await page.getByRole('button', { name: /continuar con google/i }).click()
    await expect(page.getByRole('alert')).toHaveText(/todavía no está activado/)
  })
})
