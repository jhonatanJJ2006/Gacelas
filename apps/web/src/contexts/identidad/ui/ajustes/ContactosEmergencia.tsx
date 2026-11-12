import { useState } from 'react'
import type { ContactoEmergencia } from '../../../../shared/kernel/tipos'
import {
  MAX_CONTACTOS,
  normalizarTelefono,
  validarContacto,
} from '../../../../shared/lib/contactos'
import { useIdentidad } from '../../application/estado'
import { GrupoC2 } from './Comunes'

/** Hasta 3 contactos que reciben el enlace del SOS. Se guardan en user_settings.prefs. */
export function ContactosEmergencia() {
  const contactos = useIdentidad((s) => s.ajustes.contactosEmergencia ?? [])
  const actualizar = useIdentidad((s) => s.actualizarAjustes)
  const [nombre, setNombre] = useState('')
  const [telefono, setTelefono] = useState('')
  const [error, setError] = useState<string | null>(null)
  const agregar = () => {
    const c: ContactoEmergencia = { nombre: nombre.trim(), telefono: normalizarTelefono(telefono) }
    const e = validarContacto(c)
    if (e) return setError(e)
    if (contactos.some((x) => normalizarTelefono(x.telefono) === c.telefono))
      return setError('Ese teléfono ya está en tus contactos')
    setError(null)
    setNombre('')
    setTelefono('')
    void actualizar({ contactosEmergencia: [...contactos, c] })
  }
  const quitar = (tel: string) =>
    void actualizar({ contactosEmergencia: contactos.filter((x) => x.telefono !== tel) })
  const campo =
    'w-full h-11 px-3 bg-c2-s2 border border-c2-linea text-white placeholder:text-c2-muted focus:border-c2-violeta outline-none'
  return (
    <div id="contactos">
      <GrupoC2 titulo="Contactos de emergencia (SOS)">
        <div className="px-4 py-3 space-y-3" data-testid="contactos-emergencia">
          <p className="text-[13px] text-c2-muted">
            Si mantienes pulsado el botón SOS durante una carrera, podrás enviarles por SMS o
            WhatsApp un enlace con tu ubicación en vivo. Máximo {MAX_CONTACTOS}.
          </p>
          {contactos.length === 0 && (
            <p className="text-[14px] text-white">Aún no tienes contactos de emergencia.</p>
          )}
          {contactos.map((c) => (
            <div
              key={c.telefono}
              className="flex items-center gap-3 bg-c2-s2 border border-c2-linea px-3 py-2"
            >
              <span className="material-symbols-outlined text-c2-violeta">contact_emergency</span>
              <span className="flex-1 min-w-0">
                <span className="block font-bold text-white truncate">{c.nombre}</span>
                <span className="block text-[13px] text-c2-muted">{c.telefono}</span>
              </span>
              <button
                type="button"
                onClick={() => quitar(c.telefono)}
                aria-label={`Quitar a ${c.nombre}`}
                className="w-10 h-10 flex items-center justify-center text-c2-muted hover:text-c2-peligro"
              >
                <span className="material-symbols-outlined">delete</span>
              </button>
            </div>
          ))}
          {contactos.length < MAX_CONTACTOS && (
            <form
              className="grid gap-2"
              onSubmit={(e) => {
                e.preventDefault()
                agregar()
              }}
            >
              <input
                aria-label="Nombre del contacto"
                placeholder="Nombre (por ejemplo, Mamá)"
                value={nombre}
                onChange={(e) => setNombre(e.target.value)}
                className={campo}
              />
              <input
                aria-label="Teléfono del contacto"
                placeholder="Teléfono (0991234567)"
                inputMode="tel"
                type="tel"
                value={telefono}
                onChange={(e) => setTelefono(e.target.value)}
                className={campo}
              />
              {error && <p className="text-c2-peligro text-[13px]">{error}</p>}
              <button
                type="submit"
                className="h-11 -skew-x-12 font-black italic uppercase text-[14px] text-white bg-gradient-to-r from-[#7B1FA2] to-c2-morado"
              >
                Agregar contacto
              </button>
            </form>
          )}
        </div>
      </GrupoC2>
    </div>
  )
}
