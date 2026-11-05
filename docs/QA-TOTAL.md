# QA total - app Android Gacelas (rama `fix/qa-total`)

Fecha: 4 nov 2026. Sin emulador: todo lo automatizable se probó con Robolectric (sdk 34) + Compose UI test + Roborazzi.
Lo que necesita hardware real (GPS, brújula, notificaciones push, Google, actualización in-app) está en "Pruebas manuales".

Leyenda: OK pasa · Corregido arreglado en esta rama · Manual requiere dispositivo real

## Matriz: pantallas (navegación)

Todas las rutas de `Destinos.todas` más MIS_REPORTES, DETALLE_CARRERA y ADMIN se prueban en `QaTotalTest`:
abrir la ruta a se dibuja sin excepción a atrás vuelve a la pantalla anterior. Se repite con fuente 1.5x y con pantalla pequeña 360x640.

| Pantalla                                             | Elemento                                     | Acción esperada                          | Cómo se probó                                          | Resultado                                                       |
| ---------------------------------------------------- | -------------------------------------------- | ---------------------------------------- | ------------------------------------------------------ | --------------------------------------------------------------- |
| Bienvenida / acceso                                  | Saltar, Crear cuenta, Iniciar sesión, Google | Navegan a su pantalla                    | QaTotalTest.flujoAcceso + AccesoTest existentes        | OK / Google Manual                                              |
| Inicio                                               | Correr, Eventos, evento, Comunidad, Rutas    | Abren su destino                         | Humo de rutas + revisión de cableado en GacelasNavHost | OK                                                              |
| Rutas                                                | Buscar, ruta, Dibujar                        | Buscador / detalle / crear ruta          | Humo de rutas                                          | OK                                                              |
| Comunidad / Eventos / Clubes                         | Eventos, En vivo, Clubes, evento, club       | Abren su destino                         | Humo de rutas                                          | OK                                                              |
| Detalle de evento                                    | Atrás, En vivo                               | Vuelve / abre evento en vivo             | Humo de rutas                                          | OK                                                              |
| Perfil                                               | Editar, Avatar, Ajustes, carrera, Reportes   | Abren su destino                         | Humo de rutas                                          | OK                                                              |
| Editar perfil / Avatar                               | Atrás, Guardar                               | Vuelven                                  | Humo de rutas                                          | OK                                                              |
| Correr / en curso / resumen                          | Iniciar, pausar, descartar                   | Flujo de carrera                         | Tests existentes de carrera + modal descartar          | OK / GPS Manual                                                 |
| Ajustes, Privacidad, Config. notificaciones, Legales | Atrás, enlaces legales, eliminar cuenta      | Navegan                                  | Humo de rutas                                          | OK                                                              |
| Notificaciones                                       | Abrir notificación de evento                 | Antes abría siempre el evento "1" (fijo) | Revisión estática + test                               | Corregido ahora abre la lista de eventos                        |
| Brújula, Mapas offline, Mapa vivo                    | Abrir / atrás                                | Se dibujan y vuelven                     | Humo de rutas                                          | OK / sensores Manual                                            |
| Admin, Mis reportes, Detalle carrera                 | Abrir / atrás                                | No estaban en el humo                    | Añadidas al humo de QaTotalTest                        | Corregido cubiertas                                             |
| Todas                                                | Doble toque rápido en un botón               | Una sola navegación                      | QaTotalTest.dobleIr / dobleAtras                       | Corregido antes apilaba la pantalla 2 veces o hacía doble atrás |
| Todas                                                | Rotación / recreación                        | Mantiene la pantalla                     | StateRestorationTester (AJUSTES)                       | OK                                                              |
| Todas                                                | Fuente grande 1.5x, pantalla 360x640         | Sin crash                                | QaTotalTest                                            | OK                                                              |

## Matriz: modales

| Modal                                                                | Botón                                           | Toque fuera         | Atrás | Toque dentro no cierra | Prueba                                                 |
| -------------------------------------------------------------------- | ----------------------------------------------- | ------------------- | ----- | ---------------------- | ------------------------------------------------------ |
| DialogoC2 (todos los diálogos: cerrar sesión, eliminar, etc.)        | OK y el botón ejecuta SU acción, no el descarte | OK                  | OK    | OK                     | ModalesCierreQaTest                                    |
| HojaC2 (hojas inferiores: compartir, reportar incidente, en vivo...) | OK                                              | OK                  | OK    | OK                     | ModalesCierreQaTest                                    |
| DialogoDescartarCarrera                                              | OK (tests existentes)                           | hereda de DialogoC2 | OK    | hereda de DialogoC2    | ModalesCierreQaTest (atrás) + tests existentes         |
| Tamaños / bordes / paridad                                           | -                                               | -                   | -     | -                      | ModalesTamanosTest, ModalesBordeTest, ParidadModalTest |

Capturas: `docs/capturas/qa-total/` (dialogo-abierto, hoja-abierta, descartar-carrera).

Nota técnica: con el modo gráfico "legacy" de Robolectric los recortes con forma (CutCornerShape) no aceptan toques y parecía que los
botones de los modales no respondían. Es un falso positivo del entorno de prueba, no del código: los tests de modales usan ahora
`@GraphicsMode(NATIVE)`, que reproduce el comportamiento real.

## Revisión estática

- `onClick` vacíos: todos los callbacks con valor por defecto `{}` están conectados en GacelasNavHost. OK
- TODO pendiente: `LocationService.kt:44` - la posición en vivo aún no se envía a Supabase (pendiente de backend). PENDIENTE
- Lint local: 5 errores `ProduceStateDoesNotAssignValue` (falsos positivos; en CI pasa). Sin cambios.
- Tema: la app tiene un único tema oscuro, no aplica modo claro/oscuro.

## Números

- Pantallas/rutas cubiertas en humo: todas las de `Destinos.todas` x 3 configuraciones.
- Modales cubiertos: DialogoC2, HojaC2, DialogoDescartarCarrera x 4 vías de interacción.
- Tests nuevos: 15 (QaTotalTest 7 + ModalesCierreQaTest 8). Total en app: ~293.
- Bugs arreglados: 2 (doble toque en navegación, notificación de evento con id fijo) + 3 rutas sin cobertura.

## Pruebas manuales (dispositivo real) Manual

1. Iniciar sesión con Google y cerrar sesión; volver a entrar.
2. Correr 5 min con GPS: pausar, reanudar, descartar (atrás y botón) y guardar; ver resumen.
3. Notificaciones: aceptar permiso, recibir una de evento a debe abrir la lista de Eventos.
4. Brújula y mapa offline: descargar zona, modo avión, abrir mapa.
5. Compartir carrera / compartir en vivo: abrir hoja, cerrar tocando fuera, con atrás y con el botón.
6. Tocar dos veces rápido cualquier botón de navegación: debe abrir una sola pantalla.
7. Actualización in-app (requiere versión publicada en Play).
