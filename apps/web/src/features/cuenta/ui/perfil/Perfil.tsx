import { useEsEscritorio } from '../../../../lib/useEsEscritorio'
import { ListaLogros } from '../../../social/Social'
import { EstadisticasPerfil } from '../../../../components/PanelesEscritorio'
import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { BotonTexto, Dialogo } from '../../../../components/ui'
import { historico, kmMensual, medallas, records, resumenMes } from '../../../../lib/estadisticas'
import {
  Contadores,
  EstadisticasHistoricas,
  KilometrajeMensual,
  Medallero,
  Records,
} from './SeccionesPerfil'
import { formatearHms, formatearMs, formatearRitmo, Unidades } from '../../../../lib/geo'
import { usuarioActualMock } from '../../../../mocks/data'
import { TarjetaRacha } from '../../../../components/Racha'
import { useApp } from '../../../../store/app'

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
      className={`${className} rounded-full bg-gradient-to-br from-primary-600 to-primary-400 border-2 border-primary-600 flex items-center justify-center text-on-primary text-2xl font-bold`}
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
  const seguidores = useApp((s) => s.seguidores)
  const siguiendo = useApp((s) => s.siguiendo)
  const refrescarFeed = useApp((s) => s.refrescarFeed)
  useEffect(() => {
    if (sesion) void refrescarFeed()
  }, [sesion, refrescarFeed])
  const hist = useMemo(() => historico(carreras), [carreras])
  const meses = useMemo(() => kmMensual(carreras, Date.now()), [carreras])
  const recs = useMemo(() => records(carreras), [carreras])
  const meds = useMemo(() => medallas(carreras), [carreras])

  return (
    <div className="flex flex-col w-full contenedor-escritorio lg:max-w-5xl">
      <div className="px-margin pt-space-md pb-space-xl">
        <div className="relative flex flex-col items-center text-center mb-4">
          <Link
            to="/perfil/editar"
            aria-label="Editar perfil"
            className="absolute right-0 top-0 w-10 h-10 rounded-full bg-surface-container flex items-center justify-center text-text-secondary hover:text-text-primary transition-colors"
          >
            <span className="material-symbols-outlined">edit</span>
          </Link>
          <FotoPerfil foto={sesion?.foto} nombre={nombre} className="w-24 h-24" />
          <h1 className="font-display italic uppercase text-[26px] leading-tight text-text-primary mt-3">
            {nombre}
          </h1>
          <p className="font-body-md text-body-md text-text-secondary">
            @{sesion?.username ?? usuarioActualMock.username}
          </p>
          {sesion?.ubicacion && (
            <p className="font-body-sm text-body-sm text-text-secondary flex items-center gap-1">
              <span className="material-symbols-outlined text-[16px]">location_on</span>
              {sesion.ubicacion}
            </p>
          )}
          {sesion?.bio && <p className="text-body-sm text-text-secondary mt-1">{sesion.bio}</p>}
          <span className="mt-2 rounded-full bg-surface-container border border-border-subtle px-3 py-1 font-label-md text-label-md text-text-secondary">
            Club Gacelas Loja
          </span>
        </div>

        <Contadores
          carreras={carreras.length}
          seguidores={seguidores}
          siguiendo={siguiendo.length}
        />

        <div className="grid grid-cols-2 gap-3 mb-6">
          <Link
            to="/perfil/avatar"
            className="flex items-center justify-center gap-2 rounded-xl bg-primary-600 hover:bg-primary-700 text-on-primary py-3 font-label-lg text-label-lg"
          >
            <span className="material-symbols-outlined text-[18px]">person</span>
            Editar avatar 3D
          </Link>
          <Link
            to="/ajustes"
            className="flex items-center justify-center gap-2 rounded-xl bg-surface-container border border-border-subtle text-text-primary py-3 font-label-lg text-label-lg"
          >
            <span className="material-symbols-outlined text-[18px]">settings</span>
            Configuración
          </Link>
        </div>

        <TarjetaRacha />
        <div className="mb-6">
          <ListaLogros />
        </div>

        <EstadisticasHistoricas h={hist} m={m} />
        <KilometrajeMensual meses={meses} m={m} />

        <div className={`tarjeta-c2 p-4 mb-6`}>
          <h2 className="font-title-md text-title-md text-text-primary mb-2">Este mes</h2>
          <div className="grid grid-cols-3 text-center">
            <div>
              <div className="font-title-md text-title-md text-primary" data-testid="distancia-mes">
                {Unidades.texto(mes.km, m, 1)} {Unidades.etiquetaDistancia(m)}
              </div>
              <div className="text-[11px] text-text-secondary">Distancia</div>
            </div>
            <div>
              <div className="font-title-md text-title-md text-secondary">
                {formatearHms(mes.segundos)}
              </div>
              <div className="text-[11px] text-text-secondary">Tiempo</div>
            </div>
            <div>
              <div className="font-title-md text-title-md text-text-primary">{mes.desnivelM} m</div>
              <div className="text-[11px] text-text-secondary">Elevación</div>
            </div>
          </div>
        </div>

        <Records lista={recs} />
        <Medallero lista={meds} />
      </div>

      <div className="px-margin pb-space-3xl">
        <div className="flex items-center justify-between mb-4">
          <h2 className="font-title-md text-title-md text-text-primary">Actividades recientes</h2>
          <span className="font-label-md text-label-md text-text-secondary">
            {carreras.length} en total
          </span>
        </div>
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
              <div className="flex items-start justify-between mb-3 gap-2">
                <div className="min-w-0">
                  <Link
                    to={`/perfil/carrera/${c.id}`}
                    className="block font-body-md text-body-md text-text-primary font-semibold truncate hover:text-primary-400"
                  >
                    {c.titulo}
                  </Link>
                  <span className="font-body-sm text-body-sm text-text-secondary">
                    {new Date(c.inicioMs).toLocaleString('es-EC', {
                      weekday: 'long',
                      day: 'numeric',
                      month: 'short',
                      hour: '2-digit',
                      minute: '2-digit',
                    })}
                  </span>
                </div>
                <div className="flex items-center gap-2 shrink-0">
                  {!c.publica && (
                    <span className="rounded-full bg-surface-container-high px-2 py-0.5 text-[11px] text-text-secondary">
                      Privada
                    </span>
                  )}
                  <button
                    onClick={() => setBorrar(c.id)}
                    aria-label={`Eliminar ${c.titulo}`}
                    className="text-text-secondary hover:text-danger"
                  >
                    <span className="material-symbols-outlined text-[18px]">delete</span>
                  </button>
                </div>
              </div>
              <div className="grid grid-cols-3 rounded-lg bg-surface-container-high py-2 text-center">
                <div>
                  <div className="text-[11px] text-text-secondary">Distancia</div>
                  <div className="font-label-lg text-label-lg text-text-primary">
                    {Unidades.texto(c.distanciaKm, m)} {Unidades.etiquetaDistancia(m)}
                  </div>
                </div>
                <div>
                  <div className="text-[11px] text-text-secondary">Ritmo</div>
                  <div className="font-label-lg text-label-lg text-text-primary">
                    {formatearRitmo(
                      Unidades.ritmo(
                        c.distanciaKm > 0.01 ? Math.floor(c.segundos / c.distanciaKm) : null,
                        m
                      )
                    )}{' '}
                    {Unidades.etiquetaRitmo(m)}
                  </div>
                </div>
                <div>
                  <div className="text-[11px] text-text-secondary">Tiempo</div>
                  <div className="font-label-lg text-label-lg text-text-primary">
                    {formatearMs(c.segundos)}
                  </div>
                </div>
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
