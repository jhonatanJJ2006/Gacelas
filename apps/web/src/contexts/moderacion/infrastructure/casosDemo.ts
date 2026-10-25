import type { CasoRepositorio } from '../application/puertos'
import type { AccionModeracion, EstadoReporte, ReporteAdmin } from '../domain/caso'

/** Casos de ejemplo para /admin?demo. */
export function casosDemo(): CasoRepositorio {
  const reportes: ReporteAdmin[] = (
    [
      [
        'post',
        'p1',
        'ofensivo',
        'Insulta a otro club en la descripción.',
        'Fondo de domingo',
        'pendiente',
        2,
      ],
      [
        'post',
        'p2',
        'falso',
        'Ruta que empieza y termina en un domicilio.',
        '42 km en 1 hora',
        'en_revision',
        1,
      ],
      [
        'usuario',
        'u9',
        'suplantacion',
        'Usa el nombre y la foto de un atleta conocido.',
        'Richard Carapaz',
        'pendiente',
        3,
      ],
      [
        'comentario',
        'c4',
        'acoso',
        'Comentarios repetidos contra la misma corredora.',
        'Eres lentísima, deja de publicar',
        'pendiente',
        1,
      ],
      [
        'evento',
        'e3',
        'peligroso',
        'Convoca a correr de noche por la vía a Cuenca sin permisos.',
        'Reto nocturno vía a Cuenca',
        'pendiente',
        2,
      ],
      [
        'incidente',
        'i2',
        'falso',
        'La foto es de otra ciudad y nadie más lo confirmó.',
        'robo asalto en Av. Universitaria: me quitaron el celular',
        'pendiente',
        2,
      ],
      [
        'post',
        'p3',
        'spam',
        'Zapatillas a mitad de precio, escribe al número.',
        'Oferta zapatillas',
        'resuelto',
        4,
      ],
      ['comentario', 'c7', 'otro', 'No me gustó.', 'Buen ritmo', 'descartado', 1],
    ] as const
  ).map(([target_type, target_id, reason, detail, resumen, status, total], i) => ({
    id: `r${i}`,
    target_type,
    target_id,
    reason,
    detail,
    status,
    created_at: new Date(Date.now() - (i + 1) * 3_600_000).toISOString(),
    reportante: ['Andrea Montaño', 'Kevin Jaramillo', 'Luis Ordóñez'][i % 3],
    resumen,
    total_reportes: total,
    acciones:
      status === 'pendiente'
        ? []
        : [
            {
              accion: (status === 'resuelto' ? 'ocultar_contenido' : status) as AccionModeracion,
              nota: status === 'descartado' ? 'No incumple las normas.' : 'Revisando el trazado.',
              fecha: new Date(Date.now() - i * 1_800_000).toISOString(),
              moderador: 'Daniela Cueva',
            },
          ],
  }))
  return {
    pendientes: () => Promise.resolve(reportes),
    async moderar(r, accion, nota) {
      const estado: Partial<Record<AccionModeracion, EstadoReporte>> = {
        en_revision: 'en_revision',
        resuelto: 'resuelto',
        descartado: 'descartado',
        reabierto: 'pendiente',
        ocultar_contenido: 'resuelto',
        suspender_usuario: 'resuelto',
      }
      r.status = estado[accion] ?? r.status
      r.acciones = [
        ...r.acciones,
        { accion, nota, fecha: new Date().toISOString(), moderador: 'Jhonatan Jara' },
      ]
    },
  }
}
