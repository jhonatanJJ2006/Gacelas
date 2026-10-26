import type { ClienteSupabase } from '../../src/shared/supabase'

/** Doble minimo del cliente de Supabase: registra las llamadas y devuelve la respuesta dada. */
export function clienteFalso(respuesta: { data: unknown; error: { message: string } | null }) {
  const llamadas: { metodo: string; args: unknown[] }[] = []
  const consulta: Record<string, unknown> = {}
  for (const m of ['select', 'order', 'limit'])
    consulta[m] = (...args: unknown[]) => {
      llamadas.push({ metodo: m, args })
      return m === 'limit' ? Promise.resolve(respuesta) : consulta
    }
  const sb = {
    rpc: (...args: unknown[]) => {
      llamadas.push({ metodo: 'rpc', args })
      return Promise.resolve(respuesta)
    },
    from: (...args: unknown[]) => {
      llamadas.push({ metodo: 'from', args })
      return consulta
    },
  } as unknown as ClienteSupabase
  return { sb, llamadas }
}
