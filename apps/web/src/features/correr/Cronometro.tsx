import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';

interface Split {
  id: number;
  distance: number;
  pace: string;
  time: string;
  label: string;
  isCurrent?: boolean;
}

export function Cronometro() {
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState<'splits' | 'intervals' | 'metronome'>('splits');
  const [time, setTime] = useState(1753);
  const [distance] = useState(5.42);
  const [isPaused, setIsPaused] = useState(false);

  const splits: Split[] = [
    { id: 5, distance: 5.42, pace: '4:52', time: '02:18', label: 'En curso actual', isCurrent: true },
    { id: 4, distance: 1.0, pace: '5:12', time: '05:12', label: 'Subida San Cayetano' },
    { id: 3, distance: 1.0, pace: '4:48', time: '04:48', label: 'Mejor vuelta' },
    { id: 2, distance: 1.0, pace: '5:20', time: '05:20', label: 'Av. Universitaria' },
    { id: 1, distance: 1.0, pace: '5:08', time: '05:08', label: 'Inicio Jipiro' },
  ];

  useEffect(() => {
    if (!isPaused) {
      const interval = setInterval(() => {
        setTime((t) => t + 1);
      }, 1000);
      return () => clearInterval(interval);
    }
  }, [isPaused]);

  const formatTime = (seconds: number) => {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins}:${secs.toString().padStart(2, '0')}`;
  };

  const progress = (time / 3600) * 100;

  return (
    <div className="w-full min-h-screen bg-surface flex flex-col">
      <header className="fixed top-0 inset-x-0 z-50 bg-surface/85 backdrop-blur-xl pt-safe">
        <div className="h-16 px-space-md flex items-center justify-between">
          <div className="flex items-center gap-space-xs">
            <button
              onClick={() => navigate('/correr/en-curso')}
              className="w-12 h-12 flex items-center justify-center rounded-full text-on-surface hover:text-primary-500 active:bg-surface-container transition-colors"
            >
              <span className="material-symbols-outlined text-[24px]">arrow_back</span>
            </button>
            <h1 className="text-headline-sm font-headline-sm text-on-surface tracking-tight">
              Cronómetro Profesional
            </h1>
          </div>
        </div>
      </header>

      <main className="flex-1 pt-16 pb-24 overflow-auto">
        <div className="px-margin pt-space-sm pb-space-xs">
          <div className="flex items-center gap-space-xs p-1 bg-surface-container rounded-xl overflow-x-auto">
            <button className="flex-1 min-w-[110px] py-2 px-space-sm rounded-lg text-label-md font-label-md transition-all bg-primary-container/20 text-primary-400 flex items-center justify-center gap-1.5 shadow-sm">
              <span className="material-symbols-outlined text-[16px]">directions_run</span>
              <span>Modo Libre</span>
            </button>
            <button className="flex-1 min-w-[110px] py-2 px-space-sm rounded-lg text-label-md font-label-md text-text-secondary hover:text-on-surface flex items-center justify-center gap-1.5">
              <span className="material-symbols-outlined text-[16px]">timer_3</span>
              <span>Fartlek</span>
            </button>
            <button className="flex-1 min-w-[110px] py-2 px-space-sm rounded-lg text-label-md font-label-md text-text-secondary hover:text-on-surface flex items-center justify-center gap-1.5">
              <span className="material-symbols-outlined text-[16px]">speed</span>
              <span>Ritmo Pacer</span>
            </button>
          </div>
        </div>

        <div className="px-margin my-space-xs">
          <div className="p-space-sm rounded-xl bg-surface-container-low shadow-sm flex items-center justify-between gap-space-sm">
            <div className="flex items-center gap-2 min-w-0">
              <div className="w-2.5 h-2.5 rounded-full bg-secondary-500 animate-ping"></div>
              <div className="flex flex-col min-w-0">
                <span className="text-body-sm font-body-sm text-text-secondary truncate">
                  Objetivo Pacer: <strong className="text-on-surface">5:00 /km</strong>
                </span>
                <span className="text-label-md font-label-md text-secondary-500 tracking-wide">
                  0.8s más rápido que objetivo
                </span>
              </div>
            </div>
          </div>
        </div>

        <div className="relative px-margin my-space-sm flex flex-col items-center justify-center">
          <div className="relative w-72 h-72 flex items-center justify-center">
            <div className="absolute inset-4 rounded-full bg-gradient-to-tr from-primary-600/10 via-secondary-500/10 to-transparent blur-2xl pointer-events-none"></div>

            <svg className="w-full h-full -rotate-90 transform" viewBox="0 0 240 240">
              <circle
                className="text-surface-container-high"
                cx="120"
                cy="120"
                fill="none"
                r="104"
                stroke="currentColor"
                strokeWidth="6"
              />
              <circle
                className="text-primary-500 transition-all duration-300"
                cx="120"
                cy="120"
                fill="none"
                r="104"
                stroke="currentColor"
                strokeDasharray="653.45"
                strokeDashoffset={653.45 - (653.45 * progress) / 100}
                strokeLinecap="round"
                strokeWidth="7"
              />
            </svg>

            <div className="absolute inset-0 flex flex-col items-center justify-center text-center px-4">
              <div className="flex items-center gap-1.5 px-2.5 py-0.5 rounded-full bg-surface-container-highest mb-1 shadow-sm">
                <span className="w-2 h-2 rounded-full bg-secondary-500 animate-pulse"></span>
                <span className="text-label-md font-label-md text-on-surface uppercase tracking-wider">
                  En Marcha
                </span>
                <span className="text-body-sm font-body-sm text-outline">•</span>
                <span className="text-label-md font-label-md text-secondary-500 flex items-center">
                  <span className="material-symbols-outlined text-[13px] mr-0.5">steps</span>
                  168 spm
                </span>
              </div>

              <div className="flex items-baseline justify-center tracking-tight font-metric-huge text-metric-huge text-on-surface">
                <span>{formatTime(time)}</span>
                <span className="font-headline-sm text-headline-sm text-primary-400 ml-1">.45</span>
              </div>

              <div className="mt-1 flex items-center gap-1 text-label-md font-label-md text-text-secondary bg-surface-container/60 px-3 py-1 rounded-full">
                <span className="material-symbols-outlined text-[15px] text-primary-500">flag</span>
                <span>Km 6 en curso: <strong className="text-on-surface">2:18</strong></span>
              </div>
            </div>
          </div>
        </div>

        <div className="px-margin my-space-xs">
          <div className="grid grid-cols-2 gap-space-sm">
            <div className="bg-surface-container p-space-md rounded-xl flex items-center gap-space-sm shadow-sm">
              <div className="w-10 h-10 rounded-xl bg-surface-container-high flex items-center justify-center text-primary-500 shrink-0">
                <span className="material-symbols-outlined text-[22px]">distance</span>
              </div>
              <div className="min-w-0 flex flex-col">
                <span className="text-label-md font-label-md text-text-secondary">Distancia Total</span>
                <span className="text-headline-sm font-headline-sm text-on-surface tabular-nums truncate">
                  {distance.toFixed(2)} <span className="text-label-lg font-label-lg text-text-secondary">km</span>
                </span>
              </div>
            </div>

            <div className="bg-surface-container p-space-md rounded-xl flex items-center gap-space-sm shadow-sm">
              <div className="w-10 h-10 rounded-xl bg-surface-container-high flex items-center justify-center text-secondary-500 shrink-0">
                <span className="material-symbols-outlined text-[22px]">pace</span>
              </div>
              <div className="min-w-0 flex flex-col">
                <span className="text-label-md font-label-md text-text-secondary">Ritmo Actual</span>
                <span className="text-headline-sm font-headline-sm text-on-surface tabular-nums truncate">
                  4:52 <span className="text-label-lg font-label-lg text-text-secondary">/km</span>
                </span>
              </div>
            </div>

            <div className="bg-surface-container p-space-md rounded-xl flex items-center gap-space-sm shadow-sm">
              <div className="w-10 h-10 rounded-xl bg-surface-container-high flex items-center justify-center text-tertiary shrink-0">
                <span className="material-symbols-outlined text-[22px]">trending_up</span>
              </div>
              <div className="min-w-0 flex flex-col">
                <span className="text-label-md font-label-md text-text-secondary">Ritmo Promedio</span>
                <span className="text-headline-sm font-headline-sm text-on-surface tabular-nums truncate">
                  5:08 <span className="text-label-lg font-label-lg text-text-secondary">/km</span>
                </span>
              </div>
            </div>

            <div className="bg-surface-container p-space-md rounded-xl flex items-center gap-space-sm shadow-sm">
              <div className="w-10 h-10 rounded-xl bg-surface-container-high flex items-center justify-center text-danger shrink-0">
                <span className="material-symbols-outlined text-[22px]">favorite</span>
              </div>
              <div className="min-w-0 flex flex-col">
                <div className="flex items-center gap-1">
                  <span className="text-label-md font-label-md text-text-secondary">Cardio</span>
                  <span className="px-1.5 py-0.2 rounded text-[10px] bg-secondary-container/30 text-secondary-fixed font-label-md">
                    Z3
                  </span>
                </div>
                <span className="text-headline-sm font-headline-sm text-on-surface tabular-nums truncate">
                  154 <span className="text-label-lg font-label-lg text-text-secondary">bpm</span>
                </span>
              </div>
            </div>
          </div>
        </div>

        <div className="px-margin my-space-md">
          <div className="flex items-center justify-between gap-space-md">
            <button className="w-16 h-16 rounded-full bg-surface-container hover:bg-surface-container-high active:scale-95 text-on-surface flex flex-col items-center justify-center transition-all shadow-md">
              <span className="material-symbols-outlined text-[22px] text-primary-400">flag</span>
              <span className="text-label-md font-label-md">Vuelta</span>
            </button>

            <button
              onClick={() => setIsPaused(!isPaused)}
              className="w-20 h-20 rounded-full bg-primary-600 hover:bg-primary-700 active:scale-95 text-bg-primary flex items-center justify-center shadow-lg transition-transform relative"
            >
              <div className="absolute inset-0 rounded-full bg-primary-500/20 animate-ping pointer-events-none"></div>
              <span className="material-symbols-outlined text-[36px] font-bold">
                {isPaused ? 'play_arrow' : 'pause'}
              </span>
            </button>

            <button className="w-16 h-16 rounded-full bg-surface-container hover:bg-surface-container-high active:scale-95 text-danger flex flex-col items-center justify-center transition-all shadow-md">
              <span className="material-symbols-outlined text-[22px]">stop</span>
              <span className="text-label-md font-label-md">Fin</span>
            </button>
          </div>
          <p className="text-center text-body-sm font-body-sm text-text-secondary mt-2">
            Mantén presionado Fin por 2 seg para guardar tu sesión.
          </p>
        </div>

        <div className="px-margin mt-space-sm">
          <div className="flex items-center border-none bg-surface-container-low p-1 rounded-xl">
            <button
              onClick={() => setActiveTab('splits')}
              className={`flex-1 py-1.5 rounded-lg text-label-md font-label-md transition-colors flex items-center justify-center gap-1 ${
                activeTab === 'splits' ? 'bg-surface-container text-on-surface font-semibold shadow-sm' : 'text-text-secondary'
              }`}
            >
              <span className="material-symbols-outlined text-[16px]">list_alt</span>
              <span>Splits</span>
            </button>
            <button
              onClick={() => setActiveTab('intervals')}
              className={`flex-1 py-1.5 rounded-lg text-label-md font-label-md transition-colors flex items-center justify-center gap-1 ${
                activeTab === 'intervals' ? 'bg-surface-container text-on-surface font-semibold shadow-sm' : 'text-text-secondary'
              }`}
            >
              <span className="material-symbols-outlined text-[16px]">repeat</span>
              <span>Intervalos</span>
            </button>
            <button
              onClick={() => setActiveTab('metronome')}
              className={`flex-1 py-1.5 rounded-lg text-label-md font-label-md transition-colors flex items-center justify-center gap-1 ${
                activeTab === 'metronome' ? 'bg-surface-container text-on-surface font-semibold shadow-sm' : 'text-text-secondary'
              }`}
            >
              <span className="material-symbols-outlined text-[16px]">graphic_eq</span>
              <span>Metrónomo</span>
            </button>
          </div>
        </div>

        {activeTab === 'splits' && (
          <div className="px-margin mt-space-sm flex flex-col gap-space-xs">
            <div className="flex items-center justify-between text-label-md font-label-md text-text-secondary px-space-sm py-1">
              <span>PARCIAL / DISTANCIA</span>
              <div className="flex items-center gap-6">
                <span>RITMO</span>
                <span>TIEMPO</span>
              </div>
            </div>

            <div className="flex flex-col gap-space-xs max-h-56 overflow-y-auto">
              {splits.map((split) => (
                <div
                  key={split.id}
                  className={`p-space-sm rounded-xl flex items-center justify-between shadow-sm ${
                    split.isCurrent ? 'bg-surface-container' : 'bg-surface-container-low'
                  }`}
                >
                  <div className="flex items-center gap-2">
                    <div
                      className={`w-6 h-6 rounded-full text-label-md font-label-md flex items-center justify-center ${
                        split.isCurrent
                          ? 'bg-secondary-container/20 text-secondary-500'
                          : split.id === 3
                          ? 'bg-primary-container text-on-primary-container'
                          : 'bg-surface-container-high text-on-surface'
                      }`}
                    >
                      {split.id}
                    </div>
                    <div>
                      <div className="text-body-md font-body-md text-on-surface font-medium">
                        {split.isCurrent ? `Km ${split.distance}` : `${split.distance} km`}
                      </div>
                      <div className="text-body-sm font-body-sm text-text-secondary">{split.label}</div>
                    </div>
                  </div>
                  <div className="flex items-center gap-6 text-right">
                    <span className="text-label-lg font-label-lg text-on-surface tabular-nums">{split.pace}</span>
                    <span
                      className={`text-label-lg font-label-lg tabular-nums ${
                        split.isCurrent ? 'text-primary-400' : 'text-text-secondary'
                      }`}
                    >
                      {split.time}
                    </span>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}
      </main>
    </div>
  );
}
