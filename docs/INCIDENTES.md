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

Los incidentes son un tipo de objetivo más de la cola de moderación genérica (`docs/REPORTES.md`). La migración `031_moderacion_incidentes.sql` registra `incidente` en `report_target_types` sin cambiar 030:

- Resumen en la cola: tipo, dirección y descripción del incidente.
- Ocultar (botón "Rechazar incidente"): el incidente pasa a `rechazado` y deja de contar en el índice y en el mapa.
- Suspender autor: suspende a quien lo reportó (solo administradores).
- Al cerrar el caso: `resuelto` deja el incidente en `rechazado`; `descartado` (el incidente es válido) lo deja en `verificado`.
- Los moderadores pueden seguir cambiando `estado` directamente (política de actualización para `es_moderador()`).

En los clientes, `incidente` ya está registrado: web en `contexts/moderacion/domain/denuncia.ts` (`reportarContenido('incidente', id, motivo, detalle)`) y Android en `TipoContenido.Incidente` (`SocialRepository.reportar(id, motivo, detalle, TipoContenido.Incidente)`). Prueba: `backend/tests/rls/031_moderacion_incidentes.sql`.

## Arquitectura

Todo pertenece al contexto DDD Seguridad de rutas (`seguridad`, ver `docs/ARQUITECTURA-DDD.md`). El importador del backend está en `backend/src/contexts/seguridad/application/`; el núcleo de lectura y agregación se comparte con la Edge Function en `supabase/functions/importar-ecu911/nucleo.ts`.

## Interfaz

- Android: botón "Reportar incidente" en la cabecera de la carrera y sobre el mapa en vivo, hoja inferior oscura con los tipos, marcadores en el mapa (más intensos si están verificados) y la lista con "Yo también lo vi". Código en el contexto Seguridad de rutas: `apps/android/contextos/seguridad/domain` (modelo y puerto `IncidentesRepositorio`), `apps/android/contextos/seguridad/data` (adaptador de Supabase) y las pantallas en `apps/android/app/src/main/java/com/gacelas/seguridad/`.
- Web: los mismos elementos en `apps/web/src/contexts/seguridad/`, con capas `domain`, `infrastructure` (Supabase y Nominatim), `application` (casos de uso) y `ui`. La UI no llama a Supabase: usa los casos de uso.
- Vista previa: `?vista-previa` en la web, o la app sin servidor configurado, muestra reportes de ejemplo ficticios; en ese modo no se envía nada.

## Privacidad

La sección "Reportes de incidentes" de la Política de privacidad explica qué se guarda y qué se publica. Ver `docs/LEGAL.md`.

## Estado en producción

Desde el 25 de octubre de 2026 están aplicadas las migraciones 030 y 031, existe el bucket público `incidentes` con sus políticas de subida y borrado, y la Edge Function `importar-ecu911` está desplegada. La función lee XLSX con `npm:xlsx` porque el empaquetador de Supabase no acepta imports desde cdn.sheetjs.com.

## Alertas de zona durante la carrera (0.9.14)

Ticket 86e3uf09b (N-11 y VI-08). Mientras se corre, la app lee los reportes de `incidentes_publicos` al empezar y cada 5 minutos. Con cada punto GPS compara la posicion con los reportes de los ultimos 7 dias y, si hay uno a 200 m o menos, avisa una sola vez por reporte: vibracion en dos pulsos y un aviso arriba ("Atención: reporte de robo o asalto a 150 m", con "(verificado)" si lo esta).

- Android: `AlertasZona` (dominio de seguridad, comun) y `AlertasZonaCarrera` en la pantalla de la carrera. Aviso con `AvisosC2` y tono corto.
- Web: `alertasNuevas` (`src/contexts/seguridad/domain/alertasZona.ts`) y `AlertaZona` en `CarreraEnCurso`: banner `role="alert"` durante 8 s y `navigator.vibrate` donde exista.
- Se apagan en Ajustes, Privacidad y ubicacion, fila "Alertas de zona" (`prefs.alertasZona`, por defecto activadas, compartido entre web y Android).
- Segundo plano (0.9.15): el calculo vive en el servicio de ubicacion en primer plano de la carrera (`AlertasZonaServicio` dentro de `LocationService`), asi que avisa tambien con la pantalla apagada o la app minimizada. El aviso es una notificacion del canal "Alertas de zona" (prioridad alta, vibracion en dos pulsos, se borra sola a los 10 minutos) y, con la app abierta, ademas el aviso arriba de la pantalla de la carrera. Si el usuario apago las notificaciones, solo vibra.
- Bateria: no se pide ubicacion extra; se usan los puntos que el servicio ya recibe. Los reportes se releen cada 5 minutos (una consulta pequena) y la distancia solo se recalcula cuando el corredor se movio 25 m o pasaron 15 s (`MonitorAlertasZona`), descartando puntos con precision peor que 50 m. Al terminar la carrera el servicio se detiene y con el las alertas.
- Ajuste: el servicio escucha `prefs.alertasZona`; si se apaga a mitad de carrera deja de avisar en el acto.
- Tests: `Version0915Test` (una sola notificacion por reporte, ajuste apagado sin avisos, recalculo solo al moverse).
