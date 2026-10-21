import { useEffect, useState } from 'react'
import type { Visibilidad } from '../../../../data/tipos'
import { useApp } from '../../../../store/app'
import { FilaInterruptor, PantallaAjustes, Seccion } from './Comunes'

const OPCIONES: { id: Visibilidad; icono: string; titulo: string; descripcion: string }[] = [
  {
    id: 'publico',
    icono: 'public',
    titulo: 'Público',
    descripcion: 'Cualquiera puede ver tu perfil y actividades públicas',
  },
  {
    id: 'seguidores',
    icono: 'group',
    titulo: 'Solo seguidores',
    descripcion: 'Solo seguidores aceptados pueden ver tus actividades',
  },
  {
    id: 'privado',
    icono: 'lock',
    titulo: 'Privado',
    descripcion: 'Solo tú puedes ver tus actividades',
  },
]

export function PrivacidadUbicacion() {
  const ajustes = useApp((s) => s.ajustes)
  const actualizar = useApp((s) => s.actualizarAjustes)
  const [radio, setRadio] = useState(ajustes.radioHogarM)
  useEffect(() => setRadio(ajustes.radioHogarM), [ajustes.radioHogarM])
  const guardarRadio = () => void actualizar({ radioHogarM: radio })
  const carreras = useApp((s) => s.carreras)
  const descargar = () => {
    const blob = new Blob([JSON.stringify({ carreras }, null, 2)], { type: 'application/json' })
    const a = document.createElement('a')
    a.href = URL.createObjectURL(blob)
    a.download = 'gacelas-mis-datos.json'
    a.click()
    URL.revokeObjectURL(a.href)
  }

  return (
    <PantallaAjustes
      titulo="Privacidad y Ubicación"
      subtitulo="Gacelas Loja • Club oficial"
      volverA="/ajustes"
    >
      <div className="flex gap-3 p-4 rounded-2xl bg-primary-600/10 border border-primary-600/40">
        <span className="material-symbols-outlined text-primary">shield</span>
        <div>
          <p className="font-title-sm text-title-sm text-primary">Privacidad activa por defecto</p>
          <p className="text-body-sm text-text-secondary">
            Tus recorridos no revelan el punto exacto de tu residencia. Tu ubicación en vivo
            permanece desactivada hasta que decidas compartirla.
          </p>
        </div>
      </div>
      <Seccion titulo="Visibilidad del perfil">
        <div role="radiogroup" aria-label="Visibilidad del perfil" className="space-y-2">
          {OPCIONES.map((o) => {
            const activo = ajustes.visibilidad === o.id
            return (
              <button
                key={o.id}
                role="radio"
                aria-checked={activo}
                onClick={() => void actualizar({ visibilidad: o.id })}
                className={`w-full text-left p-4 rounded-xl border-2 transition-colors ${activo ? 'bg-primary-container/20 border-primary-600' : 'bg-surface-container border-border-subtle'}`}
              >
                <div className="flex items-center gap-3">
                  <span className="material-symbols-outlined text-primary-500">{o.icono}</span>
                  <span className="flex-1 font-body-md text-text-primary font-semibold">
                    {o.titulo}
                  </span>
                  <span
                    className={`material-symbols-outlined ${activo ? 'text-primary-500' : 'text-text-secondary'}`}
                  >
                    {activo ? 'check_circle' : 'radio_button_unchecked'}
                  </span>
                </div>
                <p className="text-body-sm text-text-secondary mt-1">{o.descripcion}</p>
              </button>
            )
          })}
        </div>
      </Seccion>

      <Seccion titulo="Visibilidad de actividades">
        <FilaInterruptor
          icono="monitor_heart"
          titulo="Ocultar datos biométricos"
          descripcion="Pulsaciones y cadencia ocultas en resúmenes públicos"
          activo={ajustes.ocultarBiometricos}
          onCambiar={(v) => void actualizar({ ocultarBiometricos: v })}
        />
        <FilaInterruptor
          icono="leaderboard"
          titulo="Aparecer en rankings y retos"
          descripcion="Figurar en el top semanal de altimetría y distancia de Loja"
          activo={ajustes.aparecerRankings}
          onCambiar={(v) => void actualizar({ aparecerRankings: v })}
        />
      </Seccion>

      <Seccion titulo="Ubicación GPS">
        <FilaInterruptor
          icono="my_location"
          titulo="Ubicación precisa"
          descripcion="Graba tu ubicación exacta durante carreras"
          activo={ajustes.ubicacionPrecisa}
          onCambiar={(v) => void actualizar({ ubicacionPrecisa: v })}
        />
      </Seccion>

      <Seccion titulo="Protección de hogar">
        <FilaInterruptor
          icono="home"
          titulo="Ocultar ubicación de casa"
          descripcion="Difumina los puntos de inicio/fin cerca de tu casa"
          activo={ajustes.ocultarHogar}
          onCambiar={(v) => void actualizar({ ocultarHogar: v })}
        />
        {ajustes.ocultarHogar && (
          <div className="px-4 py-3 bg-surface-container rounded-xl space-y-3">
            <div className="flex gap-2" aria-label="Radios sugeridos">
              {[200, 500, 1000].map((r) => (
                <button
                  key={r}
                  aria-pressed={ajustes.radioHogarM === r}
                  onClick={() => void actualizar({ radioHogarM: r })}
                  className={`flex-1 py-2 rounded-lg border text-label-md ${ajustes.radioHogarM === r ? 'border-primary-600 bg-primary-600/15 text-primary' : 'border-border-subtle text-text-secondary'}`}
                >
                  {r >= 1000 ? `${r / 1000} km` : `${r} m`}
                  {r === 200 ? ' (recomendado)' : ''}
                </button>
              ))}
            </div>
            <div className="flex justify-between mb-2">
              <label htmlFor="radio-hogar" className="text-body-md text-text-secondary">
                Radio de protección
              </label>
              <span className="text-label-lg text-primary-500" data-testid="radio-hogar-valor">
                {radio}m
              </span>
            </div>
            <input
              id="radio-hogar"
              type="range"
              min={100}
              max={1000}
              step={50}
              value={radio}
              onChange={(e) => setRadio(Number(e.target.value))}
              onPointerUp={guardarRadio}
              onKeyUp={guardarRadio}
              onBlur={guardarRadio}
              className="h-8 w-full accent-primary-600"
            />
            <div className="flex justify-between text-body-sm text-text-secondary">
              <span>100m</span>
              <span>1 km</span>
            </div>
          </div>
        )}
      </Seccion>

      <Seccion titulo="Compartir en vivo">
        <div className="p-4 rounded-xl bg-secondary-600/10 border border-secondary-600/40">
          <p className="text-body-md text-text-secondary mb-2">
            Cuando compartes tu ubicación en vivo, solo las personas que elijas pueden verte. Puedes
            detener el seguimiento en cualquier momento.
          </p>
          <p className="flex items-center gap-2 text-secondary-500 font-semibold">
            <span className="material-symbols-outlined">shield</span>
            Tu privacidad está protegida
          </p>
        </div>
      </Seccion>
      <Seccion titulo="Gestión de datos de la cuenta">
        <button
          onClick={descargar}
          className="w-full flex items-center gap-3 px-4 py-3 bg-surface-container rounded-xl text-left hover:bg-surface-container-high"
        >
          <span className="material-symbols-outlined text-primary">download</span>
          <span className="flex-1">
            <span className="block text-text-primary font-semibold">Descargar mis datos y rutas</span>
            <span className="block text-body-sm text-text-secondary">
              Exporta tus carreras en un archivo JSON
            </span>
          </span>
          <span className="material-symbols-outlined text-text-secondary">chevron_right</span>
        </button>
      </Seccion>
    </PantallaAjustes>
  )
}
