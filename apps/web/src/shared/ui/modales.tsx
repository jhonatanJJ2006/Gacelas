import type { ReactNode } from 'react'
import { BotonTexto, Dialogo } from './ui'

/** Modal C2 especifico: icono, titulo, texto, tarjeta opcional, boton principal y secundario. */
export function ModalC2({
  abierto,
  icono,
  titulo,
  texto,
  primario,
  onPrimario,
  onCerrar,
  secundario,
  onSecundario,
  peligro = false,
  children,
}: {
  abierto: boolean
  icono: string
  titulo: string
  texto: ReactNode
  primario: string
  onPrimario: () => void
  onCerrar: () => void
  secundario?: string
  onSecundario?: () => void
  peligro?: boolean
  children?: ReactNode
}) {
  return (
    <Dialogo
      titulo={titulo}
      abierto={abierto}
      onCerrar={onCerrar}
      icono={icono}
      peligro={peligro}
      acciones={
        <>
          {secundario && <BotonTexto onClick={onSecundario ?? onCerrar}>{secundario}</BotonTexto>}
          <button
            type="button"
            onClick={onPrimario}
            className={`px-5 h-11 -skew-x-12 font-black italic uppercase text-[14px] text-white ${peligro ? 'bg-c2-peligro' : 'bg-gradient-to-r from-[#7B1FA2] to-c2-morado'} hover:brightness-110`}
          >
            {primario}
          </button>
        </>
      }
    >
      <p>{texto}</p>
      {children}
    </Dialogo>
  )
}

/** Tarjeta de datos dentro de un modal. */
export function ResumenModal({ datos }: { datos: [string, string][] }) {
  return (
    <div className="grid grid-cols-3 gap-2 bg-c2-s2 border border-c2-linea p-3">
      {datos.map(([valor, etiqueta], i) => (
        <div key={etiqueta}>
          <span className="block text-[10px] uppercase tracking-wider font-bold text-c2-muted">
            {etiqueta}
          </span>
          <span
            className={`font-display italic text-[20px] ${i === 0 ? 'text-c2-violeta' : 'text-white'}`}
          >
            {valor}
          </span>
        </div>
      ))}
    </div>
  )
}
