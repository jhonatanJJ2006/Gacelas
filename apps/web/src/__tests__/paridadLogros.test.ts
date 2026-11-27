import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { describe, expect, it } from 'vitest'
import { LOGROS } from '../contexts/logros/domain/logros'

/*
 * El catalogo de logros vive en tres sitios que deben decir lo mismo: la web (LOGROS), Android (enum
 * Logro) y la base (migracion 049). packages/shared-types/logros-catalogo.json es la referencia.
 */
interface EntradaCatalogo {
  codigo: string
  titulo: string
  descripcion: string
  comoConseguirlo: string
  categoria: string
  meta: number
  unidad: string
  medallaOrigen: string | null
  orden: number
}

const leer = (ruta: string) => readFileSync(fileURLToPath(new URL(ruta, import.meta.url)), 'utf8')
const catalogo = (
  JSON.parse(leer('../../../../packages/shared-types/logros-catalogo.json')) as {
    logros: EntradaCatalogo[]
  }
).logros

const MEDALLAS_ANTIGUAS = [
  'primera',
  '5k',
  '10k',
  '21k',
  '42k',
  'centenario',
  '500k',
  '1000k',
  'racha7',
  'racha30',
  'desnivel1000',
  'desnivel5000',
]

describe('catalogo de logros unico', () => {
  it('el JSON tiene 26 logros sin repetir y mapea las 12 medallas antiguas una sola vez', () => {
    expect(catalogo).toHaveLength(26)
    expect(new Set(catalogo.map((l) => l.codigo)).size).toBe(26)
    expect(new Set(catalogo.map((l) => l.titulo)).size).toBe(26)
    expect(
      catalogo
        .map((l) => l.medallaOrigen)
        .filter(Boolean)
        .sort()
    ).toEqual([...MEDALLAS_ANTIGUAS].sort())
  })

  it('la web es igual al JSON, en el mismo orden', () => {
    expect(
      LOGROS.map(({ codigo, titulo, descripcion, comoConseguirlo, categoria, meta, unidad }) => ({
        codigo,
        titulo,
        descripcion,
        comoConseguirlo,
        categoria,
        meta,
        unidad,
      }))
    ).toEqual(
      catalogo.map(({ codigo, titulo, descripcion, comoConseguirlo, categoria, meta, unidad }) => ({
        codigo,
        titulo,
        descripcion,
        comoConseguirlo,
        categoria,
        meta,
        unidad,
      }))
    )
  })

  it('Android declara los mismos 26 logros con la misma meta', () => {
    const kt = leer('../../../android/shared/src/commonMain/kotlin/com/gacelas/social/Social.kt')
    const enumKt = kt.slice(kt.indexOf('enum class Logro('), kt.indexOf('fun progresoLogro'))
    const declarados = [
      ...enumKt.matchAll(
        /^\s+\w+\("([a-z0-9_]+)", "([^"]+)", "([^"]+)", "([^"]+)", CategoriaLogro\.(\w+), (\d+), "([^"]*)"\)/gm
      ),
    ].map((m) => ({
      codigo: m[1],
      titulo: m[2],
      descripcion: m[3],
      comoConseguirlo: m[4],
      categoria: m[5].toLowerCase().normalize('NFD').replace(/\p{M}/gu, ''),
      meta: Number(m[6]),
      unidad: m[7],
    }))
    expect(declarados).toEqual(
      catalogo.map(({ codigo, titulo, descripcion, comoConseguirlo, categoria, meta, unidad }) => ({
        codigo,
        titulo,
        descripcion,
        comoConseguirlo,
        categoria,
        meta,
        unidad,
      }))
    )
  })

  it('la migracion 049 inserta el mismo catalogo y la prueba SQL espera 26', () => {
    const sql = leer('../../../../supabase/migrations/049_logros_unificados.sql')
    for (const l of catalogo) {
      expect(sql).toContain(`('${l.codigo}', '${l.titulo.replaceAll("'", "''")}'`)
      if (l.medallaOrigen) expect(sql).toContain(`'${l.medallaOrigen}', ${l.orden})`)
    }
    expect(sql.match(/^ {2}\('[a-z0-9_]+', '/gm)).toHaveLength(26)
  })
})
