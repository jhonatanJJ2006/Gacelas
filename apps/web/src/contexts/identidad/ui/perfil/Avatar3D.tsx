import { lazy, Suspense, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import type { ConfigAvatar } from '../../../../shared/kernel/tipos'
import { Interruptor } from '../../../../shared/ui/ui'
import { useIdentidad } from '../../application/estado'

const Runner3D = lazy(() =>
  import('../../../../shared/ui/Runner3D').then((m) => ({ default: m.Runner3D }))
)

const EQUIPACIONES = ['bg-primary-600', 'bg-secondary-600', 'bg-danger', 'bg-tertiary']
const NOMBRES_EQUIPACION = ['Naranja', 'Verde', 'Rojo', 'Celeste']
const TONOS = ['#f5d0b5', '#e0ac85', '#c68863', '#8d5a3b', '#5c3a24']
const SHORTS: [string, string][] = [
  ['#111827', 'Negro'],
  ['#7b1fa2', 'Morado'],
  ['#1e3a8a', 'Azul'],
  ['#e5e7eb', 'Blanco'],
]
const ESTILOS = [
  { id: 'Dinámico', icono: 'directions_run', animacion: 'Run' },
  { id: 'Relajado', icono: 'self_improvement', animacion: 'Idle' },
  { id: 'Veloz', icono: 'bolt', animacion: 'Jog' },
] as const

export function Avatar3D() {
  const navigate = useNavigate()
  const guardado = useIdentidad((s) => s.ajustes.avatar)
  const actualizar = useIdentidad((s) => s.actualizarAjustes)
  const [avatar, setAvatar] = useState<ConfigAvatar>(guardado)
  const cambiar = (c: Partial<ConfigAvatar>) => setAvatar((a) => ({ ...a, ...c }))
  const animacion = ESTILOS.find((e) => e.id === avatar.estilo)?.animacion ?? 'Idle'

  const guardar = async () => {
    await actualizar({ avatar })
    navigate('/perfil')
  }

  return (
    <div className="w-full min-h-screen bg-surface flex flex-col">
      <header className="px-margin h-16 flex items-center bg-bg-secondary">
        <div className="flex items-center gap-1 w-full">
          <button
            onClick={() => navigate('/perfil')}
            aria-label="Volver"
            className="w-12 h-12 -ml-3 shrink-0 flex items-center justify-center text-text-primary"
          >
            <span className="material-symbols-outlined">arrow_back</span>
          </button>
          <h1 className="flex-1 min-w-0 font-title-md text-title-md text-text-primary">
            Personalizar Avatar
          </h1>
          <button
            onClick={() => void guardar()}
            className="px-2 py-2 text-primary-500 font-label-lg text-label-lg"
          >
            Guardar
          </button>
        </div>
      </header>

      <main className="lg:w-full lg:max-w-6xl lg:mx-auto flex-1 flex flex-col pb-24 lg:flex-row lg:gap-8 lg:p-8 lg:items-start">
        <div className="relative h-[480px] short:h-[400px] bg-surface-container-low lg:flex-none lg:w-3/5 lg:h-[640px] lg:rounded-2xl lg:overflow-hidden lg:border lg:border-border-subtle">
          <Suspense
            fallback={
              <div className="w-full h-full flex items-center justify-center">
                <div className="flex flex-col items-center gap-2">
                  <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
                  <span className="text-sm text-text-secondary">Cargando avatar 3D...</span>
                </div>
              </div>
            }
          >
            <Runner3D
              animation={animacion}
              autoRotate
              cameraPosition={[0, 1.2, 5.1]}
              target={[0, 0.8, 0]}
            />
          </Suspense>

          <div className="absolute bottom-4 left-4 right-4">
            <div className="tarjeta-c2 p-4">
              <div className="flex items-center gap-2 mb-3">
                <span className="material-symbols-outlined text-[20px] text-primary-500">
                  palette
                </span>
                <span className="font-title-md text-title-md text-text-primary">Equipación</span>
              </div>
              <div className="flex gap-3">
                {EQUIPACIONES.map((color, i) => (
                  <button
                    key={color}
                    onClick={() => cambiar({ equipacion: i })}
                    aria-label={`Equipación ${NOMBRES_EQUIPACION[i]}`}
                    aria-pressed={avatar.equipacion === i}
                    className={`w-12 h-12 rounded-full ${color} border-[3px] ${avatar.equipacion === i ? 'border-white' : 'border-transparent'}`}
                  ></button>
                ))}
              </div>
            </div>
          </div>
        </div>

        <div className="px-margin py-6 space-y-4 bg-surface lg:flex-1 lg:px-0 lg:py-0">
          <section aria-label="Rasgos del corredor" className="space-y-3">
            <h2 className="text-[12px] font-extrabold text-c2-violeta uppercase tracking-[.12em]">
              Género y complexión
            </h2>
            <div className="flex flex-wrap gap-2">
              {(['hombre', 'mujer'] as const).map((c) => (
                <button
                  key={c}
                  aria-pressed={(avatar.complexion ?? 'hombre') === c}
                  onClick={() => cambiar({ complexion: c })}
                  className="pastilla-c2 justify-center"
                >
                  {c}
                </button>
              ))}
            </div>
            <h2 className="text-[12px] font-extrabold text-c2-violeta uppercase tracking-[.12em]">
              Tono de piel
            </h2>
            <div className="flex gap-3">
              {TONOS.map((t, i) => (
                <button
                  key={t}
                  aria-label={`Tono de piel ${i + 1}`}
                  aria-pressed={(avatar.tonoPiel ?? 1) === i}
                  onClick={() => cambiar({ tonoPiel: i })}
                  style={{ background: t }}
                  className={`w-10 h-10 rounded-full border-2 ${(avatar.tonoPiel ?? 1) === i ? 'border-primary' : 'border-transparent'}`}
                />
              ))}
            </div>
            <h2 className="text-[12px] font-extrabold text-c2-violeta uppercase tracking-[.12em]">
              Cabello y peinado
            </h2>
            <div className="flex flex-wrap gap-2">
              {(
                [
                  ['corto', 'Corto runner'],
                  ['coleta', 'Coleta'],
                  ['rapado', 'Rapado'],
                ] as const
              ).map(([id, t]) => (
                <button
                  key={id}
                  aria-pressed={(avatar.peinado ?? 'corto') === id}
                  onClick={() => cambiar({ peinado: id })}
                  className="pastilla-c2 justify-center"
                >
                  {t}
                </button>
              ))}
            </div>
            <h2 className="text-[12px] font-extrabold text-c2-violeta uppercase tracking-[.12em]">
              Shorts
            </h2>
            <div className="flex gap-3">
              {SHORTS.map(([c, n], i) => (
                <button
                  key={n}
                  aria-label={`Shorts ${n}`}
                  aria-pressed={(avatar.colorShorts ?? 0) === i}
                  onClick={() => cambiar({ colorShorts: i })}
                  style={{ background: c }}
                  className={`w-10 h-10 rounded-lg border-2 ${(avatar.colorShorts ?? 0) === i ? 'border-primary' : 'border-transparent'}`}
                />
              ))}
            </div>
          </section>
          <div>
            <div className="flex items-center justify-between mb-3">
              <label className="font-body-md text-body-md text-text-primary font-medium">
                Estilo de carrera
              </label>
            </div>
            <div className="grid grid-cols-3 gap-2">
              {ESTILOS.map((e) => {
                const activo = avatar.estilo === e.id
                return (
                  <button
                    key={e.id}
                    onClick={() => cambiar({ estilo: e.id })}
                    aria-pressed={activo}
                    className={`px-4 py-3 border-2 rounded-xl flex flex-col items-center gap-1 ${activo ? 'bg-primary-container/20 border-primary-600' : 'bg-surface-container border-transparent hover:border-border-subtle'}`}
                  >
                    <span
                      className={`material-symbols-outlined text-[24px] ${activo ? 'text-primary-500' : 'text-text-secondary'}`}
                    >
                      {e.icono}
                    </span>
                    <span
                      className={`font-label-md text-label-md ${activo ? 'text-text-primary' : 'text-text-secondary'}`}
                    >
                      {e.id}
                    </span>
                  </button>
                )
              })}
            </div>
          </div>

          <div>
            <div className="flex items-center justify-between mb-3">
              <label className="font-body-md text-body-md text-text-primary font-medium">
                Accesorios
              </label>
            </div>
            <div className="space-y-2">
              {(
                [
                  ['lentes', 'visibility', 'Lentes deportivos'],
                  ['reloj', 'watch', 'Reloj GPS'],
                  ['audifonos', 'headphones', 'Audífonos'],
                ] as const
              ).map(([clave, icono, etiqueta]) => (
                <div
                  key={clave}
                  className="flex items-center justify-between px-4 py-3 bg-surface-container rounded-xl"
                >
                  <div className="flex items-center gap-3">
                    <span className="material-symbols-outlined text-[20px] text-text-secondary">
                      {icono}
                    </span>
                    <span className="font-body-md text-body-md text-text-primary">{etiqueta}</span>
                  </div>
                  <Interruptor
                    etiqueta={etiqueta}
                    activo={avatar[clave]}
                    onCambiar={(v) => cambiar({ [clave]: v })}
                  />
                </div>
              ))}
            </div>
          </div>
        </div>
      </main>
    </div>
  )
}
