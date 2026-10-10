import { useEffect, useState } from 'react'
import type { Visibilidad } from '../../data/tipos'
import { useApp } from '../../store/app'
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

  return (
    <PantallaAjustes titulo="Privacidad y Ubicación" volverA="/ajustes">
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
          <div className="px-4 py-3 bg-surface-container rounded-xl">
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
              max={500}
              step={50}
              value={radio}
              onChange={(e) => setRadio(Number(e.target.value))}
              onPointerUp={guardarRadio}
              onKeyUp={guardarRadio}
              onBlur={guardarRadio}
              className="w-full accent-primary-600"
            />
            <div className="flex justify-between text-body-sm text-text-secondary">
              <span>100m</span>
              <span>500m</span>
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
    </PantallaAjustes>
  )
}
