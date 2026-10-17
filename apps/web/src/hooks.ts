import { useCallback, useEffect, useRef, useState } from 'react'

/** Muestra un aviso temporal; devuelve [mensaje, mostrar]. */
export function useAviso(duracionMs = 2200): [string | null, (m: string) => void] {
  const [mensaje, setMensaje] = useState<string | null>(null)
  const t = useRef<ReturnType<typeof setTimeout>>()
  useEffect(() => () => clearTimeout(t.current), [])
  const mostrar = useCallback(
    (m: string) => {
      setMensaje(m)
      clearTimeout(t.current)
      t.current = setTimeout(() => setMensaje(null), duracionMs)
    },
    [duracionMs]
  )
  return [mensaje, mostrar]
}

/** Comparte con la Web Share API o copia al portapapeles. Devuelve el resultado para avisar. */
export async function compartir(
  titulo: string,
  texto: string
): Promise<'compartido' | 'copiado' | 'error'> {
  try {
    if (navigator.share) {
      await navigator.share({ title: titulo, text: texto, url: window.location.origin })
      return 'compartido'
    }
    await navigator.clipboard.writeText(`${texto} ${window.location.origin}`)
    return 'copiado'
  } catch {
    return 'error'
  }
}
