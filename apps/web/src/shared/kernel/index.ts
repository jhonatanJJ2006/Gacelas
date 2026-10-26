/**
 * Kernel compartido de los bounded contexts (docs/ARQUITECTURA-DDD.md, seccion 4).
 * Solo tipos y utilidades sin dependencias de React ni de Supabase.
 */

export interface ErrorDominio {
  codigo: string
  mensaje: string
}

export type Resultado<T> = { ok: true; valor: T } | { ok: false; error: ErrorDominio }

export const exito = <T>(valor: T): Resultado<T> => ({ ok: true, valor })
export const fallo = (codigo: string, mensaje: string): Resultado<never> => ({
  ok: false,
  error: { codigo, mensaje },
})

/** Identificador tipado: evita mezclar, por ejemplo, un RutaId con un CarreraId. */
export type Id<Marca extends string> = string & { readonly __marca: Marca }

/** Evento de dominio publicado por un contexto, con nombre en pasado. */
export interface EventoDominio<N extends string = string, D = unknown> {
  nombre: N
  ocurridoEn: string
  datos: D
}

type Oyente = (evento: EventoDominio) => void

/** Bus de eventos en memoria para comunicar contextos dentro de la app. */
export class BusEventos {
  private oyentes = new Map<string, Set<Oyente>>()

  suscribir(nombre: string, oyente: Oyente): () => void {
    const set = this.oyentes.get(nombre) ?? new Set<Oyente>()
    set.add(oyente)
    this.oyentes.set(nombre, set)
    return () => set.delete(oyente)
  }

  publicar(evento: EventoDominio): void {
    this.oyentes.get(evento.nombre)?.forEach((o) => o(evento))
  }
}
