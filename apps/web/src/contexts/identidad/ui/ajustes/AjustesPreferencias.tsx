import type { ReactNode } from 'react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { nivelCorredor } from '../../../../shared/kernel/nivel'
import { BotonTexto, Dialogo, Interruptor } from '../../../../shared/ui/ui'
import { descartarCarrera, useActividad } from '../../../actividad/api'
import { InsigniaNivel } from '../../../comunidad'
import { useIdentidad } from '../../application/estado'
import { FotoPerfil } from '../perfil/Perfil'
import { PantallaAjustes, Seccion } from './Comunes'

export const VERSION_APP = '1.0.0'

function Fila({
  icono,
  titulo,
  descripcion,
  children,
}: {
  icono: string
  titulo: string
  descripcion: string
  children: ReactNode
}) {
  return (
    <div className="flex items-center justify-between gap-3 px-4 py-3.5">
      <div className="flex items-center gap-3 min-w-0">
        <span className="w-[34px] h-[34px] shrink-0 rounded bg-c2-s2 flex items-center justify-center material-symbols-outlined fill text-[18px] text-primary-500">
          {icono}
        </span>
        <div className="min-w-0">
          <div className="font-body-md text-body-md text-text-primary font-semibold">{titulo}</div>
          <p className="font-body-sm text-body-sm text-text-secondary">{descripcion}</p>
        </div>
      </div>
      {children}
    </div>
  )
}

