import { useNavigate } from 'react-router-dom';

export function Bienvenida() {
  const navigate = useNavigate();

  return (
    <div className="w-full min-h-screen bg-surface flex flex-col justify-between overflow-hidden relative">
      <header className="w-full z-20 shrink-0">
        <div className="flex items-center justify-between px-margin py-2">
          <div className="flex items-center gap-2.5">
            <div className="w-9 h-9 rounded-xl bg-primary-600/15 border border-primary-600/30 flex items-center justify-center text-primary-500 shadow-sm">
              <span className="material-symbols-outlined text-[20px]">sprint</span>
            </div>
            <div className="flex flex-col">
              <span className="font-headline-md text-text-primary text-base font-extrabold tracking-wider leading-none">
                GACELAS
              </span>
              <span className="text-[10px] font-semibold text-primary-400 tracking-widest mt-0.5">
                LOJA, ECUADOR
              </span>
            </div>
          </div>
          <button
            onClick={() => navigate('/inicio')}
            className="text-text-secondary hover:text-text-primary text-sm font-semibold tracking-wide px-3 py-1.5 rounded-lg active:bg-surface-variant transition-colors min-h-[48px] flex items-center"
            type="button"
          >
            Saltar
          </button>
        </div>
      </header>

      <section className="relative w-full flex-1 flex flex-col items-center justify-center px-4 overflow-hidden min-h-[260px] max-h-[360px]">
        <div className="absolute w-72 h-72 rounded-full bg-primary-600/20 blur-3xl -top-4 pointer-events-none"></div>
        <div className="absolute w-52 h-52 rounded-full bg-secondary-600/15 blur-2xl top-20 pointer-events-none"></div>

        <div className="absolute inset-x-0 bottom-6 h-48 pointer-events-none opacity-40">
          <svg className="w-full h-full" fill="none" preserveAspectRatio="none" viewBox="0 0 400 200">
            <path d="M0,200 L0,120 L80,70 L150,110 L230,45 L320,105 L400,60 L400,200 Z" fill="#141b2b" />
            <path d="M0,200 L0,150 L60,110 L130,135 L200,90 L280,140 L360,100 L400,130 L400,200 Z" fill="#1F2937" />
            <path
              d="M0,150 L60,110 L130,135 L200,90 L280,140 L360,100 L400,130"
              stroke="#F59E0B"
              strokeLinecap="round"
              strokeOpacity="0.4"
              strokeWidth="1.5"
            />
          </svg>
        </div>

        <div className="absolute bottom-8 w-64 h-12 bg-primary-600/30 rounded-full blur-xl transform -scale-y-50"></div>
        <div className="absolute bottom-9 w-48 h-5 rounded-full border border-primary-500/40 bg-gradient-to-r from-transparent via-primary-600/30 to-transparent"></div>

        <div className="relative z-10 w-full max-w-[280px] aspect-[4/5] max-h-[280px] rounded-2xl overflow-hidden border border-border-subtle/80 bg-surface-container-low/60 shadow-lg flex items-center justify-center backdrop-blur-sm">
          <div className="absolute top-3 left-3 bg-surface/90 border border-border-subtle backdrop-blur-md px-2.5 py-1 rounded-full flex items-center gap-1.5 shadow-md">
            <span className="w-2 h-2 rounded-full bg-secondary-500 animate-pulse"></span>
            <span className="text-[11px] font-semibold text-text-primary tracking-wide">2,100 msnm</span>
          </div>
          <div className="absolute bottom-3 right-3 bg-surface/90 border border-border-subtle backdrop-blur-md px-2.5 py-1 rounded-full flex items-center gap-1.5 shadow-md">
            <span className="material-symbols-outlined text-primary-400 text-[14px]">wb_twilight</span>
            <span className="text-[11px] font-medium text-on-surface">14°C Fresco</span>
          </div>
        </div>
      </section>

      <div className="w-full z-20 px-6 pb-4 pt-1 flex flex-col justify-end bg-gradient-to-t from-surface via-surface/95 to-transparent">
        <div aria-label="Progreso de introducción: Paso 1 de 3" className="flex items-center justify-center gap-2 mb-3.5">
          <div className="h-2 w-8 rounded-full bg-primary-600 shadow-[0_0_10px_rgba(217,119,6,0.6)]"></div>
          <div className="h-2 w-2 rounded-full bg-border-subtle"></div>
          <div className="h-2 w-2 rounded-full bg-border-subtle"></div>
        </div>

        <h1 className="text-text-primary font-headline-lg text-[27px] font-extrabold text-center tracking-tight leading-snug mb-2">
          Corre con la manada de Loja
        </h1>

        <p className="text-text-secondary text-sm font-body-md text-center leading-relaxed max-w-[340px] mx-auto mb-4">
          Graba tus carreras, descubre rutas locales y entrena con el grupo Gacelas.
        </p>

        <div className="flex flex-wrap items-center justify-center gap-2 mb-6">
          <div className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-md bg-surface-container border border-border-subtle text-xs text-on-surface">
            <span className="material-symbols-outlined text-primary-500 text-[14px]">route</span>
            <span className="font-medium text-[11px]">Jipiro y Zamora</span>
          </div>
          <div className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-md bg-surface-container border border-border-subtle text-xs text-on-surface">
            <span className="material-symbols-outlined text-secondary-500 text-[14px]">sensors</span>
            <span className="font-medium text-[11px]">GPS en vivo</span>
          </div>
          <div className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-md bg-surface-container border border-border-subtle text-xs text-on-surface">
            <span className="material-symbols-outlined text-primary-400 text-[14px]">groups</span>
            <span className="font-medium text-[11px]">Comunidad</span>
          </div>
        </div>

        <div className="w-full flex flex-col gap-2.5">
          <button
            onClick={() => navigate('/registro')}
            className="w-full h-14 bg-primary-600 hover:bg-primary-700 active:scale-[0.98] text-bg-primary font-headline-md font-bold text-base rounded-xl flex items-center justify-center gap-2 shadow-[0_8px_20px_-4px_rgba(217,119,6,0.35)] transition-all cursor-pointer"
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
        </div>

        <div className="w-full flex justify-center pt-4 pb-1">
          <div className="w-32 h-1 rounded-full bg-border-subtle/70"></div>
        </div>
      </div>
    </div>
  );
}
