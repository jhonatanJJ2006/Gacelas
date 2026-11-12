import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { useIdentidad } from '../../identidad/api'
import { Dialogo } from '../../../shared/ui/ui'
import { useSos } from '../application/sos'
import {
  enlaceSms,
  enlaceSos,
  enlaceWhatsapp,
  MANTENER_MS,
  mensajeSos,
  NUMERO_EMERGENCIA,
} from '../domain/sos'

type Pos = { lat: number; lng: number }

/**
 * Boton SOS: hay que mantenerlo pulsado 3 segundos (anillo de progreso). Un toque corto solo
 * muestra la ayuda; asi no se activa por accidente. Al completarse abre la hoja del SOS.
 */
export function BotonSOS({
  posicion,
  className = '',
}: {
  posicion: Pos | null
  className?: string
}) {
  const [progreso, setProgreso] = useState(0)
  const [ayuda, setAyuda] = useState(false)
  const [hoja, setHoja] = useState(false)
  const inicio = useRef<number | null>(null)
  const cuadro = useRef<number | null>(null)
  const activa = useSos((s) => s.alertaId != null)
  const activar = useSos((s) => s.activar)
  const enviar = useSos((s) => s.posicion)
  useEffect(() => {
    if (activa && posicion) enviar(posicion)
  }, [activa, posicion, enviar])

  const soltar = () => {
    if (cuadro.current) cancelAnimationFrame(cuadro.current)
    cuadro.current = null
    const corto = inicio.current != null && performance.now() - inicio.current < 600
    inicio.current = null
    setProgreso(0)
    if (corto && !activa) {
      setAyuda(true)
      setTimeout(() => setAyuda(false), 2500)
    }
  }
  const pulsar = () => {
    if (activa) return setHoja(true)
    inicio.current = performance.now()
    const paso = () => {
      if (inicio.current == null) return
      const p = Math.min(1, (performance.now() - inicio.current) / MANTENER_MS)
      setProgreso(p)
      if (p >= 1) {
        inicio.current = null
        setProgreso(0)
        navigator.vibrate?.([80, 60, 80])
        setHoja(true)
        void activar(posicion)
        return
      }
      cuadro.current = requestAnimationFrame(paso)
    }
    cuadro.current = requestAnimationFrame(paso)
  }
  const R = 19
  const C = 2 * Math.PI * R
  return (
    <>
      <div className={`relative ${className}`}>
        <button
          type="button"
          aria-label={
            activa ? 'SOS activo: ver opciones' : 'SOS: mantén pulsado 3 segundos para pedir ayuda'
          }
          title="SOS: mantén pulsado 3 segundos"
          data-testid="boton-sos"
          data-activo={activa}
          onPointerDown={(e) => {
            e.currentTarget.setPointerCapture?.(e.pointerId)
            pulsar()
          }}
          onPointerUp={soltar}
          onPointerCancel={soltar}
          onPointerLeave={() => inicio.current != null && soltar()}
          onKeyDown={(e) => {
            if ((e.key === 'Enter' || e.key === ' ') && !e.repeat) {
              e.preventDefault()
              pulsar()
            }
          }}
          onKeyUp={(e) => (e.key === 'Enter' || e.key === ' ') && soltar()}
          onContextMenu={(e) => e.preventDefault()}
          className={`relative w-[56px] h-[56px] rounded-full flex flex-col items-center justify-center text-white shadow-lg select-none touch-none ${activa ? 'bg-[#DC2626] animate-pulse' : 'bg-[#B91C1C]'} border-2 border-white/70`}
        >
          <svg className="absolute inset-0 -rotate-90" viewBox="0 0 44 44" aria-hidden="true">
            <circle
              cx="22"
              cy="22"
              r={R}
              fill="none"
              stroke="white"
              strokeWidth="3"
              strokeDasharray={C}
              strokeDashoffset={C * (1 - progreso)}
              strokeLinecap="round"
              opacity={progreso > 0 ? 1 : 0}
            />
          </svg>
          <span className="text-[15px] font-black leading-none tracking-wide">SOS</span>
          <span className="text-[8px] font-bold uppercase leading-none mt-0.5 opacity-90">
            {activa ? 'Activo' : 'Mantén'}
          </span>
        </button>
        {ayuda && (
          <span
            role="status"
            className="absolute right-[64px] top-1/2 -translate-y-1/2 whitespace-nowrap bg-c2-card border border-c2-linea px-3 py-2 text-[12px] text-white shadow-lg"
          >
            Mantén pulsado 3 s para pedir ayuda
          </span>
        )}
      </div>
      <HojaSOS abierta={hoja} onCerrar={() => setHoja(false)} />
    </>
  )
}

