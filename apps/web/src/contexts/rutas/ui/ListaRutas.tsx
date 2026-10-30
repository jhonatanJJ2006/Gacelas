import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { ID_USUARIO } from '../../../mocks/data'
import { Unidades } from '../../../shared/lib/geo'
import { MapaRutas } from '../../../shared/ui/MapaEditable'
import { useIdentidad } from '../../identidad/api'
import { useRutas } from '../application/estado'
import { filtrarRutas, PESTANAS_RUTAS, type PestanaRutas } from '../domain/filtros'
import {
  aplicarFiltros,
  CATEGORIAS_RUTA,
  ETIQUETAS_RUTA,
  FILTROS_DISTANCIA,
  FILTROS_INICIALES,
  ordenarPorCercania,
  type FiltroDistancia,
} from '../domain/rutas'

/** Insignia C2 de las rutas del catalogo oficial de Loja. */
export function InsigniaOficial() {
  return (
    <span
      data-testid="insignia-oficial"
      className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full border border-c2-violeta/60 bg-c2-violeta/15 text-c2-violeta text-[11px] font-semibold uppercase tracking-wide"
    >
      <span className="material-symbols-outlined text-[14px]">verified</span>
      Ruta oficial
    </span>
  )
}

export function ListaRutas() {
  const [pestana, setPestana] = useState<PestanaRutas>('Explorar')
  const [consulta, setConsulta] = useState('')
  const guardadas = useRutas((s) => s.rutasGuardadas)
  const alternar = useRutas((s) => s.alternarRutaGuardada)
  const m = useIdentidad((s) => s.ajustes.unidadesMetricas)
  const todas = useRutas((s) => s.rutas)
  const rutasCercanas = useRutas((s) => s.rutasCercanas)
  const [filtros, setFiltros] = useState(FILTROS_INICIALES)
  const [verMapa, setVerMapa] = useState(false)
  const navigate = useNavigate()
  const sociales = useRutas((s) => s.rutasSociales)
  const [cerca, setCerca] = useState<Record<string, number> | null>(null)
  const [buscandoCerca, setBuscandoCerca] = useState(false)
  const filtradas = aplicarFiltros(
    filtrarRutas(todas, pestana, guardadas, ID_USUARIO, consulta),
    filtros
  )
  const rutas = cerca ? ordenarPorCercania(filtradas, cerca) : filtradas
  const alternarCerca = () => {
    if (cerca) return setCerca(null)
    if (!navigator.geolocation) return
    setBuscandoCerca(true)
    navigator.geolocation.getCurrentPosition(
      (p) =>
        void rutasCercanas(p.coords.latitude, p.coords.longitude)
          .then(setCerca)
          .finally(() => setBuscandoCerca(false)),
      () => setBuscandoCerca(false),
      { maximumAge: 60_000, timeout: 10_000 }
    )
  }
  const chip = (activo: boolean) => (activo ? 'pastilla-c2' : 'pastilla-c2')
  // Linea de datos extra, igual que Android: valoracion, me gusta, distancia a ti y autor.
  const extras = (r: (typeof rutas)[number]) =>
    [
      r.valoracion != null ? `★ ${r.valoracion.toFixed(1)} (${r.resenas ?? 0})` : null,
      (r.meGusta ?? 0) > 0 ? `♥ ${r.meGusta}` : null,
      cerca?.[r.id] != null
        ? `a ${Unidades.texto(cerca[r.id], m, 1)} ${Unidades.etiquetaDistancia(m)}`
        : null,
      r.autorNombre ? `por ${r.autorNombre}` : null,
    ].filter((x): x is string => x != null)

  return (
    <div className="flex flex-col w-full contenedor-escritorio">
      <div className="px-margin pt-space-md pb-space-sm lg:px-0 lg:pb-6">
        <div className="flex items-center justify-between mb-4">
          <h1 className="font-headline-lg text-headline-lg leading-9 text-text-primary">
            Rutas de Loja
          </h1>
          <div className="flex items-center gap-2">
            <button
              onClick={() => setVerMapa((v) => !v)}
              aria-pressed={verMapa}
              data-testid="ver-mapa-rutas"
              aria-label={verMapa ? 'Ver lista' : 'Ver mapa'}
              className="w-12 h-12 rounded-full text-text-primary flex items-center justify-center"
            >
              <span className="material-symbols-outlined fill">{verMapa ? 'list' : 'map'}</span>
            </button>
            {sociales && (
              <Link
                to="/rutas/crear"
                aria-label="Dibujar ruta"
                className="w-12 h-12 rounded-full text-text-primary flex items-center justify-center"
              >
                <span className="material-symbols-outlined fill">draw</span>
              </Link>
            )}
            <Link
              to="/rutas/buscar"
              aria-label="Planificar ruta"
              className="w-12 h-12 rounded-full text-text-primary flex items-center justify-center"
            >
              <span className="material-symbols-outlined fill">directions</span>
            </Link>
          </div>
        </div>

        <div className="flex gap-2 mb-4" role="tablist">
          {PESTANAS_RUTAS.map((tab) => (
            <button
              key={tab}
              role="tab"
              aria-selected={tab === pestana}
              onClick={() => setPestana(tab)}
              className="pastilla-c2"
            >
              {tab === 'Guardadas' ? `Guardadas (${guardadas.length})` : tab}
            </button>
          ))}
        </div>

        <label
          htmlFor="buscar-rutas"
          className="block text-sm font-semibold text-text-secondary mb-1.5"
        >
          Buscar
        </label>
        <div className="relative">
          <input
            id="buscar-rutas"
            type="search"
            aria-label="Buscar rutas"
            value={consulta}
            onChange={(e) => setConsulta(e.target.value)}
            placeholder="Buscar rutas..."
            className="w-full h-14 pl-4 pr-12 text-base bg-surface-container border border-border-subtle rounded-xl text-text-primary placeholder-text-secondary focus:outline-none focus:border-primary-600"
          />
          <span
            aria-hidden="true"
            className="material-symbols-outlined absolute right-4 top-1/2 -translate-y-1/2 text-text-secondary"
          >
            search
          </span>
        </div>

        <div
          className="mt-3 flex gap-2 overflow-x-auto pb-1"
          role="group"
          aria-label="Categoría"
          data-testid="filtro-categoria"
        >
          <button
            aria-pressed={!filtros.categoria}
            onClick={() => setFiltros({ ...filtros, categoria: null })}
            className={chip(!filtros.categoria)}
          >
            Todas
          </button>
          {CATEGORIAS_RUTA.map(([k, v]) => (
            <button
              key={k}
              aria-pressed={filtros.categoria === k}
              onClick={() =>
                setFiltros({ ...filtros, categoria: filtros.categoria === k ? null : k })
              }
              className={chip(filtros.categoria === k)}
            >
              {v}
            </button>
          ))}
        </div>
        <div
          className="mt-2 flex gap-2 overflow-x-auto pb-1"
          aria-label="Filtros de rutas"
          role="group"
        >
          <button
            onClick={alternarCerca}
            aria-pressed={!!cerca}
            data-testid="rutas-cerca"
            className={chip(!!cerca)}
          >
            {buscandoCerca ? 'Buscando…' : 'Cerca de mí'}
          </button>
          {(['Todas', 'Fácil', 'Moderado', 'Difícil'] as const).map((d) => (
            <button
              key={d}
              aria-pressed={filtros.dificultad === d}
              onClick={() => setFiltros({ ...filtros, dificultad: d })}
              className={chip(filtros.dificultad === d)}
            >
              {d === 'Todas' ? 'Toda dificultad' : d}
            </button>
          ))}
        </div>
        <div
          className="mt-2 flex gap-2 overflow-x-auto pb-1"
          role="group"
          aria-label="Distancia y tipo"
        >
          {FILTROS_DISTANCIA.map((d: FiltroDistancia) => (
            <button
              key={d}
              aria-pressed={filtros.distancia === d}
              onClick={() => setFiltros({ ...filtros, distancia: d })}
              className={chip(filtros.distancia === d)}
            >
              {d === 'Todas' ? 'Toda distancia' : d}
            </button>
          ))}
          {Object.entries(ETIQUETAS_RUTA).map(([k, v]) => (
            <button
              key={k}
              aria-pressed={filtros.etiqueta === k}
              onClick={() =>
                setFiltros({ ...filtros, etiqueta: filtros.etiqueta === k ? null : k })
              }
              className={chip(filtros.etiqueta === k)}
            >
              #{v}
            </button>
          ))}
        </div>
      </div>

      {verMapa && (
        <div className="px-margin lg:px-0 pb-4">
          <MapaRutas
            className="h-[420px] lg:h-[560px] rounded-[10px] border border-c2-linea overflow-hidden"
            onAbrir={(id) => navigate(`/rutas/${id}`)}
            rutas={rutas
              .filter((r) => (r.puntos?.length ?? 0) > 0)
              .map((r) => ({ id: r.id, nombre: r.nombre, salida: r.puntos![0] }))}
          />
        </div>
      )}
      <div className="px-margin pb-2 lg:px-0" data-testid="encabezado-lista-rutas">
        <h2 className="font-title-md text-title-md tracking-[0.03em] text-text-primary">
          Rutas locales de Loja
        </h2>
        <p className="font-body-sm text-body-sm text-text-secondary">
          {rutas.length} {rutas.length === 1 ? 'sendero' : 'senderos'}
        </p>
      </div>
      <div className="px-margin space-y-4 pb-space-3xl lg:px-0 lg:grid lg:grid-cols-2 xl:grid-cols-3 lg:gap-6 lg:space-y-0">
        {rutas.length === 0 && (
          <p
            className="tarjeta-c2 p-4 text-body-md text-text-secondary lg:col-span-full"
            data-testid="rutas-vacio"
          >
            {pestana === 'Guardadas'
              ? 'Aún no guardas rutas. Toca el marcador de una ruta para guardarla.'
              : pestana === 'Mis Rutas'
                ? 'Todavía no creaste rutas.'
                : 'No hay rutas que coincidan con la búsqueda.'}
          </p>
        )}
        {rutas.map((ruta) => {
          const guardada = guardadas.includes(ruta.id)
          return (
            <article key={ruta.id} className="relative tarjeta-c2 overflow-hidden">
              <Link to={`/rutas/${ruta.id}`} className="block" aria-label={`Abrir ${ruta.nombre}`}>
                <div className="flex gap-3 p-3 pr-12">
                  <div className="w-[88px] h-[88px] shrink-0 rounded bg-gradient-to-br from-c2-s2 to-[#0c1322] flex items-center justify-center">
                    <span className="material-symbols-outlined fill text-[36px] text-c2-violeta">
                      route
                    </span>
                  </div>
                  <div className="flex-1 min-w-0 flex flex-col gap-1">
                    <h2 className="font-title-md text-title-md tracking-[0.03em] text-text-primary line-clamp-2">
                      {ruta.nombre}
                    </h2>
                    {ruta.oficial && (
                      <span className="self-start">
                        <InsigniaOficial />
                      </span>
                    )}
                    <p className="text-body-sm text-c2-muted line-clamp-2">{ruta.descripcion}</p>
                    {extras(ruta).length > 0 && (
                      <p
                        className="text-label-md font-semibold text-c2-muted"
                        data-testid="ruta-extra"
                      >
                        {extras(ruta).join('  ·  ')}
                      </p>
                    )}
                    <div
                      className="flex flex-wrap items-center gap-x-3.5 gap-y-1"
                      data-testid="metricas-ruta"
                    >
                      <span className="text-label-lg font-semibold text-c2-violeta">
                        {Unidades.texto(ruta.distanciaKm, m, 1)} {Unidades.etiquetaDistancia(m)}
                      </span>
                      <span className="text-label-lg font-semibold text-c2-cielo">
                        +{ruta.elevacionM} m
                      </span>
                      <span
                        className={`text-label-md font-semibold px-2 py-0.5 rounded-full ${
                          ruta.dificultad === 'Fácil'
                            ? 'bg-secondary-500/20 text-secondary-500'
                            : ruta.dificultad === 'Moderado'
                              ? 'bg-primary-500/20 text-primary-500'
                              : 'bg-danger/20 text-danger'
                        }`}
                      >
                        {ruta.dificultad}
                      </span>
                    </div>
                  </div>
                </div>
              </Link>
              <button
                onClick={() => void alternar(ruta.id)}
                aria-label={
                  guardada ? `Quitar ${ruta.nombre} de guardadas` : `Guardar ${ruta.nombre}`
                }
                aria-pressed={guardada}
                className="absolute top-3 right-3 w-8 h-8 flex items-center justify-center text-c2-violeta"
              >
                <span
                  className="material-symbols-outlined"
                  style={{ fontVariationSettings: guardada ? "'FILL' 1" : "'FILL' 0" }}
                >
                  bookmark
                </span>
              </button>
            </article>
          )
        })}
      </div>
    </div>
  )
}
