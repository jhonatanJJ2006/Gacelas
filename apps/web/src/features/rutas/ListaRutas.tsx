import { useState } from 'react'
import {
  aplicarFiltros,
  CATEGORIAS_RUTA,
  ETIQUETAS_RUTA,
  FILTROS_DISTANCIA,
  FILTROS_INICIALES,
  ordenarPorCercania,
  type FiltroDistancia,
} from '../../lib/rutas'
import { Estrellas } from './Estrellas'
import { Link, useNavigate } from 'react-router-dom'
import { MapaRutas } from '../../components/MapaEditable'
import { filtrarRutas, PESTANAS_RUTAS, type PestanaRutas } from '../../lib/filtros'
import { Unidades } from '../../lib/geo'
import { ID_USUARIO } from '../../mocks/data'
import { useApp } from '../../store/app'

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
  const guardadas = useApp((s) => s.rutasGuardadas)
  const alternar = useApp((s) => s.alternarRutaGuardada)
  const m = useApp((s) => s.ajustes.unidadesMetricas)
  const todas = useApp((s) => s.rutas)
  const rutasCercanas = useApp((s) => s.rutasCercanas)
  const [filtros, setFiltros] = useState(FILTROS_INICIALES)
  const [verMapa, setVerMapa] = useState(false)
  const navigate = useNavigate()
  const sociales = useApp((s) => s.rutasSociales)
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
  const chip = (activo: boolean) =>
    `shrink-0 min-h-[40px] px-3 rounded-full font-label-md text-label-md border transition-colors ${
      activo
        ? 'bg-primary-600 text-on-primary border-primary-600'
        : 'bg-surface-container text-text-secondary border-border-subtle hover:text-text-primary'
    }`

  return (
    <div className="flex flex-col w-full contenedor-escritorio">
      <div className="px-margin pt-space-md pb-space-sm lg:px-0 lg:pb-6">
        <div className="flex items-center justify-between mb-4">
          <h1 className="font-display italic uppercase text-[26px] text-text-primary">
            Rutas de Loja
          </h1>
          <div className="flex items-center gap-2">
            <button
              onClick={() => setVerMapa((v) => !v)}
              aria-pressed={verMapa}
              data-testid="ver-mapa-rutas"
              className="h-10 px-3 rounded-full bg-surface-container text-text-primary flex items-center gap-1"
            >
              <span className="material-symbols-outlined text-[20px]">
                {verMapa ? 'list' : 'map'}
              </span>
              {verMapa ? 'Lista' : 'Mapa'}
            </button>
            {sociales && (
              <Link
                to="/rutas/crear"
                aria-label="Dibujar ruta"
                className="w-10 h-10 rounded-full bg-surface-container text-text-primary flex items-center justify-center"
              >
                <span className="material-symbols-outlined">draw</span>
              </Link>
            )}
            <Link
              to="/rutas/buscar"
              aria-label="Planificar ruta"
              className="w-10 h-10 rounded-full bg-primary-600 text-on-primary flex items-center justify-center shadow-lg"
            >
              <span className="material-symbols-outlined">directions</span>
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
              className={`px-4 py-2 rounded-full font-label-md text-label-md transition-colors ${
                tab === pestana
                  ? 'bg-primary-600 text-on-primary'
                  : 'bg-surface-container text-text-secondary hover:text-text-primary'
              }`}
            >
              {tab === 'Guardadas' ? `Guardadas (${guardadas.length})` : tab}
            </button>
          ))}
        </div>

        <div className="relative">
          <input
            type="search"
            aria-label="Buscar rutas"
            value={consulta}
            onChange={(e) => setConsulta(e.target.value)}
            placeholder="Buscar rutas..."
            className="w-full h-12 pl-12 pr-4 bg-surface-container border border-border-subtle rounded-xl text-text-primary placeholder-text-secondary focus:outline-none focus:ring-2 focus:ring-primary-600 focus:border-transparent"
          />
          <span className="material-symbols-outlined absolute left-4 top-1/2 -translate-y-1/2 text-text-secondary">
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
            className={chip(!!cerca) + ' flex items-center gap-1'}
          >
            <span className="material-symbols-outlined text-[18px]">near_me</span>
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
        <h2 className="font-title-md text-title-md text-text-primary">Rutas locales de Loja</h2>
        <p className="font-body-sm text-body-sm text-text-secondary">
          {rutas.length} {rutas.length === 1 ? 'sendero' : 'senderos'}
        </p>
      </div>
      <div className="px-margin space-y-4 pb-space-3xl lg:px-0 lg:grid lg:grid-cols-2 xl:grid-cols-3 lg:gap-6 lg:space-y-0">
        {rutas.length === 0 && (
          <p
            className="text-center text-text-secondary py-8 lg:col-span-full"
            data-testid="rutas-vacio"
          >
            {pestana === 'Guardadas' && !consulta
              ? 'Aún no guardas rutas. Toca el marcador de una ruta para guardarla.'
              : 'No hay rutas que coincidan.'}
          </p>
        )}
        {rutas.map((ruta) => {
          const guardada = guardadas.includes(ruta.id)
          return (
            <article
              key={ruta.id}
              className="relative tarjeta-c2 overflow-hidden hover:border-c2-violeta/60 transition-colors"
            >
              <Link to={`/rutas/${ruta.id}`} className="block" aria-label={`Abrir ${ruta.nombre}`}>
                <div className="flex gap-3 p-3 pr-12">
                  <div className="w-[88px] h-[88px] shrink-0 rounded-lg bg-c2-s2 border border-c2-linea flex items-center justify-center">
                    <span className="material-symbols-outlined text-[36px] text-c2-violeta">
                      route
                    </span>
                  </div>
                  <div className="flex-1 min-w-0">
                    <h2 className="font-semibold text-[15px] text-text-primary mb-0.5 truncate">
                      {ruta.nombre}
                    </h2>
                    <p className="text-[12px] text-c2-muted mb-1 line-clamp-1">
                      {ruta.descripcion}
                    </p>
                    <div className="flex flex-wrap items-center gap-x-3 gap-y-1 mb-2 text-sm text-text-secondary">
                      {ruta.valoracion != null && (
                        <span className="flex items-center gap-1">
                          <Estrellas valor={ruta.valoracion} tamano={16} />
                          {ruta.valoracion.toFixed(1)} ({ruta.resenas})
                        </span>
                      )}
                      {(ruta.meGusta ?? 0) > 0 && (
                        <span className="flex items-center gap-1">
                          <span className="material-symbols-outlined text-[16px]">favorite</span>
                          {ruta.meGusta}
                        </span>
                      )}
                      {cerca?.[ruta.id] != null && (
                        <span className="flex items-center gap-1" data-testid="ruta-cerca-km">
                          <span className="material-symbols-outlined text-[16px]">near_me</span>a{' '}
                          {Unidades.texto(cerca[ruta.id], m, 1)} {Unidades.etiquetaDistancia(m)}
                        </span>
                      )}
                      {ruta.autorNombre && <span>por {ruta.autorNombre}</span>}
                    </div>
                    <div className="mb-2 flex flex-wrap items-center gap-1.5">
                      {ruta.oficial && <InsigniaOficial />}
                      <span
                        className={`font-label-md text-label-md px-2 py-0.5 rounded-full ${
                          ruta.dificultad === 'Fácil'
                            ? 'bg-secondary-600/20 text-secondary-500'
                            : ruta.dificultad === 'Moderado'
                              ? 'bg-primary-600/20 text-primary-500'
                              : 'bg-danger/20 text-red-300'
                        }`}
                      >
                        {ruta.dificultad}
                      </span>
                    </div>
                    <div
                      className="grid grid-cols-3 gap-1 pt-1 border-t border-c2-linea"
                      data-testid="metricas-ruta"
                    >
                      <div>
                        <div className="text-[11px] text-text-secondary">Distancia</div>
                        <div className="text-[13px] font-semibold text-text-primary">
                          {Unidades.texto(ruta.distanciaKm, m, 1)} {Unidades.etiquetaDistancia(m)}
                        </div>
                      </div>
                      <div>
                        <div className="text-[11px] text-text-secondary">Desnivel</div>
                        <div className="text-[13px] font-semibold text-text-primary">
                          +{ruta.elevacionM} m
                        </div>
                      </div>
                      <div>
                        <div className="text-[11px] text-text-secondary">Valoración</div>
                        <div className="text-[13px] font-semibold text-c2-violeta">
                          {ruta.valoracion != null
                            ? `${ruta.valoracion.toFixed(1)} (${ruta.resenas ?? 0})`
                            : '--'}
                        </div>
                      </div>
                    </div>
                    <span className="mt-1 inline-flex items-center gap-1 text-[12px] font-semibold text-c2-violeta">
                      <span className="material-symbols-outlined text-[16px]">info</span>
                      Ver detalles
                    </span>
                  </div>
                </div>
              </Link>
              <button
                onClick={() => void alternar(ruta.id)}
                aria-label={
                  guardada ? `Quitar ${ruta.nombre} de guardadas` : `Guardar ${ruta.nombre}`
                }
                aria-pressed={guardada}
                className="absolute top-2 right-2 w-9 h-9 rounded-full bg-c2-s2 backdrop-blur flex items-center justify-center text-primary-400"
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
