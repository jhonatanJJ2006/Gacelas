import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { usuarioActualMock } from '../../../../mocks/data'
import {
  historico,
  kmMensual,
  medallas,
  records,
  resumenMes,
} from '../../../../shared/kernel/estadisticas'
import { formatearHms, formatearMs, formatearRitmo, Unidades } from '../../../../shared/lib/geo'
import { useEsEscritorio } from '../../../../shared/lib/useEsEscritorio'
import { BotonTexto, Dialogo } from '../../../../shared/ui/ui'
import { TarjetaRacha } from '../../../actividad'
import { useActividad } from '../../../actividad/api'
import { EstadisticasPerfil } from '../../../analitica'
import { ListaLogros } from '../../../comunidad'
import { useComunidad } from '../../../comunidad/api'
import { useIdentidad } from '../../application/estado'
import {
  Contadores,
  EstadisticasHistoricas,
  KilometrajeMensual,
  Medallero,
  Records,
} from './SeccionesPerfil'

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
  const sesion = useIdentidad((s) => s.sesion)
  const carreras = useActividad((s) => s.carreras)
  const m = useIdentidad((s) => s.ajustes.unidadesMetricas)
  const eliminar = useActividad((s) => s.eliminarCarrera)
  const [borrar, setBorrar] = useState<string | null>(null)
  const mes = resumenMes(carreras, Date.now())
  const nombre = sesion?.nombre ?? usuarioActualMock.nombre
  const seguidores = useComunidad((s) => s.seguidores)
  const siguiendo = useComunidad((s) => s.siguiendo)
  const refrescarFeed = useComunidad((s) => s.refrescarFeed)
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
        {/* Android: cabecera en fila con foto 80, nombre Anton y datos en una linea. */}
        <div className="flex items-center mb-4">
          <FotoPerfil foto={sesion?.foto} nombre={nombre} className="w-20 h-20 shrink-0" />
          <div className="flex-1 min-w-0 pl-3.5">
            <h1 className="font-headline-md text-headline-md text-text-primary truncate">
              {nombre}
            </h1>
            <p className="text-body-sm text-c2-muted truncate">
              {[`@${sesion?.username ?? usuarioActualMock.username}`, sesion?.ubicacion]
                .filter(Boolean)
                .join(' · ')}
            </p>
            {sesion?.bio && <p className="text-body-sm text-c2-muted line-clamp-2">{sesion.bio}</p>}
            <p className="mt-1 text-[12px] font-extrabold uppercase tracking-[0.12em] text-c2-violeta">
              Club Gacelas Loja
            </p>
          </div>
          <Link
            to="/perfil/editar"
            aria-label="Editar perfil"
            className="self-start w-12 h-12 rounded bg-c2-s2 flex items-center justify-center text-c2-violeta"
          >
            <span className="material-symbols-outlined fill">edit</span>
          </Link>
        </div>

        <Contadores
          carreras={carreras.length}
          seguidores={seguidores}
          siguiendo={siguiendo.length}
        />

        <div className="grid grid-cols-2 gap-2 mb-2">
          <Link
            to="/perfil/avatar"
            className="forma-boton-c2 flex items-center justify-center gap-1 bg-c2-morado text-on-primary py-3 font-label-lg text-label-lg"
          >
            <span className="material-symbols-outlined fill text-[18px]">person</span>
            Editar avatar 3D
          </Link>
          <Link
            to="/ajustes"
            className="forma-boton-c2 flex items-center justify-center gap-1 bg-c2-s2 text-text-primary py-3 font-label-lg text-label-lg"
          >
            <span className="material-symbols-outlined fill text-[18px]">settings</span>
            Configuración
          </Link>
        </div>

        <Link
          to="/perfil/reportes"
          data-testid="ir-reportes"
          className="forma-boton-c2 mb-4 flex items-center justify-center gap-1 bg-c2-s2 py-3 font-label-lg text-label-lg text-text-primary"
        >
          <span className="material-symbols-outlined fill text-[18px] text-c2-violeta">
            bar_chart
          </span>
          Mis reportes
        </Link>
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
