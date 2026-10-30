import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { MapaEditable } from '../../../shared/ui/MapaEditable'
import { useIdentidad } from '../../identidad/api'
import { useRutas } from '../application/estado'
import { desnivelPositivo, perfilElevacion } from '../domain/elevacion'
import { ETIQUETAS_RUTA, longitudKm, type Coordenada } from '../domain/rutas'

export function DibujarRuta() {
  const navigate = useNavigate()
  const sesion = useIdentidad((s) => s.sesion)
  const sociales = useRutas((s) => s.rutasSociales)
  const publicarRuta = useRutas((s) => s.publicarRuta)
  const [puntos, setPuntos] = useState<Coordenada[]>([])
  const [nombre, setNombre] = useState('')
  const [descripcion, setDescripcion] = useState('')
  const [dificultad, setDificultad] = useState<'Fácil' | 'Moderado' | 'Difícil'>('Moderado')
  const [etiquetas, setEtiquetas] = useState<string[]>([])
  const [error, setError] = useState<string | null>(null)
  const [guardando, setGuardando] = useState(false)
  const km = longitudKm(puntos)

  const publicar = async () => {
    if (nombre.trim().length < 3) return setError('Ponle un nombre de al menos 3 letras')
    if (puntos.length < 2) return setError('Toca el mapa al menos dos veces para trazar la ruta')
    setGuardando(true)
    setError(null)
    try {
      const perfil = await perfilElevacion(puntos).catch(() => [])
      const r = await publicarRuta({
        nombre: nombre.trim(),
        descripcion: descripcion.trim(),
        dificultad,
        etiquetas,
        puntos,
        elevacionM: desnivelPositivo(perfil),
        perfilElevacion: perfil,
        origen: 'dibujada',
      })
      navigate(`/rutas/${r.id}`, { replace: true })
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudo publicar')
      setGuardando(false)
    }
  }

  return (
    <main className="w-full min-h-screen bg-surface contenedor-escritorio lg:max-w-5xl pb-28">
      <header className="flex items-center gap-1 px-margin h-16 bg-bg-secondary mb-4">
        <button
          onClick={() => navigate(-1)}
          aria-label="Volver"
          className="w-12 h-12 -ml-3 shrink-0 flex items-center justify-center text-text-primary"
        >
          <span className="material-symbols-outlined">arrow_back</span>
        </button>
        <h1 className="flex-1 min-w-0 font-title-md text-title-md text-text-primary">
          Dibujar ruta
        </h1>
      </header>
      {!sesion || !sociales ? (
        <p className="px-margin text-text-secondary">
          Inicia sesión para dibujar y publicar rutas.
        </p>
      ) : (
        <div className="px-margin lg:px-0 lg:grid lg:grid-cols-[minmax(0,1fr)_360px] lg:gap-8 space-y-4 lg:space-y-0">
          <div className="space-y-2">
            <MapaEditable
              puntos={puntos}
              onTocar={(p) => setPuntos((x) => [...x, p])}
              className="h-[360px] lg:h-[520px] rounded-2xl overflow-hidden bg-surface-container"
            />
            <div className="flex items-center gap-2">
              <span className="flex-1 text-text-primary" data-testid="km-dibujados">
                {km.toFixed(2)} km · {puntos.length} puntos
              </span>
              <button
                onClick={() => setPuntos((x) => x.slice(0, -1))}
                disabled={!puntos.length}
                className="h-10 px-3 rounded-full bg-surface-container text-text-primary disabled:opacity-50"
              >
                Deshacer
              </button>
              <button
                onClick={() => setPuntos([])}
                disabled={!puntos.length}
                className="h-10 px-3 rounded-full bg-surface-container text-text-primary disabled:opacity-50"
              >
                Borrar
              </button>
            </div>
            <p className="text-xs text-text-secondary">
              Toca el mapa para ir agregando puntos en orden.
            </p>
          </div>
          <div className="space-y-3">
            <input
              aria-label="Nombre de la ruta"
              value={nombre}
              maxLength={80}
              onChange={(e) => setNombre(e.target.value)}
              placeholder="Nombre de la ruta"
              className="w-full h-12 px-4 bg-surface-container border border-border-subtle rounded-xl text-text-primary placeholder-text-secondary"
            />
            <textarea
              aria-label="Descripción"
              value={descripcion}
              onChange={(e) => setDescripcion(e.target.value)}
              rows={3}
              placeholder="Descripción (opcional)"
              className="w-full px-4 py-3 bg-surface-container border border-border-subtle rounded-xl text-text-primary placeholder-text-secondary"
            />
            <div role="radiogroup" aria-label="Dificultad" className="flex gap-2">
              {(['Fácil', 'Moderado', 'Difícil'] as const).map((d) => (
                <button
                  key={d}
                  role="radio"
                  aria-checked={dificultad === d}
                  onClick={() => setDificultad(d)}
                  className={`flex-1 min-h-[40px] rounded-full text-sm font-semibold ${dificultad === d ? 'bg-primary-600 text-on-primary' : 'bg-surface-container text-text-secondary'}`}
                >
                  {d}
                </button>
              ))}
            </div>
            <div className="flex flex-wrap gap-2" role="group" aria-label="Etiquetas">
              {Object.entries(ETIQUETAS_RUTA).map(([k, v]) => (
                <button
                  key={k}
                  aria-pressed={etiquetas.includes(k)}
                  onClick={() =>
                    setEtiquetas((x) => (x.includes(k) ? x.filter((y) => y !== k) : [...x, k]))
                  }
                  className={`min-h-[36px] px-3 rounded-full text-sm ${etiquetas.includes(k) ? 'bg-secondary-600 text-on-primary' : 'bg-surface-container text-text-secondary'}`}
                >
                  #{v}
                </button>
              ))}
            </div>
            {error && (
              <p role="alert" className="text-sm text-red-300">
                {error}
              </p>
            )}
            <button
              onClick={() => void publicar()}
              disabled={guardando}
              data-testid="publicar-dibujada"
              className="w-full h-12 rounded-xl bg-primary-600 text-on-primary font-semibold disabled:opacity-60"
            >
              {guardando ? 'Publicando…' : 'Publicar ruta'}
            </button>
          </div>
        </div>
      )}
    </main>
  )
}
