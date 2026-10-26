import type { ReactNode } from 'react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { nivelCorredor } from '../../../../shared/kernel/nivel'
import { BotonTexto, Dialogo, Interruptor } from '../../../../shared/ui/ui'
import { useApp } from '../../../../store/app'
import { descartarCarrera } from '../../../actividad/api'
import { InsigniaNivel } from '../../../comunidad'
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
    <div className="flex items-center justify-between gap-3 px-4 py-3 bg-surface-container rounded-xl">
      <div className="flex items-center gap-3 min-w-0">
        <span className="w-9 h-9 rounded-lg bg-surface-container-highest flex items-center justify-center material-symbols-outlined text-[20px] text-text-secondary">
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

  const sesion = useApp((s) => s.sesion)
  const carreras = useApp((s) => s.carreras)
  const km = carreras.reduce((a, c) => a + c.distanciaKm, 0)
  const nombre = sesion?.nombre ?? 'Corredor'
  const m = ajustes.unidadesMetricas
  const ritmo = ajustes.ritmoObjetivoSeg
  const ritmoTxt = `${String(Math.floor(ritmo / 60)).padStart(2, '0')}:${String(ritmo % 60).padStart(2, '0')}`

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
          <select
            aria-label="Ritmo objetivo"
            value={ritmo}
            onChange={(e) => void actualizar({ ritmoObjetivoSeg: Number(e.target.value) })}
            className="rounded-lg bg-surface-container-highest text-primary font-mono px-2 py-1 border border-border-subtle"
          >
            {Array.from({ length: 25 }, (_, i) => 210 + i * 15).map((s) => (
              <option key={s} value={s}>
                {`${Math.floor(s / 60)}:${String(s % 60).padStart(2, '0')} /${m ? 'km' : 'mi'}`}
              </option>
            ))}
          </select>
          <span className="sr-only">{ritmoTxt}</span>
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
          <select
            aria-label="Cuenta regresiva"
            value={ajustes.cuentaRegresivaSeg}
            onChange={(e) => void actualizar({ cuentaRegresivaSeg: Number(e.target.value) })}
            className="rounded-lg bg-surface-container-highest text-success font-mono px-2 py-1 border border-border-subtle"
          >
            {[0, 3, 5, 10].map((n) => (
              <option key={n} value={n}>
                {n === 0 ? 'Sin cuenta' : `${n} seg`}
              </option>
            ))}
          </select>
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
            className="px-3 py-1 rounded-lg bg-surface-container-highest text-primary text-label-md font-semibold"
          >
            {ajustes.modoOscuro ? 'Oscuro' : 'Claro'}
          </button>
        </Fila>
        <Fila
          icono="language"
          titulo="Idioma de la aplicación"
          descripcion="Configuración regional"
        >
          <select
            aria-label="Idioma"
            value={ajustes.idioma}
            onChange={(e) => void actualizar({ idioma: e.target.value as 'es-EC' | 'en' })}
            className="rounded-lg bg-surface-container-highest text-text-primary px-2 py-1 border border-border-subtle"
          >
            <option value="es-EC">Español (Ecuador)</option>
            <option value="en">English</option>
          </select>
        </Fila>
      </Seccion>

      <Seccion titulo="Rachas" etiqueta="Constancia">
        <Fila
          icono="local_fire_department"
          titulo="Meta semanal"
          descripcion="Carreras por semana para mantener la racha"
        >
          <select
            aria-label="Meta semanal de carreras"
            value={ajustes.metaSemanal}
            onChange={(e) => void actualizar({ metaSemanal: Number(e.target.value) })}
            className="rounded-lg bg-surface-container-highest text-text-primary px-2 py-1 border border-border-subtle"
          >
            {[1, 2, 3, 4, 5, 6, 7].map((n) => (
              <option key={n} value={n}>
                {n}
              </option>
            ))}
          </select>
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
