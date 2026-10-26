import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { ModalC2 } from '../../../components/modales'
import { FilaInterruptor, PantallaAjustes, Seccion } from '../../identidad'
import { dejarDeCompartir } from '../../../features/comunidad/domain/mapaVivo'
import {
  eliminarCuenta,
  historial,
  otorgado,
  registrarConsentimiento,
  requiereAceptarTerminos,
  terminosRemotos,
  vigente,
} from '../infrastructure/consentimientos'
import { PRIVACIDAD, TERMINOS, TEXTO_UBICACION_VISIBLE, VERSIONES_LEGALES } from '../domain/textos'

function Documento({ terminos }: { terminos: boolean }) {
  const secciones = terminos ? TERMINOS : PRIVACIDAD
  const version = terminos ? VERSIONES_LEGALES.terminos : VERSIONES_LEGALES.privacidad
  return (
    <PantallaAjustes
      titulo={terminos ? 'Términos y condiciones' : 'Política de privacidad'}
      volverA={-1}
    >
      <div
        data-testid={terminos ? 'doc-terminos' : 'doc-privacidad'}
        className="p-4 rounded-xl bg-[#191F2F] border border-[#2C3446]"
      >
        <p className="text-[#C084FC] font-semibold text-sm">Versión {version}</p>
        <p className="text-[#A8B0C2] text-sm mt-1">
          Adaptado a la Ley Orgánica de Protección de Datos Personales del Ecuador.
        </p>
      </div>
      {secciones.map((s) => (
        <section key={s.titulo}>
          <h2 className="font-title-md text-title-md text-text-primary">{s.titulo}</h2>
          <p className="text-[#A8B0C2] text-body-md mt-1 leading-relaxed">{s.texto}</p>
        </section>
      ))}
    </PantallaAjustes>
  )
}

export const Terminos = () => <Documento terminos />
export const PoliticaPrivacidad = () => <Documento terminos={false} />

/** Pide aceptar los Terminos si la version aceptada en Supabase (o, sin conexion, en el dispositivo) no es la vigente. */
export function ReaceptarTerminos() {
  const navigate = useNavigate()
  const [pedir, setPedir] = useState(() => {
    const h = historial()
    return vigente(h, 'terminos') != null && requiereAceptarTerminos(h)
  })
  // La fuente de verdad es la version aceptada en Supabase (consents), no solo el dispositivo.
  useEffect(() => {
    let vivo = true
    void terminosRemotos().then((r) => {
      if (vivo && r !== undefined) setPedir(requiereAceptarTerminos(r ? [r] : []))
    })
    return () => {
      vivo = false
    }
  }, [])
  const aceptar = async () => {
    await registrarConsentimiento('terminos', true)
    await registrarConsentimiento('privacidad', true)
    setPedir(false)
  }
  return (
    <ModalC2
      abierto={pedir}
      icono="gavel"
      titulo="Actualizamos los Términos"
      texto={`Publicamos una nueva versión (${VERSIONES_LEGALES.terminos}) de los Términos y la Política de privacidad. Léelos y acéptalos para seguir usando Gacelas.`}
      primario="Aceptar"
      onPrimario={() => void aceptar()}
      onCerrar={() => undefined}
      secundario="Leer términos"
      onSecundario={() => navigate('/legal/terminos')}
    />
  )
}

/** Enlaces pequenos a los documentos legales (pie del login). */
export function EnlacesLegales() {
  return (
    <div className="flex justify-center gap-4 text-xs text-[#A8B0C2]">
      <Link to="/legal/terminos" className="py-2 hover:text-text-primary">
        Términos
      </Link>
      <Link to="/legal/privacidad" className="py-2 hover:text-text-primary">
        Privacidad
      </Link>
    </div>
  )
}

/** Consentimientos revocables y eliminacion de cuenta, dentro de Privacidad. */
export function SeccionConsentimientos() {
  const navigate = useNavigate()
  const [gps, setGps] = useState(() => otorgado(historial(), 'gps_carrera'))
  const [visible, setVisible] = useState(() => otorgado(historial(), 'ubicacion_visible'))
  const [explicar, setExplicar] = useState(false)
  const [eliminar, setEliminar] = useState(false)
  const [error, setError] = useState<string | null>(null)
  useEffect(() => setError(null), [eliminar])

  const cambiarVisible = (v: boolean) => {
    if (v) return setExplicar(true)
    setVisible(false)
    void registrarConsentimiento('ubicacion_visible', false).then(() => dejarDeCompartir())
  }
  const confirmarEliminar = async () => {
    if (await eliminarCuenta()) {
      localStorage.clear()
      navigate('/bienvenida', { replace: true })
      location.reload()
    } else setError('No se pudo eliminar la cuenta. Inténtalo con conexión y sesión iniciada.')
  }

  return (
    <>
      <Seccion titulo="Consentimientos">
        <FilaInterruptor
          icono="my_location"
          color="text-[#C084FC]"
          titulo="Seguimiento GPS en carrera"
          descripcion="Usar tu GPS mientras grabas una carrera"
          activo={gps}
          onCambiar={(v) => {
            setGps(v)
            void registrarConsentimiento('gps_carrera', v)
          }}
        />
        <FilaInterruptor
          icono="sensors"
          color="text-[#68DBA9]"
          titulo="Ubicación visible"
          descripcion="Aparecer en el mapa en vivo para otros corredores"
          activo={visible}
          onCambiar={cambiarVisible}
        />
      </Seccion>
      <Seccion titulo="Documentos legales">
        <div className="grid grid-cols-2 gap-2">
          <Link
            to="/legal/terminos"
            className="px-4 py-3 bg-surface-container rounded-xl text-text-primary font-semibold"
          >
            Términos
          </Link>
          <Link
            to="/legal/privacidad"
            className="px-4 py-3 bg-surface-container rounded-xl text-text-primary font-semibold"
          >
            Privacidad
          </Link>
        </div>
        <button
          onClick={() => setEliminar(true)}
          className="w-full flex items-center gap-3 px-4 py-3 bg-surface-container rounded-xl text-left text-[#F472B6] font-semibold"
        >
          <span className="material-symbols-outlined">delete_forever</span>
          Eliminar cuenta y datos
        </button>
      </Seccion>
      <ModalC2
        abierto={explicar}
        icono="sensors"
        titulo="¿Mostrar tu ubicación?"
        texto={TEXTO_UBICACION_VISIBLE}
        primario="Activar"
        onPrimario={() => {
          setExplicar(false)
          setVisible(true)
          void registrarConsentimiento('ubicacion_visible', true)
        }}
        onCerrar={() => setExplicar(false)}
        secundario="Ahora no"
        onSecundario={() => setExplicar(false)}
      />
      <ModalC2
        abierto={eliminar}
        icono="delete_forever"
        titulo="¿Eliminar tu cuenta?"
        texto={
          error ??
          'Se borrarán tu perfil, carreras, rutas, publicaciones y consentimientos. Esta acción no se puede deshacer.'
        }
        primario="Eliminar"
        peligro
        onPrimario={() => void confirmarEliminar()}
        onCerrar={() => setEliminar(false)}
        secundario="Cancelar"
        onSecundario={() => setEliminar(false)}
      />
    </>
  )
}
