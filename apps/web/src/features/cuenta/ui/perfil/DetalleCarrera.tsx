import { useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { TrazadoRuta } from '../../../../components/TrazadoRuta'
import { BotonTexto, Dialogo } from '../../../../components/ui'
import { formatearMs, formatearRitmo, Unidades } from '../../../../lib/geo'
import { useApp } from '../../../../store/app'

/** Detalle de una carrera guardada: igual que DetalleCarreraContenido de Android. */
export function DetalleCarrera() {
  const { id = '' } = useParams()
  const navigate = useNavigate()
  const c = useApp((s) => s.carreras.find((x) => x.id === id))
  const m = useApp((s) => s.ajustes.unidadesMetricas)
  const eliminar = useApp((s) => s.eliminarCarrera)
  const [confirmar, setConfirmar] = useState(false)

  const encabezado = (
    <header className="flex items-center gap-2 px-margin pt-space-md pb-space-sm lg:px-0">
      <button
        onClick={() => navigate(-1)}
        aria-label="Atrás"
        className="w-11 h-11 rounded-full flex items-center justify-center text-text-primary hover:bg-surface-container"
      >
        <span className="material-symbols-outlined">arrow_back</span>
      </button>
      <h1 className="font-headline-sm text-headline-sm text-text-primary truncate">
        {c?.titulo ?? 'Carrera'}
      </h1>
    </header>
  )

  if (!c)
    return (
      <div className="contenedor-escritorio" data-testid="detalle-carrera">
        {encabezado}
        <p className="px-margin text-text-secondary">Esta carrera ya no está.</p>
      </div>
    )

  const ritmo = c.distanciaKm > 0.01 ? Math.floor(c.segundos / c.distanciaKm) : null
  const fecha = new Date(c.inicioMs).toLocaleString('es-EC', {
    weekday: 'long',
    day: 'numeric',
    month: 'long',
    hour: '2-digit',
    minute: '2-digit',
  })

  return (
    <div className="contenedor-escritorio max-w-3xl pb-space-3xl" data-testid="detalle-carrera">
      {encabezado}
      <div className="px-margin lg:px-0 space-y-4">
        <p className="text-body-md text-text-secondary">{fecha}</p>
        {c.puntos.length >= 2 && (
          <div className="h-56 lg:h-80 rounded-2xl overflow-hidden bg-gradient-to-br from-primary-700/30 to-secondary-600/20">
            <TrazadoRuta puntos={c.puntos} className="w-full h-full p-4" />
          </div>
        )}
        <section className="grid grid-cols-4 gap-2 p-4 rounded-2xl bg-surface-container border border-border-subtle text-center">
          {[
            [Unidades.texto(c.distanciaKm, m), Unidades.etiquetaDistancia(m), 'text-primary-400'],
            [formatearMs(c.segundos), 'Tiempo', 'text-text-primary'],
            [
              formatearRitmo(Unidades.ritmo(ritmo, m)),
              Unidades.etiquetaRitmo(m),
              'text-text-primary',
            ],
            [`${Math.round(c.desnivelM)} m`, 'Desnivel', 'text-text-primary'],
          ].map(([v, e, color]) => (
            <div key={e}>
              <div className={`font-headline-sm text-headline-sm ${color}`}>{v}</div>
              <div className="font-label-md text-label-md text-text-secondary">{e}</div>
            </div>
          ))}
        </section>
        {c.parciales.length > 0 && (
          <section className="p-4 rounded-2xl bg-surface-container border border-border-subtle">
            <h2 className="font-title-md text-title-md text-text-primary mb-2">Parciales</h2>
            <ul>
              {c.parciales.map((p) => (
                <li key={`${p.manual}-${p.numero}`} className="flex justify-between py-1">
                  <span className="text-text-secondary">
                    {p.manual ? `Vuelta ${p.numero}` : `Km ${p.numero}`}
                  </span>
                  <span className="font-label-md text-text-primary">{formatearMs(p.segundos)}</span>
                </li>
              ))}
            </ul>
          </section>
        )}
        {c.nota.trim() && (
          <p className="p-4 rounded-2xl bg-surface-container border border-border-subtle text-text-primary">
            {c.nota}
          </p>
        )}
        <p className="text-body-sm text-text-secondary">{c.publica ? 'Pública' : 'Privada'}</p>
        <button
          onClick={() => setConfirmar(true)}
          className="w-full h-12 rounded-xl border border-danger text-danger font-semibold"
        >
          Eliminar carrera
        </button>
      </div>
      <Dialogo
        titulo="¿Eliminar esta carrera?"
        abierto={confirmar}
        onCerrar={() => setConfirmar(false)}
        acciones={
          <>
            <BotonTexto onClick={() => setConfirmar(false)}>Cancelar</BotonTexto>
            <BotonTexto
              onClick={() => {
                setConfirmar(false)
                void eliminar(c.id).then(() => navigate('/perfil'))
              }}
            >
              Eliminar
            </BotonTexto>
          </>
        }
      >
        <p className="text-text-secondary">
          Se quita de tu historial y de tu cuenta. No se puede deshacer.
        </p>
      </Dialogo>
    </div>
  )
}
