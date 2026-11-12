import type { ContactoEmergencia } from '../../../shared/kernel/tipos'
import { normalizarTelefono } from '../../../shared/lib/contactos'

export {
  MAX_CONTACTOS,
  normalizarTelefono,
  telefonoValido,
  validarContacto,
} from '../../../shared/lib/contactos'

/* Reglas del boton SOS (ticket 86e3uf097). Sin dependencias de UI ni de Supabase. */

/** Numero unico de emergencias en Ecuador (ECU 911). */
export const NUMERO_EMERGENCIA = '911'
/** Hay que mantener pulsado el boton este tiempo para activar el SOS (evita toques accidentales). */
export const MANTENER_MS = 3000
/** Cada cuanto se manda la posicion mientras el SOS esta activo. */
export const ENVIO_CADA_MS = 10_000
export const SITIO_SOS = 'https://gacelas.vercel.app'

export const enlaceSos = (id: string, sitio = SITIO_SOS) => `${sitio}/sos/${id}`

export function mensajeSos(
  nombre: string,
  id: string,
  pos: { lat: number; lng: number } | null,
  sitio = SITIO_SOS
): string {
  const ubicacion = pos
    ? ` Última posición: https://maps.google.com/?q=${pos.lat.toFixed(6)},${pos.lng.toFixed(6)}`
    : ''
  return `SOS de Runcity: ${nombre} necesita ayuda. Sigue su ubicación en vivo: ${enlaceSos(id, sitio)}.${ubicacion} Si no responde, llama al ${NUMERO_EMERGENCIA}.`
}

/** Enlace sms: para varios destinatarios con el mensaje ya escrito. */
export function enlaceSms(contactos: ContactoEmergencia[], mensaje: string): string {
  const numeros = contactos.map((c) => normalizarTelefono(c.telefono)).join(',')
  return `sms:${numeros}?body=${encodeURIComponent(mensaje)}`
}

/** Enlace de WhatsApp a un contacto con el mensaje ya escrito. */
export function enlaceWhatsapp(c: ContactoEmergencia, mensaje: string): string {
  return `https://wa.me/${normalizarTelefono(c.telefono).replace('+', '')}?text=${encodeURIComponent(mensaje)}`
}
