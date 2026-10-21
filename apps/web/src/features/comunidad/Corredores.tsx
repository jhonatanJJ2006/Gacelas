import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import type { CorredorPublico } from '../../data/repositorios'
import { useApp } from '../../store/app'

/** Buscar corredores por nombre o usuario y seguirlos. */
export function Corredores() {
  const navigate = useNavigate()
  const buscar = useApp((s) => s.buscarCorredores)
  const siguiendo = useApp((s) => s.siguiendo)
  const alternarSeguir = useApp((s) => s.alternarSeguir)
  const [consulta, setConsulta] = useState('')
  const [resultados, setResultados] = useState<CorredorPublico[] | null>(null)

  useEffect(() => {
    let vigente = true
    setResultados(null)
    const t = setTimeout(() => {
      void buscar(consulta).then((r) => vigente && setResultados(r))
    }, 250)
    return () => {
      vigente = false
      clearTimeout(t)
    }
  }, [consulta, buscar])

  return (
    <div className="flex flex-col w-full min-h-screen bg-surface contenedor-escritorio lg:max-w-5xl">
      <header className="px-margin pt-space-md pb-4 lg:px-0 lg:pt-8">
        <div className="flex items-center gap-3 mb-4">
          <button
            onClick={() => navigate('/comunidad')}
            aria-label="Volver"
            className="w-11 h-11 rounded-full bg-surface-container flex items-center justify-center text-text-secondary hover:text-text-primary"
          >
            <span className="material-symbols-outlined">arrow_back</span>
          </button>
          <h1 className="font-headline-md text-headline-md text-text-primary">Buscar corredores</h1>
        </div>
        <div className="relative">
          <span className="material-symbols-outlined absolute left-4 top-1/2 -translate-y-1/2 text-text-secondary">
            search
          </span>
          <input
            type="search"
            value={consulta}
            onChange={(e) => setConsulta(e.target.value)}
            aria-label="Buscar corredores"
            placeholder="Nombre o usuario"
            className="w-full h-12 pl-12 pr-4 bg-surface-container border border-border-subtle rounded-xl text-text-primary placeholder-text-secondary focus:outline-none focus:border-primary-600"
          />
        </div>
      </header>

      <main className="flex-1 px-margin pb-24 lg:px-0">
        {resultados === null ? (
          <div className="flex justify-center py-10" role="status" aria-label="Buscando">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600"></div>
          </div>
        ) : resultados.length === 0 ? (
          <p className="text-center text-text-secondary py-10" data-testid="corredores-vacio">
            {consulta.trim()
              ? 'No encontramos corredores con ese nombre.'
              : 'Aún no hay otros corredores.'}
          </p>
        ) : (
          <ul className="grid gap-3 lg:grid-cols-2">
            {resultados.map((c) => {
              const sigo = siguiendo.includes(c.id)
              return (
                <li
                  key={c.id}
                  data-testid="corredor"
                  className="flex items-center gap-3 min-w-0 bg-surface-container border border-border-subtle rounded-2xl p-4"
                >
                  {c.foto ? (
                    <img
                      src={c.foto}
                      alt=""
                      className="w-12 h-12 shrink-0 rounded-full object-cover"
                    />
                  ) : (
                    <div className="w-12 h-12 shrink-0 rounded-full bg-primary-600/30 flex items-center justify-center text-primary-400 font-bold">
                      {c.nombre.charAt(0).toUpperCase()}
                    </div>
                  )}
                  <div className="flex-1 min-w-0">
                    <p className="font-semibold text-text-primary truncate">{c.nombre}</p>
                    <p className="text-sm text-text-secondary truncate">
                      @{c.username}
                      {c.ubicacion ? ` · ${c.ubicacion}` : ''}
                    </p>
                  </div>
                  <button
                    onClick={() => void alternarSeguir(c.id)}
                    aria-pressed={sigo}
                    aria-label={`${sigo ? 'Dejar de seguir' : 'Seguir'} a ${c.nombre}`}
                    className={`h-11 px-4 shrink-0 rounded-full text-sm font-semibold transition-colors ${
                      sigo
                        ? 'bg-surface-container-high text-text-primary border border-border-subtle'
                        : 'bg-primary-600 text-on-primary hover:bg-primary-700'
                    }`}
                  >
                    {sigo ? 'Siguiendo' : 'Seguir'}
                  </button>
                </li>
              )
            })}
          </ul>
        )}
        <p className="text-xs text-text-secondary mt-6">
          Las carreras de cada corredor se ven según su privacidad: las de perfiles &quot;solo
          seguidores&quot; aparecen en tu feed cuando lo sigues.
        </p>
      </main>
    </div>
  )
}
