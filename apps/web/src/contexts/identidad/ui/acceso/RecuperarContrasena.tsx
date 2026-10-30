import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { MensajeError } from '../../../../shared/ui/ui'
import { useIdentidad } from '../../application/estado'

export function RecuperarContrasena() {
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [submitted, setSubmitted] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const recuperar = useIdentidad((s) => s.recuperarContrasena)
  const [espera, setEspera] = useState(0)
  useEffect(() => {
    if (espera <= 0) return
    const t = setTimeout(() => setEspera(espera - 1), 1000)
    return () => clearTimeout(t)
  }, [espera])
  const reenviar = async () => {
    const r = await recuperar(email)
    if (r.ok) setEspera(45)
    else setError(r.error)
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    const r = await recuperar(email)
    if (r.ok) {
      setSubmitted(true)
      setEspera(45)
    } else setError(r.error)
  }

  if (submitted) {
    return (
      <div className="w-full min-h-[100dvh] bg-surface flex flex-col">
        <header className="w-full z-20 px-margin py-2">
          <button
            onClick={() => navigate('/inicio-sesion')}
            aria-label="Atrás"
            className="w-12 h-12 -ml-2 rounded-full flex items-center justify-center text-text-primary"
          >
            <span className="material-symbols-outlined">arrow_back</span>
          </button>
        </header>

        <main className="flex-1 px-6 pb-8 flex flex-col">
          <div className="flex flex-col items-center gap-4 pt-12 max-w-md mx-auto w-full text-center">
            <div className="w-24 h-24 rounded-full bg-secondary-600/20 flex items-center justify-center">
              <span className="material-symbols-outlined text-[48px] text-secondary-500">
                mark_email_read
              </span>
            </div>

            <h1 className="text-text-primary font-headline-lg text-[32px] leading-10">
              Revisa tu correo
            </h1>
            <p className="text-text-secondary text-base">
              Te hemos enviado un enlace para restablecer tu contraseña a {email}
            </p>
            <div className="w-full flex items-center justify-between p-3 rounded-xl bg-surface-container text-xs">
              <span className="text-text-secondary">¿No recibiste el correo?</span>
              <button
                onClick={() => void reenviar()}
                disabled={espera > 0}
                className="p-1 text-[13px] text-primary-500 font-semibold"
              >
                {espera > 0 ? `Reenviar en 0:${String(espera).padStart(2, '0')}` : 'Reenviar'}
              </button>
            </div>
            <MensajeError mensaje={error} />

            <button onClick={() => navigate('/inicio-sesion')} className="boton-c2">
              Volver al inicio de sesión
            </button>
          </div>
        </main>
      </div>
    )
  }

  return (
    <div className="w-full min-h-[100dvh] bg-surface flex flex-col">
      <header className="w-full z-20 px-margin py-2">
        <button
          onClick={() => navigate('/inicio-sesion')}
          aria-label="Atrás"
          className="w-12 h-12 -ml-2 rounded-full flex items-center justify-center text-text-primary"
        >
          <span className="material-symbols-outlined">arrow_back</span>
        </button>
      </header>

      <main className="flex-1 px-6 pb-8 flex flex-col">
        <div className="flex flex-col gap-5 pt-6 max-w-md mx-auto w-full">
          <div className="flex flex-col items-center gap-3 text-center">
            <div className="w-20 h-20 rounded-full bg-primary-600/20 flex items-center justify-center">
              <span className="material-symbols-outlined text-[40px] text-primary-500">
                lock_reset
              </span>
            </div>

            <h1 className="text-text-primary font-headline-lg text-[32px] leading-10">
              Recuperar contraseña
            </h1>
            <p className="text-text-secondary text-base">
              Ingresa tu correo electrónico y te enviaremos un enlace para restablecer tu contraseña
            </p>
          </div>

          <form onSubmit={handleSubmit} noValidate className="space-y-5">
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
                className="w-full h-14 px-4 text-base bg-surface-container border border-border-subtle rounded-xl text-text-primary placeholder-text-secondary focus:outline-none focus:border-primary-600"
                placeholder="tu@email.com"
                required
              />
            </div>

            <MensajeError mensaje={error} />

            <button type="submit" className="boton-c2">
              <span>Enviar enlace</span>
              <span className="material-symbols-outlined text-[22px]" aria-hidden="true">
                arrow_forward
              </span>
            </button>
          </form>
        </div>
      </main>
    </div>
  )
}
