import { EsqueletoLista } from '../../../shared/ui/Esqueletos'
import { useMemo, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { ID_USUARIO, type Ruta } from '../../../mocks/data'
import { Unidades } from '../../../shared/lib/geo'
import { useAnchoMinimo, useEsEscritorio } from '../../../shared/lib/useEsEscritorio'
import { MapaRutas } from '../../../shared/ui/MapaEditable'
import { MiniMapa, Panel, PerfilElevacion, Rotulo } from '../../../shared/ui/Visual'
import { useSeguridadDeRutas, textoNivelSeguridad, type SeguridadRuta } from '../../../app/datosApp'
import { ritmoHistorico } from '../../../app/inicio/PanelesInicio'
import { useRutaSugerida } from '../../../app/inicio/SeccionesInicio'
import { useActividad } from '../../actividad/api'
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
      <span aria-hidden="true" className="material-symbols-outlined text-[14px]">
        verified
      </span>
      Ruta oficial
    </span>
  )
}

const ORDENES = [
  'Recomendadas',
  'Más seguras',
  'Más cortas',
  'Más largas',
  'Menos desnivel',
] as const
type Orden = (typeof ORDENES)[number]

const PESO_NIVEL: Record<string, number> = { bajo: 0, medio: 1, alto: 2 }

/** Ordena la lista visible; "Más seguras" usa el nivel real y los reportes de 7 dias. */
export function ordenarRutas(rutas: Ruta[], orden: Orden, seg: Map<string, SeguridadRuta> | null) {
  const copia = [...rutas]
  if (orden === 'Más cortas') return copia.sort((a, b) => a.distanciaKm - b.distanciaKm)
  if (orden === 'Más largas') return copia.sort((a, b) => b.distanciaKm - a.distanciaKm)
  if (orden === 'Menos desnivel') return copia.sort((a, b) => a.elevacionM - b.elevacionM)
  if (orden === 'Más seguras' && seg)
    return copia.sort((a, b) => {
      const x = seg.get(a.id)
      const y = seg.get(b.id)
      return (
        (PESO_NIVEL[x?.nivel ?? 'medio'] ?? 1) - (PESO_NIVEL[y?.nivel ?? 'medio'] ?? 1) ||
        (x?.reportes7d ?? 0) - (y?.reportes7d ?? 0)
      )
    })
  return copia
}

const colorDificultad = (d: string) =>
  d === 'Fácil'
    ? 'bg-c2-menta/15 text-c2-menta'
    : d === 'Moderado'
      ? 'bg-c2-morado/20 text-c2-violeta'
      : 'bg-c2-peligro/20 text-pink-300'

function ChipSeguridad({ s }: { s: SeguridadRuta | undefined }) {
  if (!s) return null
  const color =
    s.nivel === 'bajo' ? 'text-c2-menta' : s.nivel === 'alto' ? 'text-pink-300' : 'text-amber-300'
  return (
    <span
      className={`inline-flex items-center gap-1 text-[11.5px] font-semibold ${color}`}
      title={`${textoNivelSeguridad(s.nivel)} a esta hora. ${s.reportes7d} reportes de corredores en 7 días.`}
      data-testid="seguridad-ruta"
    >
      <span aria-hidden="true" className="material-symbols-outlined text-[14px]">
        shield
      </span>
      {s.puntaje != null ? `${s.puntaje}/100` : textoNivelSeguridad(s.nivel)}
    </span>
  )
}

