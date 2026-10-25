import { describe, expect, it } from 'vitest'
import { claveObjetivo, definicionTipo, registrarTipoObjetivo, tiposRegistrados } from '../denuncia'

describe('cola de moderación genérica', () => {
  it('conoce los cuatro tipos de la migración 023', () => {
    expect(tiposRegistrados()).toEqual(['post', 'comentario', 'evento', 'usuario'])
    expect(definicionTipo('usuario').accionOcultar).toBe('Suspender usuario')
  })

  it('un tipo desconocido no rompe la cola', () => {
    const d = definicionTipo('ruta_insegura')
    expect(d.etiqueta).toBe('Ruta insegura')
    expect(d.accionOcultar).toBe('Ocultar contenido')
    expect(d.motivos.length).toBeGreaterThan(0)
  })

  it('otro contexto registra su tipo', () => {
    registrarTipoObjetivo('incidente', {
      etiqueta: 'Incidente',
      titulo: 'Reportar incidente',
      motivos: [['falso', 'Incidente falso']],
      accionOcultar: 'Ocultar incidente',
      tieneAutor: true,
    })
    expect(definicionTipo('incidente').etiqueta).toBe('Incidente')
    expect(tiposRegistrados()).toContain('incidente')
  })

  it('las publicaciones conservan la clave local antigua', () => {
    expect(claveObjetivo({ tipo: 'post', id: 'p1' })).toBe('p1')
    expect(claveObjetivo({ tipo: 'evento', id: 'e1' })).toBe('evento:e1')
  })
})
