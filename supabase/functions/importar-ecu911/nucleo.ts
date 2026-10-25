// Nucleo compartido del importador del ECU 911 (Node y Deno). Sin dependencias: el lector de
// XLSX se inyecta desde fuera para que funcione igual en el script y en la Edge Function.

export interface FilaEcu911 {
  fecha: string
  provincia: string
  canton: string
  codParroquia: string
  parroquia: string
  servicio: string
  subtipo: string
}

export interface Agregado {
  cod_parroquia: string
  servicio: string
  subtipo: string
  anio: number
  mes: number
  dia_semana: number
  conteo: number
}

export interface ResultadoAgregacion {
  agregados: Agregado[]
  filas: number
  filasLoja: number
  descartadas: number
  periodos: string[]
}

/** Normaliza un nombre de columna o un valor: sin tildes, minusculas, solo letras y numeros. */
export function normalizar(texto: unknown): string {
  return String(texto ?? '')
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase()
    .replace(/[^a-z0-9]/g, '')
}

const COLUMNAS: Record<keyof FilaEcu911, string[]> = {
  fecha: ['fecha'],
  provincia: ['provincia'],
  canton: ['canton'],
  codParroquia: ['codparroquia', 'codigoparroquia', 'codparr'],
  parroquia: ['parroquia'],
  servicio: ['servicio'],
  subtipo: ['subtipo'],
}

/** Devuelve el indice de cada columna requerida o lanza un error con las que faltan. */
export function mapearColumnas(cabecera: unknown[]): Record<keyof FilaEcu911, number> {
  const norm = cabecera.map(normalizar)
  const mapa = {} as Record<keyof FilaEcu911, number>
  const faltan: string[] = []
  for (const [clave, alias] of Object.entries(COLUMNAS) as [keyof FilaEcu911, string[]][]) {
    const i = norm.findIndex((c) => alias.includes(c))
    if (i < 0) faltan.push(clave)
    mapa[clave] = i
  }
  if (faltan.length) throw new Error(`Faltan columnas en el archivo: ${faltan.join(', ')}`)
  return mapa
}

/** Parser CSV minimo con comillas; detecta separador ; o , en la cabecera. */
export function parsearCsv(texto: string): string[][] {
  const limpio = texto.replace(/^\uFEFF/, '')
  const primera = limpio.slice(0, limpio.indexOf('\n') >>> 0)
  const sep = (primera.match(/;/g)?.length ?? 0) > (primera.match(/,/g)?.length ?? 0) ? ';' : ','
  const filas: string[][] = []
  let fila: string[] = []
  let campo = ''
  let comillas = false
  for (let i = 0; i < limpio.length; i++) {
    const c = limpio[i]
    if (comillas) {
      if (c === '"' && limpio[i + 1] === '"') {
        campo += '"'
        i++
      } else if (c === '"') comillas = false
      else campo += c
    } else if (c === '"') comillas = true
    else if (c === sep) {
      fila.push(campo)
      campo = ''
    } else if (c === '\n' || c === '\r') {
      if (c === '\r' && limpio[i + 1] === '\n') i++
      fila.push(campo)
      if (fila.some((v) => v.trim() !== '')) filas.push(fila)
      fila = []
      campo = ''
    } else campo += c
  }
  fila.push(campo)
  if (fila.some((v) => v.trim() !== '')) filas.push(fila)
  return filas
}

/** Convierte una tabla (cabecera + filas) en filas tipadas. */
export function tablaAFilas(tabla: unknown[][]): FilaEcu911[] {
  if (!tabla.length) return []
  const m = mapearColumnas(tabla[0])
  return tabla.slice(1).map((r) => {
    const v = (k: keyof FilaEcu911) => String(r[m[k]] ?? '').trim()
    return {
      fecha: v('fecha'),
      provincia: v('provincia'),
      canton: v('canton'),
      codParroquia: v('codParroquia'),
      parroquia: v('parroquia'),
      servicio: v('servicio'),
      subtipo: v('subtipo'),
    }
  })
}

