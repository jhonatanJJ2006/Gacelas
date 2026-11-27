# Sistema único de logros

Hasta la migración 049 convivían dos sistemas paralelos en Runcity. Este documento describe el análisis, el diseño unificado, la migración de datos y el plan de aplicación y reversión.

## 1. Situación anterior

| Aspecto           | Logros                                                                                                                                                                                                                                                                      | Medallas                                                                                                                                         |
| ----------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------ |
| Cantidad          | 20 en Android, 4 en la web                                                                                                                                                                                                                                                  | 12 en Android, 6 en la web                                                                                                                       |
| Dónde se guardan  | Tabla `logros.user_achievements` (vista `public.user_achievements`) y registro local (DataStore en Android, `localStorage` en la web)                                                                                                                                       | En ningún sitio: se calculaban en el teléfono o el navegador con las carreras locales                                                            |
| Quién los concede | El cliente sube el código; desde la migración 046 el servidor valida con carreras verificadas, pero solo 4 códigos (`primera_carrera`, `km_5`, `km_10`, `racha_7`). Los otros 16 se rechazaban y, como la app los sube en una sola petición, un rechazo tiraba todo el lote | El cliente, sin validación del servidor                                                                                                          |
| Cuándo se otorgan | Al guardar una carrera (aviso al ganar uno) y, en el servidor, al verificarse una carrera (`logros.conceder_verificados`)                                                                                                                                                   | Se recalculaban cada vez que se abría Perfil o Inicio, sin aviso                                                                                 |
| Pantallas         | Android: tarjeta en Perfil y pantalla `perfil/logros`. Web: pestaña Logros de Perfil                                                                                                                                                                                        | Android: tarjeta Medallero en Perfil y pantalla `perfil/medallero`. Web: panel Medallas (Resumen y pestaña Logros) y "Próximos logros" en Inicio |
| Reglas            | `racha_7` con la racha actual en Android y con la mejor en el servidor                                                                                                                                                                                                      | Racha con la mejor racha, sin verificar                                                                                                          |

### Duplicados entre ambos

Seis medallas eran el mismo hito que un logro: Primera zancada y Primera carrera, Gacela 5K y 5 km, Diez mil y 10 km, Media maratón en ambos, Semana perfecta y Racha de 7 días, Mes de hierro y Racha de 30 días. Las otras seis medallas (Maratón, Centenario, Quinientos, Mil kilómetros, Andino y Cumbre) no tenían equivalente.

Otros hallazgos del análisis:

- La web tenía 4 logros y 6 medallas; Android 20 y 12. Ahora ambas muestran el mismo catálogo.
- `racha_7` usaba la racha actual en Android, la mejor en la web y el servidor. Se unifica en la mejor racha: un logro ganado no se pierde al romper la racha.
- La descripción de la medalla Cumbre afirmaba que 5000 m de desnivel era "más que subir al Chimborazo desde el mar". El Chimborazo mide 6263 m, así que se quitó esa frase.

## 2. Diseño unificado

Un catálogo de **26 logros** sin duplicados (20 logros más las 6 medallas que no tenían equivalente), una sola tabla donde se guarda lo ganado (`logros.user_achievements`, sin cambios de estructura) y un solo apartado en cada app.

Fuente de verdad: `packages/shared-types/logros-catalogo.json`. La tabla `logros.catalogo_logros` (migración 049), la lista `LOGROS` de la web y el enum `Logro` de Android dicen lo mismo, y pruebas automáticas lo comprueban (`apps/web/src/__tests__/paridadLogros.test.ts`, `ParidadLogrosTest` en Android y `backend/tests/rls/049_logros_unificados.sql`).

