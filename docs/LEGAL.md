# Términos, privacidad y consentimientos

Importante: los textos de Términos y condiciones y de Política de privacidad son un borrador adaptado a la Ley Orgánica de Protección de Datos Personales (LOPDP) de Ecuador. Deben revisarse con un abogado antes de publicar la aplicación.

## Dónde están los textos

- Android e iOS: `apps/android/shared/src/commonMain/kotlin/com/gacelas/legal/TextosLegales.kt`.
- Web: `apps/web/src/contexts/legal/domain/textos.ts`.

Las dos copias deben mantenerse iguales.

## Páginas web de los documentos

En la web cada documento tiene su propia URL y se lee sin iniciar sesión: `/legal/terminos` y `/legal/privacidad`. Son páginas normales, no modales. La presentación está en `apps/web/src/contexts/legal/ui/DocumentoLegal.tsx` (versión con fecha legible, índice de secciones, enlace al otro documento y estilos de impresión); solo cambia el diseño, los textos siguen en `textos.ts`. Se enlazan desde la landing (`/`), el registro, Ajustes y el aviso de nuevos Términos.

## Versiones

- La versión vigente está en `VersionesLegales` (Kotlin) y en `VERSIONES_LEGALES` (web), y en la tabla `legal_versions` de Supabase.
- Para publicar una versión nueva: cambia la constante en ambas plataformas e inserta la fila en `legal_versions`. Al abrir la app, quien aceptó una versión anterior debe aceptar la nueva.
- El panel de administración muestra la versión vigente.

## Consentimientos

La tabla `consents` (migración 019) guarda el historial: tipo, versión, otorgado o revocado, y fecha. Nunca se edita ni se borra: revocar es una fila nueva con `granted = false`. Con RLS, cada usuario solo ve y registra los suyos.

Tipos:

- `terminos` y `privacidad`: obligatorios al registrarse.
- `gps_carrera`: seguimiento GPS durante la carrera.
- `ubicacion_visible`: opcional, muestra la posición en el mapa en vivo. Al revocarlo se borra la fila de `runner_positions`.

## Eliminar la cuenta

La función `eliminar_mi_cuenta()` borra el usuario de `auth.users`. Sus datos se borran en cascada.

## Consentimiento de GPS al correr

Antes de la primera carrera se muestra un modal que explica qué se usa (GPS del dispositivo), para qué (distancia, ritmo y recorrido), con quién se comparte (nadie, salvo que publiques la carrera) y por cuánto tiempo (solo mientras grabas). La aceptación se guarda en la tabla consents con su versión y fecha, y se puede revocar en Ajustes, Privacidad.

## Reportes de incidentes

La Política de privacidad incluye la sección "6. Reportes de incidentes" (las siguientes se renumeraron). Resumen del tratamiento según la LOPDP:

- Qué: tipo, descripción y fotos opcionales, punto GPS, dirección aproximada y hora, ligados a la cuenta.
- Base legal: el consentimiento `gps_carrera`; sin él no se puede reportar.
- Minimización: la vista pública no muestra el usuario, redondea la ubicación a unos 100 m y la hora a la hora, y oculta la descripción hasta que moderación la verifica. Las fotos se suben sin EXIF y con nombres aleatorios.
- Conservación: los reportes dejan de publicarse a los 90 días. El autor puede borrarlos y se borran al eliminar la cuenta.
- Datos del ECU 911: son abiertos, agregados y sin datos personales (licencia CC-BY).

Al publicar este cambio hay que subir la versión de la Política en `VersionesLegales`, `VERSIONES_LEGALES` y `legal_versions`, para que los usuarios acepten el texto nuevo.