/** dd/mm/yyyy (tambien d/m/yyyy, con - o con hora al final) o serial de Excel. */
export function parsearFecha(valor: string): { anio: number; mes: number; dia: number } | null {
  const s = valor.trim()
  const m = s.match(/^(\d{1,2})[/-](\d{1,2})[/-](\d{4})/)
  let d: Date
  if (m) d = new Date(Date.UTC(Number(m[3]), Number(m[2]) - 1, Number(m[1])))
  else if (/^\d{4}-\d{2}-\d{2}/.test(s)) d = new Date(s.slice(0, 10) + 'T00:00:00Z')
  else if (/^\d{5}(\.\d+)?$/.test(s)) d = new Date(Date.UTC(1899, 11, 30) + Number(s) * 86400000)
  else return null
  if (Number.isNaN(d.getTime())) return null
  if (m && (d.getUTCDate() !== Number(m[1]) || d.getUTCMonth() !== Number(m[2]) - 1)) return null
  return { anio: d.getUTCFullYear(), mes: d.getUTCMonth() + 1, dia: d.getUTCDate() }
}

/** dia de la semana 0 = domingo ... 6 = sabado. */
export function diaSemana(anio: number, mes: number, dia: number): number {
  return new Date(Date.UTC(anio, mes - 1, dia)).getUTCDay()
}

/** Filtra canton Loja (provincia Loja) y agrega por parroquia, servicio, subtipo, mes y dia. */
export function agregarLoja(filas: FilaEcu911[]): ResultadoAgregacion {
  const mapa = new Map<string, Agregado>()
  let filasLoja = 0
  let descartadas = 0
  const periodos = new Set<string>()
  for (const f of filas) {
    if (normalizar(f.provincia) !== 'loja' || normalizar(f.canton) !== 'loja') continue
    const fecha = parsearFecha(f.fecha)
    const cod = f.codParroquia.replace(/\D/g, '').padStart(6, '0')
    if (!fecha || !cod.startsWith('1101') || cod.length !== 6) {
      descartadas++
      continue
    }
    filasLoja++
    const servicio = f.servicio.trim() || 'Sin servicio'
    const subtipo = f.subtipo.trim()
    const dia_semana = diaSemana(fecha.anio, fecha.mes, fecha.dia)
    periodos.add(`${fecha.anio}-${String(fecha.mes).padStart(2, '0')}`)
    const clave = [cod, servicio, subtipo, fecha.anio, fecha.mes, dia_semana].join('|')
    const a = mapa.get(clave)
    if (a) a.conteo++
    else
      mapa.set(clave, {
        cod_parroquia: cod,
        servicio,
        subtipo,
        anio: fecha.anio,
        mes: fecha.mes,
        dia_semana,
        conteo: 1,
      })
  }
  return {
    agregados: [...mapa.values()],
    filas: filas.length,
    filasLoja,
    descartadas,
    periodos: [...periodos].sort(),
  }
}

export interface RecursoCkan {
  name?: string
  url: string
  format?: string
  created?: string
  last_modified?: string | null
}

/** Elige el recurso mensual mas reciente (por el AAAAMM del nombre o URL). */
export function recursoMasReciente(recursos: RecursoCkan[]): RecursoCkan | null {
  const candidatos = recursos
    .map((r) => {
      const m = `${r.name ?? ''} ${r.url}`.match(/(20\d{2})(0[1-9]|1[0-2])/)
      return { r, clave: m ? Number(m[1] + m[2]) : 0 }
    })
    .filter((x) => x.clave > 0 && /\.(csv|xlsx)(\?|$)/i.test(x.r.url))
  candidatos.sort((a, b) => b.clave - a.clave)
  return candidatos[0]?.r ?? null
}

export const URL_CKAN =
  'https://www.datosabiertos.gob.ec/api/3/action/package_show?id=base-de-emergencias'

export class PortalBloqueadoError extends Error {
  constructor(public estado: number) {
    super(
      `El portal datosabiertos.gob.ec respondio ${estado}. Suele bloquear conexiones desde fuera ` +
        'de Ecuador. Descarga el archivo a mano y pasalo al importador (ver docs/INCIDENTES-ECU911.md).'
    )
  }
}

/** Convierte bytes de un archivo (.csv o .xlsx) en tabla usando el lector XLSX inyectado. */
export function leerTabla(
  nombre: string,
  bytes: Uint8Array,
  leerXlsx: (b: Uint8Array) => unknown[][]
): unknown[][] {
  const esXlsx = /\.xlsx$/i.test(nombre) || (bytes[0] === 0x50 && bytes[1] === 0x4b)
  if (esXlsx) return leerXlsx(bytes)
  let texto = new TextDecoder('utf-8').decode(bytes)
  if (texto.includes('\uFFFD')) texto = new TextDecoder('latin1').decode(bytes)
  return parsearCsv(texto)
}
