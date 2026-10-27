import { expect, test } from '@playwright/test'
import { instalarFakeSupabase } from './fakeSupabase'

// Sin el fixture de ayuda.ts, que da la intro por vista.
test.describe('Intro de marca', () => {
  test.beforeEach(async ({ context }) => {
    await instalarFakeSupabase(context)
  })

  test('aparece al cargar, se salta con un toque y no reaparece al recargar enseguida', async ({
    page,
  }) => {
    await page.goto('/')
    const intro = page.getByTestId('intro-animada')
    await expect(intro).toBeVisible()
    await intro.click()
    await expect(intro).toBeHidden()
    await page.reload()
    await page.waitForTimeout(500)
    await expect(intro).toBeHidden()
  })

  test('es un video sobre fondo de marca que termina solo en unos 2 segundos', async ({ page }) => {
    await page.goto('/')
    const intro = page.getByTestId('intro-animada')
    await expect(intro).toBeVisible()
    await expect(intro).toHaveCSS('background-color', 'rgb(12, 19, 34)')
    await expect(intro.locator('video')).toHaveAttribute('playsinline', '')
    await expect(intro).toBeHidden({ timeout: 6000 })
  })

  test('fallback: si el video no carga, la app se muestra igual', async ({ page }) => {
    await page.route('**/marca/intro.*', (r) => r.abort())
    await page.goto('/')
    await expect(page.getByTestId('intro-animada')).toBeHidden({ timeout: 3000 })
  })

  test('con movimiento reducido no se muestra', async ({ browser }) => {
    const context = await browser.newContext({ reducedMotion: 'reduce' })
    await instalarFakeSupabase(context)
    const page = await context.newPage()
    await page.goto('http://localhost:4173/')
    await page.waitForTimeout(500)
    await expect(page.getByTestId('intro-animada')).toBeHidden()
    await context.close()
  })

  test('tiene icono, manifest y theme-color', async ({ page, request }) => {
    await page.goto('/')
    await expect(page.locator('meta[name="theme-color"]')).toHaveAttribute('content', '#0C1322')
    const m = await (await request.get('/manifest.webmanifest')).json()
    expect(m.icons.map((i: { sizes: string }) => i.sizes)).toEqual([
      '192x192',
      '512x512',
      '512x512',
    ])
    for (const r of [
      '/favicon.svg',
      '/favicon.ico',
      '/apple-touch-icon.png',
      '/icon-maskable-512.png',
    ])
      expect((await request.get(r)).ok()).toBe(true)
  })
})
