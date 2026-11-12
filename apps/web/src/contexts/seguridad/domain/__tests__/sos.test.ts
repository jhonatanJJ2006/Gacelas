import { describe, expect, it } from 'vitest'
import {
  enlaceSms,
  enlaceWhatsapp,
  mensajeSos,
  normalizarTelefono,
  telefonoValido,
  validarContacto,
} from '../sos'

describe('SOS', () => {
  it('normaliza moviles de Ecuador a +593', () => {
    expect(normalizarTelefono('099 123 4567')).toBe('+593991234567')
    expect(normalizarTelefono('593991234567')).toBe('+593991234567')
    expect(normalizarTelefono('+1 (415) 555-0100')).toBe('+14155550100')
  })
  it('valida contactos', () => {
    expect(telefonoValido('123')).toBe(false)
    expect(validarContacto({ nombre: 'Mamá', telefono: '0991234567' })).toBeNull()
    expect(validarContacto({ nombre: '', telefono: '0991234567' })).toMatch(/nombre/)
  })
  it('arma el mensaje con el enlace privado y la posicion', () => {
    const m = mensajeSos('Ana', 'abc', { lat: -4, lng: -79.2 })
    expect(m).toContain('https://gacelas.vercel.app/sos/abc')
    expect(m).toContain('maps.google.com/?q=-4.000000,-79.200000')
    expect(m).toContain('911')
  })
  it('enlaces de SMS y WhatsApp con el mensaje codificado', () => {
    const c = [
      { nombre: 'A', telefono: '0991234567' },
      { nombre: 'B', telefono: '0987654321' },
    ]
    expect(enlaceSms(c, 'hola mundo')).toBe('sms:+593991234567,+593987654321?body=hola%20mundo')
    expect(enlaceWhatsapp(c[0], 'hola')).toBe('https://wa.me/593991234567?text=hola')
  })
})
