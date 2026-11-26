import { describe, expect, it, beforeEach } from 'vitest'
import { fireEvent, screen, waitFor } from '@testing-library/react'
import { prepararApp, renderRuta } from '../test/utils'
import { fuenteDemo } from '../contexts/admin/ui/fuente'

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
    // La cola y el detalle del caso elegido muestran el mismo titulo.
    expect(screen.getAllByText('Reto nocturno vía a Cuenca').length).toBeGreaterThan(0)
  })

  it('el resumen muestra cifras reales de la fuente y la accion rapida lleva al siguiente caso', async () => {
    renderRuta('/admin?demo')
    const kpis = await screen.findByTestId('kpis')
    await waitFor(() => expect(kpis).toHaveTextContent('2.418'))
    expect(kpis).toHaveTextContent('52')
    expect(await screen.findAllByTestId('marca-corredor')).toHaveLength(17)
    expect(screen.getAllByTestId('marca-incidente')).toHaveLength(9)
    // El SOS no tiene lectura para el panel: se dice, no se inventa una cifra.
    expect(screen.getByTestId('sos-pendiente')).toHaveTextContent('fuente pendiente')
    const atender = await screen.findByTestId('atender-siguiente')
    await waitFor(() => expect(atender).toBeEnabled())
    fireEvent.click(atender)
    expect(await screen.findByTestId('detalle-caso')).toBeInTheDocument()
  })

  it('asignarme el siguiente caso toma el pendiente mas antiguo (en revision)', async () => {
    renderRuta('/admin/moderacion?demo')
    const boton = await screen.findByTestId('asignarme-siguiente')
    await waitFor(() => expect(boton).toBeEnabled())
    fireEvent.click(boton)
    await waitFor(() => expect(screen.getByRole('tab', { name: /En revisión 2/ })).toBeInTheDocument())
  })

  it('usuarios filtra por suspendidos y muestra la ficha', async () => {
    renderRuta('/admin/usuarios?demo')
    expect((await screen.findAllByText('Andrea Montaño')).length).toBeGreaterThan(0)
    expect(screen.getByTestId('ficha-usuario')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Suspendidos' }))
    expect(screen.queryByText('Andrea Montaño')).not.toBeInTheDocument()
  })

  it('rutas y eventos lista rutas con su trazado y los eventos proximos', async () => {
    renderRuta('/admin/rutas?demo')
    expect(await screen.findByText('Parque Jipiro')).toBeInTheDocument()
    expect(await screen.findByText('Trail Podocarpus')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Crear evento' })).toBeInTheDocument()
  })

  it('ajustes reune versiones, terminos y notificaciones', async () => {
    renderRuta('/admin/ajustes?demo')
    expect(await screen.findByTestId('push-no-disponible')).toBeInTheDocument()
    expect(await screen.findByTestId('version-terminos')).toHaveTextContent('2026-10-25')
    expect(screen.getByLabelText('Nueva versión mínima')).toBeInTheDocument()
  })

  it('muestra estadísticas con gráficos y exportación en modo demo', async () => {
    renderRuta('/admin/estadisticas?demo')
    expect(await screen.findByTestId('estadisticas')).toBeInTheDocument()
    expect(screen.getByTestId('grafico-carreras')).toBeInTheDocument()
    expect(screen.getByTestId('rutas-top')).toHaveTextContent('Parque Recreacional Jipiro')
    expect(screen.getByTestId('estadisticas-pdf')).toBeEnabled()
  })
})
