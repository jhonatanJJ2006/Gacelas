import { useEffect, useRef, useState } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import { MensajeError } from '../../../components/ui'
import { estaActiva } from '../../../lib/runTracker'
import { useApp } from '../../../store/app'
import {
  empezarSeguimiento,
  geolocalizacionDisponible,
  mensajeErrorGps,
  tracker,
} from '../../../store/carrera'
import { calidadSenal } from '../../../lib/logica'
import { ModalC2 } from '../../../components/modales'
import { historial, otorgado, registrarConsentimiento } from '../../legal/consentimientos'

type EstadoGps =
  { tipo: 'buscando' } | { tipo: 'listo'; precisionM: number } | { tipo: 'error'; mensaje: string }

export function Correr() {
  const navigate = useNavigate()
  const precisa = useApp((s) => s.ajustes.ubicacionPrecisa)
  const [gps, setGps] = useState<EstadoGps>({ tipo: 'buscando' })
  const [errorCarrera, setErrorCarrera] = useState<string | null>(null)

  useEffect(() => {
    if (!geolocalizacionDisponible()) {
      setGps({ tipo: 'error', mensaje: 'Este navegador no tiene GPS.' })
      return
    }
    let vivo = true
    navigator.geolocation.getCurrentPosition(
      (p) => vivo && setGps({ tipo: 'listo', precisionM: Math.round(p.coords.accuracy) }),
      (e) => vivo && setGps({ tipo: 'error', mensaje: mensajeErrorGps(e) }),
      { enableHighAccuracy: precisa, timeout: 15_000, maximumAge: 5_000 }
    )
    return () => {
      vivo = false
    }
  }, [precisa])

  const cuentaSeg = useApp((s) => s.ajustes.cuentaRegresivaSeg ?? 0)
  const [cuenta, setCuenta] = useState<number | null>(null)
  useEffect(() => {
    if (cuenta === null) return
    const t = setTimeout(() => (cuenta <= 1 ? arrancarRef.current() : setCuenta(cuenta - 1)), 1000)
    return () => clearTimeout(t)
  }, [cuenta])
  const arrancarRef = useRef(() => {})
  const [pedirGps, setPedirGps] = useState(false)

  if (estaActiva(tracker.estado)) return <Navigate to="/correr/en-curso" replace />

  const arrancar = () => {
    setCuenta(null)
    setErrorCarrera(null)
    empezarSeguimiento({ precisa, onError: setErrorCarrera })
    navigate('/correr/en-curso')
  }
  const iniciar = () => (cuentaSeg > 0 ? setCuenta(cuentaSeg) : arrancar())
  const handleStart = () => (otorgado(historial(), 'gps_carrera') ? iniciar() : setPedirGps(true))

  arrancarRef.current = arrancar
  const listo = gps.tipo === 'listo'
  if (cuenta !== null)
    return (
      <div
        className="fixed inset-0 z-50 bg-surface flex flex-col items-center justify-center gap-6"
        role="timer"
        aria-label="Cuenta regresiva"
      >
        <span className="text-body-md text-text-secondary">Prepárate, arrancamos en</span>
        <span
          className="font-mono text-[96px] leading-none text-primary"
          data-testid="cuenta-regresiva"
        >
          {cuenta}
        </span>
        <button
          onClick={arrancar}
          className="px-4 py-2 rounded-xl bg-surface-container text-text-primary"
        >
          Empezar ya
        </button>
      </div>
    )

  return (
    <div className="w-full min-h-screen bg-surface flex flex-col">
      <header className="px-margin py-4">
        <button
          onClick={() => navigate('/inicio')}
          aria-label="Cerrar"
          className="w-10 h-10 rounded-full bg-surface-container flex items-center justify-center text-text-secondary hover:text-text-primary transition-colors"
        >
          <span className="material-symbols-outlined">close</span>
        </button>
      </header>

      <main className="lg:w-full lg:max-w-5xl lg:mx-auto flex-1 flex flex-col items-center justify-center px-6 pb-24">
        <div className="text-center mb-12">
          <div className="w-24 h-24 rounded-full bg-primary-600/20 flex items-center justify-center mx-auto mb-6">
            <span className="material-symbols-outlined text-[64px] text-primary-500">
              directions_run
            </span>
          </div>
          <h1 className="font-headline-lg text-headline-lg text-text-primary mb-3">
            ¿Listo para correr?
          </h1>
          <p className="font-body-md text-body-md text-text-secondary max-w-sm mx-auto">
            Activa el GPS y comienza a grabar tu carrera
          </p>
        </div>

        <div className="w-full max-w-md space-y-4 mb-8">
          <div className="tarjeta-c2 p-6">
            <div className="flex items-center gap-4 mb-4">
              <div
                className={`w-12 h-12 rounded-full flex items-center justify-center ${listo ? 'bg-secondary-600/20 text-secondary-500' : gps.tipo === 'error' ? 'bg-danger/20 text-red-300' : 'bg-primary-600/20 text-primary-500'}`}
              >
                <span className="material-symbols-outlined text-[24px]">
                  {gps.tipo === 'error' ? 'location_off' : 'location_on'}
                </span>
              </div>
              <div className="flex-1">
                <p
                  className="font-body-md text-body-md text-text-primary font-semibold mb-1"
                  data-testid="estado-gps"
                >
                  {listo
                    ? 'GPS Activado'
                    : gps.tipo === 'error'
                      ? 'GPS no disponible'
                      : 'Buscando GPS...'}
                </p>
                <p className="font-body-sm text-body-sm text-text-secondary">
                  {listo
                    ? `${calidadSenal(gps.precisionM)} (±${gps.precisionM} m)`
                    : gps.tipo === 'error'
                      ? gps.mensaje
                      : 'Espera unos segundos al aire libre'}
                </p>
              </div>
              {listo && (
                <span className="material-symbols-outlined text-secondary-500">check_circle</span>
              )}
            </div>

            <div className="space-y-3 pt-4 border-t border-border-subtle">
              <div className="flex items-center justify-between">
                <span className="font-body-md text-text-secondary">Tipo de actividad</span>
                <span className="font-body-md text-text-primary font-medium">Carrera</span>
              </div>
              <div className="flex items-center justify-between">
                <span className="font-body-md text-text-secondary">Precisión</span>
                <span className="font-body-md text-text-primary font-medium">
                  {precisa ? 'Alta' : 'Ahorro de batería'}
                </span>
              </div>
            </div>
          </div>
          <MensajeError mensaje={errorCarrera} />
        </div>

        <ModalC2
          abierto={pedirGps}
          icono="my_location"
          titulo="Usar tu ubicación durante la carrera"
          texto="Gacelas usa el GPS del dispositivo solo mientras grabas una carrera, para medir distancia, ritmo y dibujar tu recorrido. La ruta se guarda en tu historial y solo se comparte si tú la publicas. Nadie te ve en el mapa en vivo salvo que actives ese permiso aparte. Puedes revocarlo cuando quieras en Ajustes, Privacidad."
          primario="Aceptar y empezar"
          onPrimario={() => {
            setPedirGps(false)
            void registrarConsentimiento('gps_carrera', true)
            iniciar()
          }}
          secundario="Ahora no"
          onCerrar={() => setPedirGps(false)}
        />
        <button
          onClick={handleStart}
          aria-label="Comenzar carrera"
          className="w-24 h-24 rounded-full bg-primary-600 hover:bg-primary-700 active:scale-95 shadow-[0_12px_28px_-4px_rgba(123,31,162,0.5)] flex items-center justify-center transition-all"
        >
          <span className="material-symbols-outlined text-[48px] text-on-primary">play_arrow</span>
        </button>

        <p className="font-label-lg text-label-lg text-text-secondary mt-6">
          {listo
            ? 'Toca para comenzar'
            : 'Puedes empezar; la distancia se suma cuando llegue la señal'}
        </p>
      </main>
    </div>
  )
}
