import { expect, test, registrarse } from './ayuda'

// v0.9.3: el corredor 3D de Inicio carga sin red despues de la primera visita (service worker).
const dir = process.env.GACELAS_SHOTS ?? 'test-results/v093'

test.use({ serviceWorkers: 'allow' })

test('Inicio muestra el avatar 3D tambien sin red', async ({ page, context }) => {
  test.setTimeout(90_000)
  await page.setViewportSize({ width: 393, height: 873 })
  await registrarse(page)
  const avatar = page.getByTestId('runner3d')
  await expect(avatar).toHaveAttribute('data-listo', '1', { timeout: 30_000 })
  await page.screenshot({ path: `${dir}/web-inicio-con-red.png` })

  // El service worker ya controla la pagina y guardo el GLB.
  await page.evaluate(async () => {
    await navigator.serviceWorker.ready
    if (!navigator.serviceWorker.controller)
      await new Promise((r) => navigator.serviceWorker.addEventListener('controllerchange', r))
  })
  await expect
    .poll(() =>
      page.evaluate(async () =>
        Boolean(await caches.match('/models/gacelas-runner-animated.glb', { ignoreSearch: true }))
      )
    )
    .toBe(true)
  await expect
    .poll(() =>
      page.evaluate(async () => {
        const c = await caches.open('runcity-v1')
        const ks = await c.keys()
        return ks.filter((k) => k.url.includes('fonts.gstatic')).length
      })
    )
    .toBeGreaterThanOrEqual(3)

  await context.setOffline(true)
  await page.reload()
  await expect(page.getByTestId('inicio-escenario')).toBeVisible({ timeout: 20_000 })
  await expect(page.getByTestId('runner3d')).toHaveAttribute('data-listo', '1', { timeout: 30_000 })
  await expect
    .poll(() =>
      page.evaluate(() => document.fonts.check('24px "Material Symbols Outlined"', 'home'))
    )
    .toBe(true)
  await page.evaluate(() => document.fonts.ready)
  await page.waitForTimeout(800)
  await page.screenshot({ path: `${dir}/web-inicio-sin-red.png` })
  await context.setOffline(false)
})