| Categoría   | Logros | Códigos                                                                                             |
| ----------- | ------ | --------------------------------------------------------------------------------------------------- |
| Distancia   | 9      | `km_5`, `km_10`, `km_15`, `km_21`, `km_42`, `total_50`, `total_100`, `total_500`, `total_1000`      |
| Constancia  | 7      | `primera_carrera`, `racha_3`, `racha_7`, `racha_30`, `carreras_10`, `carreras_50`, `semanas_meta_4` |
| Velocidad   | 3      | `ritmo_6`, `ritmo_5`, `ritmo_430`                                                                   |
| Exploracion | 4      | `escalador`, `explorador`, `desnivel_1000`, `desnivel_5000`                                         |
| Horario     | 3      | `madrugador`, `nocturno`, `fin_de_semana`                                                           |

### Mapeo medalla a logro

Cada una de las 12 medallas apunta a un único logro. El código de la medalla antigua queda en la columna `logros.catalogo_logros.medalla_origen`.

| Medalla anterior            | Logro unificado                     | Tratamiento                    |
| --------------------------- | ----------------------------------- | ------------------------------ |
| `5k` (Gacela 5K)            | `km_5` (5 km)                       | Ya existía como logro; se unen |
| `10k` (Diez mil)            | `km_10` (10 km)                     | Ya existía como logro; se unen |
| `21k` (Media maratón)       | `km_21` (Media maratón)             | Ya existía como logro; se unen |
| `42k` (Maratón)             | `km_42` (Maratón)                   | Pasa a ser un logro nuevo      |
| `centenario` (Centenario)   | `total_100` (Centenario)            | Pasa a ser un logro nuevo      |
| `500k` (Quinientos)         | `total_500` (Quinientos)            | Pasa a ser un logro nuevo      |
| `1000k` (Mil kilómetros)    | `total_1000` (Mil kilómetros)       | Pasa a ser un logro nuevo      |
| `primera` (Primera zancada) | `primera_carrera` (Primera carrera) | Ya existía como logro; se unen |
| `racha7` (Semana perfecta)  | `racha_7` (Racha de 7 días)         | Ya existía como logro; se unen |
| `racha30` (Mes de hierro)   | `racha_30` (Racha de 30 días)       | Ya existía como logro; se unen |
| `desnivel1000` (Andino)     | `desnivel_1000` (Andino)            | Pasa a ser un logro nuevo      |
| `desnivel5000` (Cumbre)     | `desnivel_5000` (Cumbre)            | Pasa a ser un logro nuevo      |

Los 14 logros restantes del catálogo (`km_15`, `total_50`, `racha_3`, `carreras_10`, `carreras_50`, `semanas_meta_4`, `ritmo_6`, `ritmo_5`, `ritmo_430`, `escalador`, `explorador`, `madrugador`, `nocturno`, `fin_de_semana`) ya eran logros de Android y no tienen medalla. `escalador` (100 m en una carrera) y `desnivel_1000` (1000 m acumulados) no son el mismo hito, y tampoco `total_50` y `total_100`: se conservan los dos.

Para los 6 duplicados se conserva el título del logro (no el de la medalla). Los textos de los 6 logros que antes eran medallas salen de la medalla.

### Reglas del servidor

`logros.logro_merecido(uid, codigo)` valida los 26 códigos solo con carreras verificadas (migración 046). Zona horaria `America/Guayaquil`, igual que `recalcular_racha`.

| Código                                             | Regla                                                                      |
| -------------------------------------------------- | -------------------------------------------------------------------------- |
| `primera_carrera`                                  | Al menos una carrera verificada                                            |
| `km_5`, `km_10`, `km_15`, `km_21`, `km_42`         | Una carrera verificada de 5000, 10000, 15000, 21097 o 42195 m              |
| `total_50`, `total_100`, `total_500`, `total_1000` | Suma de carreras verificadas de 50, 100, 500 o 1000 km                     |
| `carreras_10`, `carreras_50`                       | 10 o 50 carreras verificadas                                               |
| `racha_3`, `racha_7`, `racha_30`                   | Mejor de la racha actual y la mejor racha diaria (`user_streaks`)          |
| `semanas_meta_4`                                   | Mejor racha semanal de 4                                                   |
| `ritmo_6`, `ritmo_5`                               | 3 km o más a menos de 360 o 300 s/km de media                              |
| `ritmo_430`                                        | 5 km o más a menos de 270 s/km de media                                    |
| `escalador`                                        | 100 m o más de desnivel en una carrera verificada                          |
| `desnivel_1000`, `desnivel_5000`                   | Suma de desnivel de carreras verificadas                                   |
| `explorador`                                       | 5 lugares de salida distintos (latitud y longitud truncadas a 0,01 grados) |
| `madrugador`, `nocturno`                           | 3 carreras que empiezan antes de las 7:00 o a partir de las 19:00          |
| `fin_de_semana`                                    | 5 carreras en sábado o domingo                                             |

