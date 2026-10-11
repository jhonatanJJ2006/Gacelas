import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { useApp } from '../../store/app'
import { MensajeError } from '../../components/ui'
import { BotonGoogle } from '../../components/BotonGoogle'
import { PanelCorredorAcceso } from '../../components/PanelCorredorAcceso'

export function InicioSesion() {
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [cargando, setCargando] = useState(false)
  const iniciarSesion = useApp((s) => s.iniciarSesion)

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setCargando(true)
    const r = await iniciarSesion(email, password)
    setCargando(false)
    if (r.ok) navigate('/inicio', { replace: true })
    else setError(r.error)
  }

  return (
    <div className="w-full min-h-[100dvh] bg-surface flex flex-col">
      <header className="w-full z-20 px-margin py-2 short:py-1 md:absolute md:top-2 md:left-2 md:w-auto">
        <button
          onClick={() => navigate('/bienvenida')}
          className="w-10 h-10 rounded-full bg-surface-container flex items-center justify-center text-text-secondary hover:text-text-primary transition-colors"
        >
          <span className="material-symbols-outlined">arrow_back</span>
        </button>
      </header>

      <main className="flex-1 px-6 pb-4 short:pb-2 flex flex-col">
        <div className="flex-1 flex flex-col justify-center max-w-md mx-auto w-full md:max-w-4xl md:grid md:grid-cols-2 md:gap-x-16 md:content-center md:items-center">
          <div className="mb-5 short:mb-3 md:mb-0 md:row-span-2">
            <div className="flex items-center gap-2.5 mb-6 short:hidden md:flex">
              <div className="w-12 h-12 rounded-xl bg-primary-600/15 border border-primary-600/30 flex items-center justify-center text-primary-500 shadow-sm">
                <span className="material-symbols-outlined text-[24px]">sprint</span>
              </div>
              <div className="flex flex-col">
                <span className="font-headline-md text-text-primary text-lg font-extrabold tracking-wider leading-none">
                  GACELAS
                </span>
                <span className="text-xs font-semibold text-primary-400 tracking-widest">
                  LOJA, ECUADOR
                </span>
              </div>
            </div>

            <h1 className="text-text-primary font-headline-lg text-2xl md:text-4xl font-bold mb-2">
              Bienvenido de vuelta
            </h1>
            <p className="text-text-secondary text-sm">
              Inicia sesión para continuar con tus carreras
            </p>
            <PanelCorredorAcceso />
          </div>

          <form
            onSubmit={handleSubmit}
            noValidate
            className="space-y-4 short:space-y-2.5 md:col-start-2"
          >
            <div>
              <label htmlFor="email" className="block text-sm font-medium text-text-primary mb-1.5">
                Correo electrónico
              </label>
              <input
                type="email"
                id="email"
                value={email}
                onChange={(e) => {
                  setEmail(e.target.value)
                  setError(null)
                }}
                className="w-full h-12 short:h-10 px-4 bg-surface-container border border-border-subtle rounded-lg text-text-primary placeholder-text-secondary focus:outline-none focus:ring-2 focus:ring-primary-600 focus:border-transparent"
                placeholder="tu@email.com"
                required
              />
            </div>

            <div>
              <label
                htmlFor="password"
                className="block text-sm font-medium text-text-primary mb-1.5"
              >
                Contraseña
              </label>
              <input
                type="password"
                id="password"
                value={password}
                onChange={(e) => {
                  setPassword(e.target.value)
                  setError(null)
                }}
                className="w-full h-12 short:h-10 px-4 bg-surface-container border border-border-subtle rounded-lg text-text-primary placeholder-text-secondary focus:outline-none focus:ring-2 focus:ring-primary-600 focus:border-transparent"
                placeholder="••••••••"
                required
              />
            </div>

            <div className="flex justify-end">
              <Link
                to="/recuperar-contrasena"
                className="inline-block py-2 text-sm text-primary-500 hover:text-primary-400 font-medium"
              >
                ¿Olvidaste tu contraseña?
              </Link>
            </div>

            <MensajeError mensaje={error} />

            <button
              type="submit"
              disabled={cargando}
              className="w-full h-12 bg-primary-600 hover:bg-primary-700 active:scale-[0.98] text-bg-primary font-headline-md font-bold text-base rounded-xl flex items-center justify-center gap-2 shadow-[0_8px_20px_-4px_rgba(217,119,6,0.35)] transition-all cursor-pointer disabled:opacity-60"
            >
              <span>{cargando ? 'Entrando...' : 'Iniciar sesión'}</span>
              <span className="material-symbols-outlined text-[20px]">arrow_forward</span>
            </button>

            <div className="text-center pt-2">
              <span className="text-text-secondary text-sm">
                ¿No tienes cuenta?{' '}
                <Link
                  to="/registro"
                  className="inline-block py-2 text-primary-500 hover:text-primary-400 font-medium"
                >
                  Crear cuenta
                </Link>
              </span>
            </div>
          </form>

          <div className="mt-4 space-y-3 md:col-start-2">
            <div className="flex items-center gap-3 text-xs text-text-secondary">
              <span className="flex-1 h-px bg-border-subtle" />o
              <span className="flex-1 h-px bg-border-subtle" />
            </div>
            <BotonGoogle />
          </div>
        </div>
      </main>
    </div>
  )
}
