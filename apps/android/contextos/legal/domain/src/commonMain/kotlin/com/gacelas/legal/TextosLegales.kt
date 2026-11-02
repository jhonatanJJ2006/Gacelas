package com.gacelas.legal

/** Seccion de un documento legal. */
data class SeccionLegal(val titulo: String, val texto: String)

/**
 * Textos de Terminos y Politica de privacidad, adaptados a la Ley Organica de Proteccion de
 * Datos Personales (LOPDP) de Ecuador. Deben revisarse con un abogado antes de publicarse
 * (docs/legal.md). La web usa la misma copia en apps/web/src/contexts/legal/textos.ts.
 */
object TextosLegales {
    val terminos = listOf(
        SeccionLegal("1. Aceptación", "Al crear una cuenta en Runcity aceptas estos Términos. Si no estás de acuerdo, no uses la aplicación. Debes tener al menos 15 años; si eres menor de edad necesitas la autorización de tu representante legal."),
        SeccionLegal("2. El servicio", "Runcity permite registrar carreras con GPS, guardar y compartir rutas, unirte a clubes y eventos y, si lo decides, mostrar tu ubicación a otros corredores. El servicio se ofrece tal cual y puede cambiar o interrumpirse."),
        SeccionLegal("3. Tu cuenta", "Eres responsable de la información que publicas y de mantener segura tu contraseña. Puedes eliminar tu cuenta en cualquier momento desde Ajustes, Privacidad."),
        SeccionLegal("4. Uso aceptable", "No publiques contenido ofensivo, falso, ilegal o que vulnere derechos de terceros. No acoses a otros corredores ni uses su ubicación para fines distintos al deporte. Podemos retirar contenido reportado y suspender cuentas que incumplan estas normas."),
        SeccionLegal("5. Salud y seguridad", "Runcity no es un servicio médico. Corre con precaución, respeta las normas de tránsito y consulta a un profesional de la salud antes de iniciar un entrenamiento exigente."),
        SeccionLegal("6. Contenido", "Conservas los derechos sobre tus fotos, rutas y textos. Nos concedes una licencia limitada para mostrarlos dentro de la aplicación según la visibilidad que elijas."),
        SeccionLegal("7. Responsabilidad", "En la medida que permita la ley ecuatoriana, Runcity no responde por lesiones, pérdidas o daños derivados del uso de la aplicación o de la información publicada por otros usuarios."),
        SeccionLegal("8. Cambios", "Si cambiamos estos Términos te pediremos que los aceptes de nuevo antes de seguir usando la aplicación. La versión vigente y su fecha se muestran al inicio de este documento."),
        SeccionLegal("9. Ley aplicable", "Estos Términos se rigen por las leyes de la República del Ecuador. Cualquier controversia se resolverá ante los jueces competentes de la ciudad de Loja."),
    )

    val privacidad = listOf(
        SeccionLegal("1. Responsable", "El responsable del tratamiento es el equipo de Runcity (Loja, Ecuador). Puedes escribirnos para ejercer tus derechos al correo de contacto indicado en la tienda de aplicaciones."),
        SeccionLegal("2. Datos que tratamos", "Datos de cuenta (nombre, correo, foto), datos de actividad (carreras, rutas, tiempos, ritmo, desnivel), datos de ubicación GPS y datos técnicos del dispositivo necesarios para que la aplicación funcione."),
        SeccionLegal("3. Finalidades y base legal", "Prestarte el servicio (ejecución del contrato), registrar tus carreras con GPS (tu consentimiento), mostrar tu ubicación en el mapa en vivo (consentimiento separado y opcional) y mantener la seguridad de la comunidad (interés legítimo)."),
        SeccionLegal("4. Ubicación durante la carrera", "Solo mientras grabas una carrera usamos tu GPS para calcular distancia, ritmo y trazado. El recorrido se guarda en tu cuenta con la visibilidad que elijas y puedes ocultar el inicio y final cerca de tu casa."),
        SeccionLegal("5. Ubicación visible en el mapa en vivo", "Si lo activas, otros corredores con sesión iniciada verán tu última posición, tu nombre, tu foto y si estás corriendo, mientras uses la aplicación. La posición se actualiza como mucho cada 15 segundos y deja de mostrarse a los 2 minutos sin actualizar. Puedes desactivarlo en cualquier momento en Privacidad y tu posición se borra al instante."),
        SeccionLegal("6. Reportes de incidentes", "Si reportas un incidente guardamos el tipo, la descripción y las fotos opcionales, el punto GPS donde estás, la dirección aproximada y la hora, junto a tu cuenta (base legal: tu consentimiento de ubicación). Las fotos se suben sin metadatos EXIF. Otros usuarios ven el reporte sin tu nombre, con la ubicación redondeada a unos 100 metros y la hora redondeada a la hora; la descripción solo se muestra si moderación la verifica. Los reportes dejan de mostrarse a los 90 días y puedes borrarlos cuando quieras. Para el indicador de seguridad de las rutas usamos además datos abiertos y agregados del SIS ECU 911 (licencia CC-BY), que no contienen datos personales."),
        SeccionLegal("7. Con quién compartimos", "Usamos Supabase como proveedor de base de datos y autenticación y servicios de mapas para mostrar teselas. Para el clima de Inicio enviamos a Open-Meteo solo coordenadas aproximadas (redondeadas a unos 1 km), o las de Loja si no diste permiso de ubicación; no pasan por nuestros servidores. No vendemos tus datos ni los usamos para publicidad. Estos proveedores pueden estar fuera de Ecuador con garantías adecuadas según la LOPDP."),
        SeccionLegal("8. Conservación", "Conservamos tus datos mientras tengas una cuenta. Las posiciones del mapa en vivo se ocultan a los 2 minutos y se sobrescriben con la siguiente. Al eliminar tu cuenta borramos tus datos personales, salvo los que la ley obligue a conservar."),
        SeccionLegal("9. Tus derechos", "Según la LOPDP puedes acceder, rectificar, actualizar, eliminar y portar tus datos, oponerte a su tratamiento y revocar tus consentimientos. Desde Privacidad puedes descargar tus datos, revocar consentimientos y eliminar tu cuenta. También puedes reclamar ante la Superintendencia de Protección de Datos Personales."),
        SeccionLegal("10. Seguridad", "Aplicamos cifrado en tránsito, control de acceso por filas en la base de datos y el principio de mínimo privilegio. Si ocurre una vulneración que afecte tus datos te lo notificaremos conforme a la ley."),
        SeccionLegal("11. Cambios", "Si cambiamos esta Política te lo avisaremos en la aplicación. La versión vigente y su fecha se muestran al inicio de este documento."),
    )

    const val GPS_CARRERA = "Usaremos el GPS de tu teléfono solo mientras grabas una carrera para calcular distancia, ritmo y trazado. El recorrido se guarda en tu cuenta con la visibilidad que elijas. Puedes revocarlo en Privacidad."
    const val UBICACION_VISIBLE = "Otros corredores con sesión iniciada verán en el mapa tu última posición, tu nombre, tu foto y si estás corriendo, mientras uses la app. Se actualiza como mucho cada 15 s y se oculta a los 2 minutos sin actualizar. Puedes desactivarlo cuando quieras y tu posición se borra al instante."
}
