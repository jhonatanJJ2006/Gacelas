import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { MensajeError } from '../../../../shared/ui/ui'
import { useIdentidad } from '../../application/estado'
import {
  AtrasC4,
  BotonC4,
  ChipC4,
  claseCampoC4,
  claseEtiquetaC4,
  ETIQUETA_PORTADA,
  FondoMapaC4,
  IconoCampoC4,
  IndicadorZonaC4,
  MarcaC4,
  PieC4,
  SeparadorC4,
  TarjetaC4,
} from './AccesoC4'
import { BotonGoogle } from './BotonGoogle'
import { CampoContrasena } from './CampoContrasena'

/** Inicio de sesion C4, igual que Android InicioSesionC4 (AccesoPantallas.kt). */
export function InicioSesion() {
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [cargando, setCargando] = useState(false)
  const iniciarSesion = useIdentidad((s) => s.iniciarSesion)

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setCargando(true)
    const r = await iniciarSesion(email, password)
    setCargando(false)
    if (r.ok) navigate('/inicio', { replace: true })
    else setError(r.error)
  }

  return (
    <div
      className="relative flex min-h-[100dvh] w-full flex-col bg-[#0C1322]"
      data-testid="pantalla-login"
    >
      <FondoMapaC4 login />
      <div className="relative z-10 mx-auto flex min-h-[100dvh] w-full max-w-md flex-col">
        <header className="flex items-center gap-3 px-5 pt-4 short:pt-2">
          <AtrasC4 onClick={() => navigate('/bienvenida')} />
          <MarcaC4 />
          <span className="flex-1" />
          <ChipC4 texto={ETIQUETA_PORTADA} />
        </header>
        <div className="flex min-h-0 flex-1 items-end px-5 pb-3">
          <span className="short:hidden">
            <IndicadorZonaC4 />
          </span>
        </div>
        <div className="px-5 pt-2 pb-6 short:pb-2">
          <TarjetaC4 alpha={0.96}>
            <h1 className="font-display text-[24px] leading-[30px] tracking-[0.015em] text-white short:text-[20px]">
              Bienvenido de vuelta
            </h1>
            <p className="mt-1.5 text-[13px] leading-[18px] text-[#D5DAE4]/80 short:hidden">
              Inicia sesión para seguir corriendo
            </p>
            <form onSubmit={handleSubmit} noValidate className="mt-5 short:mt-3.5">
              <div className="flex flex-col gap-1.5">
                <label htmlFor="email" className={claseEtiquetaC4}>
                  Correo electrónico
                </label>
                <div className="relative">
                  <IconoCampoC4 icono="mail" />
                  <input
                    type="email"
                    id="email"
                    autoComplete="email"
                    value={email}
                    onChange={(e) => {
                      setEmail(e.target.value)
                      setError(null)
                    }}
                    className={claseCampoC4}
                    placeholder="tu@email.com"
                    required
                  />
                </div>
              </div>
              <div className="mt-4 flex flex-col gap-1.5">
                <div className="flex items-center">
                  <label htmlFor="password" className={`${claseEtiquetaC4} flex-1`}>
                    Contraseña
                  </label>
                  <Link
                    to="/recuperar-contrasena"
                    className="text-[11px] font-medium leading-[14px] text-[#C084FC]"
                  >
                    ¿Olvidaste tu contraseña?
                  </Link>
                </div>
                <div className="relative">
                  <IconoCampoC4 icono="lock" />
                  <CampoContrasena
                    id="password"
                    autoComplete="current-password"
                    value={password}
                    onChange={(e) => {
                      setPassword(e.target.value)
                      setError(null)
                    }}
                    className={claseCampoC4}
                    placeholder="••••••••"
                    required
                  />
                </div>
              </div>
              <div className="mt-2">
                <MensajeError mensaje={error} />
              </div>
              <div className="mt-5">
                <BotonC4
                  type="submit"
                  texto={cargando ? 'Ingresando...' : 'Iniciar sesión'}
                  alto={48}
                  tamTexto={16}
                  disabled={cargando}
                />
              </div>
            </form>
            <div className="short:hidden">
              <SeparadorC4 />
              <BotonGoogle />
              <PieC4 texto="¿No tienes cuenta?" enlace="Crear cuenta" a="/registro" />
              <div className="flex justify-center pt-1.5 text-[12px] text-[#A8B0C2]">
                <Link to="/legal/terminos" className="p-1.5" data-testid="pie-terminos">
                  Términos
                </Link>
                <Link to="/legal/privacidad" className="p-1.5" data-testid="pie-privacidad">
                  Privacidad
                </Link>
              </div>
            </div>
          </TarjetaC4>
        </div>
      </div>
    </div>
  )
}