El trigger `zz_logro_verificado` sigue rechazando un `INSERT` que el servidor no respalda, y `logros.conceder_verificados` concede ahora los 26 al verificarse una carrera. Además, `user_achievements.code` pasa de un `CHECK` con la lista escrita a mano a una clave foránea al catálogo, así que añadir un logro es una fila nueva y no una migración que reescribe una restricción.

### Apps

- Web: un solo apartado Logros. La pestaña Logros del Perfil muestra los 26 por categoría con avance, y el panel de Resumen y "Próximos logros" de Inicio salen del mismo catálogo. Se eliminan el panel Medallas y `medallas()`.
- Android: la tarjeta Logros del Perfil y la pantalla `perfil/logros` muestran los 26. Se eliminan la tarjeta Medallero, la pantalla `perfil/medallero`, el destino `MEDALLERO` y `EstadisticasPerfil.medallas()`.
- Ambas suben cada logro por separado: un rechazo del servidor ya no tira el resto del lote.
- Primer arranque con el catálogo único en un dispositivo: lo que ya cumplían las carreras (incluidas las antiguas medallas) se registra en silencio, sin una fila de avisos por logros ganados antes.

## 3. Migración de datos

Archivo: `supabase/migrations/049_logros_unificados.sql`. Reversión: `supabase/rollback/049_logros_unificados.down.sql`.

1. Crea `logros.catalogo_logros` (RLS, lectura para usuarios autenticados, vista `public.catalogo_logros`, trigger de suspensión y registro en `kernel.propiedad_tablas`) y la llena con los 26 logros.
2. Se detiene con un error si `user_achievements` tuviera algún código fuera del catálogo. No borra nada.
3. Cambia el `CHECK` de códigos por una clave foránea al catálogo.
4. Reemplaza `logros.logro_merecido` y `logros.conceder_verificados` por las reglas de los 26 logros.
5. Recalcula lo ganado: `conceder_verificados` para cada usuario con carreras verificadas. Solo agrega filas; las existentes y sus fechas no se tocan.

Las medallas nunca se guardaron en la base: lo "ganado" de una medalla se recalcula con las carreras verificadas. Las filas nuevas llevan `unlocked_at` con la fecha de aplicación. Los dispositivos conservan la fecha original de cada logro en su registro local.

### Datos de producción (simulación de solo lectura del 26 de noviembre de 2026)

Se evaluaron las reglas de 049 contra producción con consultas de solo lectura, sin aplicar nada:

| Dato                                                              | Valor                                                                  |
| ----------------------------------------------------------------- | ---------------------------------------------------------------------- |
| Usuarios con carreras verificadas                                 | 10                                                                     |
| Filas actuales en `user_achievements`                             | 22 (10 usuarios)                                                       |
| Filas que se agregarían                                           | 21 (a 8 usuarios)                                                      |
| Filas tras aplicar                                                | 43                                                                     |
| Filas actuales que el servidor ya no respaldaría                  | 0                                                                      |
| Filas agregadas de las 6 medallas que pasan a logro               | 1 (`desnivel_1000`)                                                    |
| Filas agregadas de los 16 códigos que antes el servidor rechazaba | 20 (`escalador` 6, `ritmo_6` 5, `ritmo_5` 4, `ritmo_430` 4, `km_15` 1) |
| Medallas equivalentes ganadas tras aplicar (12 códigos)           | 23, de las que 22 ya estaban en la tabla                               |

