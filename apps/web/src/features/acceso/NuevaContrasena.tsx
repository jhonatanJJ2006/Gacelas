import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { CampoContrasena } from '../../components/CampoContrasena'
import { useApp } from '../../store/app'
import { MensajeError } from '../../components/ui'

/** Pantalla a la que lleva el enlace de recuperacion: pide la contrasena nueva. */
export function NuevaContrasena() {
  const navigate = useNavigate()
  const cambiar = useApp((s) => s.cambiarContrasena)
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [cargando, setCargando] = useState(false)

  const enviar = async (e: React.FormEvent) => {
    e.preventDefault()
    setCargando(true)
    const r = await cambiar(password)
    setCargando(false)
    if (r.ok) navigate('/inicio', { replace: true })
    else setError(r.error)
  }

  return (
    <div className="w-full min-h-[100dvh] bg-surface flex flex-col px-6 py-8">
      <form onSubmit={enviar} noValidate className="max-w-md mx-auto w-full my-auto space-y-4">
        <h1 className="text-text-primary font-headline-lg text-2xl font-bold">Nueva contraseña</h1>
        <p className="text-text-secondary text-sm">
          Escribe la contraseña que quieres usar desde ahora.
        </p>
        <label htmlFor="nueva" className="block text-sm font-medium text-text-primary">
          Contraseña nueva
        </label>
        <CampoContrasena
          id="nueva"
          value={password}
          onChange={(e) => {
            setPassword(e.target.value)
            setError(null)
          }}
          className="w-full h-12 px-4 bg-surface-container border border-border-subtle rounded-lg text-text-primary focus:outline-none focus:ring-2 focus:ring-primary-600"
        />
        <MensajeError mensaje={error} />
        <button
          type="submit"
          disabled={cargando}
          className="w-full h-14 bg-primary-600 hover:bg-primary-700 text-bg-primary font-bold rounded-xl disabled:opacity-60"
        >
          {cargando ? 'Guardando...' : 'Guardar contraseña'}
        </button>
      </form>
    </div>
  )
}
