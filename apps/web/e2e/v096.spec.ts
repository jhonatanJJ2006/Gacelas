import { expect, test, registrarse } from './ayuda'

// v0.9.6: en Inicio el corredor 3D gira sobre su eje al arrastrarlo en horizontal.
const dir = process.env.GACELAS_SHOTS ?? 'test-results/v096'

test('arrastrar en horizontal gira el corredor de Inicio', async ({ page }) => {
  test.setTimeout(90_000)
  await page.setViewportSize({ width: 393, height: 873 })
  await registrarse(page)
  const avatar = page.getByTestId('runner3d')
  await expect(avatar).toHaveAttribute('data-listo', '1', { timeout: 30_000 })
  await expect(avatar).toHaveCSS('touch-action', 'pan-y')
  await page.waitForTimeout(600)
  await page.screenshot({ path: `${dir}/web-inicio-antes-arrastre.png` })

  const caja = (await avatar.boundingBox())!
  const y = caja.y + caja.height / 2
  const x0 = caja.x + caja.width * 0.25
  await page.mouse.move(x0, y)
  await page.mouse.down()
  // El arrastre empieza con el primer movimiento horizontal (umbral de 4 px); data-giro se escribe en
  // cada cuadro, asi que se espera un cuadro antes de leerlo (en CI el WebGL por software va lento).
  await page.mouse.move(x0 + 10, y)
  await page.waitForTimeout(400)
  const antes = Number(await avatar.getAttribute('data-giro'))
  for (let i = 2; i <= 11; i++) await page.mouse.move(x0 + i * 10, y)
  await page.waitForTimeout(400)
  const durante = Number(await avatar.getAttribute('data-giro'))
  // 100 px * 0.4 grados/px = 40 grados (sin giro automatico mientras se arrastra). El avatar del
  // hero redisenado es mas angosto: el arrastre queda dentro del lienzo.
  expect(((durante - antes + 360) % 360) - 40).toBeLessThan(6)
  expect((durante - antes + 360) % 360).toBeGreaterThan(34)
  await page.screenshot({ path: `${dir}/web-inicio-arrastrando.png` })
  await page.mouse.up()
  await page.waitForTimeout(1500)
  // Despues de soltar sigue girando solo.
  const luego = Number(await avatar.getAttribute('data-giro'))
  await page.waitForTimeout(1000)
  expect(Number(await avatar.getAttribute('data-giro'))).not.toBe(luego)
  await page.screenshot({ path: `${dir}/web-inicio-despues-arrastre.png` })
})
