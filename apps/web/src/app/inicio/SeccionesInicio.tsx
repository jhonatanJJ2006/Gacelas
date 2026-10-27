import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import type { Evento, Ruta } from '../../mocks/data'
import type { PublicacionRemota } from '../../shared/kernel/comunidad'
import type { ResumenPeriodo, RetoMes } from '../../shared/kernel/estadisticas'
import type { CarreraGuardada } from '../../shared/kernel/tipos'
import { formatearHms, formatearRitmo, Unidades } from '../../shared/lib/geo'

import { haceTiempo } from '../../shared/lib/tiempo'
import {
  elegirRutaSugerida,
  guardarCacheSugerida,
  leerCacheSugerida,
  rutaSugeridaRemota,
  type RespuestaSugerida,
} from '../../contexts/rutas/api'

const DIAS = ['L', 'M', 'X', 'J', 'V', 'S', 'D']

const iniciales = (n: string) =>
  n
    .split(' ')
    .filter(Boolean)
    .slice(0, 2)
    .map((p) => p[0]!.toUpperCase())
    .join('')

export function SemanaAtletica({
  semana,
  dias,
  hoy,
  metaCarreras,
  rango,
  m,
}: {
  semana: ResumenPeriodo
  dias: Set<number>
  hoy: number
  metaCarreras: number
  rango: string
  m: boolean
}) {
  const pct = Math.min(100, Math.round((semana.carreras / Math.max(1, metaCarreras)) * 100))
  const ritmo = semana.km > 0.01 ? Math.floor(semana.segundos / semana.km) : null
  return (
    <section className="space-y-space-md lg:col-span-5" data-testid="semana-atletica">
      <div className="flex items-center justify-between">
        <h2 className="font-title-md text-title-md text-text-primary">Tu semana atlética</h2>
        <span className="font-label-md text-label-md text-text-secondary">{rango}</span>
      </div>
      <div className="tarjeta-c2 p-4 space-y-2">
        <div className="flex items-center justify-between">
          <span className="font-label-lg text-label-lg text-text-primary flex items-center gap-2">
            <span className="material-symbols-outlined text-[18px] text-secondary">flag</span>
            Meta semanal: {metaCarreras} {metaCarreras === 1 ? 'carrera' : 'carreras'}
          </span>
          <span className="font-label-md text-label-md text-secondary" data-testid="meta-pct">
            {pct}%
          </span>
        </div>
        <div className="h-2 rounded-full bg-surface-container-highest overflow-hidden">
          <div className="h-full rounded-full bg-secondary" style={{ width: `${pct}%` }} />
        </div>
        <div className="flex justify-between font-body-sm text-body-sm text-text-secondary">
          <span>
            {Unidades.texto(semana.km, m, 1)} {Unidades.etiquetaDistancia(m)} recorridos
          </span>
          <span>
            {semana.carreras} de {metaCarreras}
          </span>
        </div>
      </div>
      <div className="grid grid-cols-3 gap-2">
        {[
          ['Tiempo total', formatearHms(semana.segundos), 'schedule'],
          [
            `Ritmo (${Unidades.etiquetaRitmo(m)})`,
            formatearRitmo(Unidades.ritmo(ritmo, m)),
            'speed',
          ],
          ['Desnivel', `+${semana.desnivelM} m`, 'landscape'],
        ].map(([t, v, i]) => (
          <div key={t} className="tarjeta-c2 p-3 flex flex-col items-center">
            <span className="material-symbols-outlined text-[18px] text-primary">{i}</span>
            <span className="font-title-md text-title-md text-text-primary whitespace-nowrap">
              {v}
            </span>
            <span className="font-label-sm text-[11px] text-text-secondary text-center">{t}</span>
          </div>
        ))}
      </div>
      <div className="flex gap-2">
        {DIAS.map((day, index) => (
          <div
            key={day}
            data-corrido={dias.has(index)}
            className={`flex-1 aspect-square rounded-xl flex flex-col items-center justify-center ${
              dias.has(index)
                ? 'bg-secondary-600/20 border-2 border-secondary-600'
                : index === hoy
                  ? 'bg-surface-container border-2 border-primary-600'
                  : 'bg-surface-container border border-border-subtle'
            }`}
          >
            <span className="font-label-md text-label-md text-text-primary">{day}</span>
            {dias.has(index) && (
              <span className="material-symbols-outlined text-[16px] text-secondary-500 mt-1">
                check_circle
              </span>
            )}
          </div>
        ))}
      </div>
    </section>
  )
}

