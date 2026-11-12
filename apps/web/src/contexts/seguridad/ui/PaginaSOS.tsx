import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { MapaGoogle } from '../../../shared/ui/MapaGoogle'
import { NUMERO_EMERGENCIA } from '../domain/sos'
import { verSos, type AlertaPublica } from '../infrastructure/sosRemoto'

const hora = (iso: string | null) =>
  iso ? new Date(iso).toLocaleTimeString('es-EC', { hour: '2-digit', minute: '2-digit' }) : '--'

/** Pagina publica del enlace SOS: la ven los contactos sin cuenta. Se actualiza cada 10 s. */
export function PaginaSOS() {
  const { id = '' } = useParams()
  const [a, setA] = useState<AlertaPublica | null | undefined>(undefined)
  useEffect(() => {
    let vivo = true
    const leer = () => void verSos(id).then((x) => vivo && setA(x))
    leer()
    const t = setInterval(leer, 10_000)
    return () => {
      vivo = false
      clearInterval(t)
    }
  }, [id])
  return (
    <main className="min-h-[100dvh] bg-gradient-to-b from-[#2A0A3D] via-[#1A0B2E] to-[#120821] text-white px-5 pt-[max(env(safe-area-inset-top),20px)] pb-10">
      <div className="max-w-lg mx-auto space-y-4">
        <p className="font-display italic text-[14px] tracking-[0.2em] text-c2-violeta">RUNCITY</p>
        {a === undefined && (
          <div className="space-y-3" aria-busy="true" data-testid="sos-cargando">
            <div className="h-9 w-2/3 bg-white/10 animate-pulse" />
            <div className="h-[340px] bg-white/10 animate-pulse" />
          </div>
        )}
        {a === null && (
          <p data-testid="sos-no-existe">Este enlace de SOS no existe o ya no es válido.</p>
        )}
        {a && (
          <>
            <h1 className="font-display italic text-[30px] leading-tight">
              {a.activa ? `${a.nombre} pidió ayuda` : `El SOS de ${a.nombre} terminó`}
            </h1>
            <p
              role="status"
              data-testid="sos-estado"
              className={`inline-flex items-center gap-2 px-3 py-1.5 text-sm font-bold ${a.activa ? 'bg-[#DC2626]' : 'bg-white/10'}`}
            >
              {a.activa
                ? `SOS activo desde las ${hora(a.inicio)}. Última posición a las ${hora(a.ultima)}.`
                : 'Ya no comparte su ubicación.'}
            </p>
            {a.activa && a.pos && (
              <div className="h-[340px] overflow-hidden border border-c2-linea">
                <MapaGoogle
                  puntos={a.puntos.length ? a.puntos : [a.pos]}
                  seguir
                  className="w-full h-full"
                  alternativa={
                    <a
                      href={`https://maps.google.com/?q=${a.pos.lat},${a.pos.lng}`}
                      className="h-full flex items-center justify-center text-c2-violeta underline"
                    >
                      Ver la posición en Google Maps
                    </a>
                  }
                />
              </div>
            )}
            {a.activa && !a.pos && (
              <p>Todavía no hay una posición GPS. La página se actualiza sola.</p>
            )}
            <div className="grid gap-2">
              <a
                href={`tel:${NUMERO_EMERGENCIA}`}
                className="h-12 flex items-center justify-center font-black italic uppercase bg-[#DC2626]"
              >
                Llamar al {NUMERO_EMERGENCIA} (ECU 911)
              </a>
              {a.activa && a.pos && (
                <a
                  href={`https://www.google.com/maps/dir/?api=1&destination=${a.pos.lat},${a.pos.lng}`}
                  target="_blank"
                  rel="noreferrer"
                  className="h-12 flex items-center justify-center font-black italic uppercase bg-gradient-to-r from-[#7B1FA2] to-c2-morado"
                >
                  Cómo llegar en Google Maps
                </a>
              )}
            </div>
          </>
        )}
      </div>
    </main>
  )
}
