import { describe, expect, it, beforeEach } from 'vitest'
import { screen } from '@testing-library/react'
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
    await f.resolverReporte(r, true)
    expect((await f.metricas()).reportes_pendientes).toBe(2)
    await f.guardarEvento({ title: 'Nuevo', published: true })
    expect((await f.eventos())[0]).toMatchObject({ title: 'Nuevo', published: true })
    await f.publicarTerminos('2027-01-01')
    expect(await f.versionTerminos()).toBe('2027-01-01')
  })
})