Las 21 filas nuevas son: `escalador` 6, `ritmo_6` 5, `ritmo_5` 4, `ritmo_430` 4, `km_15` 1 y `desnivel_1000` 1. Nadie pierde ningún logro. Las cifras salen de datos reales y de demostración de producción; se repetirán justo antes de aplicar.

## 4. Plan de aplicación

Requiere la autorización de Jhona. No se aplicó en producción.

1. Confirmar que el PR está fusionado en `main` y que el CI (incluida la prueba `backend/tests/rls/049_logros_unificados.sql`) está en verde.
2. Respaldo previo (solo lectura), en `/workspace/backup-prod-pre-049/` (privado):
   - `logros.user_achievements` completa.
   - `logros.user_streaks`, para poder repetir el cálculo.
   - Definición actual de `logros.logro_merecido`, `logros.conceder_verificados` y de la restricción `user_achievements_code_check`.
3. Repetir la simulación de solo lectura y comparar con la tabla anterior.
4. Aplicar `049_logros_unificados.sql` en una sola transacción (el archivo ya lleva `BEGIN` y `COMMIT`).
5. Verificar:
   - `SELECT count(*) FROM logros.catalogo_logros` da 26 y `count(medalla_origen)` da 12.
   - `SELECT count(*) FROM logros.user_achievements` es igual al esperado y ninguna fila anterior cambió (comparar con el respaldo).
   - `SELECT code, count(*) FROM logros.user_achievements GROUP BY 1` solo muestra códigos del catálogo.
   - Un `INSERT` de un logro no merecido sigue fallando con `logro no verificado`.
6. Publicar las apps. Las versiones anteriores siguen funcionando: usan los mismos 20 códigos de siempre, que están en el catálogo. No conocen los 6 nuevos hasta actualizar.

## 5. Plan de reversión

1. Si algo falla durante la aplicación, la transacción se deshace sola.
2. Si falla después: ejecutar `supabase/rollback/049_logros_unificados.down.sql`. Borra los 6 logros que antes eran solo medallas (no caben en el `CHECK` anterior), restaura el `CHECK` de la migración 048 y las reglas de la 046, y elimina el catálogo, su vista y su registro.
3. Los logros de los 20 códigos de la 048 que se concedieron al aplicar se conservan: eran válidos antes y lo siguen siendo. Para volver exactamente al estado previo, restaurar `logros.user_achievements` desde el respaldo del paso 2 (`TRUNCATE` y carga del respaldo, con confirmación de Jhona).
4. Las apps nuevas siguen funcionando contra la base revertida: suben cada logro por separado y el servidor rechaza los que no reconoce.

## 6. Pruebas

- Web: `logros-unificados.test.ts` (reglas, estado, registro local), `paridadLogros.test.ts` (JSON, web, Android y migración), y las pruebas existentes de Perfil, Inicio y logros actualizadas.
- Android: `LogrosTest` (reglas de los 26), `ParidadLogrosTest` (enum igual al JSON y a la migración) y las pruebas de capturas de Perfil.
- Base de datos: `backend/tests/rls/049_logros_unificados.sql` en el job `Base de datos` del CI. La lógica de la migración (concesión, conservación de filas y fechas, reaplicar, revertir) también se probó con un Postgres local de pruebas con las tablas reducidas.

## 7. Pendientes

- El servidor valida `explorador` con la ubicación de salida de la carrera y la app con el primer punto del recorrido: pueden diferir en un borde de cuadrícula. El servidor manda; un logro rechazado se concede al verificarse más carreras.
- Los logros ganados en el teléfono con carreras que nunca llegaron al servidor (sin verificar) se ven en esa app, pero la tabla no los guarda. Es el comportamiento de 046 para los 4 códigos anteriores.
