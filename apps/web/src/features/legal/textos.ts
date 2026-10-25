/**
 * Textos de Terminos y Politica de privacidad adaptados a la LOPDP de Ecuador.
 * Copia de TextosLegales.kt (Android/iOS): mantener ambas iguales. Revision de abogado
 * pendiente antes de publicar (docs/LEGAL.md).
 */
export interface SeccionLegal {
  titulo: string
  texto: string
}

export const VERSIONES_LEGALES = {
  terminos: '2026-10-25',
  privacidad: '2026-10-25',
  gps_carrera: '2026-10-25',
  ubicacion_visible: '2026-10-25',
} as const

export const TERMINOS: SeccionLegal[] = [
  {
    titulo: '1. Aceptación',
    texto:
      'Al crear una cuenta en Gacelas aceptas estos Términos. Si no estás de acuerdo, no uses la aplicación. Debes tener al menos 15 años; si eres menor de edad necesitas la autorización de tu representante legal.',
  },
  {
    titulo: '2. El servicio',
    texto:
      'Gacelas permite registrar carreras con GPS, guardar y compartir rutas, unirte a clubes y eventos y, si lo decides, mostrar tu ubicación a otros corredores. El servicio se ofrece tal cual y puede cambiar o interrumpirse.',
  },
  {
    titulo: '3. Tu cuenta',
    texto:
      'Eres responsable de la información que publicas y de mantener segura tu contraseña. Puedes eliminar tu cuenta en cualquier momento desde Ajustes, Privacidad.',
  },
  {
    titulo: '4. Uso aceptable',
    texto:
      'No publiques contenido ofensivo, falso, ilegal o que vulnere derechos de terceros. No acoses a otros corredores ni uses su ubicación para fines distintos al deporte. Podemos retirar contenido reportado y suspender cuentas que incumplan estas normas.',
  },
  {
    titulo: '5. Salud y seguridad',
    texto:
      'Gacelas no es un servicio médico. Corre con precaución, respeta las normas de tránsito y consulta a un profesional de la salud antes de iniciar un entrenamiento exigente.',
  },
  {
    titulo: '6. Contenido',
    texto:
      'Conservas los derechos sobre tus fotos, rutas y textos. Nos concedes una licencia limitada para mostrarlos dentro de la aplicación según la visibilidad que elijas.',
  },
  {
    titulo: '7. Responsabilidad',
    texto:
      'En la medida que permita la ley ecuatoriana, Gacelas no responde por lesiones, pérdidas o daños derivados del uso de la aplicación o de la información publicada por otros usuarios.',
  },
  {
    titulo: '8. Cambios',
    texto:
      'Si cambiamos estos Términos te pediremos que los aceptes de nuevo antes de seguir usando la aplicación. La versión vigente y su fecha se muestran al inicio de este documento.',
  },
  {
    titulo: '9. Ley aplicable',
    texto:
      'Estos Términos se rigen por las leyes de la República del Ecuador. Cualquier controversia se resolverá ante los jueces competentes de la ciudad de Loja.',
  },
]

export const PRIVACIDAD: SeccionLegal[] = [
  {
    titulo: '1. Responsable',
    texto:
      'El responsable del tratamiento es el equipo de Gacelas (Loja, Ecuador). Puedes escribirnos para ejercer tus derechos al correo de contacto indicado en la tienda de aplicaciones.',
  },
  {
    titulo: '2. Datos que tratamos',
    texto:
      'Datos de cuenta (nombre, correo, foto), datos de actividad (carreras, rutas, tiempos, ritmo, desnivel), datos de ubicación GPS y datos técnicos del dispositivo necesarios para que la aplicación funcione.',
  },
  {
    titulo: '3. Finalidades y base legal',
    texto:
      'Prestarte el servicio (ejecución del contrato), registrar tus carreras con GPS (tu consentimiento), mostrar tu ubicación en el mapa en vivo (consentimiento separado y opcional) y mantener la seguridad de la comunidad (interés legítimo).',
  },
  {
    titulo: '4. Ubicación durante la carrera',
    texto:
      'Solo mientras grabas una carrera usamos tu GPS para calcular distancia, ritmo y trazado. El recorrido se guarda en tu cuenta con la visibilidad que elijas y puedes ocultar el inicio y final cerca de tu casa.',
  },
  {
    titulo: '5. Ubicación visible en el mapa en vivo',
    texto:
      'Si lo activas, otros corredores con sesión iniciada verán tu última posición, tu nombre, tu foto y si estás corriendo, mientras uses la aplicación. La posición se actualiza como mucho cada 15 segundos y deja de mostrarse a los 2 minutos sin actualizar. Puedes desactivarlo en cualquier momento en Privacidad y tu posición se borra al instante.',
  },
  {
    titulo: '6. Con quién compartimos',
    texto:
      'Usamos Supabase como proveedor de base de datos y autenticación y servicios de mapas para mostrar teselas. No vendemos tus datos ni los usamos para publicidad. Estos proveedores pueden estar fuera de Ecuador con garantías adecuadas según la LOPDP.',
  },
  {
    titulo: '7. Conservación',
    texto:
      'Conservamos tus datos mientras tengas una cuenta. Las posiciones del mapa en vivo se ocultan a los 2 minutos y se sobrescriben con la siguiente. Al eliminar tu cuenta borramos tus datos personales, salvo los que la ley obligue a conservar.',
  },
  {
    titulo: '8. Tus derechos',
    texto:
      'Según la LOPDP puedes acceder, rectificar, actualizar, eliminar y portar tus datos, oponerte a su tratamiento y revocar tus consentimientos. Desde Privacidad puedes descargar tus datos, revocar consentimientos y eliminar tu cuenta. También puedes reclamar ante la Superintendencia de Protección de Datos Personales.',
  },
  {
    titulo: '9. Seguridad',
    texto:
      'Aplicamos cifrado en tránsito, control de acceso por filas en la base de datos y el principio de mínimo privilegio. Si ocurre una vulneración que afecte tus datos te lo notificaremos conforme a la ley.',
  },
  {
    titulo: '10. Cambios',
    texto:
      'Si cambiamos esta Política te lo avisaremos en la aplicación. La versión vigente y su fecha se muestran al inicio de este documento.',
  },
]

export const TEXTO_GPS_CARRERA =
  'Usaremos el GPS de tu teléfono solo mientras grabas una carrera para calcular distancia, ritmo y trazado. El recorrido se guarda en tu cuenta con la visibilidad que elijas. Puedes revocarlo en Privacidad.'
export const TEXTO_UBICACION_VISIBLE =
  'Otros corredores con sesión iniciada verán en el mapa tu última posición, tu nombre, tu foto y si estás corriendo, mientras uses la app. Se actualiza como mucho cada 15 s y se oculta a los 2 minutos sin actualizar. Puedes desactivarlo cuando quieras y tu posición se borra al instante.'
