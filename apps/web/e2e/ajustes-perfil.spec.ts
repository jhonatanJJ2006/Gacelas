import { expect, test } from '@playwright/test'
import { registrarse } from './ayuda'

test.describe('Ajustes, perfil y notificaciones', () => {
  test.beforeEach(async ({ page }) => registrarse(page))

  test('modo claro y unidades persisten', async ({ page }) => {
    await page.goto('/ajustes')
    await page.getByRole('switch', { name: 'Modo oscuro' }).click()
    await expect(page.locator('html')).toHaveClass(/tema-claro/)
    await page.getByRole('switch', { name: 'Unidades métricas' }).click()
    await page.reload()
    await expect(page.locator('html')).toHaveClass(/tema-claro/)
    await expect(page.getByRole('switch', { name: 'Unidades métricas' })).toHaveAttribute(
      'aria-checked',
      'false'
    )
    await page.goto('/rutas')
    await expect(page.getByText(/ mi/).first()).toBeVisible()
  })

  test('privacidad y notificaciones', async ({ page }) => {
    await page.goto('/ajustes/privacidad')
    await page.getByRole('radio', { name: /Privado/ }).click()
    await page.reload()
    await expect(page.getByRole('radio', { name: /Privado/ })).toHaveAttribute(
      'aria-checked',
      'true'
    )

    await page.goto('/ajustes/notificaciones')
    await page.getByRole('switch', { name: /Carreras en vivo/ }).click()
    await page.goto('/notificaciones')
    await expect(page.getByTestId('notificacion')).toHaveCount(4)
    await page.getByRole('button', { name: /marcar todas/i }).click()
    await page.getByRole('tab', { name: 'Sin leer' }).click()
    await expect(page.getByText('No hay notificaciones.')).toBeVisible()
  })

  test('editar perfil', async ({ page }) => {
    await page.goto('/perfil/editar')
    await page.getByLabel('Nombre completo').fill('Jhona Gacela')
    await page.getByLabel('Bio').fill('Corro en Loja')
    await page.getByRole('button', { name: 'Guardar' }).click()
    await expect(page.getByText('Jhona Gacela')).toBeVisible()
    await page.reload()
    await expect(page.getByText('Corro en Loja')).toBeVisible()
  })
})
