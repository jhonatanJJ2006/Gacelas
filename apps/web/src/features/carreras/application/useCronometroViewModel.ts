import { useCallback, useMemo } from 'react'
import { useCarrera, empezarSeguimiento, finalizarCarrera, tracker } from './carreraStore'

/** ViewModel del cronometro (MVVM web). */
export function useCronometroViewModel() {
  const estado = useCarrera()

  const acciones = useMemo(
    () => ({
      empezar: (precisa = true, onErrorGps?: (msg: string) => void) =>
        empezarSeguimiento({ precisa, onError: onErrorGps }),
      finalizar: () => finalizarCarrera(),
      tracker,
    }),
    []
  )

  const reiniciarVista = useCallback(() => {
    /* reservado para reset de UI sin tocar dominio */
  }, [])

  return { estado, acciones, reiniciarVista }
}
