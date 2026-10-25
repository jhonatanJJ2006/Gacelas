// Importador del ECU 911 (Base de Emergencias) para el canton Loja.
// Uso:
//   pnpm --filter backend incidentes:importar -- <archivo.csv|archivo.xlsx>
//   pnpm --filter backend incidentes:importar -- --descargar
//   pnpm --filter backend incidentes:importar -- <archivo> --simular   (no escribe en Supabase)
// Variables: SUPABASE_URL y SUPABASE_SERVICE_ROLE_KEY (nunca se imprimen).
// Codigos de salida: 0 ok, 1 error general, 2 uso incorrecto, 3 portal bloqueado (403).
import { readFileSync } from 'node:fs'
import { basename, resolve } from 'node:path'
import { createClient } from '@supabase/supabase-js'
import * as XLSX from 'xlsx'
import {
  agregarLoja,
  leerTabla,
  PortalBloqueadoError,
  recursoMasReciente,
  tablaAFilas,
  URL_CKAN,
  type RecursoCkan,
} from '../../../../../supabase/functions/importar-ecu911/nucleo.ts'

export const SALIDA = { ok: 0, error: 1, uso: 2, bloqueado: 3 } as const

export function leerXlsx(bytes: Uint8Array): unknown[][] {
  const libro = XLSX.read(bytes, { type: 'array', raw: false })
  const hoja = libro.Sheets[libro.SheetNames[0]]
  return XLSX.utils.sheet_to_json(hoja, { header: 1, raw: false, defval: '' }) as unknown[][]
}

async function descargar(): Promise<{ nombre: string; bytes: Uint8Array }> {
  console.log('Consultando CKAN de datosabiertos.gob.ec...')
  const r = await fetch(URL_CKAN, { headers: { 'User-Agent': 'Gacelas-importador/1.0' } })
  if (r.status === 403) throw new PortalBloqueadoError(403)
  if (!r.ok) throw new Error(`CKAN respondio ${r.status}`)
  const json = (await r.json()) as { result?: { resources?: RecursoCkan[] } }
  const recurso = recursoMasReciente(json.result?.resources ?? [])
  if (!recurso) throw new Error('No se encontro un recurso mensual CSV/XLSX en el dataset')
  console.log(`Descargando ${recurso.name ?? recurso.url}`)
  const a = await fetch(recurso.url)
  if (a.status === 403) throw new PortalBloqueadoError(403)
  if (!a.ok) throw new Error(`La descarga del recurso respondio ${a.status}`)
  return {
    nombre: basename(new URL(recurso.url).pathname),
    bytes: new Uint8Array(await a.arrayBuffer()),
  }
}

async function main(args: string[]): Promise<number> {
  const opciones = args.filter((a) => a !== '--')
  const simular = opciones.includes('--simular')
  const descarga = opciones.includes('--descargar')
  const ruta = opciones.find((a) => !a.startsWith('--'))
  if (!descarga && !ruta) {
    console.error('Uso: incidentes:importar -- <archivo.csv|xlsx> | --descargar [--simular]')
    return SALIDA.uso
  }
  const url = process.env.SUPABASE_URL
  const clave = process.env.SUPABASE_SERVICE_ROLE_KEY
  if (!simular && (!url || !clave)) {
    console.error('Faltan SUPABASE_URL y/o SUPABASE_SERVICE_ROLE_KEY en el entorno.')
    return SALIDA.uso
  }
  const supabase = simular ? null : createClient(url!, clave!, { auth: { persistSession: false } })
  let nombre = ruta ? basename(ruta) : 'descarga'
  try {
    const archivo = ruta
      ? // pnpm --filter cambia el cwd a backend/; INIT_CWD es donde se lanzo el comando.
        { nombre, bytes: new Uint8Array(readFileSync(resolve(process.env.INIT_CWD ?? '.', ruta))) }
      : await descargar()
    nombre = archivo.nombre
    const res = agregarLoja(tablaAFilas(leerTabla(archivo.nombre, archivo.bytes, leerXlsx)))
    console.log(
      `Filas: ${res.filas}; canton Loja: ${res.filasLoja}; descartadas: ${res.descartadas}; ` +
        `agregados: ${res.agregados.length}; periodos: ${res.periodos.join(', ') || 'ninguno'}`
    )
    if (!res.agregados.length) throw new Error('El archivo no tiene filas del canton Loja')
    if (!supabase) {
      console.log('Simulacion: no se escribio nada en Supabase.')
      return SALIDA.ok
    }
    const { data, error } = await supabase.rpc('reemplazar_incidentes_ecu911', {
      p_agregados: res.agregados,
      p_archivo: nombre,
      p_filas: res.filas,
      p_filas_loja: res.filasLoja,
    })
    if (error) throw new Error(error.message)
    console.log(`Listo: ${data} agregados reemplazados para ${res.periodos.join(', ')}.`)
    return SALIDA.ok
  } catch (e) {
    const mensaje = e instanceof Error ? e.message : String(e)
    console.error(`Error: ${mensaje}`)
    if (supabase)
      await supabase.rpc('registrar_error_ecu911', { p_archivo: nombre, p_mensaje: mensaje })
    return e instanceof PortalBloqueadoError ? SALIDA.bloqueado : SALIDA.error
  }
}

main(process.argv.slice(2)).then((codigo) => process.exit(codigo))
