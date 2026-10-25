# Módulo de reportes

El módulo tiene tres partes: estadísticas del panel de administración, reportes personales del corredor y reportes de contenido ampliados. El servidor está en la migración `supabase/migrations/023_reportes.sql`, que es idempotente y no se aplica sola.

## 1. Estadísticas del administrador

Sección "Estadísticas" del panel (`/admin/estadisticas`).

- Rango de fechas con atajos (7, 30 y 90 días o un año) o fechas libres. Máximo dos años.
- Indicadores: usuarios nuevos, usuarios activos, carreras, kilómetros, inscripciones a eventos y reportes, con la variación respecto al periodo anterior del mismo largo.
- Gráficos: serie diaria de carreras y kilómetros, usuarios nuevos, retención (cuántos activos del periodo anterior vuelven), rutas más corridas (marcando las oficiales), eventos con más inscritos y reportes por estado, tipo y motivo.
- Exportar: CSV (separador punto y coma, con BOM para Excel) y PDF generado en el navegador.

Servidor: `admin_estadisticas(desde, hasta)` devuelve un JSON con `totales`, `previos`, `serie`, `retencion`, `rutas_top`, `eventos_top` y `reportes`. Es `SECURITY DEFINER` y lo primero que hace es comprobar `is_admin()`; si no, lanza una excepción.

## 2. Reportes personales

Web: `/perfil/reportes` (enlace "Mis reportes" en Perfil). Android: pantalla "Mis reportes" desde Perfil.

- Pestañas Semana (desde el lunes), Mes y Año.
- Distancia, carreras, tiempo, ritmo medio y desnivel, comparados con el periodo anterior.
- Gráfico de kilómetros por día (semana y mes) o por mes (año).
- Mejores marcas del periodo: carrera más larga, mejor ritmo (carreras de al menos 1 km) y mayor desnivel.
- Racha actual y mejor racha de días seguidos.
- Rutas favoritas: carreras cuyo inicio está a menos de 150 m del inicio de una ruta conocida.
- Compartir como texto y exportar CSV (en la web también PDF).

El cálculo es local con las carreras del usuario (`apps/web/src/contexts/analitica/domain/resumenPersonal.ts` y `ReportesPersonales` en `contextos/analitica/domain`), con la misma regla en ambas plataformas. En el servidor, `reporte_personal(periodo, referencia)` hace el mismo resumen solo con las carreras de `auth.uid()`, de modo que nadie puede pedir el reporte de otra persona.

## 3. Reportes de contenido

Se puede reportar:

- Publicaciones (como antes).
- Usuarios, desde la lista de corredores.
- Comentarios, desde el feed.
- Eventos, desde el detalle del evento.

Motivos: spam, ofensivo, acoso, falso, suplantación, peligroso y otro. Cada tipo muestra solo los motivos que tienen sentido para él.

### Tablas

- `report_target_types`: registro de tipos de objetivo. Cada fila tiene la clave, la etiqueta, el texto del aviso (por ejemplo "la publicación") y tres funciones opcionales: resumen, ocultar y autor.
- `content_reports`: objetivo (`target_type`, clave foránea a `report_target_types`, y `target_id`), quién reporta, motivo, detalle, estado, nota de resolución y quién lo resolvió. Un usuario solo puede reportar una vez el mismo contenido.
- `content_report_actions`: historial de cada acción del moderador con fecha, autor y nota.
- `post_reports` se mantiene para las apps ya publicadas. Un trigger copia cada reporte nuevo a `content_reports` y la migración copia los antiguos. Al moderar, el estado se sincroniza de vuelta en `post_reports`.

### Estados y acciones

Estados: `pendiente`, `en_revision`, `resuelto` y `descartado`.

`moderar_reporte(reporte, accion, nota)` aplica una acción y la anota en el historial:

- `en_revision`: el moderador toma el caso.
- `ocultar_contenido`: llama a la función `ocultar_fn` del tipo (borra la publicación o el comentario, despublica el evento o suspende el perfil reportado); el reporte queda resuelto. Si el tipo no tiene función, la acción se rechaza.
- `suspender_usuario`: suspende al autor que devuelve `autor_fn` (solo administradores). Si el tipo no tiene autor, se rechaza.
- `resuelto` y `descartado`: cierran el reporte.
- `reabierto`: vuelve a pendiente.
- `nota`: añade una nota sin cambiar el estado.

