// Datos de ejemplo FICTICIOS: no son registros reales del ECU 911.
import { readFileSync } from 'node:fs'
import { describe, expect, it } from 'vitest'
import * as XLSX from 'xlsx'
import {
  agregarLoja,
  diaSemana,
  leerTabla,
  mapearColumnas,
  normalizar,
  parsearCsv,
  parsearFecha,
  recursoMasReciente,
  tablaAFilas,
} from '../../../supabase/functions/importar-ecu911/nucleo'

const leerXlsx = (b: Uint8Array) => {
  const libro = XLSX.read(b, { type: 'array', raw: false })
  return XLSX.utils.sheet_to_json(libro.Sheets[libro.SheetNames[0]], {
    header: 1,
    raw: false,
    defval: '',
  }) as unknown[][]
}
const csv = readFileSync(new URL('./ejemplo-ecu911.csv', import.meta.url), 'utf-8')

describe('parser', () => {
  it('normaliza tildes y mayusculas en las columnas', () => {
    expect(normalizar('Cantón')).toBe('canton')
    expect(normalizar('COD_PARROQUIA')).toBe('codparroquia')
    expect(
      mapearColumnas([
        'FECHA',
        'provincia',
        'Cantón',
        'Cod Parroquia',
        'Parroquia',
        'Servicio',
        'SubTipo',
      ])
    ).toMatchObject({ fecha: 0, canton: 2, codParroquia: 3, subtipo: 6 })
  })
  it('falla si falta una columna', () => {
    expect(() => mapearColumnas(['Fecha', 'Provincia'])).toThrow(/Faltan columnas/)
  })
  it('lee CSV con ; y comillas', () => {
    const t = parsearCsv(csv)
    expect(t).toHaveLength(8)
    expect(t[7][6]).toBe('EJEMPLO; FICTICIO D')
    expect(parsearCsv('a,b\n1,2\r\n')).toEqual([
      ['a', 'b'],
      ['1', '2'],
    ])
  })
  it('parsea fechas dd/mm/yyyy y rechaza invalidas', () => {
    expect(parsearFecha('01/03/2026')).toEqual({ anio: 2026, mes: 3, dia: 1 })
    expect(parsearFecha('31/02/2026')).toBeNull()
    expect(parsearFecha('xx')).toBeNull()
    expect(diaSemana(2026, 3, 1)).toBe(0) // domingo
  })
})

describe('agregacion', () => {
  it('filtra canton Loja y agrega por parroquia, servicio, subtipo y dia', () => {
    const r = agregarLoja(tablaAFilas(parsearCsv(csv)))
    expect(r.filas).toBe(7)
    expect(r.filasLoja).toBe(4)
    expect(r.descartadas).toBe(1)
    expect(r.periodos).toEqual(['2026-03'])
    const a = r.agregados.find((x) => x.cod_parroquia === '110150')
    expect(a).toMatchObject({ conteo: 2, dia_semana: 0, anio: 2026, mes: 3 })
    expect(r.agregados.some((x) => x.cod_parroquia === '110350')).toBe(false)
  })
  it('lee el mismo contenido desde XLSX', () => {
    const hoja = XLSX.utils.aoa_to_sheet(parsearCsv(csv))
    const libro = XLSX.utils.book_new()
    XLSX.utils.book_append_sheet(libro, hoja, 'Hoja1')
    const bytes = new Uint8Array(XLSX.write(libro, { type: 'array', bookType: 'xlsx' }))
    const r = agregarLoja(tablaAFilas(leerTabla('ejemplo.xlsx', bytes, leerXlsx)))
    expect(r.filasLoja).toBe(4)
  })
  it('elige el recurso mensual mas reciente', () => {
    const r = recursoMasReciente([
      { name: 'Diccionario', url: 'https://x/dd.xlsx' },
      { name: 'ECU911_Base de Emergencias_202602', url: 'https://x/a_202602.csv' },
      { name: 'ECU911_Base de Emergencias_202603', url: 'https://x/a_202603.csv' },
      { name: 'ECU911_Base de Emergencias_202512', url: 'https://x/a_202512.xlsx' },
    ])
    expect(r?.url).toBe('https://x/a_202603.csv')
  })
})
