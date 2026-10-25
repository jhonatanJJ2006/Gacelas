import { Link } from 'react-router-dom'
import { TrazadoRuta } from '../../components/TrazadoRuta'
import type { PuntoGps } from '../../lib/geo'
import type { Evento } from '../../mocks/data'

/** Nivel corto estilo C2: NV 1, NV 2, NV 3 o ÉLITE (misma regla que Android). */
export function nivelCorto(nivel: string): string {
  const n = nivel.toLowerCase()
  if (n.includes('elite') || n.includes('élite')) return 'ÉLITE'
  if (n.includes('avanz')) return 'NV 3'
  if (n.includes('inter')) return 'NV 2'
  return 'NV 1'
}

/** Insignia de nivel del corredor: paralelogramo violeta. */
export function InsigniaNivel({ nivel }: { nivel: string }) {
  return (
    <span
      className="shrink-0 inline-flex items-center px-1.5 h-4 bg-[#7b1fa2] text-white text-[9px] font-black italic whitespace-nowrap [clip-path:polygon(3px_0,100%_0,calc(100%-3px)_100%,0_100%)]"
      data-testid="insignia-nivel"
      title={nivel}
    >
      {nivelCorto(nivel)}
    </span>
  )
}

/** Usuario corto para mostrar: @nombreapellido sin tildes. */
export function handle(nombre: string): string {
  const base = nombre
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '')
  return `@${base.slice(0, 14) || 'gacela'}`
}

/** Avatar con la inicial (36 px, o 24 px en comentarios). */
export function AvatarLetra({ nombre, chico = false }: { nombre: string; chico?: boolean }) {
  return (
    <span
      className={`${chico ? 'w-6 h-6 text-[11px]' : 'w-9 h-9 text-[14px]'} shrink-0 rounded-full bg-[#7b1fa2] text-white font-bold flex items-center justify-center`}
      aria-hidden="true"
    >
      {(nombre.trim().charAt(0) || 'G').toUpperCase()}
    </span>
  )
}

/** Media del post (maximo 200 px): ruta a la izquierda y hasta dos fotos apiladas a la derecha. */
export function MediaPost({ imagenes, puntos }: { imagenes: string[]; puntos: PuntoGps[] }) {
  const fotos = imagenes.slice(0, 2)
  const ruta = puntos.length > 1
  const alto = fotos.length > 0 ? 'h-[200px]' : 'h-[170px]'
  return (
    <div
      className={`grid gap-1 ${alto} max-w-[520px] rounded-[10px] overflow-hidden border border-c2-linea ${(ruta || fotos.length === 0) && fotos.length > 0 ? 'grid-cols-2' : 'grid-cols-1'}`}
      data-testid="carrusel"
    >
      {(ruta || fotos.length === 0) && (
        <div className="relative bg-c2-s2 min-h-0 flex items-center justify-center">
          {ruta ? (
            <TrazadoRuta puntos={puntos} className="w-full h-full p-3" />
          ) : (
            <span className="material-symbols-outlined text-[48px] text-c2-violeta/70">route</span>
          )}
          <span className="absolute left-2 bottom-2 text-[9px] font-black italic text-white">
            RUTA
          </span>
        </div>
      )}
      {fotos.length > 0 && (
        <div className={`grid gap-1 min-h-0 ${fotos.length > 1 ? 'grid-rows-2' : 'grid-rows-1'}`}>
          {fotos.map((src, k) => (
            <img
              key={src}
              src={src}
              alt={`Foto ${k + 1}`}
              loading="lazy"
              className="w-full h-full object-cover min-h-0"
            />
          ))}
        </div>
      )}
    </div>
  )
}

/** Comunicado del club con el proximo encuentro y boton Asistire (event_rsvps). */
export function TarjetaClub({
  evento,
  inscrito,
  onAlternar,
}: {
  evento: Evento
  inscrito: boolean
  onAlternar: () => void
}) {
  return (
    <article className="tarjeta-c2 p-4" data-testid="publicacion-club">
      <header className="flex items-center gap-3 mb-3">
        <span className="w-11 h-11 rounded-full bg-primary-600 text-on-primary font-bold flex items-center justify-center">
          GL
        </span>
        <div>
          <p className="font-body-md text-body-md text-text-primary font-semibold">
            Club Gacelas Loja
          </p>
          <p className="font-body-sm text-body-sm text-text-secondary">Comunicado oficial</p>
        </div>
      </header>
      <Link
        to={`/comunidad/eventos/${evento.id}`}
        className="block rounded-xl bg-bg-primary/40 border border-border-subtle p-3"
      >
        <p className="font-title-md text-title-md text-text-primary flex items-center gap-2">
          <span className="material-symbols-outlined text-primary">event</span>
          {evento.titulo}
        </p>
        <p className="font-body-sm text-body-sm text-text-secondary mt-1">{evento.descripcion}</p>
        <p className="font-label-md text-label-md text-primary mt-2">
          {evento.fecha}, {evento.hora}
          {evento.ubicacion ? ` · ${evento.ubicacion}` : ''}
        </p>
      </Link>
      <button
        onClick={onAlternar}
        aria-pressed={inscrito}
        className={`mt-3 w-full h-11 rounded-xl font-label-lg text-label-lg flex items-center justify-center gap-2 ${
          inscrito
            ? 'bg-secondary-600 text-white'
            : 'bg-primary-600 text-on-primary hover:bg-primary-700'
        }`}
      >
        <span className="material-symbols-outlined text-[18px]">
          {inscrito ? 'check_circle' : 'event_available'}
        </span>
        {inscrito ? 'Asistiré' : 'Asistir'} ({evento.participantes + (inscrito ? 1 : 0)}{' '}
        confirmados)
      </button>
    </article>
  )
}