/** Opciones del SOS: avisar a los contactos (SMS, WhatsApp, compartir), llamar al 911 y terminar. */
export function HojaSOS({ abierta, onCerrar }: { abierta: boolean; onCerrar: () => void }) {
  const sesion = useIdentidad((s) => s.sesion)
  const contactos = useIdentidad((s) => s.ajustes.contactosEmergencia ?? [])
  const { alertaId, activando, error, ultimaPos, terminar } = useSos()
  const [terminando, setTerminando] = useState(false)
  const nombre = sesion?.nombre ?? 'Un corredor'
  const mensaje = alertaId ? mensajeSos(nombre, alertaId, ultimaPos) : ''
  const compartir = async () => {
    if (!alertaId) return
    const datos = { title: 'SOS de Runcity', text: mensaje, url: enlaceSos(alertaId) }
    if (navigator.share) await navigator.share(datos).catch(() => undefined)
    else await navigator.clipboard?.writeText(mensaje).catch(() => undefined)
  }
  const boton =
    'w-full h-12 flex items-center justify-center gap-2 font-black italic uppercase text-[14px]'
  return (
    <Dialogo
      titulo={alertaId ? 'SOS activo' : activando ? 'Activando SOS' : 'SOS'}
      abierto={abierta}
      onCerrar={onCerrar}
      icono="sos"
      peligro
    >
      <div className="space-y-3" data-testid="hoja-sos">
        <p className="text-white">
          {alertaId
            ? 'Tu ubicación se comparte con un enlace privado mientras el SOS esté activo. Avisa a tus contactos o llama al 911.'
            : activando
              ? 'Creando el enlace de tu ubicación...'
              : 'No hay un SOS activo.'}
        </p>
        {error && <p className="text-c2-peligro text-sm">{error}</p>}
        <a
          href={`tel:${NUMERO_EMERGENCIA}`}
          className={`${boton} bg-[#DC2626] text-white`}
          data-testid="llamar-911"
        >
          <span className="material-symbols-outlined">call</span>
          Llamar al {NUMERO_EMERGENCIA} (ECU 911)
        </a>
        {alertaId && contactos.length > 0 && (
          <>
            <a
              href={enlaceSms(contactos, mensaje)}
              className={`${boton} bg-gradient-to-r from-[#7B1FA2] to-c2-morado text-white`}
              data-testid="sos-sms"
            >
              <span className="material-symbols-outlined">sms</span>
              Enviar SMS a mis contactos ({contactos.length})
            </a>
            <div className="grid gap-2">
              {contactos.map((c) => (
                <a
                  key={c.telefono}
                  href={enlaceWhatsapp(c, mensaje)}
                  target="_blank"
                  rel="noreferrer"
                  className="flex items-center gap-3 px-3 py-2 bg-c2-s2 border border-c2-linea text-white"
                >
                  <span className="material-symbols-outlined text-c2-menta">chat</span>
                  <span className="flex-1 text-left">WhatsApp a {c.nombre}</span>
                  <span className="text-c2-muted text-xs">{c.telefono}</span>
                </a>
              ))}
            </div>
          </>
        )}
        {alertaId && contactos.length === 0 && (
          <p className="text-sm text-c2-muted" data-testid="sos-sin-contactos">
            No tienes contactos de emergencia. Comparte el enlace con alguien de confianza o
            agrégalos en{' '}
            <Link
              to="/ajustes/privacidad#contactos"
              className="text-c2-violeta underline"
              onClick={onCerrar}
            >
              Ajustes, Privacidad
            </Link>
            .
          </p>
        )}
        {alertaId && (
          <button
            type="button"
            onClick={() => void compartir()}
            className={`${boton} bg-c2-s2 border border-c2-linea text-white`}
          >
            <span className="material-symbols-outlined">share</span>
            Compartir enlace de ubicación
          </button>
        )}
        {alertaId && (
          <button
            type="button"
            disabled={terminando}
            onClick={async () => {
              setTerminando(true)
              await terminar()
              setTerminando(false)
              onCerrar()
            }}
            className="w-full h-11 text-c2-muted underline"
            data-testid="terminar-sos"
          >
            {terminando ? 'Terminando...' : 'Estoy bien, terminar SOS'}
          </button>
        )}
      </div>
    </Dialogo>
  )
}
