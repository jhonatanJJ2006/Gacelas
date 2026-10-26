// Edge Function importar-ecu911: descarga el recurso mensual mas reciente de la Base de
// Emergencias del ECU 911 (CKAN de datosabiertos.gob.ec), filtra el canton Loja, agrega y
// reemplaza los agregados del periodo. Tambien acepta un archivo en el cuerpo (POST con
// Content-Type text/csv o el XLSX en binario y el encabezado x-archivo con el nombre).
// Se programa con pg_cron (ver docs/INCIDENTES-ECU911.md). No guarda filas crudas.
import { createClient } from 'https://esm.sh/@supabase/supabase-js@2.45.4'
import * as XLSX from 'npm:xlsx@0.18.5'
import {
  agregarLoja,
  leerTabla,
  PortalBloqueadoError,
  recursoMasReciente,
  tablaAFilas,
  URL_CKAN,
  type RecursoCkan,
} from './nucleo.ts'

declare const Deno: {
  env: { get(nombre: string): string | undefined }
  serve(h: (req: Request) => Response | Promise<Response>): void
}

function leerXlsx(bytes: Uint8Array): unknown[][] {
  const libro = XLSX.read(bytes, { type: 'array', cellDates: false, raw: false })
  const hoja = libro.Sheets[libro.SheetNames[0]]
  return XLSX.utils.sheet_to_json(hoja, { header: 1, raw: false, defval: '' }) as unknown[][]
}

async function descargar(): Promise<{ nombre: string; bytes: Uint8Array }> {
  const r = await fetch(URL_CKAN, { headers: { 'User-Agent': 'Gacelas-importador/1.0' } })
  if (r.status === 403) throw new PortalBloqueadoError(403)
  if (!r.ok) throw new Error(`CKAN respondio ${r.status}`)
  const json = (await r.json()) as { result?: { resources?: RecursoCkan[] } }
  const recurso = recursoMasReciente(json.result?.resources ?? [])
  if (!recurso) throw new Error('No se encontro un recurso mensual CSV/XLSX en el dataset')
  const a = await fetch(recurso.url)
  if (a.status === 403) throw new PortalBloqueadoError(403)
  if (!a.ok) throw new Error(`La descarga del recurso respondio ${a.status}`)
  return {
    nombre: recurso.url.split('/').pop() ?? 'recurso',
    bytes: new Uint8Array(await a.arrayBuffer()),
  }
}

Deno.serve(async (req) => {
  const url = Deno.env.get('SUPABASE_URL')
  const clave = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')
  if (!url || !clave) return new Response('Faltan variables de entorno', { status: 500 })
  const auth = req.headers.get('Authorization') ?? ''
  if (auth !== `Bearer ${clave}`) return new Response('No autorizado', { status: 401 })
  const supabase = createClient(url, clave, { auth: { persistSession: false } })
  let nombre = 'desconocido'
  try {
    let archivo: { nombre: string; bytes: Uint8Array }
    const tipo = req.headers.get('content-type') ?? ''
    if (req.method === 'POST' && !tipo.includes('application/json') && req.body) {
      archivo = {
        nombre:
          req.headers.get('x-archivo') ?? (tipo.includes('csv') ? 'manual.csv' : 'manual.xlsx'),
        bytes: new Uint8Array(await req.arrayBuffer()),
      }
    } else {
      archivo = await descargar()
    }
    nombre = archivo.nombre
    const res = agregarLoja(tablaAFilas(leerTabla(archivo.nombre, archivo.bytes, leerXlsx)))
    if (!res.agregados.length) throw new Error('El archivo no tiene filas del canton Loja')
    const { data, error } = await supabase.rpc('reemplazar_incidentes_ecu911', {
      p_agregados: res.agregados,
      p_archivo: nombre,
      p_filas: res.filas,
      p_filas_loja: res.filasLoja,
    })
    if (error) throw new Error(error.message)
    return Response.json({ ok: true, archivo: nombre, periodos: res.periodos, agregados: data })
  } catch (e) {
    const mensaje = e instanceof Error ? e.message : String(e)
    await supabase.rpc('registrar_error_ecu911', { p_archivo: nombre, p_mensaje: mensaje })
    const estado = e instanceof PortalBloqueadoError ? 502 : 500
    return Response.json(
      { ok: false, error: mensaje, bloqueado: e instanceof PortalBloqueadoError },
      { status: estado }
    )
  }
})
