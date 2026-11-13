import { useEffect, useRef, useState } from 'react'
import { listarIncidentesRecientes } from '../application/incidentes'
import { alertasNuevas, mensajeAlerta, REFRESCO_ALERTAS_MS } from '../domain/alertasZona'
import type { IncidentePublico } from '../domain/incidentes'

/**
 * Alerta de zona en la carrera: lee los reportes recientes al empezar y cada 5 min; con cada posicion
 * avisa una sola vez por reporte a menos de 200 m (vibracion y banner role="alert" durante 8 s).
 */
export function AlertaZona({
  posicion,
  activa,
}: {
  posicion: { lat: number; lng: number } | null
  activa: boolean
}) {
  const lat = posicion?.lat
  const lng = posicion?.lng
  const [reportes, setReportes] = useState<IncidentePublico[]>([])
  const [mensaje, setMensaje] = useState<string | null>(null)
  const avisados = useRef(new Set<string>())
  useEffect(() => {
    if (!activa) return
    let vivo = true
    const leer = () => void listarIncidentesRecientes().then((l) => vivo && setReportes(l))
    leer()
    const t = setInterval(leer, REFRESCO_ALERTAS_MS)
    return () => {
      vivo = false
      clearInterval(t)
    }
  }, [activa])
  useEffect(() => {
    if (!activa || lat == null || lng == null) return
    const nuevas = alertasNuevas({ lat, lng }, reportes, avisados.current)
    if (!nuevas.length) return
    nuevas.forEach((n) => avisados.current.add(n.incidente.id))
    setMensaje(mensajeAlerta(nuevas[0].incidente, nuevas[0].metros))
    navigator.vibrate?.([350, 180, 350])
  }, [activa, lat, lng, reportes])
  useEffect(() => {
    if (!mensaje) return
    const t = setTimeout(() => setMensaje(null), 8000)
    return () => clearTimeout(t)
  }, [mensaje])
  if (!mensaje) return null
  return (
    <div
      role="alert"
      data-testid="alerta-zona"
      className="fixed left-3 right-3 top-[calc(env(safe-area-inset-top)+64px)] z-[60] flex items-start gap-3 border border-danger bg-[#2a0f12]/95 px-4 py-3 text-sm text-text-primary shadow-lg [clip-path:polygon(0_0,100%_0,100%_calc(100%-14px),calc(100%-14px)_100%,0_100%)]"
    >
      <span className="flex-1">{mensaje}</span>
      <button
        type="button"
        onClick={() => setMensaje(null)}
        className="font-semibold text-c2-violeta"
        aria-label="Cerrar alerta de zona"
      >
        Entendido
      </button>
    </div>
  )
}
