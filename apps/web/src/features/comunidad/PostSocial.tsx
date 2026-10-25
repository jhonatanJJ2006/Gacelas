import { useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { TrazadoRuta } from '../../components/TrazadoRuta'
import type { PuntoGps } from '../../lib/geo'
import type { Evento } from '../../mocks/data'

/** Insignia de nivel del corredor (misma regla que Android). */
export function InsigniaNivel({ nivel }: { nivel: string }) {
  return (
    <span
      className="inline-flex items-center gap-1 rounded-full bg-secondary-600/20 text-secondary px-2 py-0.5 text-[11px] font-semibold whitespace-nowrap"
      data-testid="insignia-nivel"
    >
      <span className="material-symbols-outlined text-[13px]">directions_run</span>
      {nivel}
    </span>
  )
}

/** Carrusel de fotos con el trazado de la ruta como primera diapositiva. */
export function Carrusel({
  imagenes,
  puntos,
  desnivelM,
}: {
  imagenes: string[]
  puntos: PuntoGps[]
  desnivelM?: number
}) {
  const ref = useRef<HTMLDivElement>(null)
  const [i, setI] = useState(0)
  const mapa = puntos.length > 1
  const total = imagenes.length + (mapa || imagenes.length === 0 ? 1 : 0)
  return (
    <div className="relative mb-2 max-h-[200px] lg:max-w-[420px]">
      <div
        ref={ref}
        onScroll={(e) => {
          const el = e.currentTarget
          setI(Math.round(el.scrollLeft / Math.max(1, el.clientWidth)))
        }}
        className="flex overflow-x-auto snap-x snap-mandatory rounded-lg border border-c2-linea no-scrollbar"
        data-testid="carrusel"
        aria-label="Fotos de la publicación"
      >
        {(mapa || imagenes.length === 0) && (
          <div className="snap-center shrink-0 w-full h-[180px] lg:h-[150px] bg-c2-s2 flex items-center justify-center relative">
            {mapa ? (
              <TrazadoRuta puntos={puntos} className="w-full h-full p-4" />
            ) : (
              <span className="material-symbols-outlined text-[64px] text-primary">route</span>
            )}
            {(desnivelM ?? 0) > 0 && (
              <span className="absolute left-3 bottom-3 rounded-full bg-bg-primary/80 px-3 py-1 text-[11px] text-text-primary flex items-center gap-1">
                <span className="material-symbols-outlined text-[14px] text-primary">
                  landscape
                </span>
                Desnivel +{desnivelM} m
              </span>
            )}
          </div>
        )}
        {imagenes.map((src, k) => (
          <img
            key={src}
            src={src}
            alt={`Foto ${k + 1}`}
            loading="lazy"
            className="snap-center shrink-0 w-full h-[180px] lg:h-[150px] object-cover"
          />
        ))}
      </div>
      {total > 1 && (
        <div className="absolute bottom-2 inset-x-0 flex justify-center gap-1.5">
          {Array.from({ length: total }).map((_, k) => (
            <span
              key={k}
              className={`h-1.5 rounded-full transition-all ${k === i ? 'w-4 bg-primary' : 'w-1.5 bg-white/60'}`}
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
