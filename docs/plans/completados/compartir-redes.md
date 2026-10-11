# Compartir rutas y carreras en redes

Estado: completado.

## Qué incluye

- Imagen de marca generada en el dispositivo: historia 1080x1920, post 1080x1080 y sticker 900x900 con fondo transparente. Trazo sobre mapa oscuro estilizado, distancia, tiempo, ritmo, desnivel, racha, logo y corredor.
- Web (`apps/web/src/lib/compartir.ts`, `components/CompartirDialogo.tsx`): Web Share API nivel 2 con archivo en móvil; en escritorio descarga, copiar enlace y `wa.me`.
- Android (`com.gacelas.compartir`): Bitmap con Canvas, `FileProvider` (`${applicationId}.archivos`) y selector `ACTION_SEND`; botón directo "Instagram Historia" (`com.instagram.share.ADD_TO_STORY`, fondo + sticker) y WhatsApp.
- Enlace público `https://gacelas.vercel.app/r/:id` (función de Vercel `apps/web/api/r.js`) con metadatos Open Graph y redirección a `/rutas/:id`. La vista previa es `share-images/rutas/<id>.png` (la sube el autor al compartir su ruta; migración `011_compartir.sql`) o `og-default.png`.
- Privacidad: al compartir una carrera se quitan los primeros y últimos 250 m del trazo.

## Pendiente del usuario

- Instagram pide un Facebook App ID en `source_application` para las historias. Definir `FACEBOOK_APP_ID` en `apps/android/local.properties`; sin él se envía el nombre del paquete y algunas versiones de Instagram pueden rechazarlo.

## Plantillas sobre tu foto (estilo Strava)

- Cuatro plantillas transparentes: trazo + datos, solo trazo, datos grandes y minimal. Trazo naranja, datos finos en blanco con sombra suave, marca GACELAS pequeña y @usuario (por defecto @gacelasrunningteam, editable o vacío).
- Compositor en web y Android: elegir foto de la galería o cámara, elegir plantilla y tamaño (historia 9:16, post 4:5 o 1:1), arrastrar y escalar los datos y exportar o compartir.
- Web: compartir con Web Share (archivos) o descargar; descarga del PNG transparente.
- Android: Instagram Historia envía la plantilla como sticker (`interactive_asset_uri`) sin fondo para que el usuario elija su foto en Instagram, o con la foto elegida como `background_asset`.
- El trazo de las carreras sigue recortado 250 m en cada extremo.
