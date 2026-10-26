import { useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { Unidades } from '../../../lib/geo'
import { useApp } from '../../../store/app'
import { etiquetaOpcion, opcionesBuscador } from '../../../lib/logica'
import { MapaGoogle } from '../../../components/MapaGoogle'
import { trazoDeOpcion } from '../../../lib/mapas'

export function BuscadorRutas() {
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const rutaParam = params.get('ruta')
  const m = useApp((s) => s.ajustes.unidadesMetricas)
  const [searchQuery, setSearchQuery] = useState('')
  const [selectedRoute, setSelectedRoute] = useState<string | null>(
    rutaParam ? `ruta-${rutaParam}` : 'primary'
  )
  const routes = opcionesBuscador(searchQuery, rutaParam)
  const seleccionada = routes.find((r) => r.id === selectedRoute) ?? null
  const rutas = useApp((s) => s.rutas)
  const guardadas = useApp((s) => s.rutasGuardadas)
  const alternarGuardada = useApp((s) => s.alternarRutaGuardada)
  const destinos = rutas.slice(0, 4).map((r) => r.nombre)
  const rutaId = seleccionada?.id.startsWith('ruta-') ? seleccionada.id.slice(5) : null
  const guardada = rutaId != null && guardadas.includes(rutaId)

  return (
    <main className="w-full min-h-screen bg-[#070b13] flex flex-col relative">
      <h1 className="sr-only">Buscar rutas</h1>
      <div className="absolute inset-0 z-0 bg-[#0b1220] overflow-hidden">
        <MapaGoogle
          puntos={trazoDeOpcion(selectedRoute)}
          zoom={14}
          className="w-full h-full"
          alternativa={
            <svg className="w-full h-full object-cover scale-110 opacity-90" viewBox="0 0 600 900">
              <defs>
                <pattern id="urban-grid" width="40" height="40" patternUnits="userSpaceOnUse">
                  <path d="M 40 0 L 0 0 0 40" fill="none" stroke="#162238" strokeWidth="0.8" />
                </pattern>
                <linearGradient id="routeGlow" x1="0%" y1="0%" x2="100%" y2="100%">
                  <stop offset="0%" stopColor="#10B981" />
                  <stop offset="50%" stopColor="#A855F7" />
                  <stop offset="100%" stopColor="#7B1FA2" />
                </linearGradient>
              </defs>

              <rect width="600" height="900" fill="#090f1a" />
              <rect width="600" height="900" fill="url(#urban-grid)" />

              <path
                d="M-50,150 Q180,80 340,190 T650,120 L650,0 L-50,0 Z"
                fill="#0d182b"
                opacity="0.6"
              />

              <path
                d="M 330,0 C 310,220 340,380 290,560 C 260,680 280,800 270,900"
                fill="none"
                stroke="#1e3a5f"
                strokeWidth="12"
                strokeLinecap="round"
                opacity="0.7"
              />

              <path
                d="M 280,110 C 380,100 450,150 430,230 C 380,280 300,270 260,210 Z"
                fill="#064e3b"
                opacity="0.35"
                stroke="#10b981"
                strokeWidth="1"
              />

              {seleccionada?.color === 'primary' && (
                <path
                  d="M 330,210 C 310,260 270,320 280,380 C 290,450 270,520 310,570 C 330,590 350,610 380,620"
                  fill="none"
                  stroke="#7B1FA2"
                  strokeWidth="8"
                  strokeLinecap="round"
                  opacity="0.95"
                />
              )}

              {seleccionada?.color === 'secondary' && (
                <path
                  d="M 250,220 C 240,320 220,450 250,560 C 270,610 320,620 370,630"
                  fill="none"
                  stroke="#10B981"
                  strokeWidth="8"
                  strokeLinecap="round"
                  opacity="0.95"
                />
              )}

              <circle cx="330" cy="210" r="12" fill="#7B1FA2" opacity="0.9">
                <animate attributeName="r" values="12;16;12" dur="2s" repeatCount="indefinite" />
              </circle>

              <circle cx="380" cy="620" r="10" fill="#10B981" />
            </svg>
          }
        />
      </div>

      <div className="relative z-30 flex-1 flex flex-col">
        <div className="pt-6 px-4 space-y-3">
          <div className="flex items-center gap-2">
            <button
              onClick={() => navigate('/rutas')}
              aria-label="Volver"
              className="w-10 h-10 rounded-full bg-black/60 backdrop-blur-md flex items-center justify-center text-white shadow-lg"
            >
              <span className="material-symbols-outlined">arrow_back</span>
            </button>
            <span className="px-3 py-1 rounded-full bg-black/60 text-white text-label-md flex items-center gap-1">
              <span className="w-2 h-2 rounded-full bg-success" />
              Loja • GPS
            </span>
          </div>
          <div className="rounded-2xl bg-[#0f1625]/95 border border-white/10 p-3 space-y-2 shadow-xl">
            <div className="flex items-center gap-3">
              <span className="w-3 h-3 rounded-full bg-success shrink-0" />
              <div className="flex-1 h-11 px-3 rounded-xl bg-[#141c2e] text-white flex items-center justify-between">
                <span>Mi ubicación</span>
                <span className="text-label-sm text-success">GPS</span>
              </div>
            </div>
            <div className="flex items-center gap-3">
              <span className="w-3 h-3 rounded-sm bg-primary shrink-0" />
              <div className="relative flex-1">
                <input
                  type="text"
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  placeholder="¿A dónde quieres correr?"
                  aria-label="Destino"
                  className="w-full h-11 pl-3 pr-9 bg-[#141c2e] border border-primary/50 rounded-xl text-white placeholder-slate-400 focus:outline-none focus:border-primary"
                />
                {searchQuery && (
                  <button
                    onClick={() => setSearchQuery('')}
                    aria-label="Borrar destino"
                    className="absolute right-2 top-1/2 -translate-y-1/2 text-slate-400"
                  >
                    <span className="material-symbols-outlined text-[18px]">close</span>
                  </button>
                )}
              </div>
            </div>
            <div className="flex gap-2 overflow-x-auto no-scrollbar pt-1">
              {destinos.map((d) => (
                <button
                  key={d}
                  onClick={() => setSearchQuery(d)}
                  className="px-3 py-1.5 rounded-lg bg-[#141c2e] text-slate-300 text-label-md whitespace-nowrap flex items-center gap-1"
                >
                  <span className="material-symbols-outlined text-[16px]">place</span>
                  {d}
                </button>
              ))}
            </div>
          </div>
        </div>

        <div className="flex-1" />

        <div className="rounded-t-3xl bg-[#0f1625]/95 border-t border-white/10 p-5 pb-8">
          {seleccionada && (
            <div className="flex items-start justify-between mb-3">
              <div>
                <div className="text-label-md text-primary uppercase">Ruta seleccionada</div>
                <div className="text-white font-semibold">{seleccionada.name}</div>
              </div>
              <span className="px-2 py-1 rounded-lg bg-success/15 text-success text-label-md">
                {seleccionada.duration} min estimado
              </span>
            </div>
          )}
          <div className="flex gap-3 overflow-x-auto no-scrollbar pb-1 max-h-[40vh]">
            {routes.length === 0 && (
              <p className="text-slate-400 text-sm text-center w-full">
                No hay rutas con ese nombre.
              </p>
            )}
            {routes.map((route) => {
              const activa = selectedRoute === route.id
              const etiqueta = etiquetaOpcion(route, routes)
              return (
                <button
                  key={route.id}
                  onClick={() => setSelectedRoute(route.id)}
                  aria-pressed={activa}
                  className={`min-w-[150px] p-3 rounded-2xl border-2 text-left transition-all ${
                    activa ? 'bg-primary-600/10 border-primary' : 'bg-[#141c2e] border-[#1f2b45]'
                  }`}
                >
                  {etiqueta && (
                    <span className="inline-block mb-1 px-2 py-0.5 rounded-md bg-primary-600/20 text-primary text-[10px] font-bold uppercase">
                      {etiqueta}
                    </span>
                  )}
                  <h2 className="text-white font-semibold text-sm">{route.name}</h2>
                  <div className="text-xs text-slate-400">
                    {Unidades.texto(route.distance, m, 1)} {Unidades.etiquetaDistancia(m)} •{' '}
                    {route.duration} min
                  </div>
                  <div className="flex justify-between text-xs mt-2 border-t border-white/10 pt-1">
                    <span className="text-slate-300">
                      {route.distance > 0
                        ? `${Math.floor((route.duration * 60) / route.distance / 60)}:${String(
                            Math.round(((route.duration * 60) / route.distance) % 60)
                          ).padStart(2, '0')}/km`
                        : '--'}
                    </span>
                    <span className="text-success">+{route.elevation}m</span>
                  </div>
                </button>
              )
            })}
          </div>

          <div className="flex gap-2 mt-4">
            <button
              onClick={() =>
                seleccionada &&
                navigate('/rutas/navegacion', {
                  state: { ruta: seleccionada.name, trazoId: seleccionada.id },
                })
              }
              disabled={!seleccionada}
              className="flex-1 h-14 bg-gradient-to-r from-[#7B1FA2] to-[#A855F7] text-white font-bold rounded-2xl shadow-lg disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2"
            >
              <span className="material-symbols-outlined">play_circle</span>
              Confirmar y comenzar navegación
            </button>
            {rutaId && (
              <button
                onClick={() => void alternarGuardada(rutaId)}
                aria-pressed={guardada}
                aria-label={guardada ? 'Quitar de guardadas' : 'Guardar ruta'}
                className="w-14 h-14 rounded-2xl bg-[#141c2e] border border-white/10 text-primary flex items-center justify-center"
              >
                <span className="material-symbols-outlined">
                  {guardada ? 'bookmark_added' : 'bookmark'}
                </span>
              </button>
            )}
          </div>
        </div>
      </div>
    </main>
  )
}