export function TarjetaEvento({ evento, inscrito }: { evento: Evento; inscrito: boolean }) {
  const d = evento.inicioMs ? new Date(evento.inicioMs) : null
  return (
    <Link
      to={`/comunidad/eventos/${evento.id}`}
      className="block tarjeta-c2 p-4"
      data-testid="tarjeta-evento"
    >
      <div className="flex items-start justify-between gap-3">
        <h3 className="font-title-md text-title-md text-text-primary">{evento.titulo}</h3>
        {d && (
          <div className="shrink-0 w-14 rounded-xl bg-primary-600/20 border border-primary/30 flex flex-col items-center py-1">
            <span className="font-title-md text-title-md text-text-primary leading-none">
              {d.getDate()}
            </span>
            <span className="text-[11px] uppercase text-primary">
              {d.toLocaleDateString('es-EC', { month: 'short' }).replace('.', '')}
            </span>
          </div>
        )}
      </div>
      <div className="flex flex-wrap gap-2 mt-3">
        <span className="inline-flex items-center gap-1 rounded-full bg-surface-container-high px-3 py-1 font-label-md text-label-md text-text-primary">
          <span className="material-symbols-outlined text-[16px] text-primary">schedule</span>
          {evento.hora}
        </span>
        {evento.ubicacion && (
          <span className="inline-flex items-center gap-1 rounded-full bg-surface-container-high px-3 py-1 font-label-md text-label-md text-text-primary">
            <span className="material-symbols-outlined text-[16px] text-secondary">
              location_on
            </span>
            {evento.ubicacion}
          </span>
        )}
      </div>
      <p className="font-label-md text-label-md text-text-secondary mt-3">
        {evento.participantes + (inscrito ? 1 : 0)} inscritos
        {inscrito ? ' (tú incluido)' : ''}
      </p>
    </Link>
  )
}

export function TarjetaReto({ reto, m }: { reto: RetoMes; m: boolean }) {
  const pct = Math.min(100, (reto.km / reto.metaKm) * 100)
  return (
    <section className="lg:col-span-5" data-testid="reto-mes">
      <div className="rounded-2xl border border-border-subtle p-4 bg-gradient-to-br from-primary-700/40 to-surface-container">
        <div className="flex justify-between items-center">
          <span className="font-label-md text-label-md text-primary uppercase tracking-wide">
            Reto del mes
          </span>
          <span className="font-label-md text-label-md text-text-secondary">
            Termina en {reto.diasRestantes} {reto.diasRestantes === 1 ? 'día' : 'días'}
          </span>
        </div>
        <h3 className="font-title-lg text-title-lg text-text-primary mt-1">{reto.titulo}</h3>
        <div className="h-2 rounded-full bg-surface-container-highest overflow-hidden mt-3">
          <div className="h-full rounded-full bg-primary" style={{ width: `${pct}%` }} />
        </div>
        <div className="flex justify-between font-body-sm text-body-sm text-text-secondary mt-1">
          <span className="text-text-primary">
            {Unidades.texto(reto.km, m, 1)} {Unidades.etiquetaDistancia(m)}
          </span>
          <span>
            Meta {Unidades.texto(reto.metaKm, m, 0)} {Unidades.etiquetaDistancia(m)}
          </span>
        </div>
      </div>
    </section>
  )
}

