import { useState } from 'react'
import { useNavigate } from 'react-router-dom'

export function PrivacidadUbicacion() {
  const navigate = useNavigate()
  const [privacidadPerfil, setPrivacidadPerfil] = useState('followers')
  const [ubicacionPrecisa, setUbicacionPrecisa] = useState(true)
  const [homeFuzzing, setHomeFuzzing] = useState(true)
  const [radioMetros, setRadioMetros] = useState(300)

  return (
    <div className="w-full min-h-screen bg-surface flex flex-col">
      <header className="px-margin py-4 border-b border-border-subtle">
        <div className="flex items-center gap-4">
          <button
            onClick={() => navigate('/ajustes')}
            className="w-10 h-10 rounded-full bg-surface-container flex items-center justify-center text-text-secondary hover:text-text-primary transition-colors"
          >
            <span className="material-symbols-outlined">arrow_back</span>
          </button>
          <h1 className="font-headline-md text-headline-md text-text-primary">
            Privacidad y Ubicación
          </h1>
        </div>
      </header>

      <main className="flex-1 px-margin py-6 space-y-6 pb-24">
        <section>
          <h2 className="font-title-md text-title-md text-text-primary mb-3">
            Visibilidad del perfil
          </h2>
          <div className="space-y-2">
            <button
              onClick={() => setPrivacidadPerfil('public')}
              className={`w-full p-4 rounded-xl border-2 text-left transition-colors ${
                privacidadPerfil === 'public'
                  ? 'bg-primary-container/20 border-primary-600'
                  : 'bg-surface-container border-border-subtle hover:border-border-subtle/80'
              }`}
            >
              <div className="flex items-center justify-between mb-2">
                <div className="flex items-center gap-3">
                  <span className="material-symbols-outlined text-[24px] text-primary-500">
                    public
                  </span>
                  <span className="font-body-md text-body-md text-text-primary font-semibold">
                    Público
                  </span>
                </div>
                <span
                  className={`material-symbols-outlined ${
                    privacidadPerfil === 'public' ? 'text-primary-500' : 'text-text-secondary'
                  }`}
                >
                  {privacidadPerfil === 'public' ? 'check_circle' : 'radio_button_unchecked'}
                </span>
              </div>
              <p className="font-body-sm text-body-sm text-text-secondary">
                Cualquiera puede ver tu perfil y actividades públicas
              </p>
            </button>

            <button
              onClick={() => setPrivacidadPerfil('followers')}
              className={`w-full p-4 rounded-xl border-2 text-left transition-colors ${
                privacidadPerfil === 'followers'
                  ? 'bg-primary-container/20 border-primary-600'
                  : 'bg-surface-container border-border-subtle hover:border-border-subtle/80'
              }`}
            >
              <div className="flex items-center justify-between mb-2">
                <div className="flex items-center gap-3">
                  <span className="material-symbols-outlined text-[24px] text-secondary-500">
                    group
                  </span>
                  <span className="font-body-md text-body-md text-text-primary font-semibold">
                    Solo seguidores
                  </span>
                </div>
                <span
                  className={`material-symbols-outlined ${
                    privacidadPerfil === 'followers' ? 'text-primary-500' : 'text-text-secondary'
                  }`}
                >
                  {privacidadPerfil === 'followers' ? 'check_circle' : 'radio_button_unchecked'}
                </span>
              </div>
              <p className="font-body-sm text-body-sm text-text-secondary">
                Solo seguidores aceptados pueden ver tus actividades
              </p>
            </button>

            <button
              onClick={() => setPrivacidadPerfil('private')}
              className={`w-full p-4 rounded-xl border-2 text-left transition-colors ${
                privacidadPerfil === 'private'
                  ? 'bg-primary-container/20 border-primary-600'
                  : 'bg-surface-container border-border-subtle hover:border-border-subtle/80'
              }`}
            >
              <div className="flex items-center justify-between mb-2">
                <div className="flex items-center gap-3">
                  <span className="material-symbols-outlined text-[24px] text-text-primary">
                    lock
                  </span>
                  <span className="font-body-md text-body-md text-text-primary font-semibold">
                    Privado
                  </span>
                </div>
                <span
                  className={`material-symbols-outlined ${
                    privacidadPerfil === 'private' ? 'text-primary-500' : 'text-text-secondary'
                  }`}
                >
                  {privacidadPerfil === 'private' ? 'check_circle' : 'radio_button_unchecked'}
                </span>
              </div>
              <p className="font-body-sm text-body-sm text-text-secondary">
                Solo tú puedes ver tus actividades
              </p>
            </button>
          </div>
        </section>

        <section>
          <h2 className="font-title-md text-title-md text-text-primary mb-3">Ubicación GPS</h2>
          <div className="space-y-2">
            <div className="flex items-center justify-between px-4 py-3 bg-surface-container rounded-xl">
              <div>
                <div className="font-body-md text-body-md text-text-primary font-semibold mb-1">
                  Ubicación precisa
                </div>
                <p className="font-body-sm text-body-sm text-text-secondary">
                  Graba tu ubicación exacta durante carreras
                </p>
              </div>
              <label className="relative inline-flex items-center cursor-pointer">
                <input
                  type="checkbox"
                  className="sr-only peer"
                  checked={ubicacionPrecisa}
                  onChange={(e) => setUbicacionPrecisa(e.target.checked)}
                />
                <div className="w-11 h-6 bg-surface-container-high peer-focus:outline-none peer-focus:ring-2 peer-focus:ring-primary-600 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-primary-600"></div>
              </label>
            </div>
          </div>
        </section>

        <section>
          <h2 className="font-title-md text-title-md text-text-primary mb-3">
            Protección de hogar
          </h2>
          <div className="space-y-3">
            <div className="flex items-center justify-between px-4 py-3 bg-surface-container rounded-xl">
              <div>
                <div className="font-body-md text-body-md text-text-primary font-semibold mb-1">
                  Ocultar ubicación de casa
                </div>
                <p className="font-body-sm text-body-sm text-text-secondary">
                  Difumina los puntos de inicio/fin cerca de tu casa
                </p>
              </div>
              <label className="relative inline-flex items-center cursor-pointer">
                <input
                  type="checkbox"
                  className="sr-only peer"
                  checked={homeFuzzing}
                  onChange={(e) => setHomeFuzzing(e.target.checked)}
                />
                <div className="w-11 h-6 bg-surface-container-high peer-focus:outline-none peer-focus:ring-2 peer-focus:ring-primary-600 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-primary-600"></div>
              </label>
            </div>

            {homeFuzzing && (
              <div className="px-4 py-3 bg-surface-container rounded-xl">
                <div className="flex items-center justify-between mb-3">
                  <span className="font-body-md text-body-md text-text-primary">
                    Radio de protección
                  </span>
                  <span className="font-headline-sm text-headline-sm text-primary">
                    {radioMetros}m
                  </span>
                </div>
                <input
                  type="range"
                  min="100"
                  max="500"
                  step="50"
                  value={radioMetros}
                  onChange={(e) => setRadioMetros(parseInt(e.target.value))}
                  className="w-full h-2 bg-surface-container-high rounded-lg appearance-none cursor-pointer accent-primary-600"
                />
                <div className="flex justify-between text-xs text-text-secondary mt-2">
                  <span>100m</span>
                  <span>500m</span>
                </div>
              </div>
            )}
          </div>
        </section>

        <section>
          <h2 className="font-title-md text-title-md text-text-primary mb-3">Compartir en vivo</h2>
          <div className="bg-surface-container rounded-xl p-4">
            <p className="font-body-sm text-body-sm text-text-secondary mb-3">
              Cuando compartes tu ubicación en vivo, solo las personas que elijas pueden verte.
              Puedes detener el seguimiento en cualquier momento.
            </p>
            <div className="flex items-center gap-2 text-secondary-500">
              <span className="material-symbols-outlined text-[18px]">shield</span>
              <span className="font-label-md text-label-md">Tu privacidad está protegida</span>
            </div>
          </div>
        </section>
      </main>
    </div>
  )
}