Al cerrar un reporte, quien lo envió recibe una notificación de tipo `reporte` que no revela la identidad de nadie.

`admin_cola_reportes(estado, tipo)` devuelve la cola con el contenido reportado, el autor, cuántas personas lo reportaron y el historial.

### Cola genérica y nuevos tipos

La cola no conoce los tipos de antemano. Los incidentes de corredores ya están conectados así (migración `031_moderacion_incidentes.sql`, ver `docs/INCIDENTES.md`). Para sumar otro tipo, su migración:

1. Crea las funciones del tipo, con firma `(objetivo TEXT)`: resumen (`RETURNS TEXT`), ocultar (`RETURNS VOID`) y, si el contenido tiene autor, autor (`RETURNS UUID`). Opcionalmente, `resolver_fn(objetivo TEXT, resultado TEXT)` recibe `resuelto` o `descartado` al cerrar el caso. Conviene quitarles `EXECUTE` a `PUBLIC`.
2. Inserta la fila en `report_target_types`:

```sql
INSERT INTO public.report_target_types (key, etiqueta, referencia, resumen_fn, ocultar_fn, autor_fn)
VALUES ('incidente', 'Incidente', 'el incidente', 'public.moderacion_resumen_incidente',
        'public.moderacion_ocultar_incidente', 'public.moderacion_autor_incidente')
ON CONFLICT (key) DO NOTHING;
```

No hace falta tocar `content_reports`, `moderar_reporte` ni `admin_cola_reportes`. Si el motivo necesario no existe, se amplía el `CHECK` de `reason`.

En los clientes:

- Web: `registrarTipoObjetivo('incidente', { etiqueta, titulo, motivos, accionOcultar, tieneAutor })` desde `contexts/moderacion/api.ts`, y `reportarContenido('incidente', id, motivo, detalle)`. Un tipo sin registrar se muestra en la cola con una etiqueta y acciones genéricas.
- Android: `SocialRepository.denunciar(ObjetivoDenuncia("incidente", id), motivo, detalle)`; `motivosReporte("incidente")` devuelve motivos genéricos mientras el tipo no tenga los suyos.

### Estructura del código

Sigue `docs/ARQUITECTURA-DDD.md`:

- Web: `src/contexts/moderacion` (denuncias, casos de la cola, adaptadores Supabase y de ejemplo), `src/contexts/analitica` (resumen personal, estadísticas del panel, exportación CSV y PDF) y `src/contexts/rutas` (catálogo de rutas oficiales de Loja). La UI importa solo `api.ts` de cada contexto; Supabase se usa únicamente en `infrastructure`. `features/admin/api.ts` y `features/social/moderacion.ts` quedan como fachadas de compatibilidad.
- Android: `contextos/analitica/domain` (`ReportesPersonales`) y `contextos/moderacion/domain` (`ObjetivoDenuncia`, `TipoContenido`, motivos). `shared` conserva `TipoContenido` y `motivosReporte` como alias.

### Seguridad

- RLS: cada usuario ve solo sus propios reportes; los moderadores (`es_moderador()`) ven y gestionan todos.
- El historial solo se escribe a través de `moderar_reporte` (`SECURITY DEFINER`).

## Pruebas

- SQL: `backend/tests/rls/023_reportes.sql` (RLS, permisos de administrador, reporte personal, moderación, aviso al reportante y un tipo de objetivo nuevo registrado en la prueba). Termina en `ROLLBACK`.
- Web: `src/contexts/analitica/domain/__tests__/reportes.test.ts`, `src/contexts/moderacion/domain/__tests__/denuncia.test.ts` y `src/__tests__/admin.test.tsx`.
- Android: `ReportePersonalTest` y `DenunciaTest` (dominio), `NotificacionesRemotasTest` y capturas en `RutasReportesTest`.

## Revisar sin servidor

- Web: `?vista-previa` incluye carreras de ejemplo para Mis reportes, y `/admin?demo` (solo en desarrollo) trae estadísticas y una cola de reportes de varios tipos.
