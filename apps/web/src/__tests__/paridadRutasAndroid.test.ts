// Web, Android y backend deben mostrar las mismas rutas: mismo nombre, distancia, desnivel,
// dificultad y trazado. Lee las fuentes Kotlin y SQL del repositorio para que no vuelvan a divergir.
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'
import { RUTAS_LOJA } from '../contexts/rutas/infrastructure/rutasLojaOsm'
import { rutasMock } from '../mocks/data'
import { TRAZOS_RUTAS } from '../shared/lib/mapas'

const raiz = resolve(__dirname, '../../../..')
const leer = (r: string) => readFileSync(resolve(raiz, r), 'utf8')
const DIF: Record<string, string> = { Facil: 'Fácil', Moderado: 'Moderado', Dificil: 'Difícil' }

type Pt = { lat: number; lng: number }
const km = (p: Pt[]) => {
  let s = 0
  for (let i = 1; i < p.length; i++) {
    const r = Math.PI / 180
    const a =
      Math.sin(((p[i].lat - p[i - 1].lat) * r) / 2) ** 2 +
      Math.cos(p[i - 1].lat * r) *
        Math.cos(p[i].lat * r) *
        Math.sin(((p[i].lng - p[i - 1].lng) * r) / 2) ** 2
    s += 2 * 6371 * Math.asin(Math.sqrt(a))
  }
  return s
}

describe('rutas oficiales: web = Android = migracion 022', () => {
  const kt = leer(
    'apps/android/contextos/rutas/domain/src/commonMain/kotlin/com/gacelas/rutas/domain/modelo/RutasLoja.kt'
  )
  const android = [
    ...kt.matchAll(
      /id = "([^"]+)", nombre = "([^"]+)", distanciaKm = ([\d.]+), dificultad = Dificultad\.(\w+),\s*elevacionM = (\d+)[\s\S]*?puntos = trazo\("([^"]+)"\)/g
    ),
  ].map((m) => ({
    id: m[1],
    nombre: m[2],
    distanciaKm: Number(m[3]),
    dificultad: DIF[m[4]],
    elevacionM: Number(m[5]),
    puntos: m[6].split(';').map((s) => {
      const [lat, lng] = s.split(',').map(Number)
      return { lat, lng }
    }),
  }))
  const sql = leer('supabase/migrations/022_rutas_loja.sql')
  const backend = new Map(
    [...sql.matchAll(/\('(loja-[^']+)', '[^']+', '([^']+)', ([\d.]+), '([^']+)', (\d+),/g)].map(
      (m) => [
        m[1],
        { nombre: m[2], distanciaKm: Number(m[3]), dificultad: m[4], elevacionM: Number(m[5]) },
      ]
    )
  )

  it('mismas rutas en las tres fuentes', () => {
    expect(android.length).toBe(26)
    expect(RUTAS_LOJA.map((r) => r.id).sort()).toEqual(android.map((r) => r.id).sort())
    expect([...backend.keys()].sort()).toEqual(android.map((r) => r.id).sort())
  })

  it.each(android.map((r) => [r.id, r] as const))('%s coincide', (id, a) => {
    const w = RUTAS_LOJA.find((r) => r.id === id)!
    const datos = {
      nombre: a.nombre,
      distanciaKm: a.distanciaKm,
      dificultad: a.dificultad,
      elevacionM: a.elevacionM,
    }
    expect({
      nombre: w.nombre,
      distanciaKm: w.distanciaKm,
      dificultad: w.dificultad,
      elevacionM: w.elevacionM,
    }).toEqual(datos)
    expect(backend.get(id)).toEqual(datos)
    expect(w.puntos!.length).toBe(a.puntos.length)
    w.puntos!.forEach((p, i) => {
      expect(p.lat).toBeCloseTo(a.puntos[i].lat, 4)
      expect(p.lng).toBeCloseTo(a.puntos[i].lng, 4)
    })
    // La distancia publicada corresponde al trazado (tolerancia 5 %).
    expect(Math.abs(km(a.puntos) - a.distanciaKm) / a.distanciaKm).toBeLessThan(0.05)
  })
})

describe('rutas de ejemplo: web = Android', () => {
  const kt = leer('apps/android/core/datos/src/commonMain/kotlin/com/gacelas/data/mock/MockData.kt')
  const android = [
    ...kt.matchAll(/Ruta\("(\d+)", "([^"]+)", ([\d.]+), Dificultad\.(\w+), (\d+), "([^"]+)"/g),
  ].map((m) => ({
    id: m[1],
    nombre: m[2],
    distanciaKm: Number(m[3]),
    dificultad: DIF[m[4]],
    elevacionM: Number(m[5]),
    descripcion: m[6],
  }))
  const trazosKt = leer(
    'apps/android/contextos/rutas/infrastructure/src/commonMain/kotlin/com/gacelas/rutas/infrastructure/TrazosRutas.kt'
  )
  const trazos = Object.fromEntries(
    [...trazosKt.matchAll(/"(\w+)" to listOf\(([^)]*(?:\)[^)]*)*?)\),\n/g)].map((m) => [
      m[1],
      [...m[2].matchAll(/(-?[\d.]+) to (-?[\d.]+)/g)].map((p) => ({
        lat: Number(p[1]),
        lng: Number(p[2]),
      })),
    ])
  )

  it('mismos datos', () => {
    expect(android.length).toBe(rutasMock.length)
    expect(
      rutasMock.map(({ id, nombre, distanciaKm, dificultad, elevacionM, descripcion }) => ({
        id,
        nombre,
        distanciaKm,
        dificultad,
        elevacionM,
        descripcion,
      }))
    ).toEqual(android)
  })

  it('mismos trazados y la distancia corresponde al trazado', () => {
    expect(Object.keys(TRAZOS_RUTAS).sort()).toEqual(Object.keys(trazos).sort())
    for (const [id, p] of Object.entries(trazos)) {
      expect(TRAZOS_RUTAS[id]).toEqual(p)
      const r = rutasMock.find((x) => x.id === id)
      if (r) expect(Math.abs(km(p) - r.distanciaKm)).toBeLessThan(0.1)
    }
  })
})
