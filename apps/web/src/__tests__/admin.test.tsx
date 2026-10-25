import { describe, expect, it, beforeEach } from 'vitest'
import { fireEvent, screen } from '@testing-library/react'
import { prepararApp, renderRuta } from '../test/utils'
import { fuenteDemo } from '../features/admin/api'

describe('panel de administración', () => {
  beforeEach(() => prepararApp())

  it('sin servidor el panel queda restringido', async () => {
    renderRuta('/admin')
    expect(await screen.findByTestId('admin-sin-acceso')).toBeInTheDocument()
  })

  it('la fuente de ejemplo resuelve reportes y publica eventos', async () => {
    const f = fuenteDemo()
    const [r] = await f.reportes()
    await f.moderar(r, 'ocultar_contenido', 'spam')
    expect((await f.metricas()).reportes_pendientes).toBe(4)
    expect(r.status).toBe('resuelto')
    expect(r.acciones.at(-1)).toMatchObject({ accion: 'ocultar_contenido', nota: 'spam' })
    await f.guardarEvento({ title: 'Nuevo', published: true })
    expect((await f.eventos())[0]).toMatchObject({ title: 'Nuevo', published: true })
    await f.publicarTerminos('2027-01-01')
    expect(await f.versionTerminos()).toBe('2027-01-01')
  })

  it('la cola ampliada incluye usuarios, comentarios y eventos con sus estados', async () => {
    const f = fuenteDemo()
    const cola = await f.reportes()
    expect(new Set(cola.map((r) => r.target_type))).toEqual(
      new Set(['post', 'usuario', 'comentario', 'evento', 'incidente'])
    )
    const r = cola.find((x) => x.target_type === 'usuario')!
    await f.moderar(r, 'en_revision', '')
    expect(r.status).toBe('en_revision')
    await f.moderar(r, 'descartado', 'no es suplantación')
    await f.moderar(r, 'reabierto', '')
    expect(r.status).toBe('pendiente')
    expect(r.acciones.map((a) => a.accion)).toEqual(['en_revision', 'descartado', 'reabierto'])
  })

  it('muestra la cola de moderación por tipo en modo demo', async () => {
    renderRuta('/admin/moderacion?demo')
    expect(await screen.findByText('Richard Carapaz')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Evento' }))
    expect(screen.queryByText('Richard Carapaz')).not.toBeInTheDocument()
    expect(screen.getByText('Reto nocturno vía a Cuenca')).toBeInTheDocument()
  })

  it('muestra estadísticas con gráficos y exportación en modo demo', async () => {
    renderRuta('/admin/estadisticas?demo')
    expect(await screen.findByTestId('estadisticas')).toBeInTheDocument()
    expect(screen.getByTestId('grafico-carreras')).toBeInTheDocument()
    expect(screen.getByTestId('rutas-top')).toHaveTextContent('Parque Recreacional Jipiro')
    expect(screen.getByTestId('estadisticas-pdf')).toBeEnabled()
  })
})
