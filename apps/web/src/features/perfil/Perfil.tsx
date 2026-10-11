import { useEsEscritorio } from '../../lib/useEsEscritorio'
import { EstadisticasPerfil } from '../../components/PanelesEscritorio'
import { useState } from 'react'
import { Link } from 'react-router-dom'
import { BotonTexto, Dialogo } from '../../components/ui'
import { resumenMes } from '../../lib/estadisticas'
import { formatearHms, formatearMs, formatearRitmo, Unidades } from '../../lib/geo'
import { usuarioActualMock } from '../../mocks/data'
import { useApp } from '../../store/app'

export function FotoPerfil({
  foto,
  nombre,
  className = 'w-20 h-20',
}: {
  foto: string | null | undefined
  nombre: string
  className?: string
}) {
  if (foto)
    return (
      <img
        src={foto}
        alt="Foto de perfil"
        className={`${className} rounded-full object-cover border-2 border-primary-600`}
      />
    )
  return (
    <div
      className={`${className} rounded-full bg-gradient-to-br from-primary-600 to-primary-400 border-2 border-primary-600 flex items-center justify-center text-bg-primary text-2xl font-bold`}
      aria-label="Foto de perfil"
    >
      {nombre.charAt(0).toUpperCase()}
    </div>
  )
}

