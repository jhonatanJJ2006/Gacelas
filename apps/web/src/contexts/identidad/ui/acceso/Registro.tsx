import { LogoGacelas } from '../../../../shared/ui/LogoGacelas'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { MensajeError } from '../../../../shared/ui/ui'
import { registrarConsentimiento } from '../../../legal/api'
import { useIdentidad } from '../../application/estado'
import { BotonGoogle } from './BotonGoogle'
import { CampoContrasena } from './CampoContrasena'

export function Registro() {
  const navigate = useNavigate()
  const [nombre, setNombre] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [cargando, setCargando] = useState(false)
  const [acepta, setAcepta] = useState(false)
  const registrar = useIdentidad((s) => s.registrar)
  const actualizarAjustes = useIdentidad((s) => s.actualizarAjustes)
  const [nivel, setNivel] = useState<'principiante' | 'intermedio' | 'avanzado'>('principiante')
  const fuerza = [/.{8,}/, /[A-Za-z]/, /\d/, /[^A-Za-z0-9]/].filter((r) => r.test(password)).length

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!acepta) return setError('Debes aceptar los Términos y la Política de privacidad')
    setCargando(true)
    const r = await registrar(nombre, email, password)
    setCargando(false)
    if (r.ok) {
      void actualizarAjustes({ nivelDeclarado: nivel })
      void registrarConsentimiento('terminos', true).then(() =>
        registrarConsentimiento('privacidad', true)
      )
      navigate('/permisos-ubicacion', { replace: true })
    } else setError(r.error)
  }

  return (
    <div className="w-full min-h-[100dvh] bg-surface flex flex-col">
      <header className="w-full z-20 px-margin py-2 short:py-1 md:absolute md:top-2 md:left-2 md:w-auto">
        <button
          onClick={() => navigate('/bienvenida')}
          aria-label="Atrás"
          className="w-12 h-12 -ml-2 rounded-full flex items-center justify-center text-text-primary"
        >
          <span className="material-symbols-outlined">arrow_back</span>
        </button>
      </header>

      <main className="flex-1 px-6 pb-4 short:pb-2 flex flex-col">
        <div className="flex-1 flex flex-col justify-center max-w-md mx-auto w-full md:max-w-4xl md:grid md:grid-cols-2 md:gap-x-16 md:content-center md:items-center">
          <div className="mb-5 short:mb-3 md:mb-0 md:row-span-2">
            <div className="mb-6 short:hidden md:flex flex">
              <LogoGacelas tam={48} grande />
            </div>

            <h1 className="text-text-primary font-headline-lg text-2xl md:text-4xl font-bold mb-2">
              Únete a la manada
            </h1>
            <p className="text-text-secondary text-sm">
              Crea tu cuenta y empieza a correr con la comunidad
            </p>
          </div>

          <form
            onSubmit={handleSubmit}
            noValidate
            className="space-y-4 short:space-y-2.5 md:col-start-2"
          >
            <div>
              <label
                htmlFor="nombre"
                className="block text-sm font-medium text-text-primary mb-1.5"
              >
                Nombre completo
              </label>
              <input
                type="text"
                id="nombre"
                value={nombre}
                onChange={(e) => {
                  setNombre(e.target.value)
                  setError(null)
                }}
                className="w-full h-14 short:h-12 px-4 text-base bg-surface-container border border-border-subtle rounded-xl text-text-primary placeholder-text-secondary focus:outline-none focus:border-primary-600"
                placeholder="Tu nombre"
                required
              />
            </div>

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
                className="w-full h-14 short:h-12 px-4 text-base bg-surface-container border border-border-subtle rounded-xl text-text-primary placeholder-text-secondary focus:outline-none focus:border-primary-600"
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
              <CampoContrasena
                id="password"
                value={password}
                onChange={(e) => {
                  setPassword(e.target.value)
                  setError(null)
                }}
                className="w-full h-14 short:h-12 px-4 text-base bg-surface-container border border-border-subtle rounded-xl text-text-primary placeholder-text-secondary focus:outline-none focus:border-primary-600"
                placeholder="Mínimo 8 caracteres"
                required
                minLength={8}
              />
              <p className="text-xs text-text-secondary mt-1.5">
                Usa mínimo 8 caracteres con letras y números
                {password && (fuerza >= 3 ? ' • Segura' : ' • Débil')}
              </p>
              <div className="flex gap-1 mt-3" aria-label={`Fuerza de la clave ${fuerza} de 4`}>
                {[0, 1, 2, 3].map((i) => (
                  <span
                    key={i}
                    className={`h-1 flex-1 rounded-full ${i < fuerza ? (fuerza >= 3 ? 'bg-success' : 'bg-primary-600') : 'bg-surface-container-highest'}`}
                  />
                ))}
              </div>
            </div>

            <fieldset>
              <legend className="block text-sm font-medium text-text-primary mb-1.5">
                Nivel de corredor
              </legend>
              <div className="grid grid-cols-3 gap-2">
                {(
                  [
                    ['principiante', 'Principiante', '0 - 5 km'],
                    ['intermedio', 'Intermedio', '5 - 15 km'],
                    ['avanzado', 'Avanzado', '+15 km / Trail'],
                  ] as const
                ).map(([id, t, d]) => (
                  <button
                    type="button"
                    key={id}
                    aria-pressed={nivel === id}
                    onClick={() => setNivel(id)}
                    className={`p-2 rounded-xl border text-center ${nivel === id ? 'border-primary-600 bg-primary-600/15' : 'border-border-subtle bg-surface-container'}`}
                  >
                    <span className="block text-[13px] text-text-primary font-semibold">{t}</span>
                    <span className="block text-xs text-text-secondary">{d}</span>
                  </button>
                ))}
              </div>
            </fieldset>

            <div
              data-testid="aceptar-terminos"
              className={`flex items-center gap-3 p-3 rounded-xl bg-surface-container border ${acepta ? 'border-primary-600' : 'border-border-subtle'}`}
            >
              <input
                id="acepta"
                type="checkbox"
                checked={acepta}
                onChange={(e) => setAcepta(e.target.checked)}
                aria-label="Acepto los Términos y la Política de privacidad"
                className="m-3 w-[18px] h-[18px] shrink-0 accent-[#7B1FA2]"
              />
              <div className="flex-1">
                <label
                  htmlFor="acepta"
                  className="text-[15px] leading-[22px] text-text-primary cursor-pointer"
                >
                  He leído y acepto los Términos y la Política de privacidad
                </label>
                <div className="flex text-[13px] font-semibold text-primary-500">
                  <Link to="/legal/terminos" className="pt-1 pr-3" data-testid="ver-terminos">
                    Ver términos
                  </Link>
                  <Link to="/legal/privacidad" className="pt-1" data-testid="ver-privacidad">
                    Ver privacidad
                  </Link>
                </div>
              </div>
            </div>

            <MensajeError mensaje={error} />

            <button type="submit" disabled={cargando || !acepta} className="boton-c2 !mt-6">
              <span>{cargando ? 'Creando cuenta...' : 'Crear cuenta'}</span>
              <span className="material-symbols-outlined text-[22px]" aria-hidden="true">
                arrow_forward
              </span>
            </button>

            <div className="text-center pt-2">
              <span className="text-text-secondary text-sm">
                ¿Ya tienes cuenta?{' '}
                <Link
                  to="/inicio-sesion"
                  className="inline-block py-2 text-primary-500 hover:text-primary-400 font-medium"
                >
                  Iniciar sesión
                </Link>
              </span>
            </div>
          </form>

          <div className="mt-4 space-y-3 md:col-start-2">
            <BotonGoogle />
          </div>
        </div>
      </main>
    </div>
  )
}
