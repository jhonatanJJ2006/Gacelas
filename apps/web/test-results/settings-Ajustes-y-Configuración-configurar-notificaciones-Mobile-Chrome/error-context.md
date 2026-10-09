# Instructions

- Following Playwright test failed.
- Explain why, be concise, respect Playwright best practices.
- Provide a snippet of code with the fix, if possible.

# Test info

- Name: settings.spec.ts >> Ajustes y Configuración >> configurar notificaciones
- Location: e2e/settings.spec.ts:34:3

# Error details

```
Error: expect(locator).toBeVisible() failed

Locator: getByText(/notificaciones/i)
Expected: visible
Error: strict mode violation: getByText(/notificaciones/i) resolved to 4 elements:
    1) <h1 class="font-headline-md text-headline-md text-text-primary">Notificaciones</h1> aka getByRole('heading', { name: 'Notificaciones', exact: true })
    2) <h2 class="font-title-md text-title-md text-text-primary mb-3">Notificaciones push</h2> aka getByRole('heading', { name: 'Notificaciones push' })
    3) <h2 class="font-title-md text-title-md text-text-primary mb-3">Notificaciones por email</h2> aka getByRole('heading', { name: 'Notificaciones por email' })
    4) <p class="font-body-sm text-body-sm text-text-secondary mb-3">Los likes se agrupan cada 10 o cada 6 horas para …</p> aka getByText('Los likes se agrupan cada 10')

Call log:
  - Expect "toBeVisible" getByText(/notificaciones/i) with timeout 10000ms
  - waiting for getByText(/notificaciones/i)
    - locator resolved to <span class="font-body-md text-body-md text-text-primary">Notificaciones</span>
    - unexpected value "hidden"

```

# Page snapshot

```yaml
- generic [ref=f1e3]:
    - banner [ref=f1e4]:
        - generic [ref=f1e5]:
            - button "arrow_back" [ref=f1e6] [cursor=pointer]
            - heading "Notificaciones" [level=1] [ref=f1e8]
    - main [ref=f1e9]:
        - generic [ref=f1e10]:
            - heading "Notificaciones push" [level=2] [ref=f1e11]
            - generic [ref=f1e12]:
                - generic [ref=f1e13]:
                    - generic [ref=f1e14]:
                        - generic [ref=f1e15]: favorite
                        - generic [ref=f1e16]:
                            - generic [ref=f1e17]: Likes
                            - paragraph [ref=f1e18]: Cuando alguien da like a tu actividad
                    - checkbox [checked] [ref=f1e20]
                - generic [ref=f1e22]:
                    - generic [ref=f1e23]:
                        - generic [ref=f1e24]: chat_bubble
                        - generic [ref=f1e25]:
                            - generic [ref=f1e26]: Comentarios
                            - paragraph [ref=f1e27]: Cuando alguien comenta tu actividad
                    - checkbox [checked] [ref=f1e29]
                - generic [ref=f1e31]:
                    - generic [ref=f1e32]:
                        - generic [ref=f1e33]: person_add
                        - generic [ref=f1e34]:
                            - generic [ref=f1e35]: Nuevos seguidores
                            - paragraph [ref=f1e36]: Cuando alguien te sigue
                    - checkbox [checked] [ref=f1e38]
                - generic [ref=f1e40]:
                    - generic [ref=f1e41]:
                        - generic [ref=f1e42]: sensors
                        - generic [ref=f1e43]:
                            - generic [ref=f1e44]: Carreras en vivo
                            - paragraph [ref=f1e45]: Cuando alguien inicia seguimiento en vivo
                    - checkbox [checked] [ref=f1e47]
                - generic [ref=f1e49]:
                    - generic [ref=f1e50]:
                        - generic [ref=f1e51]: event
                        - generic [ref=f1e52]:
                            - generic [ref=f1e53]: Eventos
                            - paragraph [ref=f1e54]: Recordatorios de eventos y nuevos eventos
                    - checkbox [checked] [ref=f1e56]
        - generic [ref=f1e58]:
            - heading "Notificaciones por email" [level=2] [ref=f1e59]
            - generic [ref=f1e61]:
                - generic [ref=f1e62]:
                    - generic [ref=f1e63]: mail
                    - generic [ref=f1e64]:
                        - generic [ref=f1e65]: Resumen semanal
                        - paragraph [ref=f1e66]: Recibe un resumen de tu actividad cada semana
                - checkbox [ref=f1e68]
        - generic [ref=f1e70]:
            - heading "Frecuencia" [level=2] [ref=f1e71]
            - generic [ref=f1e72]:
                - paragraph [ref=f1e73]: Los likes se agrupan cada 10 o cada 6 horas para evitar notificaciones excesivas.
                - generic [ref=f1e74]:
                    - generic [ref=f1e75]: info
                    - generic [ref=f1e76]: Configuración optimizada
```

# Test source

```ts
  1  | import { test, expect } from '@playwright/test'
  2  |
  3  | test.describe('Ajustes y Configuración', () => {
  4  |   test.beforeEach(async ({ page }) => {
  5  |     await page.goto('/inicio-sesion')
  6  |     await page.fill('input[type="email"]', 'test@example.com')
  7  |     await page.fill('input[type="password"]', 'password123')
  8  |     await page.getByRole('button', { name: /iniciar sesión/i }).click()
  9  |     await expect(page).toHaveURL(/\/inicio/, { timeout: 10000 })
  10 |     await page.waitForLoadState('networkidle')
  11 |   })
  12 |
  13 |   test('acceder a ajustes', async ({ page }) => {
  14 |     await page
  15 |       .getByRole('link', { name: /perfil/i })
  16 |       .first()
  17 |       .click()
  18 |     await page.getByText(/ajustes y preferencias/i).click()
  19 |
  20 |     await expect(page).toHaveURL(/\/ajustes/)
  21 |     await expect(page.getByText(/ajustes/i)).toBeVisible()
  22 |   })
  23 |
  24 |   test('configurar privacidad', async ({ page }) => {
  25 |     await page.goto('/ajustes')
  26 |     await page
  27 |       .getByText(/privacidad y ubicación/i)
  28 |       .first()
  29 |       .click()
  30 |
  31 |     await expect(page).toHaveURL(/\/ajustes\/privacidad/)
  32 |   })
  33 |
  34 |   test('configurar notificaciones', async ({ page }) => {
  35 |     await page.goto('/ajustes')
  36 |     await page
  37 |       .getByText(/notificaciones/i)
  38 |       .first()
  39 |       .click()
  40 |
  41 |     await expect(page).toHaveURL(/\/ajustes\/notificaciones/)
> 42 |     await expect(page.getByText(/notificaciones/i)).toBeVisible()
     |                                                     ^ Error: expect(locator).toBeVisible() failed
  43 |
  44 |     const toggles = page.locator('input[type="checkbox"]')
  45 |     const count = await toggles.count()
  46 |     expect(count).toBeGreaterThan(0)
  47 |   })
  48 | })
  49 |
```
