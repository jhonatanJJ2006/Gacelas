import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { MensajeError } from '../../../../shared/ui/ui'
import { geolocalizacionDisponible, mensajeErrorGps } from '../../../actividad/api'

export function PermisosUbicacion() {
  const navigate = useNavigate()
  const [error, setError] = useState<string | null>(null)
  const [pidiendo, setPidiendo] = useState(false)

  // Pide el permiso real del navegador con una lectura de posicion.
  const handleAllow = () => {
    if (!geolocalizacionDisponible()) {
      setError('Este navegador no tiene GPS. Puedes seguir usando la app sin grabar carreras.')
      return
    }
    setPidiendo(true)
    navigator.geolocation.getCurrentPosition(
      () => {
        setPidiendo(false)
        navigate('/inicio', { replace: true })
      },
      (e) => {
        setPidiendo(false)
        setError(mensajeErrorGps(e))
      },
      { enableHighAccuracy: true, timeout: 15_000 }
    )
  }

  const handleSkip = () => {
    navigate('/inicio')
  }

  return (
    <div className="w-full min-h-screen bg-surface flex flex-col justify-between">
      <main className="flex-1 px-6 pb-8 flex flex-col justify-center items-center max-w-md mx-auto w-full text-center">
        <div className="w-24 h-24 rounded-full bg-primary-600/20 flex items-center justify-center mb-8">
          <span className="material-symbols-outlined text-[48px] text-primary-500">
            location_on
          </span>
        </div>

        <h1 className="text-text-primary font-headline-lg text-3xl font-bold mb-4">
          Activa tu ubicación
        </h1>
        <p className="text-text-secondary text-base mb-8 max-w-sm">
          Necesitamos acceso a tu ubicación para grabar tus carreras y mostrarte rutas cercanas
        </p>

        <div className="w-full space-y-3 mb-6">
          <div className="flex items-start gap-3 text-left">
            <div className="w-6 h-6 rounded-full bg-secondary-600/20 flex items-center justify-center flex-shrink-0 mt-0.5">
              <span className="material-symbols-outlined text-[16px] text-secondary-500">
                check
              </span>
            </div>
            <div>
              <p className="text-text-primary font-medium text-sm">Graba tus rutas con precisión</p>
              <p className="text-text-secondary text-xs mt-0.5">GPS activo durante tus carreras</p>
            </div>
          </div>

          <div className="flex items-start gap-3 text-left">
            <div className="w-6 h-6 rounded-full bg-secondary-600/20 flex items-center justify-center flex-shrink-0 mt-0.5">
              <span className="material-symbols-outlined text-[16px] text-secondary-500">
                check
              </span>
            </div>
            <div>
              <p className="text-text-primary font-medium text-sm">Descubre rutas cercanas</p>
              <p className="text-text-secondary text-xs mt-0.5">
                Encuentra las mejores rutas de Loja
              </p>
            </div>
          </div>

          <div className="flex items-start gap-3 text-left">
            <div className="w-6 h-6 rounded-full bg-secondary-600/20 flex items-center justify-center flex-shrink-0 mt-0.5">
              <span className="material-symbols-outlined text-[16px] text-secondary-500">
                check
              </span>
            </div>
            <div>
              <p className="text-text-primary font-medium text-sm">Tu privacidad está protegida</p>
              <p className="text-text-secondary text-xs mt-0.5">
                Controlas qué compartes y con quién
              </p>
            </div>
          </div>
        </div>

        <div className="w-full mb-3">
          <MensajeError mensaje={error} />
        </div>

        <button
          onClick={handleAllow}
          disabled={pidiendo}
          className="w-full h-14 bg-primary-600 hover:bg-primary-700 active:scale-[0.98] text-on-primary font-headline-md font-bold text-base rounded-xl flex items-center justify-center gap-2 shadow-[0_8px_20px_-4px_rgba(123,31,162,0.35)] transition-all cursor-pointer"
        >
          <span>{pidiendo ? 'Esperando permiso...' : 'Permitir ubicación'}</span>
          <span className="material-symbols-outlined text-[20px]">arrow_forward</span>
        </button>

        <button
          onClick={handleSkip}
          className="w-full h-12 bg-transparent hover:bg-surface-container active:scale-[0.99] text-text-secondary font-medium text-sm rounded-xl flex items-center justify-center transition-all cursor-pointer mt-3"
        >
          Configurar más tarde
        </button>
      </main>

      <div className="w-full flex justify-center pb-4">
        <div className="w-32 h-1 rounded-full bg-border-subtle/70"></div>
      </div>
    </div>
  )
}
