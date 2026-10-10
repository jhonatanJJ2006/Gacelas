import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { BotonTexto, Dialogo } from '../../components/ui'
import { descartarCarrera } from '../../store/carrera'
import { useApp } from '../../store/app'
import { FilaInterruptor, PantallaAjustes, Seccion } from './Comunes'

export const VERSION_APP = '1.0.0'

function Enlace({ to, icono, texto }: { to: string; icono: string; texto: string }) {
  return (
    <Link
      to={to}
      className="flex items-center justify-between px-4 py-3 bg-surface-container rounded-xl hover:bg-surface-container-high transition-colors"
    >
      <div className="flex items-center gap-3">
        <span className="material-symbols-outlined text-text-secondary">{icono}</span>
        <span className="font-body-md text-body-md text-text-primary">{texto}</span>
      </div>
      <span className="material-symbols-outlined text-text-secondary">chevron_right</span>
    </Link>
  )
}

function Boton({
  onClick,
  icono,
  texto,
  detalle,
}: {
  onClick: () => void
  icono: string
  texto: string
  detalle?: string
}) {
  return (
    <button
      onClick={onClick}
      className="w-full flex items-center justify-between px-4 py-3 bg-surface-container rounded-xl hover:bg-surface-container-high transition-colors"
    >
      <div className="flex items-center gap-3">
        <span className="material-symbols-outlined text-text-secondary">{icono}</span>
        <span className="font-body-md text-body-md text-text-primary">{texto}</span>
      </div>
      <span className="text-body-sm text-text-secondary flex items-center">
        {detalle}
        <span className="material-symbols-outlined">chevron_right</span>
      </span>
    </button>
  )
}

export function AjustesPreferencias() {
  const navigate = useNavigate()
  const ajustes = useApp((s) => s.ajustes)
  const actualizar = useApp((s) => s.actualizarAjustes)
  const cerrarSesion = useApp((s) => s.cerrarSesion)
  const [dialogo, setDialogo] = useState<'acerca' | 'ayuda' | 'salir' | null>(null)
  const cerrar = () => setDialogo(null)

  return (
    <PantallaAjustes titulo="Ajustes" volverA="/perfil">
      <Seccion titulo="Cuenta">
        <Enlace to="/perfil/editar" icono="person" texto="Editar perfil" />
      </Seccion>
      <Seccion titulo="Privacidad">
        <Enlace to="/ajustes/privacidad" icono="lock" texto="Privacidad y ubicación" />
        <Enlace to="/ajustes/notificaciones" icono="notifications" texto="Notificaciones" />
      </Seccion>
      <Seccion titulo="General">
        <FilaInterruptor
          icono="dark_mode"
          titulo="Modo oscuro"
          descripcion={ajustes.modoOscuro ? 'Activado' : 'Tema claro activo'}
          activo={ajustes.modoOscuro}
          onCambiar={(v) => void actualizar({ modoOscuro: v })}
        />
        <FilaInterruptor
          icono="straighten"
          titulo="Unidades métricas"
          descripcion={ajustes.unidadesMetricas ? 'Kilómetros y metros' : 'Millas'}
          activo={ajustes.unidadesMetricas}
          onCambiar={(v) => void actualizar({ unidadesMetricas: v })}
        />
      </Seccion>
      <Seccion titulo="Información">
        <Boton
          onClick={() => setDialogo('acerca')}
          icono="info"
          texto="Acerca de"
          detalle={`v${VERSION_APP}`}
        />
        <Boton onClick={() => setDialogo('ayuda')} icono="help" texto="Ayuda y soporte" />
      </Seccion>
      <section>
        <button
          onClick={() => setDialogo('salir')}
          className="w-full flex items-center justify-center gap-2 px-4 py-3 bg-danger/20 text-danger rounded-xl hover:bg-danger/30 transition-colors"
        >
          <span className="material-symbols-outlined">logout</span>
          <span className="font-body-md text-body-md font-medium">Cerrar sesión</span>
        </button>
      </section>

      <Dialogo
        titulo="Gacelas Loja"
        abierto={dialogo === 'acerca'}
        onCerrar={cerrar}
        acciones={<BotonTexto onClick={cerrar}>Cerrar</BotonTexto>}
      >
        <p>
          Versión {VERSION_APP}. App del grupo de running Gacelas de Loja, Ecuador. Graba tus
          carreras con GPS, descubre rutas y entrena con la comunidad.
        </p>
      </Dialogo>
      <Dialogo
        titulo="Ayuda y soporte"
        abierto={dialogo === 'ayuda'}
        onCerrar={cerrar}
        acciones={
          <>
            <BotonTexto onClick={cerrar}>Cerrar</BotonTexto>
            <a
              href="mailto:soporte@gacelas.app?subject=Ayuda%20con%20Gacelas"
              className="px-4 h-10 inline-flex items-center rounded-lg text-primary-500 hover:bg-primary-600/10 font-label-lg"
            >
              Escribir a soporte
            </a>
          </>
        }
      >
        <p>
          Para grabar una carrera toca el botón central Correr y permite la ubicación en el
          navegador. Las carreras se guardan en este navegador y aparecen en Perfil e Inicio.
        </p>
      </Dialogo>
      <Dialogo
        titulo="¿Cerrar sesión?"
        abierto={dialogo === 'salir'}
        onCerrar={cerrar}
        acciones={
          <>
            <BotonTexto onClick={cerrar}>Cancelar</BotonTexto>
            <BotonTexto
              peligro
              onClick={async () => {
                descartarCarrera()
                await cerrarSesion()
                navigate('/bienvenida', { replace: true })
              }}
            >
              Cerrar sesión
            </BotonTexto>
          </>
        }
      >
        <p>Tus carreras siguen guardadas en este navegador.</p>
      </Dialogo>
    </PantallaAjustes>
  )
}
