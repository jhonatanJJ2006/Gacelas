import { useState } from 'react'
import { Link } from 'react-router-dom'
import { calendarioMes } from '../../lib/eventos'
import type { Evento } from '../../mocks/data'

const DIAS = ['L', 'M', 'X', 'J', 'V', 'S', 'D']

export function CalendarioEventos({
  eventos,
  inscritos,
}: {
  eventos: Evento[]
  inscritos: string[]
}) {
  const hoy = new Date()
  const [mes, setMes] = useState({ anio: hoy.getFullYear(), mes: hoy.getMonth() })
  const [dia, setDia] = useState<string | null>(null)
  const semanas = calendarioMes(mes.anio, mes.mes, eventos)
  const mover = (d: number) => {
    const f = new Date(mes.anio, mes.mes + d, 1)
    setMes({ anio: f.getFullYear(), mes: f.getMonth() })
    setDia(null)
  }
  const nombreMes = new Date(mes.anio, mes.mes, 1).toLocaleDateString('es-EC', { month: 'long' })
  const titulo = `${nombreMes.charAt(0).toUpperCase()}${nombreMes.slice(1)} ${mes.anio}`
  const delDia = semanas.flat().find((d) => d.fecha.toDateString() === dia)
  return (
    <section
      aria-label="Calendario de eventos"
      className="bg-surface-container rounded-2xl p-4"
      data-testid="calendario"
    >
      <div className="flex items-center justify-between mb-3">
        <button
          onClick={() => mover(-1)}
          aria-label="Mes anterior"
          className="w-10 h-10 rounded-full hover:bg-surface-container-high text-text-primary flex items-center justify-center"
        >
          <span className="material-symbols-outlined">chevron_left</span>
        </button>
        <h2 className="font-title-md text-title-md text-text-primary">{titulo}</h2>
        <button
          onClick={() => mover(1)}
          aria-label="Mes siguiente"
          className="w-10 h-10 rounded-full hover:bg-surface-container-high text-text-primary flex items-center justify-center"
        >
          <span className="material-symbols-outlined">chevron_right</span>
        </button>
      </div>
      <div
        className="grid grid-cols-7 gap-1 text-center text-xs text-text-secondary mb-1"
        aria-hidden="true"
      >
        {DIAS.map((d) => (
          <span key={d}>{d}</span>
        ))}
      </div>
      <div className="grid grid-cols-7 gap-1">
        {semanas.flat().map((d) => {
          const esHoy = d.fecha.toDateString() === hoy.toDateString()
          const mio = d.eventos.some((e) => inscritos.includes(e.id))
          return (
            <button
              key={d.fecha.toISOString()}
              onClick={() => setDia(d.fecha.toDateString())}
              disabled={!d.eventos.length}
              aria-label={`${d.fecha.toLocaleDateString('es-EC', { day: 'numeric', month: 'long' })}: ${d.eventos.length} eventos`}
              className={`aspect-square min-h-[40px] rounded-lg flex flex-col items-center justify-center text-sm ${d.delMes ? 'text-text-primary' : 'text-text-secondary/50'} ${esHoy ? 'ring-2 ring-primary-600' : ''} ${d.eventos.length ? 'bg-surface-container-high font-semibold' : ''} ${dia === d.fecha.toDateString() ? 'bg-primary-600/30' : ''}`}
            >
              {d.fecha.getDate()}
              {d.eventos.length > 0 && (
                <span
                  className={`w-1.5 h-1.5 rounded-full mt-0.5 ${mio ? 'bg-secondary-500' : 'bg-primary-500'}`}
                />
              )}
            </button>
          )
        })}
      </div>
      {delDia && delDia.eventos.length > 0 && (
        <ul className="mt-3 space-y-2">
          {delDia.eventos.map((e) => (
            <li key={e.id}>
              <Link to={`/comunidad/eventos/${e.id}`} className="block rounded-xl bg-surface p-3">
                <span className="font-semibold text-text-primary">{e.titulo}</span>
                <span className="block text-sm text-text-secondary">
                  {e.hora} · {e.ubicacion}
                </span>
              </Link>
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}
