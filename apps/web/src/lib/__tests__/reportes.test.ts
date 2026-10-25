import { describe, expect, it } from 'vitest'
import { aCsv, aPdf } from '../exportar'
import { rangoPeriodo, reportePersonal, textoCompartir } from '../reportesPersonales'
import { estadisticasDemo } from '../../features/admin/api'
import { csvEstadisticas, pdfEstadisticas } from '../../features/admin/Estadisticas'
import { RUTAS_LOJA } from '../../data/rutasLoja'
import type { CarreraGuardada } from '../../data/tipos'

const carrera = (fecha: string, km: number, seg: number, ruta?: number): CarreraGuardada => ({
  id: fecha + km,
  titulo: 'x',
  nota: '',
  publica: true,
  inicioMs: new Date(fecha).getTime(),
  segundos: seg,
  distanciaKm: km,
  desnivelM: km * 10,
  parciales: [],
  puntos:
    ruta == null
      ? []
      : [{ ...RUTAS_LOJA[ruta].puntos![0], tiempoMs: 0, precisionM: 5, altitudM: null }],
})

describe('reporte personal', () => {
  // Miercoles 21 de octubre de 2026.
  const ref = new Date('2026-10-21T12:00:00').getTime()
  const carreras = [
    carrera('2026-10-19T07:00:00', 5, 1500, 0),
    carrera('2026-10-20T07:00:00', 10, 3300, 0),
    carrera('2026-10-21T07:00:00', 3, 960, 2),
    carrera('2026-10-14T07:00:00', 6, 1900),
    carrera('2026-09-10T07:00:00', 8, 2600),
  ]

  it('semana desde el lunes y comparacion con la anterior', () => {
    const r = reportePersonal(carreras, 'semana', ref, RUTAS_LOJA)
    expect(new Date(r.desdeMs).getDay()).toBe(1)
    expect(r.actual).toMatchObject({ carreras: 3, km: 18, segundos: 5760, ritmoSegKm: 320 })
    expect(r.previo.km).toBe(6)
    expect(r.variacionKm).toBe(200)
    expect(r.serie).toHaveLength(7)
    expect(r.serie[1].km).toBe(10)
    expect(r.mejores).toMatchObject({ masLargaKm: 10, mejorRitmoSegKm: 300 })
    expect(r.racha.actual).toBe(3)
    expect(r.rutasFavoritas[0]).toMatchObject({ id: RUTAS_LOJA[0].id, veces: 2 })
    expect(textoCompartir(r)).toContain('18.0 km (+200% vs. periodo anterior)')
  })

  it('mes y año calendario', () => {
    expect(reportePersonal(carreras, 'mes', ref).actual.carreras).toBe(4)
    expect(reportePersonal(carreras, 'mes', ref).previo.km).toBe(8)
    const anio = reportePersonal(carreras, 'anio', ref)
    expect(anio.serie).toHaveLength(12)
    expect(anio.serie[9].km).toBe(24)
    const [a, b] = rangoPeriodo('anio', ref)
    expect(new Date(a).getMonth()).toBe(0)
    expect(new Date(b).getFullYear()).toBe(2027)
  })

  it('sin carreras no rompe', () => {
    const r = reportePersonal([], 'semana', ref)
    expect(r.actual.ritmoSegKm).toBeNull()
    expect(r.variacionKm).toBeNull()
    expect(r.mejores.mejorRitmoSegKm).toBeNull()
  })
})

describe('exportacion', () => {
  it('CSV con ; BOM y comillas', () => {
    const csv = aCsv(['a', 'b'], [['x;y', 1.5]])
    expect(csv.startsWith('\uFEFF')).toBe(true)
    expect(csv).toContain('"x;y";1,5')
  })

  it('PDF valido con xref y acentos en WinAnsi', () => {
    const bytes = aPdf('Estadísticas', [
      { lineas: ['Retención (año)'], barras: [{ etiqueta: 'a', valor: 2 }] },
    ])
    const txt = Array.from(bytes, (b) => String.fromCharCode(b)).join('')
    expect(txt.startsWith('%PDF-1.4')).toBe(true)
    expect(txt.trimEnd().endsWith('%%EOF')).toBe(true)
    expect(txt).toContain('Retenci\u00f3n \\(a\u00f1o\\)')
    const xref = Number(/startxref\n(\d+)/.exec(txt)![1])
    expect(txt.slice(xref, xref + 4)).toBe('xref')
    // Cada offset de la tabla apunta al inicio de su objeto.
    const offsets = [...txt.slice(xref).matchAll(/(\d{10}) 00000 n/g)].map((m) => Number(m[1]))
    offsets.forEach((o, i) => expect(txt.slice(o).startsWith(`${i + 1} 0 obj`)).toBe(true))
  })

  it('PDF de muchas filas pasa a varias paginas', () => {
    const d = estadisticasDemo('2026-01-01', '2026-06-30')
    const txt = Array.from(pdfEstadisticas(d), (b) => String.fromCharCode(b)).join('')
    expect(Number(/\/Count (\d+)/.exec(txt)![1])).toBeGreaterThan(2)
    expect(csvEstadisticas(d).split('\r\n')).toHaveLength(d.serie.length + 2)
  })
})
