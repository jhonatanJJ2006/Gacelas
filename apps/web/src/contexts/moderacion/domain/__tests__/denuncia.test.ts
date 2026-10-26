import { describe, expect, it } from 'vitest'
import { claveObjetivo, definicionTipo, registrarTipoObjetivo, tiposRegistrados } from '../denuncia'

describe('cola de moderación genérica', () => {
  it('conoce los tipos de 023 y los incidentes de 031', () => {
    expect(tiposRegistrados()).toEqual([
      'post',
      'comentario',
      'evento',
      'usuario',
      'incidente',
      'comunidad',
      'grupo',
    ])
    expect(definicionTipo('incidente').accionOcultar).toBe('Rechazar incidente')
    expect(definicionTipo('usuario').accionOcultar).toBe('Suspender usuario')
  })

  it('un tipo desconocido no rompe la cola', () => {
    const d = definicionTipo('ruta_insegura')
    expect(d.etiqueta).toBe('Ruta insegura')
    expect(d.accionOcultar).toBe('Ocultar contenido')
    expect(d.motivos.length).toBeGreaterThan(0)
  })

  it('otro contexto registra su tipo', () => {
    registrarTipoObjetivo('ruta_cerrada', {
      etiqueta: 'Ruta cerrada',
      titulo: 'Reportar cierre',
      motivos: [['falso', 'Cierre falso']],
      accionOcultar: 'Ocultar cierre',
      tieneAutor: true,
    })
    expect(definicionTipo('ruta_cerrada').etiqueta).toBe('Ruta cerrada')
    expect(tiposRegistrados()).toContain('ruta_cerrada')
  })

  it('las publicaciones conservan la clave local antigua', () => {
    expect(claveObjetivo({ tipo: 'post', id: 'p1' })).toBe('p1')
    expect(claveObjetivo({ tipo: 'evento', id: 'e1' })).toBe('evento:e1')
  })
})