export function ListaRutas() {
  const [pestana, setPestana] = useState<PestanaRutas>('Explorar')
  const [consulta, setConsulta] = useState('')
  const guardadas = useRutas((s) => s.rutasGuardadas)
  const alternar = useRutas((s) => s.alternarRutaGuardada)
  const elegirRutaActiva = useRutas((s) => s.elegirRutaActiva)
  const m = useIdentidad((s) => s.ajustes.unidadesMetricas)
  const todas = useRutas((s) => s.rutas)
  const rutasCercanas = useRutas((s) => s.rutasCercanas)
  const [filtros, setFiltros] = useState(FILTROS_INICIALES)
  const [verMapa, setVerMapa] = useState(false)
  const [orden, setOrden] = useState<Orden>('Recomendadas')
  const [seleccion, setSeleccion] = useState<string | null>(null)
  const navigate = useNavigate()
  const sociales = useRutas((s) => s.rutasSociales)
  const cargando = useRutas((s) => s.cargandoRutas)
  const [cerca, setCerca] = useState<Record<string, number> | null>(null)
  const [buscandoCerca, setBuscandoCerca] = useState(false)
  const escritorio = useEsEscritorio()
  const conPanel = useAnchoMinimo(1280)
  const seguridad = useSeguridadDeRutas()
  const filtradas = aplicarFiltros(
    filtrarRutas(todas, pestana, guardadas, ID_USUARIO, consulta),
    filtros
  )
  const rutas = cerca
    ? ordenarPorCercania(filtradas, cerca)
    : ordenarRutas(filtradas, orden, seguridad)
  const elegida = rutas.find((r) => r.id === seleccion) ?? rutas[0]
  const oficiales = todas.filter((r) => r.oficial).length
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
  const correr = (r: Ruta) => {
    elegirRutaActiva(r.id)
    navigate(`/rutas/buscar?ruta=${r.id}`)
  }
  // Linea de datos extra, igual que Android: valoracion, me gusta, distancia a ti y autor.
  const extras = (r: Ruta) =>
    [
      r.valoracion != null ? `${r.valoracion.toFixed(1)} de 5 (${r.resenas ?? 0})` : null,
      (r.meGusta ?? 0) > 0 ? `${r.meGusta} me gusta` : null,
      cerca?.[r.id] != null
        ? `a ${Unidades.texto(cerca[r.id], m, 1)} ${Unidades.etiquetaDistancia(m)}`
        : null,
      r.autorNombre ? `por ${r.autorNombre}` : null,
    ].filter((x): x is string => x != null)
  const porCategoria = (k: string | null) =>
    k ? todas.filter((r) => r.categoria === k).length : todas.length
  const opcion = () => 'pastilla-c2 opcion-filtro'

  return (
    <div className="flex flex-col w-full px-4 lg:px-8 lg:max-w-[1440px] lg:mx-auto pb-space-3xl lg:pb-10">
      <div className="pt-space-md lg:pt-0 pb-4 flex items-center justify-between gap-3">
        <div className="min-w-0">
          <h1 className="titular-c2 text-[28px] lg:text-[30px] leading-none text-white">
            Rutas de Loja
          </h1>
          <p className="text-[12px] text-c2-muted mt-1.5">
            {todas.length} rutas · {oficiales} oficiales · {todas.length - oficiales} de la
            comunidad
          </p>
        </div>
        <div className="flex items-center gap-2 shrink-0">
          <button
            onClick={() => setVerMapa((v) => !v)}
            aria-pressed={verMapa}
            data-testid="ver-mapa-rutas"
            aria-label={verMapa ? 'Ver lista' : 'Ver mapa'}
            className="h-10 w-10 lg:w-auto lg:px-3 rounded-md bg-c2-card border border-c2-linea text-white flex items-center justify-center gap-2 text-[13px]"
          >
            <span aria-hidden="true" className="material-symbols-outlined text-[20px]">
              {verMapa ? 'list' : 'map'}
            </span>
            <span className="hidden lg:inline">{verMapa ? 'Lista' : 'Mapa'}</span>
          </button>
          <Link
            to="/rutas/buscar"
            aria-label="Planificar ruta"
            className="h-10 w-10 lg:w-auto lg:px-3 rounded-md bg-c2-card border border-c2-linea text-white flex items-center justify-center gap-2 text-[13px]"
          >
            <span aria-hidden="true" className="material-symbols-outlined text-[20px]">
              directions
            </span>
            <span className="hidden lg:inline">Planificar</span>
          </Link>
          {sociales && (
            <Link
              to="/rutas/crear"
              aria-label="Dibujar ruta"
              className="panel-c2 h-10 w-10 lg:w-auto lg:px-4 bg-[linear-gradient(90deg,#7B1FA2,#A855F7)] text-white flex items-center justify-center gap-2 text-[13px] font-bold"
            >
              <span aria-hidden="true" className="material-symbols-outlined text-[20px]">
                add
              </span>
              <span className="hidden lg:inline">Crear ruta</span>
            </Link>
          )}
        </div>
      </div>

      <div className="flex items-center justify-between gap-3 mb-4">
        <div
          className="flex gap-1 p-1 rounded-md bg-c2-card border border-c2-linea overflow-x-auto no-scrollbar"
          role="tablist"
        >
          {PESTANAS_RUTAS.map((tab) => (
            <button
              key={tab}
              role="tab"
              aria-selected={tab === pestana}
              onClick={() => setPestana(tab)}
              className={`h-8 px-3 rounded text-[13px] font-semibold whitespace-nowrap ${
                tab === pestana ? 'bg-c2-s2 text-white' : 'text-c2-muted hover:text-white'
              }`}
            >
              {tab === 'Guardadas' ? `Guardadas (${guardadas.length})` : tab}
            </button>
          ))}
        </div>
        <span className="hidden lg:block text-[11.5px] text-c2-muted">
          Datos de rutas: OpenStreetMap y comunidad Runcity
        </span>
      </div>

      {escritorio && <Destacadas rutas={todas} m={m} />}

      <div
        className={`lg:grid lg:gap-5 lg:items-start ${conPanel ? 'lg:grid-cols-[210px_minmax(0,1fr)_340px]' : 'lg:grid-cols-[210px_minmax(0,1fr)]'}`}
      >
        <aside
          aria-label="Filtros de rutas"
          className="lg:panel-c2 lg:bg-c2-card lg:border lg:border-c2-linea lg:p-4 lg:sticky lg:top-[84px] space-y-2 lg:space-y-4 mb-3 lg:mb-0"
        >
          <div>
            <Rotulo className="hidden lg:block mb-2">Tipo</Rotulo>
            <div
              className="flex gap-2 overflow-x-auto pb-1 lg:flex-col lg:gap-0.5 lg:overflow-visible lg:pb-0"
              role="group"
              aria-label="Categoría"
              data-testid="filtro-categoria"
            >
              <button
                aria-pressed={!filtros.categoria}
                onClick={() => setFiltros({ ...filtros, categoria: null })}
                className={opcion()}
              >
                Todas
                <span className="hidden lg:inline text-[11px] text-c2-muted">
                  {porCategoria(null)}
                </span>
              </button>
              {CATEGORIAS_RUTA.map(([k, v]) => (
                <button
                  key={k}
                  aria-pressed={filtros.categoria === k}
                  onClick={() =>
                    setFiltros({ ...filtros, categoria: filtros.categoria === k ? null : k })
                  }
                  className={opcion()}
                >
                  {v}
                  <span className="hidden lg:inline text-[11px] text-c2-muted">
                    {porCategoria(k)}
                  </span>
                </button>
              ))}
            </div>
          </div>
          <div>
            <Rotulo className="hidden lg:block mb-2">Dificultad y cercanía</Rotulo>
            <div
              className="flex gap-2 overflow-x-auto pb-1 lg:flex-col lg:gap-0.5 lg:overflow-visible lg:pb-0"
              aria-label="Filtros de dificultad"
              role="group"
            >
              <button
                onClick={alternarCerca}
                aria-pressed={!!cerca}
                data-testid="rutas-cerca"
                className={opcion()}
              >
                {buscandoCerca ? 'Buscando…' : 'Cerca de mí'}
              </button>
              {(['Todas', 'Fácil', 'Moderado', 'Difícil'] as const).map((d) => (
                <button
                  key={d}
                  aria-pressed={filtros.dificultad === d}
                  onClick={() => setFiltros({ ...filtros, dificultad: d })}
                  className={opcion()}
                >
                  {d === 'Todas' ? 'Toda dificultad' : d}
                </button>
              ))}
            </div>
          </div>
          <div>
            <Rotulo className="hidden lg:block mb-2">Distancia y etiquetas</Rotulo>
            <div
              className="flex gap-2 overflow-x-auto pb-1 lg:flex-col lg:gap-0.5 lg:overflow-visible lg:pb-0"
              role="group"
              aria-label="Distancia y tipo"
            >
              {FILTROS_DISTANCIA.map((d: FiltroDistancia) => (
                <button
                  key={d}
                  aria-pressed={filtros.distancia === d}
                  onClick={() => setFiltros({ ...filtros, distancia: d })}
                  className={opcion()}
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
                  className={opcion()}
                >
                  #{v}
                </button>
              ))}
            </div>
          </div>
        </aside>

        <div className="min-w-0">
          <div className="flex gap-2 mb-2">
            <div className="relative flex-1 min-w-0">
              <label htmlFor="buscar-rutas" className="sr-only">
                Buscar
              </label>
              <input
                id="buscar-rutas"
                type="search"
                aria-label="Buscar rutas"
                value={consulta}
                onChange={(e) => setConsulta(e.target.value)}
                placeholder="Buscar por nombre, parque o barrio"
                className="w-full h-11 pl-10 pr-3 text-[14px] bg-c2-card border border-c2-linea rounded-md text-white placeholder:text-c2-muted focus:outline-none focus:border-c2-morado"
              />
              <span
                aria-hidden="true"
                className="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-c2-muted text-[20px]"
              >
                search
              </span>
            </div>
            <label className="h-11 shrink-0 rounded-md bg-c2-card border border-c2-linea flex items-center gap-1 pl-3 pr-1 text-[12.5px] text-c2-muted">
              <span className="hidden sm:inline">Ordenar:</span>
              <select
                aria-label="Ordenar rutas"
                value={orden}
                onChange={(e) => setOrden(e.target.value as Orden)}
                className="bg-transparent text-white font-semibold text-[12.5px] outline-none pr-1 max-w-[132px]"
              >
                {ORDENES.map((o) => (
                  <option key={o} value={o} className="bg-c2-card">
                    {o}
                  </option>
                ))}
              </select>
            </label>
          </div>
          <div className="pb-3" data-testid="encabezado-lista-rutas">
            <h2 className="sr-only">Rutas locales de Loja</h2>
            <p className="text-[12px] text-c2-muted">
              {rutas.length} {rutas.length === 1 ? 'sendero coincide' : 'senderos coinciden'}
              {cerca ? ', los más cercanos a ti primero' : ''}
            </p>
          </div>

          {verMapa && (
            <div className="pb-4">
              <MapaRutas
                className="h-[420px] lg:h-[520px] rounded-[10px] border border-c2-linea overflow-hidden"
                onAbrir={(id) => navigate(`/rutas/${id}`)}
                rutas={rutas
                  .filter((r) => (r.puntos?.length ?? 0) > 0)
                  .map((r) => ({ id: r.id, nombre: r.nombre, salida: r.puntos![0] }))}
              />
            </div>
          )}

          <div className="space-y-3">
            {cargando && <EsqueletoLista filas={4} conImagen testId="cargando-rutas" />}
            {!cargando && rutas.length === 0 && (
              <p
                className="panel-c2 bg-c2-card border border-c2-linea p-4 text-[14px] text-c2-muted"
                data-testid="rutas-vacio"
              >
                {pestana === 'Guardadas'
                  ? 'Aún no guardas rutas. Toca el marcador de una ruta para guardarla.'
                  : pestana === 'Mis Rutas'
                    ? 'Todavía no creaste rutas.'
                    : 'No hay rutas que coincidan con la búsqueda.'}
              </p>
            )}
            {!cargando &&
              rutas.map((ruta) => {
                const guardada = guardadas.includes(ruta.id)
                const activa = conPanel && elegida?.id === ruta.id
                const categoria = CATEGORIAS_RUTA.find(([k]) => k === ruta.categoria)?.[1]
                return (
                  <article
                    key={ruta.id}
                    onMouseEnter={() => setSeleccion(ruta.id)}
                    onFocus={() => setSeleccion(ruta.id)}
                    className={`panel-c2 relative overflow-hidden border transition-colors ${
                      activa
                        ? 'bg-[linear-gradient(160deg,#2A0A3D,#191F2F)] border-c2-morado/60'
                        : 'bg-c2-card border-c2-linea hover:border-c2-morado/40'
                    }`}
                  >
                    <Link
                      to={`/rutas/${ruta.id}`}
                      className="flex gap-3 p-3 pr-11"
                      aria-label={`Abrir ${ruta.nombre}`}
                    >
                      <span className="relative shrink-0">
                        <MiniMapa
                          puntos={ruta.puntos}
                          ancho={128}
                          alto={92}
                          className="w-[96px] h-[80px] sm:w-[128px] sm:h-[92px] rounded"
                        />
                        {categoria && (
                          <span className="absolute left-1.5 bottom-1.5 text-[10px] font-semibold px-1.5 py-0.5 rounded bg-c2-bg/85 text-white">
                            {categoria}
                          </span>
                        )}
                      </span>
                      <span className="flex-1 min-w-0 flex flex-col gap-1">
                        <span className="flex flex-wrap items-center gap-x-2 gap-y-1">
                          <h2 className="titulo-panel !text-[18px] min-w-0">{ruta.nombre}</h2>
                          {ruta.oficial && <InsigniaOficial />}
                        </span>
                        {extras(ruta).length > 0 ? (
                          <span
                            className="text-[11.5px] font-semibold text-c2-muted truncate"
                            data-testid="ruta-extra"
                          >
                            {extras(ruta).join('  ·  ')}
                          </span>
                        ) : (
                          <span className="text-[12px] text-c2-muted line-clamp-1">
                            {ruta.descripcion}
                          </span>
                        )}
                        <span
                          className="flex flex-wrap items-center gap-x-3 gap-y-1"
                          data-testid="metricas-ruta"
                        >
                          <span className="cifra-c2 text-[18px] text-c2-violeta">
                            {`${Unidades.texto(ruta.distanciaKm, m, 1)} ${Unidades.etiquetaDistancia(m)}`}
                          </span>
                          <span className="cifra-c2 text-[18px] text-c2-cielo">{`+${ruta.elevacionM} m`}</span>
                          <span
                            className={`text-[11px] font-semibold px-2 py-0.5 rounded-full ${colorDificultad(ruta.dificultad)}`}
                          >
                            {ruta.dificultad}
                          </span>
                          <ChipSeguridad s={seguridad?.get(ruta.id)} />
                        </span>
                        <span className="hidden sm:block mt-0.5 max-w-[320px]">
                          <PerfilElevacion alturas={ruta.perfilElevacion} alto={22} />
                        </span>
                      </span>
                    </Link>
                    <button
                      onClick={() => void alternar(ruta.id)}
                      aria-label={
                        guardada ? `Quitar ${ruta.nombre} de guardadas` : `Guardar ${ruta.nombre}`
                      }
                      aria-pressed={guardada}
                      className="absolute top-2.5 right-2.5 w-9 h-9 flex items-center justify-center text-c2-violeta hover:text-white"
                    >
                      <span
                        aria-hidden="true"
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

        {conPanel && elegida && !cargando && (
          <PanelRuta
            ruta={elegida}
            m={m}
            seg={seguridad?.get(elegida.id)}
            guardada={guardadas.includes(elegida.id)}
            onGuardar={() => void alternar(elegida.id)}
            onCorrer={() => correr(elegida)}
          />
        )}
      </div>
    </div>
  )
}

/** Panel derecho (desde 1280 px): la ruta elegida con mapa, cifras, elevacion y acciones. */
function PanelRuta({
  ruta,
  m,
  seg,
  guardada,
  onGuardar,
  onCorrer,
}: {
  ruta: Ruta
  m: boolean
  seg: SeguridadRuta | undefined
  guardada: boolean
  onGuardar: () => void
  onCorrer: () => void
}) {
  const carreras = useActividad((s) => s.carreras)
  const ritmo = ritmoHistorico(carreras)
  const minutos = Math.round((ruta.distanciaKm * (ritmo ?? 390)) / 60)
  return (
    <Panel sinPadding className="overflow-hidden sticky top-[84px]" testId="panel-ruta-elegida">
      <MiniMapa
        puntos={ruta.puntos}
        ancho={340}
        alto={210}
        grosor={3.5}
        className="w-full h-[210px]"
        etiqueta={`Mapa de ${ruta.nombre}`}
      />
      <div className="p-5">
        <div className="flex items-start justify-between gap-2">
          <h2 className="titulo-panel !text-[21px]">{ruta.nombre}</h2>
          <ChipSeguridad s={seg} />
        </div>
        <p className="mt-1 text-[12.5px] text-c2-muted line-clamp-3">{ruta.descripcion}</p>
        <div className="grid grid-cols-3 gap-2 mt-4">
          {[
            [Unidades.texto(ruta.distanciaKm, m, 1), Unidades.etiquetaDistancia(m)],
            [`+${ruta.elevacionM}`, 'm desnivel'],
            [`${minutos}`, ritmo ? 'min a tu ritmo' : 'min a 6:30/km'],
          ].map(([v, u]) => (
            <div key={u} className="rounded bg-c2-s2 px-2 py-2 text-center">
              <span className="block cifra-c2 text-[18px] text-white">{v}</span>
              <span className="block text-[10px] text-c2-muted">{u}</span>
            </div>
          ))}
        </div>
        {(ruta.perfilElevacion?.length ?? 0) > 1 && (
          <div className="mt-4">
            <Rotulo className="mb-2">Perfil de elevación</Rotulo>
            <PerfilElevacion alturas={ruta.perfilElevacion} alto={56} />
          </div>
        )}
        {seg && (
          <p className="mt-3 text-[11.5px] text-c2-muted">
            {seg.reportes7d} {seg.reportes7d === 1 ? 'reporte' : 'reportes'} de corredores en 7 días
            y {seg.reportes30d} en 30 días cerca del trazado.
          </p>
        )}
        <div className="mt-4 flex gap-2">
          <button
            type="button"
            onClick={onCorrer}
            className="panel-c2 flex-1 h-11 bg-[linear-gradient(90deg,#7B1FA2,#A855F7)] text-white text-[13.5px] font-bold hover:brightness-110"
          >
            Correr esta ruta
          </button>
          <button
            type="button"
            onClick={onGuardar}
            aria-pressed={guardada}
            className="h-11 px-4 rounded-md border border-c2-linea text-white text-[13px] hover:border-c2-morado"
          >
            {guardada ? 'Guardada' : 'Guardar'}
          </button>
        </div>
        <Link
          to={`/rutas/${ruta.id}`}
          className="mt-3 inline-flex items-center gap-1 text-[12.5px] font-semibold text-c2-violeta"
        >
          Ver detalle, reseñas y seguridad
          <span aria-hidden="true" className="material-symbols-outlined text-[16px]">
            arrow_forward
          </span>
        </Link>
      </div>
    </Panel>
  )
}

/** Cuatro destacadas reales: segura del dia, mejor valorada, para tu ritmo y mas llana. */
function Destacadas({ rutas, m }: { rutas: Ruta[]; m: boolean }) {
  const carreras = useActividad((s) => s.carreras)
  const sugerida = useRutaSugerida(rutas, carreras)
  const ritmo = ritmoHistorico(carreras)
  const lista = useMemo(() => {
    const r: { rotulo: string; icono: string; ruta: Ruta; nota: string }[] = []
    if (sugerida?.estado === 'ok')
      r.push({
        rotulo: 'Ruta segura del día',
        icono: 'verified_user',
        ruta: sugerida.ruta,
        nota: `${Unidades.texto(sugerida.ruta.distanciaKm, m, 1)} ${Unidades.etiquetaDistancia(m)} · elegida para hoy`,
      })
    const valorada = [...rutas]
      .filter((x) => x.valoracion != null && (x.resenas ?? 0) > 0)
      .sort((a, b) => b.valoracion! - a.valoracion!)[0]
    if (valorada)
      r.push({
        rotulo: 'Mejor valorada',
        icono: 'star',
        ruta: valorada,
        nota: `${valorada.valoracion!.toFixed(1)} de 5 en ${valorada.resenas} reseñas`,
      })
    const objetivo = 30 * 60
    const paso = ritmo ?? 390
    const tuRitmo = [...rutas].sort(
      (a, b) =>
        Math.abs(a.distanciaKm * paso - objetivo) - Math.abs(b.distanciaKm * paso - objetivo)
    )[0]
    if (tuRitmo)
      r.push({
        rotulo: ritmo ? 'Para tu ritmo, 30 min' : 'Para 30 min a 6:30/km',
        icono: 'bolt',
        ruta: tuRitmo,
        nota: `${Unidades.texto(tuRitmo.distanciaKm, m, 1)} ${Unidades.etiquetaDistancia(m)} en unos ${Math.round((tuRitmo.distanciaKm * paso) / 60)} min`,
      })
    const llana = [...rutas]
      .filter((x) => x.distanciaKm >= 3)
      .sort((a, b) => a.elevacionM / a.distanciaKm - b.elevacionM / b.distanciaKm)[0]
    if (llana)
      r.push({
        rotulo: 'La más llana',
        icono: 'landscape',
        ruta: llana,
        nota: `+${llana.elevacionM} m en ${Unidades.texto(llana.distanciaKm, m, 1)} ${Unidades.etiquetaDistancia(m)}`,
      })
    return r
  }, [rutas, sugerida, ritmo, m])
  if (!lista.length) return null
  return (
    <div className="grid grid-cols-2 xl:grid-cols-4 gap-3 mb-5" data-testid="rutas-destacadas">
      {lista.map((d) => (
        <Link
          key={d.rotulo}
          to={`/rutas/${d.ruta.id}`}
          className="panel-c2 bg-c2-card border border-c2-linea p-3.5 flex items-center gap-3 hover:border-c2-morado/50 min-w-0"
        >
          <span className="w-10 h-10 shrink-0 rounded-md bg-c2-s2 grid place-items-center">
            <span
              aria-hidden="true"
              className="material-symbols-outlined text-[20px] text-c2-violeta"
            >
              {d.icono}
            </span>
          </span>
          <span className="min-w-0">
            <Rotulo className="!text-[9.5px]">{d.rotulo}</Rotulo>
            <span className="block text-[13.5px] font-bold text-white truncate">
              {d.ruta.nombre}
            </span>
            <span className="block text-[11.5px] text-c2-muted truncate">{d.nota}</span>
          </span>
        </Link>
      ))}
    </div>
  )
}
