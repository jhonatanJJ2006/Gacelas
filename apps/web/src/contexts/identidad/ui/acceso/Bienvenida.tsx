import { LogoGacelas } from '../../../../shared/ui/LogoGacelas'
import { lazy, Suspense } from 'react'
import { useNavigate } from 'react-router-dom'
import { Runner3DFallback } from '../../../../shared/ui/Runner3D'
import { BotonGoogle } from './BotonGoogle'

const Runner3D = lazy(() =>
  import('../../../../shared/ui/Runner3D').then((m) => ({ default: m.Runner3D }))
)

export function Bienvenida() {
  const navigate = useNavigate()

  return (
    <div className="w-full h-[100dvh] bg-surface flex flex-col overflow-hidden relative">
      <header className="w-full z-20 shrink-0">
        <div className="flex items-center justify-between px-margin py-1 max-w-6xl mx-auto w-full">
          <LogoGacelas tam={36} />
          <button
            onClick={() => navigate('/inicio-sesion')}
            className="text-text-secondary hover:text-text-primary text-sm font-semibold tracking-wide px-3 py-1.5 rounded-lg active:bg-surface-variant transition-colors min-h-[48px] flex items-center"
            type="button"
          >
            Saltar
          </button>
        </div>
      </header>

      <main className="flex-1 min-h-0 w-full max-w-6xl mx-auto flex flex-col md:flex-row md:items-center md:gap-10 md:px-8">
        <section className="relative w-full flex-1 min-h-0 flex flex-col items-center justify-center px-4 overflow-hidden md:h-full md:max-h-[640px]">
          <div className="absolute w-72 h-72 rounded-full bg-primary-600/20 blur-3xl -top-4 pointer-events-none"></div>
          <div className="absolute w-52 h-52 rounded-full bg-secondary-600/15 blur-2xl top-20 pointer-events-none"></div>

          <div className="absolute inset-x-0 bottom-6 h-48 pointer-events-none opacity-40">
            <svg
              className="w-full h-full"
              fill="none"
              preserveAspectRatio="none"
              viewBox="0 0 400 200"
            >
              <path
                d="M0,200 L0,120 L80,70 L150,110 L230,45 L320,105 L400,60 L400,200 Z"
                fill="#141b2b"
              />
              <path
                d="M0,200 L0,150 L60,110 L130,135 L200,90 L280,140 L360,100 L400,130 L400,200 Z"
                fill="#1F2937"
              />
              <path
                d="M0,150 L60,110 L130,135 L200,90 L280,140 L360,100 L400,130"
                stroke="#A855F7"
                strokeLinecap="round"
                strokeOpacity="0.4"
                strokeWidth="1.5"
              />
            </svg>
          </div>

          <div className="absolute bottom-8 w-64 h-12 bg-primary-600/30 rounded-full blur-xl transform -scale-y-50"></div>
          <div className="absolute bottom-9 w-48 h-5 rounded-full border border-primary-500/40 bg-gradient-to-r from-transparent via-primary-600/30 to-transparent"></div>

          <div
            data-testid="bienvenida-corredor"
            className="relative z-10 h-full max-h-[460px] min-h-[120px] aspect-[4/5] max-w-full rounded-2xl overflow-hidden border border-border-subtle/80 bg-surface-container-low/60 shadow-lg flex items-center justify-center backdrop-blur-sm"
          >
            <Suspense fallback={<Runner3DFallback />}>
              <Runner3D animation="Jog" cameraPosition={[0, 1.35, 3.9]} target={[0, 1.2, 0]} />
            </Suspense>
            <div className="absolute z-10 top-3 left-3 bg-surface/90 border border-border-subtle backdrop-blur-md px-2.5 py-1 rounded-full flex items-center gap-1.5 shadow-md">
              <span className="w-2 h-2 rounded-full bg-secondary-500 animate-pulse"></span>
              <span className="text-[11px] font-semibold text-text-primary tracking-wide">
                2,100 msnm
              </span>
            </div>
            <div className="absolute z-10 bottom-3 right-3 bg-surface/90 border border-border-subtle backdrop-blur-md px-2.5 py-1 rounded-full flex items-center gap-1.5 shadow-md">
              <span className="material-symbols-outlined text-primary-400 text-[14px]">
                wb_twilight
              </span>
              <span className="text-[11px] font-medium text-on-surface">14°C Fresco</span>
            </div>
          </div>
        </section>

        <div className="w-full shrink-0 z-20 px-6 pb-3 pt-1 flex flex-col justify-end md:w-[420px] md:px-0 md:pb-0 md:items-stretch mx-auto">
          <div
            role="img"
            aria-label="Progreso de introducción: Paso 1 de 3"
            className="flex items-center justify-center md:justify-start gap-2 mb-3 short:mb-2"
          >
            <div className="h-2 w-8 rounded-full bg-primary-600 shadow-[0_0_10px_rgba(123,31,162,0.6)]"></div>
            <div className="h-2 w-2 rounded-full bg-border-subtle"></div>
            <div className="h-2 w-2 rounded-full bg-border-subtle"></div>
          </div>

          <h1 className="text-text-primary font-headline-lg text-[26px] md:text-4xl font-extrabold text-center md:text-left leading-tight mb-2">
            Corre con la manada de Loja
          </h1>

          <p className="text-text-secondary text-sm font-body-md text-center md:text-left leading-relaxed max-w-[340px] mx-auto md:mx-0 mb-3 md:text-base">
            Graba tus carreras, descubre rutas locales y entrena con el grupo Gacelas.
          </p>

          <div className="flex flex-wrap items-center justify-center md:justify-start gap-2 mb-4 short:hidden">
            <div className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-md bg-surface-container border border-border-subtle text-xs text-on-surface">
              <span className="material-symbols-outlined text-primary-500 text-[14px]">route</span>
              <span className="font-medium text-[11px]">Jipiro y Zamora</span>
            </div>
            <div className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-md bg-surface-container border border-border-subtle text-xs text-on-surface">
              <span className="material-symbols-outlined text-secondary-500 text-[14px]">
                sensors
              </span>
              <span className="font-medium text-[11px]">GPS en vivo</span>
            </div>
            <div className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-md bg-surface-container border border-border-subtle text-xs text-on-surface">
              <span className="material-symbols-outlined text-primary-400 text-[14px]">groups</span>
              <span className="font-medium text-[11px]">Comunidad</span>
            </div>
          </div>

          <div className="w-full max-w-sm mx-auto md:mx-0 md:max-w-none flex flex-col gap-2">
            <button
              onClick={() => navigate('/registro')}
              className="w-full h-12 short:h-11 bg-primary-600 hover:bg-primary-700 active:scale-[0.98] text-on-primary font-headline-md font-bold text-base rounded-xl flex items-center justify-center gap-2 shadow-[0_8px_20px_-4px_rgba(123,31,162,0.35)] transition-all cursor-pointer"
              type="button"
            >
              <span>Crear cuenta</span>
              <span className="material-symbols-outlined text-[20px]">arrow_forward</span>
            </button>

            <button
              onClick={() => navigate('/inicio-sesion')}
              className="w-full h-12 bg-transparent hover:bg-surface-container active:scale-[0.99] border border-border-subtle/80 text-text-primary font-medium text-sm rounded-xl flex items-center justify-center transition-all cursor-pointer min-h-[48px]"
              type="button"
            >
              Ya tengo cuenta, iniciar sesión
            </button>

            <BotonGoogle />
          </div>
        </div>
      </main>
    </div>
  )
}
