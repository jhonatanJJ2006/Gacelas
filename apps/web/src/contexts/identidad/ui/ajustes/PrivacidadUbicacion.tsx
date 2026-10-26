import { useEffect, useState } from 'react'
import type { Visibilidad } from '../../../../data/tipos'
import { useApp } from '../../../../store/app'
import { FilaC2, GrupoC2, PantallaAjustes } from './Comunes'
import { Interruptor } from '../../../../components/ui'
import { SeccionConsentimientos } from '../../../legal'
import { historial } from '../../../legal/api'

const OPCIONES: { id: Visibilidad; icono: string; titulo: string; descripcion: string }[] = [
  {
    id: 'publico',
    icono: 'public',
    titulo: 'Todos',
    descripcion: 'Cualquiera puede ver tu perfil y actividades públicas',
  },
  {
    id: 'seguidores',
    icono: 'group',
    titulo: 'Seguidores',
    descripcion: 'Solo seguidores aceptados pueden ver tus actividades',
  },
  {
    id: 'privado',
    icono: 'lock',
    titulo: 'Solo yo',
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
    const blob = new Blob([JSON.stringify({ carreras, consentimientos: historial() }, null, 2)], {
      type: 'application/json',
    })
    const a = document.createElement('a')
    a.href = URL.createObjectURL(blob)
    a.download = 'gacelas-mis-datos.json'
    a.click()
    URL.revokeObjectURL(a.href)
  }

  return (
    <PantallaAjustes titulo="Privacidad" volverA="/ajustes">
      <section aria-label="Zona oculta" className="bg-c2-card p-5">
        <div className="flex items-center justify-between">
          <h2 className="text-[12px] font-extrabold uppercase tracking-[0.14em] text-c2-violeta">
            Zona oculta
          </h2>
          <Interruptor
            etiqueta="Ocultar ubicación de casa"
            activo={ajustes.ocultarHogar}
            onCambiar={(v) => void actualizar({ ocultarHogar: v })}
          />
        </div>
        <div className="relative mt-3 h-[150px] overflow-hidden bg-c2-s2" aria-hidden="true">
          <svg viewBox="0 0 310 150" className="absolute inset-0 w-full h-full">
            {[20, 60, 100, 140].map((y) => (
              <line key={y} x1="0" y1={y} x2="310" y2={y - 30} stroke="#2C3446" strokeWidth="6" />
            ))}
            {[40, 120, 200, 270].map((x) => (
              <line key={x} x1={x} y1="0" x2={x + 30} y2="150" stroke="#2C3446" strokeWidth="5" />
            ))}
            {ajustes.ocultarHogar && (
              <circle
                cx="60"
                cy="55"
                r="34"
                fill="#A855F7"
                opacity="0.18"
                stroke="#C084FC"
                strokeDasharray="4 4"
              />
            )}
            <path
              d="M60 55 C 50 75, 60 95, 80 90 S 110 110, 150 115 S 210 120, 230 100 S 250 80, 262 60 L 240 40 C 220 30, 200 25, 180 22"
              fill="none"
              stroke="#C084FC"
              strokeWidth="4"
            />
            <circle cx="60" cy="55" r="7" fill="#A855F7" />
            <circle cx="180" cy="22" r="7" fill="#68DBA9" />
          </svg>
          {ajustes.ocultarHogar && (
            <span className="absolute left-3 top-3 -skew-x-12 bg-c2-violeta px-3 py-1 text-[12px] font-black italic uppercase text-[#0C1322]">
              Casa · {radio >= 1000 ? `${radio / 1000} km` : `${radio} m`}
            </span>
          )}
        </div>
        <p className="mt-3 text-[13px] text-c2-muted">
          {ajustes.ocultarHogar
            ? 'Ocultamos el inicio y fin de tus carreras dentro de este radio.'
            : 'Activa la zona oculta para no revelar dónde vives en tus recorridos.'}
        </p>
        {ajustes.ocultarHogar && (
          <div className="mt-4 space-y-3">
            <div className="flex gap-2" aria-label="Radios sugeridos">
              {[200, 500, 1000].map((r) => (
                <button
                  key={r}
                  aria-pressed={ajustes.radioHogarM === r}
                  onClick={() => void actualizar({ radioHogarM: r })}
                  className={`flex-1 h-9 -skew-x-12 text-[12px] font-black italic uppercase ${ajustes.radioHogarM === r ? 'bg-c2-violeta text-[#0C1322]' : 'bg-c2-s2 text-white border border-c2-linea'}`}
                >
                  {r >= 1000 ? `${r / 1000} km` : `${r} m`}
                </button>
              ))}
            </div>
            <div className="flex justify-between">
              <label htmlFor="radio-hogar" className="text-[13px] text-c2-muted">
                Radio de protección
              </label>
              <span
                className="text-[13px] font-bold text-c2-violeta"
                data-testid="radio-hogar-valor"
              >
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
              className="h-8 w-full accent-[#A855F7]"
            />
          </div>
        )}
      </section>

      <section aria-label="Quién ve tus carreras">
        <h2 className="mb-2 text-[12px] font-extrabold uppercase tracking-[0.14em] text-c2-violeta">
          Quién ve tus carreras
        </h2>
        <div role="radiogroup" aria-label="Visibilidad del perfil" className="flex gap-2">
          {OPCIONES.map((o) => {
            const activo = ajustes.visibilidad === o.id
            return (
              <button
                key={o.id}
                role="radio"
                aria-checked={activo}
                title={o.descripcion}
                onClick={() => void actualizar({ visibilidad: o.id })}
                className={`h-9 px-4 -skew-x-12 text-[12px] font-black italic uppercase ${activo ? 'bg-c2-violeta text-[#0C1322]' : 'bg-c2-s2 text-white border border-c2-linea'}`}
              >
                {o.titulo}
              </button>
            )
          })}
        </div>
        <p className="mt-2 text-[13px] text-c2-muted">
          {OPCIONES.find((o) => o.id === ajustes.visibilidad)?.descripcion}
        </p>
      </section>

      <GrupoC2 titulo="Actividad">
        <FilaC2
          titulo="Aparecer en rankings"
          descripcion="Top semanal de distancia y altimetría de Loja"
          activo={ajustes.aparecerRankings}
          onCambiar={(v) => void actualizar({ aparecerRankings: v })}
        />
        <FilaC2
          titulo="Ocultar datos biométricos"
          descripcion="Pulsaciones y cadencia fuera de los resúmenes públicos"
          activo={ajustes.ocultarBiometricos}
          onCambiar={(v) => void actualizar({ ocultarBiometricos: v })}
        />
        <FilaC2
          titulo="Ubicación precisa"
          descripcion="GPS de alta precisión durante las carreras"
          activo={ajustes.ubicacionPrecisa}
          onCambiar={(v) => void actualizar({ ubicacionPrecisa: v })}
        />
      </GrupoC2>

      <SeccionConsentimientos />
      <GrupoC2 titulo="Tus datos">
        <button
          onClick={descargar}
          className="w-full flex items-center gap-3 px-4 py-3.5 text-left hover:bg-c2-s2"
        >
          <span className="material-symbols-outlined text-c2-violeta">download</span>
          <span className="flex-1">
            <span className="block text-[15px] font-bold text-white">
              Descargar mis datos y rutas
            </span>
            <span className="block text-[13px] text-c2-muted">
              Archivo JSON con carreras y consentimientos
            </span>
          </span>
          <span className="material-symbols-outlined text-c2-muted">chevron_right</span>
        </button>
      </GrupoC2>
    </PantallaAjustes>
  )
}
