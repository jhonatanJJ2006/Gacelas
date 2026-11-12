import { useEffect, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { Aviso } from './ui'

/**
 * Muestra el aviso que otra pantalla deja al navegar (state.aviso), por ejemplo "Carrera guardada"
 * al volver a Inicio, y lo borra del historial para que no se repita al recargar.
 */
export function AvisoNavegacion({ duracionMs = 2600 }: { duracionMs?: number }) {
  const location = useLocation()
  const navigate = useNavigate()
  const texto = (location.state as { aviso?: string } | null)?.aviso ?? null
  const [mensaje, setMensaje] = useState<string | null>(texto)
  useEffect(() => {
    if (!texto) return
    setMensaje(texto)
    navigate(location.pathname + location.search, { replace: true, state: null })
    const t = setTimeout(() => setMensaje(null), duracionMs)
    return () => clearTimeout(t)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [texto])
  return <Aviso mensaje={mensaje} />
}
