import { useEffect, useState } from 'react'
import { consultarSeguridadDeRuta } from '../application/incidentes'
import { AVISO_SEGURIDAD, colorNivel, DIAS_CORTOS, type SeguridadZona } from '../domain/incidentes'

/** Tarjeta "Seguridad de la zona" del detalle de ruta. Nivel por dia y tipos frecuentes. */
export function TarjetaSeguridad({ rutaId, inicial }: { rutaId: string; inicial?: SeguridadZona }) {
  const [datos, setDatos] = useState<SeguridadZona | null | undefined>(inicial)
  useEffect(() => {
    if (inicial) return
    let vivo = true
    void consultarSeguridadDeRuta(rutaId).then((d) => vivo && setDatos(d))
    return () => {
      vivo = false
    }
  }, [rutaId, inicial])
  if (datos === undefined) return null
  const hoy = new Date().getDay()
  return (
    <section
      aria-labelledby="titulo-seguridad"
      data-testid="tarjeta-seguridad"
      className="rounded-[14px] bg-c2-card border border-c2-linea p-4 space-y-3"
    >
      <div className="flex items-center justify-between">
        <h2 id="titulo-seguridad" className="font-title-md text-title-md text-text-primary">
          Seguridad de la zona
        </h2>
        {datos?.actualizadoHasta && (
          <span className="text-[11px] text-text-secondary">
            Datos hasta {datos.actualizadoHasta}
          </span>
        )}
      </div>
      {!datos || datos.dias.length === 0 ? (
        <p className="text-sm text-text-secondary">Aún no hay datos para esta ruta.</p>
      ) : (
        <>
          <ul className="grid grid-cols-7 gap-1.5" aria-label="Nivel por día de la semana">
            {[1, 2, 3, 4, 5, 6, 0].map((d) => {
              const v = datos.dias.find((x) => x.dia === d)
              return (
                <li
                  key={d}
                  className={`rounded-lg py-2 text-center bg-c2-bg ${d === hoy ? 'ring-1 ring-c2-violeta' : ''}`}
                >
                  <div className="text-[11px] text-text-secondary">{DIAS_CORTOS[d]}</div>
                  <div
                    className="mx-auto mt-1 h-1.5 w-6 rounded-full"
                    style={{ background: v ? colorNivel(v.nivel) : '#2C3446' }}
                  />
                  <div className="mt-1 text-[11px] font-semibold text-text-primary capitalize">
                    {v?.nivel ?? '-'}
                  </div>
                </li>
              )
            })}
          </ul>
          {datos.tipos.length > 0 && (
            <div>
              <p className="text-xs uppercase tracking-wider text-text-secondary mb-1">
                Incidentes más frecuentes
              </p>
              <ul className="space-y-1">
                {datos.tipos.slice(0, 3).map((t) => (
                  <li key={t.servicio + t.subtipo} className="flex justify-between text-sm">
                    <span className="text-text-primary truncate">{t.subtipo || t.servicio}</span>
                    <span className="text-text-secondary text-xs tabular-nums">{t.conteo}</span>
                  </li>
                ))}
              </ul>
            </div>
          )}
        </>
      )}
      <p className="text-[11px] leading-snug text-text-secondary">{AVISO_SEGURIDAD}</p>
      <p className="text-[11px] text-text-secondary">
        Fuente: {datos?.fuente ?? 'SIS ECU 911 (CC-BY)'}
      </p>
    </section>
  )
}
