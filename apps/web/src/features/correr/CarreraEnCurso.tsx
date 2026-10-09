import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';

export function CarreraEnCurso() {
  const navigate = useNavigate();
  const [isPaused, setIsPaused] = useState(false);
  const [time, setTime] = useState(0);
  const [distance, setDistance] = useState(0);

  useEffect(() => {
    if (!isPaused) {
      const interval = setInterval(() => {
        setTime((t) => t + 1);
        setDistance((d) => d + (Math.random() * 0.01));
      }, 1000);
      return () => clearInterval(interval);
    }
  }, [isPaused]);

  const formatTime = (seconds: number) => {
    const hrs = Math.floor(seconds / 3600);
    const mins = Math.floor((seconds % 3600) / 60);
    const secs = seconds % 60;
    return `${hrs.toString().padStart(2, '0')}:${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
  };

  const pace = distance > 0 ? Math.floor(time / distance / 60) : 0;

  const handleFinish = () => {
    navigate('/correr/resumen');
  };

  return (
    <div className="w-full min-h-screen bg-surface flex flex-col">
      <div className="relative flex-1 bg-surface-container-low">
        <div className="absolute inset-0 bg-gradient-to-b from-surface-container-low via-surface to-surface"></div>
        
        <div className="absolute top-4 inset-x-4 z-10">
          <button
            onClick={() => navigate('/inicio')}
            className="w-10 h-10 rounded-full bg-surface/90 backdrop-blur-md flex items-center justify-center text-text-primary shadow-lg"
          >
            <span className="material-symbols-outlined">close</span>
          </button>
        </div>

        <div className="relative z-10 flex flex-col items-center justify-center h-full px-6 pt-20 pb-32">
          <div className="mb-8 text-center">
            <span className="font-label-lg text-label-lg text-primary uppercase tracking-wider flex items-center justify-center gap-2">
              <span className="w-2 h-2 rounded-full bg-secondary animate-pulse"></span>
              En curso
            </span>
          </div>

          <div className="bg-surface-container/60 backdrop-blur-xl border border-border-subtle rounded-3xl p-8 mb-8 shadow-2xl w-full max-w-md">
            <div className="text-center mb-6">
              <div className="font-metric-huge text-metric-huge text-primary leading-none mb-2">
                {distance.toFixed(2)}
              </div>
              <div className="font-headline-sm text-headline-sm text-text-secondary uppercase tracking-wider">
                Kilómetros
              </div>
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div className="text-center">
                <div className="font-headline-lg text-headline-lg text-text-primary leading-none mb-1">
                  {formatTime(time)}
                </div>
                <div className="font-label-md text-label-md text-text-secondary uppercase">
                  Tiempo
                </div>
              </div>
              <div className="text-center">
                <div className="font-headline-lg text-headline-lg text-text-primary leading-none mb-1">
                  {pace}:00
                </div>
                <div className="font-label-md text-label-md text-text-secondary uppercase">
                  Ritmo /km
                </div>
              </div>
            </div>
          </div>

          <div className="flex gap-4">
            <button
              onClick={() => setIsPaused(!isPaused)}
              className="w-16 h-16 rounded-full bg-surface-container border-2 border-border-subtle flex items-center justify-center text-text-primary hover:bg-surface-container-high transition-all shadow-lg"
            >
              <span className="material-symbols-outlined text-[32px]">
                {isPaused ? 'play_arrow' : 'pause'}
              </span>
            </button>
            <button
              onClick={handleFinish}
              className="w-16 h-16 rounded-full bg-danger/20 border-2 border-danger flex items-center justify-center text-danger hover:bg-danger/30 transition-all shadow-lg"
            >
              <span className="material-symbols-outlined text-[32px]">stop</span>
            </button>
          </div>
        </div>
      </div>

      <div className="bg-surface-container-high border-t border-border-subtle p-4">
        <div className="grid grid-cols-3 gap-4 text-center">
          <div>
            <div className="font-headline-sm text-headline-sm text-text-primary">120</div>
            <div className="font-label-md text-label-md text-text-secondary">BPM</div>
          </div>
          <div>
            <div className="font-headline-sm text-headline-sm text-text-primary">128</div>
            <div className="font-label-md text-label-md text-text-secondary">Pasos/min</div>
          </div>
          <div>
            <div className="font-headline-sm text-headline-sm text-text-primary">85</div>
            <div className="font-label-md text-label-md text-text-secondary">Calorías</div>
          </div>
        </div>
      </div>
    </div>
  );
}
