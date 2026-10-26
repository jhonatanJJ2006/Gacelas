import { describe, expect, it } from 'vitest'
import { exportarSerieCsv, verPanelAdmin } from '../../../src/contexts/analitica/application/panel'
import {
  validarPeriodo,
  type Estadisticas,
} from '../../../src/contexts/analitica/domain/estadisticas'
import { analiticaSupabase } from '../../../src/contexts/analitica/infrastructure/supabase'
import { clienteFalso } from '../doble'

const est: Estadisticas = {
  desde: '2026-10-01',
  hasta: '2026-10-02',
  totales: { carreras: 3 },
  serie: [
    { dia: '2026-10-01', nuevos: 1, activos: 2, carreras: 1, km: 5.5 },
    { dia: '2026-10-02', nuevos: 0, activos: 1, carreras: 2, km: 10 },
  ],
}

describe('analitica', () => {
  it('valida el periodo', () => {
    expect(validarPeriodo({ desde: '2026-10-01', hasta: '2026-10-31' })).toBeNull()
    expect(validarPeriodo({ desde: '2026-10-31', hasta: '2026-10-01' })).toMatch(/posterior/)
    expect(validarPeriodo({ desde: '2025-01-01', hasta: '2026-10-01' })).toMatch(/año/)
    expect(validarPeriodo({ desde: '1/10/2026', hasta: '2026-10-01' })).toMatch(/formato/)
  })

  it('no consulta con un periodo invalido', async () => {
    let consultas = 0
    const consulta = { estadisticas: async () => (consultas++, est) }
    await expect(verPanelAdmin(consulta, { desde: 'x', hasta: 'y' })).rejects.toThrow()
    expect(consultas).toBe(0)
    expect(await verPanelAdmin(consulta, { desde: '2026-10-01', hasta: '2026-10-02' })).toBe(est)
  })

  it('exporta la serie en CSV', () => {
    expect(exportarSerieCsv(est)).toBe(
      'dia,nuevos,activos,carreras,km\n2026-10-01,1,2,1,5.5\n2026-10-02,0,1,2,10\n'
    )
  })

  it('el adaptador llama a admin_estadisticas', async () => {
    const { sb, llamadas } = clienteFalso({ data: est, error: null })
    expect(await analiticaSupabase(sb).estadisticas('2026-10-01', '2026-10-02')).toBe(est)
    expect(llamadas[0].args).toEqual([
      'admin_estadisticas',
      { desde: '2026-10-01', hasta: '2026-10-02' },
    ])
  })
})
