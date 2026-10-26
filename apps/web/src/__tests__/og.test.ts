import { describe, expect, it } from 'vitest'
import { htmlRuta, idValido } from '../../api/_og.js'

describe('pagina Open Graph de rutas', () => {
  it('incluye metadatos, escapa HTML y redirige a la app', () => {
    const html = htmlRuta({
      id: 'abc-1',
      ruta: { name: 'Villonaco <b>', description: 'Subida', distance_km: 8.04, elevation_m: 320 },
      imagen: 'https://x/rutas/abc-1.png',
    })
    expect(html).toContain('<meta property="og:title" content="Villonaco &lt;b&gt; · Gacelas">')
    expect(html).toContain('og:image" content="https://x/rutas/abc-1.png"')
    expect(html).toContain('8.0 km · 320 m de desnivel')
    expect(html).toContain('location.replace("https://gacelas.vercel.app/rutas/abc-1")')
  })
  it('valida el id', () => {
    expect(idValido('3')).toBe(true)
    expect(idValido('a/b')).toBe(false)
    expect(idValido('<x>')).toBe(false)
  })
})
