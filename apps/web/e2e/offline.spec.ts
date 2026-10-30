import { expect, test } from '@playwright/test'

test('tras la primera visita la app recarga sin red, incluso pantallas no visitadas', async ({
  page,
  context,
}) => {
  await page.goto('/')
  await page.evaluate(() => navigator.serviceWorker.ready)
  await page.waitForFunction(async () => (await caches.keys()).length > 0)
  await page.waitForTimeout(1500)
  await context.setOffline(true)
  await page.goto('/registro')
  await expect(page.getByText(/manada/i)).toBeVisible()
  await page.goto('/ruta-inexistente')
  await expect(page.getByRole('heading', { level: 1 })).toBeVisible()
})
