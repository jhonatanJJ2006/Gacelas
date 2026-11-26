/*
 * Animaciones C2 reutilizables de la web (equivalentes a CifraAnimada y DestelloC2 de Android):
 * cifras que cuentan, confeti sesgado para celebraciones y entrada escalonada de listas. Todas
 * respetan "reducir movimiento": el contador muestra el valor final y el confeti no se dibuja.
 */
import { useEffect, useMemo, useState, type ReactNode } from 'react'
import { MOVIMIENTO, movimientoReducido, useConteo } from '../lib/movimiento'

interface ContadorProps {
  valor: number
  /** Convierte el valor intermedio en texto (por defecto, entero). */
  formato?: (n: number) => string
  duracion?: number
  className?: string
  testId?: string
}

const NUMERO = /-?\d+(?:[.,]\d+)?/

/** Separa el primer numero de un texto ("+12,4 km") en prefijo, numero, decimales, separador y sufijo. */
export function partirCifra(texto: string) {
  // Tiempos y ritmos ("5:30") no son una cantidad que cuente: se muestran enteros.
  if (texto.includes(':')) return null
  const m = NUMERO.exec(texto)
  if (!m) return null
  const crudo = m[0]
  // "1.234" o "1,234" sin otro contexto es un millar, no un decimal: no se anima.
  if (/^\d{1,3}[.,]\d{3}$/.test(crudo)) return null
  const sep = /[.,]/.exec(crudo)?.[0] ?? ''
  const decimales = sep ? crudo.length - crudo.indexOf(sep) - 1 : 0
  return {
    prefijo: texto.slice(0, m.index),
    numero: parseFloat(crudo.replace(',', '.')),
    decimales,
    sep,
    sufijo: texto.slice(m.index + crudo.length),
  }
}

/**
 * Anima el primer numero de un texto ya formateado ("12,4 km", "+120 m", "75%") conservando su
 * formato (decimales, coma o punto, unidades). Si no hay numero animable, muestra el texto tal cual.
 */
export function CifraTexto({
  texto,
  className,
  testId,
}: {
  texto: string
  className?: string
  testId?: string
}) {
  const partes = partirCifra(texto)
  const actual = useConteo(partes?.numero ?? 0)
  let visible = texto
  if (partes && actual !== partes.numero) {
    const fijo = actual.toFixed(partes.decimales)
    visible = partes.prefijo + (partes.sep ? fijo.replace('.', partes.sep) : fijo) + partes.sufijo
  }
  return (
    <span className={className} data-testid={testId}>
      {visible}
    </span>
  )
}

/** Cifra que cuenta hacia arriba hasta [valor] y anima los cambios posteriores. */
export function Contador({ valor, formato, duracion, className, testId }: ContadorProps) {
  const actual = useConteo(valor, duracion)
  const texto = formato ? formato(actual) : String(Math.round(actual))
  return (
    <span className={className} data-testid={testId}>
      {texto}
    </span>
  )
}

interface Particula {
  dx: number
  dy: number
  giro: number
  color: string
  retraso: number
}

// Colores del tema (--c-*): cambian solos entre modo claro y oscuro.
const COLORES_CONFETI = [
  'rgb(var(--c-c2-violeta))',
  'rgb(var(--c-c2-morado))',
  'rgb(var(--c-c2-menta))',
  'rgb(var(--c-text-primary))',
]

/** Particulas fijas (sin azar) para que el confeti se vea igual en todas las plataformas. */
export function particulasConfeti(n = 28): Particula[] {
  return Array.from({ length: n }, (_, i) => {
    const angulo = (i / n) * 2 * Math.PI + (i % 3) * 0.13
    const alcance = 0.55 + ((i * 37) % 45) / 100
    return {
      dx: Math.round(Math.cos(angulo) * alcance * 150),
      dy: Math.round(Math.sin(angulo) * alcance * 110),
      giro: (i * 53) % 360,
      color: COLORES_CONFETI[i % COLORES_CONFETI.length]!,
      retraso: (i % 4) * 30,
    }
  })
}

/**
 * Confeti sesgado (paralelogramos) que sale del centro del contenedor y se desvanece. El padre
 * debe tener position: relative. No intercepta toques y se retira solo al terminar.
 */
export function Confeti({ activo = true, n = 28 }: { activo?: boolean; n?: number }) {
  const [visible, setVisible] = useState(activo)
  const particulas = useMemo(() => particulasConfeti(n), [n])
  useEffect(() => {
    if (!activo) return
    setVisible(true)
    const id = setTimeout(() => setVisible(false), 1400)
    return () => clearTimeout(id)
  }, [activo])
  if (!activo || !visible || movimientoReducido()) return null
  return (
    <div
      aria-hidden="true"
      data-testid="confeti"
      className="pointer-events-none absolute inset-0 overflow-hidden"
    >
      {particulas.map((p, i) => (
        <span
          key={i}
          className="mov-confeti"
          style={
            {
              '--dx': `${p.dx}px`,
              '--dy': `${p.dy}px`,
              '--giro': `${p.giro}deg`,
              background: p.color,
              animationDelay: `${p.retraso}ms`,
            } as React.CSSProperties
          }
        />
      ))}
    </div>
  )
}

/** Envuelve un bloque con entrada escalonada: <Escalon i={n}>...</Escalon>. */
export function Escalon({
  i,
  children,
  className = '',
}: {
  i: number
  children: ReactNode
  className?: string
}) {
  return (
    <div
      className={`mov-escalon ${className}`}
      style={{ '--i': Math.min(i, MOVIMIENTO.escalonesMax) } as React.CSSProperties}
    >
      {children}
    </div>
  )
}
