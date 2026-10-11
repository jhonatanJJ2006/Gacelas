import { useState } from 'react'
import { Link } from 'react-router-dom'
import { filtrarRutas, PESTANAS_RUTAS, type PestanaRutas } from '../../lib/filtros'
import { Unidades } from '../../lib/geo'
import { ID_USUARIO, rutasMock } from '../../mocks/data'
import { useApp } from '../../store/app'

export function ListaRutas() {
  const [pestana, setPestana] = useState<PestanaRutas>('Explorar')
  const [consulta, setConsulta] = useState('')
  const guardadas = useApp((s) => s.rutasGuardadas)
  const alternar = useApp((s) => s.alternarRutaGuardada)
  const m = useApp((s) => s.ajustes.unidadesMetricas)
  const rutas = filtrarRutas(rutasMock, pestana, guardadas, ID_USUARIO, consulta)

  return (
    <div className="flex flex-col w-full contenedor-escritorio">
      <div className="px-margin pt-space-md pb-space-sm lg:px-0 lg:pb-6">
        <div className="flex items-center justify-between mb-4">
          <h1 className="font-headline-md text-headline-md text-text-primary">Rutas de Loja</h1>
          <Link
            to="/rutas/buscar"
            aria-label="Planificar ruta"
            className="w-10 h-10 rounded-full bg-primary-600 text-bg-primary flex items-center justify-center shadow-lg"
          >
            <span className="material-symbols-outlined">directions</span>
          </Link>
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
                  ? 'bg-primary-600 text-bg-primary'
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
              className="relative bg-surface-container rounded-2xl border border-border-subtle overflow-hidden hover:border-primary-600/50 transition-colors"
            >
              <Link
                to={`/rutas/buscar?ruta=${ruta.id}`}
                className="block"
                aria-label={`Abrir ${ruta.nombre}`}
              >
                <div className="h-32 bg-gradient-to-br from-primary-600/30 to-secondary-600/30 flex items-center justify-center">
                  <span className="material-symbols-outlined text-[64px] text-primary">route</span>
                </div>
                <div className="p-4">
                  <h2 className="font-title-md text-title-md text-text-primary mb-1">
                    {ruta.nombre}
                  </h2>
                  <p className="text-body-sm text-text-secondary mb-2">{ruta.descripcion}</p>
                  <div className="flex items-center gap-4 text-text-secondary">
                    <span className="font-label-md text-label-md flex items-center gap-1">
                      <span className="material-symbols-outlined text-[16px]">straighten</span>
                      {Unidades.texto(ruta.distanciaKm, m, 1)} {Unidades.etiquetaDistancia(m)}
                    </span>
                    <span className="font-label-md text-label-md flex items-center gap-1">
                      <span className="material-symbols-outlined text-[16px]">trending_up</span>
                      {ruta.elevacionM}m
                    </span>
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
                </div>
              </Link>
              <button
                onClick={() => void alternar(ruta.id)}
                aria-label={
                  guardada ? `Quitar ${ruta.nombre} de guardadas` : `Guardar ${ruta.nombre}`
                }
                aria-pressed={guardada}
                className="absolute top-3 right-3 w-10 h-10 rounded-full bg-black/50 backdrop-blur flex items-center justify-center text-primary-400"
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
