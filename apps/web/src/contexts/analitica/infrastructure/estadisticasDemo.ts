import type { Estadisticas } from '../domain/estadisticas'

/** Estadisticas de ejemplo deterministas para /admin?demo. */
export function estadisticasDemo(desde: string, hasta: string): Estadisticas {
  const ini = new Date(`${desde}T00:00:00`)
  const fin = new Date(`${hasta}T00:00:00`)
  const dias = Math.max(1, Math.round((fin.getTime() - ini.getTime()) / 86_400_000) + 1)
  const serie = Array.from({ length: dias }, (_, i) => {
    const d = new Date(ini)
    d.setDate(d.getDate() + i)
    const finde = d.getDay() === 0 || d.getDay() === 6
    const ola = Math.sin(i / 3) * 12
    const carreras = Math.round((finde ? 210 : 140) + ola + (i % 5) * 4)
    return {
      dia: d.toISOString().slice(0, 10),
      nuevos: Math.round(14 + (i % 7) * 2 + ola / 3),
      activos: Math.round(carreras * 0.8),
      carreras,
      km: Math.round(carreras * 6.3 * 10) / 10,
    }
  })
  const suma = (k: 'nuevos' | 'carreras' | 'km') => serie.reduce((s, x) => s + x[k], 0)
  return {
    desde,
    hasta,
    totales: {
      usuarios_nuevos: suma('nuevos'),
      usuarios_activos: Math.round(Math.max(...serie.map((s) => s.activos)) * 2.1),
      carreras: suma('carreras'),
      km: Math.round(suma('km') * 10) / 10,
      eventos: Math.max(1, Math.round(dias / 5)),
      inscripciones: Math.round(dias * 11.5),
      reportes: Math.round(dias * 0.9),
      usuarios_total: 2418,
    },
    serie,
    rutas_top: [
      ['loja-jipiro', 'Parque Recreacional Jipiro', 412],
      ['loja-la-tebaida', 'Parque Lineal La Tebaida', 288],
      ['loja-orillas-zamora', 'Orillas del Zamora', 251],
      ['loja-villonaco', 'Subida al Villonaco', 133],
      ['loja-pucara', 'Parque Pucará Podocarpus', 97],
    ].map(([id, nombre, n]) => ({
      id: id as string,
      nombre: nombre as string,
      oficial: true,
      corridas: Math.round(((n as number) * dias) / 30),
      corredores: Math.round(((n as number) * dias) / 30 / 2.4),
    })),
    eventos_top: [
      {
        id: 'e1',
        titulo: '10K Ciudad de Loja',
        fecha: '2026-11-15T07:00:00-05:00',
        inscritos: 318,
      },
      { id: 'e2', titulo: 'Trail Podocarpus', fecha: '2026-11-29T06:30:00-05:00', inscritos: 96 },
    ],
    retencion: { previos: 1180, vuelven: 802, porcentaje: 68 },
    reportes: {
      por_estado: { pendiente: 4, en_revision: 1, resuelto: 9, descartado: 3 },
      por_tipo: { post: 8, comentario: 5, usuario: 3, evento: 1 },
      por_motivo: { spam: 6, ofensivo: 4, acoso: 3, falso: 2, suplantacion: 1, peligroso: 1 },
    },
  }
}
