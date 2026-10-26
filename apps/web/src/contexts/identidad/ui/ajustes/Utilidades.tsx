import { useState } from 'react'
import { ModalC2 } from '../../../../components/modales'
import { useRumboSensor } from '../../../../lib/rumbo'
import { PantallaAjustes } from './Comunes'

const PUNTOS = ['N', 'NE', 'E', 'SE', 'S', 'SO', 'O', 'NO']
export const puntoCardinal = (g: number) =>
  PUNTOS[Math.floor((((g % 360) + 360) % 360) / 45 + 0.5) % 8]

/** Brujula C2: igual que BrujulaScreen de Android. */
export function Brujula() {
  const rumbo = useRumboSensor(true)
  const [calibrando, setCalibrando] = useState(false)
  const r = rumbo == null ? null : Math.round(rumbo)
  return (
    <PantallaAjustes titulo="Brújula" volverA="/ajustes">
      <div
        className="mx-auto w-full max-w-[320px] aspect-square relative"
        data-testid="rosa-brujula"
      >
        <svg
          viewBox="0 0 200 200"
          className="w-full h-full transition-transform duration-300"
          style={{ transform: `rotate(${-(rumbo ?? 0)}deg)` }}
          aria-hidden="true"
        >
          <circle cx="100" cy="100" r="98" fill="#191F2F" stroke="#2C3446" strokeWidth="2" />
          {Array.from({ length: 72 }, (_, k) => {
            const largo = k % 18 === 0 ? 12 : k % 2 === 0 ? 7 : 4
            return (
              <line
                key={k}
                x1="100"
                y1="6"
                x2="100"
                y2={6 + largo}
                stroke={k % 18 === 0 ? '#C084FC' : 'rgba(168,176,194,0.6)'}
                strokeWidth="1.5"
                transform={`rotate(${k * 5} 100 100)`}
              />
            )
          })}
          <text x="100" y="34" textAnchor="middle" fill="#C084FC" fontWeight="900" fontSize="16">
            N
          </text>
        </svg>
        <svg viewBox="0 0 200 200" className="absolute inset-0 w-full h-full" aria-hidden="true">
          <polygon points="100,50 110,100 90,100" fill="#C084FC" />
          <polygon points="100,150 110,100 90,100" fill="#232A3A" />
          <circle cx="100" cy="100" r="4" fill="#fff" />
        </svg>
      </div>
      <div className="text-center">
        <p
          data-testid="rumbo"
          className="font-display italic text-[40px] leading-none text-c2-violeta"
        >
          {r == null ? 'Sin sensor' : `${r}° ${puntoCardinal(r)}`}
        </p>
        <p className="text-sm text-c2-muted mt-1">
          {r == null ? 'Este dispositivo no tiene brújula' : 'Rumbo respecto al norte verdadero'}
        </p>
      </div>
      <div className="tarjeta-c2 grid grid-cols-3 gap-3 p-4">
        {[
          [r == null ? '--' : `${r}°`, 'Rumbo', 'text-c2-violeta'],
          [r == null ? '--' : puntoCardinal(r), 'Dirección', 'text-c2-menta'],
          [r == null ? '--' : 'Buena', 'Calibración', 'text-white'],
        ].map(([v, e, c]) => (
          <div key={e}>
            <span className="block text-[10px] uppercase tracking-wider font-bold text-c2-muted">
              {e}
            </span>
            <span className={`font-display italic text-[20px] ${c}`}>{v}</span>
          </div>
        ))}
      </div>
      <button type="button" onClick={() => setCalibrando(true)} className="w-full boton-c2">
        Calibrar brújula
      </button>
      <ModalC2
        abierto={calibrando}
        icono="explore"
        titulo="Calibrar brújula"
        texto="Mueve el teléfono dibujando un ocho en el aire tres veces, lejos de objetos metálicos."
        primario="Listo"
        onPrimario={() => setCalibrando(false)}
        onCerrar={() => setCalibrando(false)}
      />
    </PantallaAjustes>
  )
}

/** Mapas offline: en la web el mapa depende de la red; la descarga vive en la app Android. */
export function MapasOffline() {
  return (
    <PantallaAjustes titulo="Mapas offline" volverA="/ajustes">
      <section className="tarjeta-c2 p-4 space-y-2">
        <span className="etiqueta-c2">Almacenamiento</span>
        <p className="text-sm text-c2-muted">
          Los mapas descargados se guardan en el teléfono y sirven para correr sin señal.
        </p>
        <div className="h-1.5 w-full bg-c2-s2 rounded-full" />
      </section>
      <span className="etiqueta-c2">Descargados</span>
      <section className="tarjeta-c2 p-4 flex items-center gap-3">
        <span className="material-symbols-outlined text-c2-violeta">map</span>
        <div className="flex-1">
          <p className="font-semibold text-white">Loja y alrededores</p>
          <p className="text-sm text-c2-muted">
            Descárgalo desde la app Android en Ajustes, Mapas offline. En la web el mapa necesita
            conexión.
          </p>
        </div>
      </section>
      <p className="text-sm text-c2-muted">
        Consejo: descarga la zona antes de salir a rutas de montaña como Podocarpus o Villonaco.
      </p>
    </PantallaAjustes>
  )
}
