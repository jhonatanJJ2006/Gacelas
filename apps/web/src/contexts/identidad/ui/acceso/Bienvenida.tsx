import { useNavigate } from 'react-router-dom'
import { VERSION_APP } from '../ajustes/AjustesPreferencias'
import {
  BotonC4,
  ChipC4,
  ETIQUETA_PORTADA,
  FondoMapaC4,
  IndicadorZonaC4,
  MarcaC4,
  PieC4,
  TarjetaC4,
} from './AccesoC4'
import { BotonGoogle } from './BotonGoogle'

/** Bienvenida C4, igual que Android BienvenidaC4 (AccesoPantallas.kt). */
export function Bienvenida() {
  const navigate = useNavigate()
  return (
    <div
      className="relative flex h-[100dvh] w-full flex-col overflow-hidden bg-[#0C1322]"
      data-testid="pantalla-bienvenida"
    >
      <FondoMapaC4 login={false} />
      <div className="relative z-10 mx-auto flex h-full w-full max-w-md flex-col">
        <header className="flex items-center px-5 pt-4 short:pt-2">
          <MarcaC4 />
          <span className="flex-1" />
          <ChipC4 texto={ETIQUETA_PORTADA} />
          <button
            type="button"
            onClick={() => navigate('/inicio-sesion')}
            className="ml-1 rounded-full p-2 text-[11px] font-bold tracking-[0.06em] text-[#A8B0C2] whitespace-nowrap"
          >
            Saltar
          </button>
        </header>
        <div className="flex min-h-0 flex-1 items-end px-5 pb-3">
          <IndicadorZonaC4 />
        </div>
        <div className="px-5 pt-2 pb-6 short:pb-3">
          <TarjetaC4>
            <div className="flex items-center gap-1.5">
              <span className="h-2 w-2 rounded-full bg-[#C084FC]" />
              <span className="text-[11px] font-extrabold uppercase tracking-[0.12em] text-[#C084FC]">
                Running en Loja, Ecuador
              </span>
            </div>
            <h1 className="mt-3 font-display text-[26px] leading-[30px] tracking-[0.015em] text-white">
              Corre. Comparte.
              <br className="short:hidden" /> <span className="text-white">Supera.</span>
            </h1>
            <p className="mt-2.5 text-[15px] leading-6 tracking-[-0.005em] text-[#A8B0C2]/90 short:line-clamp-2">
              Únete a la comunidad de runners en Loja. Explora rutas andinas, registra tus marcas y
              corre acompañado.
            </p>
            <div className="mt-6 flex flex-col gap-3 short:mt-4">
              <BotonC4 texto="Empezar" onClick={() => navigate('/registro')} />
              <BotonGoogle />
            </div>
            <PieC4 texto="¿Ya tienes cuenta?" enlace="Inicia sesión" a="/inicio-sesion" />
            <p
              className="pt-1.5 text-center text-[10px] text-[#A8B0C2]/50"
              data-testid="version-app"
            >
              Versión {VERSION_APP} (web)
            </p>
          </TarjetaC4>
        </div>
      </div>
    </div>
  )
}
