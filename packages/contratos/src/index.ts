/**
 * Contratos publicados entre bounded contexts (docs/ARQUITECTURA-DDD.md, 4.3 y decisión D2).
 * Los esquemas zod son la fuente de verdad para web y backend; las pruebas de Kotlin
 * validan contra los mismos nombres y campos.
 */
import { z } from 'zod'

const base = { ocurridoEn: z.string().datetime() }

export const CarreraTerminada = z.object({
  ...base,
  nombre: z.literal('actividad.CarreraTerminada'),
  datos: z.object({
    carreraId: z.string().uuid(),
    corredorId: z.string().uuid(),
    distanciaM: z.number().nonnegative(),
    duracionS: z.number().nonnegative(),
    rutaId: z.string().uuid().nullable(),
  }),
})

export const ConsentimientoCambiado = z.object({
  ...base,
  nombre: z.enum(['legal.ConsentimientoOtorgado', 'legal.ConsentimientoRevocado']),
  datos: z.object({
    corredorId: z.string().uuid(),
    tipo: z.enum(['terminos', 'privacidad', 'gps_carrera', 'ubicacion_visible']),
    version: z.string(),
  }),
})

export const EventoPublicado = z.object({
  ...base,
  nombre: z.literal('eventos.EventoPublicado'),
  datos: z.object({ eventoId: z.string().uuid(), clubId: z.string().uuid().nullable() }),
})

export const EventoContrato = z.discriminatedUnion('nombre', [
  CarreraTerminada,
  ConsentimientoCambiado.extend({ nombre: z.literal('legal.ConsentimientoOtorgado') }),
  ConsentimientoCambiado.extend({ nombre: z.literal('legal.ConsentimientoRevocado') }),
  EventoPublicado,
])

export type CarreraTerminada = z.infer<typeof CarreraTerminada>
export type ConsentimientoCambiado = z.infer<typeof ConsentimientoCambiado>
export type EventoPublicado = z.infer<typeof EventoPublicado>
export type EventoContrato = z.infer<typeof EventoContrato>

/** Lista de nombres publicados; sirve para pruebas de contrato en otros lenguajes. */
export const NOMBRES_EVENTOS = [
  'actividad.CarreraTerminada',
  'legal.ConsentimientoOtorgado',
  'legal.ConsentimientoRevocado',
  'eventos.EventoPublicado',
] as const