export function AmigosEnRuta({ lista, m }: { lista: PublicacionRemota[]; m: boolean }) {
  return (
    <section className="space-y-space-md lg:col-span-5" data-testid="amigos-en-ruta">
      <div className="flex items-center justify-between">
        <h2 className="font-title-md text-title-md text-text-primary">Amigos en ruta</h2>
        <Link
          to="/comunidad"
          className="inline-block py-2 font-label-md text-label-md text-primary"
        >
          Ver todos
        </Link>
      </div>
      {lista.length === 0 ? (
        <p className="font-body-md text-body-md text-text-secondary tarjeta-c2 p-4">
          Sigue a corredores en Comunidad para ver aquí su actividad.
        </p>
      ) : (
        <ul className="space-y-2">
          {lista.map((p) => (
            <li key={p.id} className="tarjeta-c2 p-3 flex items-center gap-3">
              {p.avatar ? (
                <img src={p.avatar} alt="" className="w-10 h-10 rounded-full object-cover" />
              ) : (
                <span className="w-10 h-10 rounded-full bg-primary-600/30 text-primary font-bold flex items-center justify-center">
                  {iniciales(p.autor)}
                </span>
              )}
              <div className="flex-1 min-w-0">
                <p className="font-label-lg text-label-lg text-text-primary truncate">{p.autor}</p>
                <p className="font-body-sm text-body-sm text-text-secondary truncate">
                  <span className="text-text-primary font-semibold">
                    {Unidades.texto(p.distanciaKm, m, 1)} {Unidades.etiquetaDistancia(m)}
                  </span>{' '}
                  en {p.titulo}
                </p>
                <p className="text-[11px] text-text-secondary">{haceTiempo(p.inicioMs)}</p>
              </div>
              <span className="inline-flex items-center gap-1 rounded-full bg-surface-container-high px-3 py-1 font-label-md text-label-md text-text-secondary">
                <span className="material-symbols-outlined text-[16px] text-primary">favorite</span>
                {p.likes}
              </span>
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}

/**
 * Ruta sugerida para hoy: la decide Supabase (ruta_sugerida_hoy) con las reglas de
 * docs/RUTA-SUGERIDA.md; sin red se usa la misma regla local con la ultima lista de rutas seguras.
 */
export function useRutaSugerida(
  rutas: Ruta[],
  carreras: CarreraGuardada[]
): ResultadoSugerida | null {
  const [remota, setRemota] = useState<RespuestaSugerida | null | undefined>(undefined)
  useEffect(() => {
    let vivo = true
    const pedir = (u?: { lat: number; lng: number } | null) =>
      rutaSugeridaRemota(u)
        .catch(() => null)
        .then((r) => {
          if (!vivo) return
          if (r) guardarCacheSugerida(r)
          setRemota(r)
        })
    // Solo usa la ubicacion si ya hay permiso; nunca pide permiso desde Inicio.
    const permisos = typeof navigator !== 'undefined' ? navigator.permissions : undefined
    if (permisos?.query && navigator.geolocation) {
      permisos
        .query({ name: 'geolocation' as PermissionName })
        .then((p) => {
          if (p.state !== 'granted') return pedir(null)
          navigator.geolocation.getCurrentPosition(
            (pos) => pedir({ lat: pos.coords.latitude, lng: pos.coords.longitude }),
            () => pedir(null),
            { maximumAge: 600_000, timeout: 4000 }
          )
        })
        .catch(() => pedir(null))
    } else pedir(null)
    return () => {
      vivo = false
    }
  }, [])
  if (remota === undefined) return null
  if (remota) {
    if (remota.estado === 'sin_ruta') return { estado: 'sin_ruta', motivo: remota.motivo ?? '' }
    const ruta = rutas.find((r) => r.id === remota.rutaId)
    if (ruta) return { estado: 'ok', ruta, nivel: remota.nivel }
  }
  const cache = leerCacheSugerida()
  const local = elegirRutaSugerida({
    rutas,
    carreras,
    seguras: new Set(cache.seguras),
    historial: cache.historial,
    ahoraMs: Date.now(),
  })
  if (local.estado === 'sin_ruta' && cache.seguras.length === 0) {
    return { estado: 'sin_ruta', motivo: 'Conéctate para ver la ruta segura de hoy.' }
  }
  return local.estado === 'ok'
    ? { estado: 'ok', ruta: local.ruta, offline: true }
    : { estado: 'sin_ruta', motivo: local.motivo }
}

export type ResultadoSugerida =
  | { estado: 'ok'; ruta: Ruta; nivel?: string; offline?: boolean }
  | { estado: 'sin_ruta'; motivo: string }

export function RutaSugerida({
  rutas,
  carreras,
  m,
}: {
  rutas: Ruta[]
  carreras: CarreraGuardada[]
  m: boolean
}) {
  const sugerida = useRutaSugerida(rutas, carreras)
  return <TarjetaRutaSugerida sugerida={sugerida} m={m} />
}

export function TarjetaRutaSugerida({
  sugerida,
  m,
}: {
  sugerida: ResultadoSugerida | null
  m: boolean
}) {
  const ruta = sugerida?.estado === 'ok' ? sugerida.ruta : undefined
  return (
    <section className="space-y-space-md lg:col-span-5" data-testid="ruta-sugerida">
      <div className="flex items-center justify-between">
        <h2 className="font-title-md text-title-md text-text-primary">Ruta sugerida para hoy</h2>
        <Link to="/rutas" className="inline-block py-2 font-label-md text-label-md text-primary">
          Ver rutas
        </Link>
      </div>
      {ruta ? (
        <Link
          to={`/rutas/${ruta.id}`}
          className="tarjeta-c2 bg-c2-card p-3 flex items-center gap-3 hover:bg-c2-s2 transition-colors"
        >
          <span className="w-[88px] h-[88px] shrink-0 bg-gradient-to-br from-c2-s2 to-c2-bg flex items-center justify-center rounded-[4px]">
            <span className="material-symbols-outlined text-[34px] text-c2-violeta">route</span>
          </span>
          <span className="flex-1 min-w-0">
            <span className="block font-title-md text-title-md text-text-primary truncate">
              {ruta.nombre}
            </span>
            <span className="block text-body-sm text-c2-muted line-clamp-2">
              {ruta.descripcion}
            </span>
            <span className="mt-1 flex gap-3 text-[13px] font-bold">
              <span className="text-c2-violeta">
                {Unidades.texto(ruta.distanciaKm, m, 1)} {Unidades.etiquetaDistancia(m)}
              </span>
              <span className="text-c2-cielo">+{ruta.elevacionM} m</span>
              <span className="text-c2-menta">{ruta.dificultad}</span>
            </span>
            <span
              className="mt-1 flex items-center gap-1 text-[12px] text-c2-menta"
              data-testid="ruta-sugerida-segura"
            >
              <span className="material-symbols-outlined text-[14px]">verified_user</span>
              {sugerida?.estado === 'ok' && sugerida.offline
                ? 'Segura según la última consulta. No la corriste en 3 días'
                : 'Zona segura a esta hora. No la corriste en 3 días'}
            </span>
          </span>
        </Link>
      ) : sugerida === null ? (
        <p className="tarjeta-c2 bg-c2-card p-4 text-body-md text-c2-muted">
          Buscando una ruta segura para hoy...
        </p>
      ) : (
        <p
          className="tarjeta-c2 bg-c2-card p-4 text-body-md text-c2-muted"
          data-testid="ruta-sugerida-vacia"
        >
          {sugerida.estado === 'sin_ruta' ? sugerida.motivo : 'No encontramos la ruta de hoy.'}{' '}
          Explora Rutas para ver todas las opciones.
        </p>
      )}
    </section>
  )
}