export function Perfil() {
  const escritorio = useEsEscritorio()
  const sesion = useApp((s) => s.sesion)
  const carreras = useApp((s) => s.carreras)
  const m = useApp((s) => s.ajustes.unidadesMetricas)
  const eliminar = useApp((s) => s.eliminarCarrera)
  const [borrar, setBorrar] = useState<string | null>(null)
  const mes = resumenMes(carreras, Date.now())
  const nombre = sesion?.nombre ?? usuarioActualMock.nombre
  const totalKm = carreras.reduce((s, c) => s + c.distanciaKm, 0)

  return (
    <div className="flex flex-col w-full contenedor-escritorio lg:max-w-5xl">
      <div className="px-margin pt-space-md pb-space-xl">
        <div className="flex items-center justify-between mb-6">
          <div className="flex items-center gap-4 min-w-0">
            <FotoPerfil foto={sesion?.foto} nombre={nombre} />
            <div className="min-w-0">
              <h1 className="font-headline-md text-headline-md text-text-primary mb-1 truncate">
                {nombre}
              </h1>
              <p className="font-body-md text-body-md text-text-secondary">
                @{sesion?.username ?? usuarioActualMock.username}
              </p>
              {sesion?.bio && <p className="text-body-sm text-text-secondary">{sesion.bio}</p>}
            </div>
          </div>
          <Link
            to="/perfil/editar"
            aria-label="Editar perfil"
            className="w-10 h-10 shrink-0 rounded-full bg-surface-container flex items-center justify-center text-text-secondary hover:text-text-primary transition-colors"
          >
            <span className="material-symbols-outlined">edit</span>
          </Link>
        </div>

        <div className="grid grid-cols-3 gap-4 mb-6">
          <div className="text-center bg-surface-container rounded-xl p-4">
            <div
              className="font-headline-lg text-headline-lg text-primary leading-none mb-1"
              data-testid="total-carreras"
            >
              {carreras.length}
            </div>
            <div className="font-label-md text-label-md text-text-secondary uppercase">
              Carreras
            </div>
          </div>
          <div className="text-center bg-surface-container rounded-xl p-4">
            <div className="font-headline-lg text-headline-lg text-text-primary leading-none mb-1">
              {carreras.filter((c) => c.publica).length}
            </div>
            <div className="font-label-md text-label-md text-text-secondary uppercase">
              Públicas
            </div>
          </div>
          <div className="text-center bg-surface-container rounded-xl p-4">
            <div className="font-headline-lg text-headline-lg text-text-primary leading-none mb-1">
              {Unidades.texto(totalKm, m, 1)}
            </div>
            <div className="font-label-md text-label-md text-text-secondary uppercase">
              {Unidades.etiquetaDistancia(m)} totales
            </div>
          </div>
        </div>

        <div className="bg-surface-container rounded-2xl border border-border-subtle p-6 mb-6">
          <h2 className="font-title-md text-title-md text-text-primary mb-4">
            Estadísticas del mes
          </h2>
          <div className="space-y-4">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <span className="material-symbols-outlined text-primary">straighten</span>
                <span className="font-body-md text-body-md text-text-primary">Distancia total</span>
              </div>
              <span
                className="font-headline-sm text-headline-sm text-primary"
                data-testid="distancia-mes"
              >
                {Unidades.texto(mes.km, m, 1)} {Unidades.etiquetaDistancia(m)}
              </span>
            </div>
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <span className="material-symbols-outlined text-secondary">schedule</span>
                <span className="font-body-md text-body-md text-text-primary">Tiempo total</span>
              </div>
              <span className="font-headline-sm text-headline-sm text-secondary">
                {formatearHms(mes.segundos)}
              </span>
            </div>
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <span className="material-symbols-outlined text-text-primary">trending_up</span>
                <span className="font-body-md text-body-md text-text-primary">Elevación</span>
              </div>
              <span className="font-headline-sm text-headline-sm text-text-primary">
                {mes.desnivelM} m
              </span>
            </div>
          </div>
        </div>

        <div className="space-y-3">
          <Link
            to="/perfil/avatar"
            className="flex items-center justify-between px-4 py-3 bg-surface-container rounded-xl hover:bg-surface-container-high transition-colors"
          >
            <div className="flex items-center gap-3">
              <span className="material-symbols-outlined text-primary">person</span>
              <span className="font-body-md text-body-md text-text-primary">
                Personalizar avatar 3D
              </span>
            </div>
            <span className="material-symbols-outlined text-text-secondary">chevron_right</span>
          </Link>
          <Link
            to="/ajustes"
            className="flex items-center justify-between px-4 py-3 bg-surface-container rounded-xl hover:bg-surface-container-high transition-colors"
          >
            <div className="flex items-center gap-3">
              <span className="material-symbols-outlined text-text-secondary">settings</span>
              <span className="font-body-md text-body-md text-text-primary">
                Ajustes y preferencias
              </span>
            </div>
            <span className="material-symbols-outlined text-text-secondary">chevron_right</span>
          </Link>
        </div>
      </div>

      <div className="px-margin pb-space-3xl">
        <h2 className="font-title-md text-title-md text-text-primary mb-4">Actividad reciente</h2>
        {carreras.length === 0 && (
          <p className="bg-surface-container rounded-xl p-4 text-text-secondary">
            Todavía no guardas carreras. Toca el botón{' '}
            <Link to="/correr" className="text-primary-500 inline-block py-2">
              Correr
            </Link>{' '}
            para grabar la primera.
          </p>
        )}
        <div className="space-y-3">
          {carreras.slice(0, 20).map((c) => (
            <div
              key={c.id}
              className="bg-surface-container rounded-xl p-4 border border-border-subtle"
              data-testid="carrera-guardada"
            >
              <div className="flex items-center justify-between mb-2 gap-2">
                <span className="font-body-md text-body-md text-text-primary font-semibold truncate">
                  {c.titulo}
                </span>
                <div className="flex items-center gap-2 shrink-0">
                  <span className="font-body-sm text-body-sm text-text-secondary">
                    {new Date(c.inicioMs).toLocaleString('es-EC', {
                      day: 'numeric',
                      month: 'short',
                      hour: '2-digit',
                      minute: '2-digit',
                    })}
                  </span>
                  <button
                    onClick={() => setBorrar(c.id)}
                    aria-label={`Eliminar ${c.titulo}`}
                    className="text-text-secondary hover:text-danger"
                  >
                    <span className="material-symbols-outlined text-[18px]">delete</span>
                  </button>
                </div>
              </div>
              <div className="flex items-center gap-4 text-text-secondary">
                <span className="font-label-md text-label-md text-primary-500">
                  {Unidades.texto(c.distanciaKm, m)} {Unidades.etiquetaDistancia(m)}
                </span>
                <span className="font-label-md text-label-md">{formatearMs(c.segundos)}</span>
                <span className="font-label-md text-label-md">
                  {formatearRitmo(
                    Unidades.ritmo(
                      c.distanciaKm > 0.01 ? Math.floor(c.segundos / c.distanciaKm) : null,
                      m
                    )
                  )}{' '}
                  {Unidades.etiquetaRitmo(m)}
                </span>
                {!c.publica && <span className="font-label-md text-label-md">Privada</span>}
              </div>
              {c.nota && <p className="text-body-sm text-text-secondary mt-2">{c.nota}</p>}
            </div>
          ))}
        </div>
      </div>

      <Dialogo
        titulo="¿Eliminar carrera?"
        abierto={borrar != null}
        onCerrar={() => setBorrar(null)}
        acciones={
          <>
            <BotonTexto onClick={() => setBorrar(null)}>Cancelar</BotonTexto>
            <BotonTexto
              peligro
              onClick={() => {
                if (borrar) void eliminar(borrar)
                setBorrar(null)
              }}
            >
              Eliminar
            </BotonTexto>
          </>
        }
      >
        <p>Esta acción no se puede deshacer.</p>
      </Dialogo>
      {escritorio && (
        <div className="px-margin pb-12 lg:px-0">
          <EstadisticasPerfil />
        </div>
      )}
    </div>
  )
}
