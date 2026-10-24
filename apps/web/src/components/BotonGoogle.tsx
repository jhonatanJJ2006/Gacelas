import { useState } from 'react'
import { useApp } from '../store/app'
import { MensajeError } from './ui'

/** Boton "Continuar con Google" (Supabase OAuth). Muestra un aviso si no esta disponible. */
export function BotonGoogle() {
  const entrarConGoogle = useApp((s) => s.entrarConGoogle)
  const [error, setError] = useState<string | null>(null)
  const [cargando, setCargando] = useState(false)

  const entrar = async () => {
    setError(null)
    setCargando(true)
    const e = await entrarConGoogle()
    // Si todo sale bien el navegador ya se esta yendo a Google.
    if (e) {
      setError(e)
      setCargando(false)
    }
  }

  return (
    <div className="w-full flex flex-col gap-2">
      <button
        type="button"
        onClick={entrar}
        disabled={cargando}
        className="w-full h-12 bg-[#232A3A] hover:bg-[#2C3446] text-white font-medium text-sm rounded-xl flex items-center justify-center gap-3 transition-all cursor-pointer disabled:opacity-60 min-h-[48px]"
      >
        <svg aria-hidden="true" width="18" height="18" viewBox="0 0 48 48">
          <path
            fill="#EA4335"
            d="M24 9.5c3.5 0 6.6 1.2 9.1 3.6l6.8-6.8C35.8 2.4 30.3 0 24 0 14.6 0 6.6 5.4 2.6 13.3l7.9 6.1C12.4 13.7 17.7 9.5 24 9.5z"
          />
          <path
            fill="#4285F4"
            d="M46.1 24.5c0-1.6-.1-3.1-.4-4.5H24v9h12.4c-.5 2.9-2.2 5.3-4.6 6.9l7.4 5.7c4.3-4 6.9-9.9 6.9-17.1z"
          />
          <path
            fill="#FBBC05"
            d="M10.5 28.6c-.5-1.4-.8-3-.8-4.6s.3-3.2.8-4.6l-7.9-6.1C1 16.6 0 20.2 0 24s.9 7.4 2.6 10.7l7.9-6.1z"
          />
          <path
            fill="#34A853"
            d="M24 48c6.5 0 11.9-2.1 15.9-5.8l-7.4-5.7c-2.1 1.4-4.8 2.3-8.5 2.3-6.3 0-11.6-4.2-13.5-10l-7.9 6.1C6.6 42.6 14.6 48 24 48z"
          />
        </svg>
        <span>{cargando ? 'Abriendo Google...' : 'Continuar con Google'}</span>
      </button>
      <MensajeError mensaje={error} />
    </div>
  )
}