function Enlace({ to, icono, texto }: { to: string; icono: string; texto: string }) {
  return (
    <Link
      to={to}
      className="flex items-center justify-between px-4 py-3.5 hover:bg-white/5 transition-colors"
    >
      <div className="flex items-center gap-3">
        <span className="w-[34px] h-[34px] shrink-0 rounded bg-c2-s2 flex items-center justify-center material-symbols-outlined fill text-[18px] text-primary-500">
          {icono}
        </span>
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
      className="w-full flex items-center justify-between px-4 py-3.5 hover:bg-white/5 transition-colors"
    >
      <div className="flex items-center gap-3">
        <span className="w-[34px] h-[34px] shrink-0 rounded bg-c2-s2 flex items-center justify-center material-symbols-outlined fill text-[18px] text-primary-500">
          {icono}
        </span>
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
  const ajustes = useIdentidad((s) => s.ajustes)
  const actualizar = useIdentidad((s) => s.actualizarAjustes)
  const cerrarSesion = useIdentidad((s) => s.cerrarSesion)
  const [dialogo, setDialogo] = useState<'acerca' | 'ayuda' | 'salir' | null>(null)
  const cerrar = () => setDialogo(null)

  const sesion = useIdentidad((s) => s.sesion)
  const carreras = useActividad((s) => s.carreras)
  const km = carreras.reduce((a, c) => a + c.distanciaKm, 0)
  const nombre = sesion?.nombre ?? 'Corredor'
  const m = ajustes.unidadesMetricas
  const ritmo = ajustes.ritmoObjetivoSeg

  return (
    <PantallaAjustes titulo="Ajustes" subtitulo="Club Gacelas Loja • 2,100 msnm" volverA="/perfil">
      <div className="flex items-center gap-4 p-4 tarjeta-c2">
        <FotoPerfil foto={sesion?.foto} nombre={nombre} className="w-14 h-14" />
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2 flex-wrap">
            <span className="font-title-md text-title-md text-text-primary truncate">{nombre}</span>
            <InsigniaNivel nivel={nivelCorredor(km)} />
          </div>
          {sesion?.email && (
            <p className="text-body-sm text-text-secondary truncate">{sesion.email}</p>
          )}
          <p className="text-body-sm text-success" data-testid="ajustes-km">
            Sede Loja • {Math.round(km)} km totales
          </p>
        </div>
        <Link
          to="/perfil/editar"
          className="px-3 py-2 rounded-lg bg-primary-600/15 text-primary text-label-md font-semibold"
        >
          Editar perfil
        </Link>
      </div>

      <Seccion titulo="Preferencias de carrera" etiqueta="Entrenamiento">
        <Fila
          icono="straighten"
          titulo="Unidades de distancia"
          descripcion="Sistema métrico para rutas andinas"
        >
          <div
            role="group"
            aria-label="Unidades de distancia"
            className="flex rounded-lg bg-surface-container-highest p-0.5"
          >
            {[
              ['km', true],
              ['mi', false],
            ].map(([t, v]) => (
              <button
                key={String(t)}
                aria-pressed={m === v}
                onClick={() => void actualizar({ unidadesMetricas: v as boolean })}
                className={`px-3 py-1 rounded-md text-label-md font-semibold ${m === v ? 'bg-primary-600 text-on-primary' : 'text-text-secondary'}`}
              >
                {t}
              </button>
            ))}
          </div>
        </Fila>
        <Fila
          icono="speed"
          titulo="Objetivo de ritmo (Pacer)"
          descripcion="Ritmo objetivo predeterminado"
        >
          {/* Android Paso: menos / valor / mas en pasos de 15 s (3:00 a 10:00). */}
          <div className="flex items-center" data-testid="paso-ritmo">
            <button
              aria-label="Bajar ritmo objetivo"
              onClick={() => void actualizar({ ritmoObjetivoSeg: Math.max(180, ritmo - 15) })}
              className="w-10 h-10 flex items-center justify-center text-primary"
            >
              <span className="material-symbols-outlined">remove</span>
            </button>
            <span className="text-label-md font-semibold text-primary whitespace-nowrap">
              {`${Math.floor(ritmo / 60)}:${String(ritmo % 60).padStart(2, '0')} /${m ? 'km' : 'mi'}`}
            </span>
            <button
              aria-label="Subir ritmo objetivo"
              onClick={() => void actualizar({ ritmoObjetivoSeg: Math.min(600, ritmo + 15) })}
              className="w-10 h-10 flex items-center justify-center text-primary"
            >
              <span className="material-symbols-outlined">add</span>
            </button>
          </div>
        </Fila>
        <Fila
          icono="motion_photos_paused"
          titulo="Autopausa"
          descripcion="Detener al parar en semáforos o cruces"
        >
          <Interruptor
            etiqueta="Autopausa"
            activo={ajustes.autopausa}
            onCambiar={(v) => void actualizar({ autopausa: v })}
          />
        </Fila>
        <Fila
          icono="record_voice_over"
          titulo="Anuncios de voz"
          descripcion={`Métricas audibles cada 1 ${m ? 'km' : 'mi'}`}
        >
          <Interruptor
            etiqueta="Anuncios de voz"
            activo={ajustes.anunciosVoz}
            onCambiar={(v) => void actualizar({ anunciosVoz: v })}
          />
        </Fila>
        <Fila
          icono="timer"
          titulo="Cuenta regresiva de inicio"
          descripcion="Tiempo antes de arrancar el GPS"
        >
          <button
            aria-label="Cuenta regresiva"
            data-testid="cuenta-regresiva"
            onClick={() => {
              const op = [0, 3, 5, 10]
              const i = op.indexOf(ajustes.cuentaRegresivaSeg)
              void actualizar({ cuentaRegresivaSeg: op[(i + 1) % op.length] })
            }}
            className="rounded-lg bg-surface-container-highest text-secondary-500 text-label-md font-semibold px-2.5 py-1 whitespace-nowrap"
          >
            {ajustes.cuentaRegresivaSeg === 0 ? 'Sin cuenta' : `${ajustes.cuentaRegresivaSeg} seg`}
          </button>
        </Fila>
      </Seccion>

      <Seccion titulo="Apariencia" etiqueta="Interfaz">
        <Fila
          icono="dark_mode"
          titulo="Tema"
          descripcion="Optimizado para salidas nocturnas y al alba"
        >
          <button
            onClick={() => void actualizar({ modoOscuro: !ajustes.modoOscuro })}
            aria-label="Cambiar tema"
            className="text-primary text-label-md font-semibold"
          >
            {ajustes.modoOscuro ? 'Oscuro' : 'Claro'}
          </button>
        </Fila>
        <Fila
          icono="language"
          titulo="Idioma de la aplicación"
          descripcion="Configuración regional"
        >
          <button
            aria-label="Idioma"
            onClick={() => void actualizar({ idioma: ajustes.idioma === 'en' ? 'es-EC' : 'en' })}
            className="text-label-md font-semibold text-text-primary whitespace-nowrap"
          >
            {ajustes.idioma === 'en' ? 'English' : 'Español (Ecuador)'}
          </button>
        </Fila>
      </Seccion>

      <Seccion titulo="Rachas" etiqueta="Constancia">
        <Fila
          icono="local_fire_department"
          titulo="Meta semanal"
          descripcion="Carreras por semana para mantener la racha"
        >
          <div className="flex items-center" aria-label="Meta semanal de carreras" role="group">
            <button
              aria-label="Menos carreras por semana"
              onClick={() => void actualizar({ metaSemanal: Math.max(1, ajustes.metaSemanal - 1) })}
              className="w-10 h-10 flex items-center justify-center text-primary-500"
            >
              <span className="material-symbols-outlined">remove</span>
            </button>
            <span className="font-title-md text-title-md text-text-primary">
              {ajustes.metaSemanal}
            </span>
            <button
              aria-label="Más carreras por semana"
              onClick={() => void actualizar({ metaSemanal: Math.min(7, ajustes.metaSemanal + 1) })}
              className="w-10 h-10 flex items-center justify-center text-primary-500"
            >
              <span className="material-symbols-outlined">add</span>
            </button>
          </div>
        </Fila>
      </Seccion>

      <Seccion titulo="Cuenta" etiqueta="Seguridad y dispositivos">
        <Enlace to="/ajustes/privacidad" icono="lock" texto="Privacidad y ubicación" />
        <Enlace to="/ajustes/notificaciones" icono="notifications" texto="Notificaciones" />
        <Enlace to="/ajustes/mapas-offline" icono="download" texto="Mapas offline" />
        <Enlace to="/ajustes/brujula" icono="explore" texto="Brújula" />
        <Boton
          onClick={() => setDialogo('acerca')}
          icono="info"
          texto="Acerca de"
          detalle={`v${VERSION_APP}`}
        />
        <Boton onClick={() => setDialogo('ayuda')} icono="help" texto="Ayuda y soporte" />
        <Enlace to="/legal/terminos" icono="gavel" texto="Términos y condiciones" />
        <Enlace to="/legal/privacidad" icono="policy" texto="Política de privacidad" />
      </Seccion>
      <section>
        <button
          onClick={() => setDialogo('salir')}
          className="w-full flex items-center justify-center gap-2 px-4 py-3 bg-danger/20 text-red-300 rounded-xl hover:bg-danger/30 transition-colors"
        >
          <span className="material-symbols-outlined">logout</span>
          <span className="font-body-md text-body-md font-medium">Cerrar sesión</span>
        </button>
      </section>

      <Dialogo
        titulo="Runcity"
        abierto={dialogo === 'acerca'}
        onCerrar={cerrar}
        acciones={<BotonTexto onClick={cerrar}>Cerrar</BotonTexto>}
      >
        <p>
          Versión {VERSION_APP}. App del grupo de running Runcity de Loja, Ecuador. Graba tus
          carreras con GPS, descubre rutas y entrena con la comunidad.
        </p>
        <p className="mt-3">
          Datos del clima:{' '}
          <a href="https://open-meteo.com/" target="_blank" rel="noreferrer" className="underline">
            Open-Meteo.com
          </a>{' '}
          (CC BY 4.0).
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
