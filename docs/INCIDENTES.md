# Reportes de incidentes de los corredores

Los corredores pueden reportar un incidente desde la pantalla de carrera o desde el mapa en vivo, en Android y en la web. La aplicación toma sola la ubicación, la dirección aproximada y la hora; el usuario solo elige qué pasó. La descripción y hasta 3 fotos son opcionales.

## Tipos

`robo_asalto`, `acoso`, `perros_agresivos`, `iluminacion_deficiente`, `trafico_peligroso`, `via_mal_estado`, `accidente`, `otro`.

## Datos y reglas (migración `030_incidentes.sql`)

- Tabla `incidentes_usuario`: tipo, descripción (máximo 280 caracteres), punto GPS, dirección, hora del incidente (entre 7 días antes y 5 minutos después del envío), rutas de fotos (máximo 3), estado (`pendiente`, `verificado`, `rechazado`) y número de confirmaciones.
- Para crear un reporte hace falta el consentimiento vigente `gps_carrera`.
- Límite por usuario: 5 reportes por hora y 20 por día (disparador en la base de datos).
- El autor ve y puede borrar sus reportes. Administradores y moderadores (`es_moderador()`) ven todo, cambian el estado y borran.
- Tabla `incidentes_confirmaciones`: "Yo también lo vi". Una por usuario y reporte; no se puede confirmar un reporte propio. Un disparador mantiene el contador.
- Vista pública `incidentes_publicos`: sin usuario, coordenadas redondeadas a 3 decimales (unos 100 m), hora truncada a la hora, descripción solo si está verificado, últimos 90 días y sin rechazados.

## Fotos

Bucket público `incidentes` en Storage: JPEG o WebP, 3 MB por archivo, ruta `<uuid aleatorio>/<n>.jpg` sin el id del usuario. Antes de subir, la app vuelve a codificar la imagen (canvas en la web, bitmap en Android), lo que elimina los metadatos EXIF, incluida la ubicación del teléfono.

## Indicador de seguridad

`nivel_seguridad_ruta` suma al índice del ECU 911 los reportes de corredores a menos de 150 m de cada tramo que estén verificados o que tengan al menos 2 confirmaciones. Cada reporte pesa 0,5 multiplicado por `exp(-días / 30)`, así que pierde peso con el tiempo. Si se pasa una hora, solo cuentan los reportes de esa franja (más o menos una hora) y del mismo día de la semana.

## Integración con moderación

La cola de moderación de contenido llega en la rama `feat/rutas-loja-reportes`. Mientras tanto la integración es mínima:

- Los moderadores cambian `estado` en `incidentes_usuario` (política de actualización para `es_moderador()`).
- Cuando se fusione esa rama, basta con añadir `incidente` como tipo de objetivo en su tabla de reportes y, al resolver, actualizar `incidentes_usuario.estado` a `verificado` o `rechazado`. No hace falta cambiar esta migración.

## Arquitectura

Todo pertenece al contexto DDD Seguridad de rutas (`seguridad`, ver `docs/ARQUITECTURA-DDD.md`). El importador del backend está en `backend/src/contexts/seguridad/application/`; el núcleo de lectura y agregación se comparte con la Edge Function en `supabase/functions/importar-ecu911/nucleo.ts`.

## Interfaz

- Android: botón "Reportar incidente" en la cabecera de la carrera y sobre el mapa en vivo, hoja inferior oscura con los tipos, marcadores en el mapa (más intensos si están verificados) y la lista con "Yo también lo vi". Código en el contexto Seguridad de rutas: `apps/android/contextos/seguridad/domain` (modelo y puerto `IncidentesRepositorio`), `apps/android/contextos/seguridad/data` (adaptador de Supabase) y las pantallas en `apps/android/app/src/main/java/com/gacelas/seguridad/`.
- Web: los mismos elementos en `apps/web/src/features/seguridad/`, con capas `domain`, `data` (Supabase y Nominatim), `application` (casos de uso) y `ui`. La UI no llama a Supabase: usa los casos de uso.
- Vista previa: `?vista-previa` en la web, o la app sin servidor configurado, muestra reportes de ejemplo ficticios; en ese modo no se envía nada.

## Privacidad

La sección "Reportes de incidentes" de la Política de privacidad explica qué se guarda y qué se publica. Ver `docs/LEGAL.md`.
