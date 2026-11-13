# Rediseno web

Implementacion en `apps/web` de la propuesta aprobada (maquetas de Inicio, Rutas, Perfil, Comunidad, Clubes, Eventos y Correr, mas Inicio en movil). Se entrega por fases.

## Reglas de datos

- Todo lo que se ve sale de Supabase o de las carreras guardadas del usuario. Sin backend (pruebas y vista previa de desarrollo) se usan los ejemplos locales.
- No se muestran cifras inventadas. Donde la maqueta tenia un dato que no existe, se muestra el equivalente real:
  - XP: no existe. Se muestra el nivel por km acumulados (`nivelCorredor` y `progresoNivel`, umbrales 30, 150 y 500 km).
  - Puntaje de seguridad de 0 a 100: solo con datos ECU 911 cargados (`seguridad_rutas().puntaje`). Sin ellos se muestra el nivel (bajo, medio, alto) y los reportes de 7 dias.
  - Meta semanal: una sola, en km (`weekly_goal_km`). La meta de carreras queda como dato secundario en "Tu semana".
  - Resultados de eventos: tabla `event_results`; vacia hasta que el organizador cargue resultados.
- Los eventos de ejemplo usan fechas relativas a hoy (`fechaEventoEjemplo` en `mocks/data.ts`) y en Supabase se reprograman cada dia (migracion 040).

## Piezas compartidas

| Archivo                | Contenido                                                                                              |
| ---------------------- | ------------------------------------------------------------------------------------------------------ |
| `shared/ui/Visual.tsx` | `Panel`, `Rotulo`, `Cifra`, `MiniMapa` (SVG del trazado), `Anillo`, `Barras`, `Progreso`, `MapaCalor`. |
| `app/ShellEscritorio`  | Barra lateral: racha de la semana, navegacion con contadores, meta semanal y usuario.                  |
| `app/BarraSuperior`    | Fecha, saludo o seccion, busqueda global, chip de clima y notificaciones (desde 1024 px).              |
| `app/BusquedaGlobal`   | Ctrl+K o Cmd+K (y "/"): pantallas, rutas, clubes, eventos y corredores. Flechas, Enter y Esc.          |
| `app/datosApp.ts`      | Eventos visibles, corredores en vivo (sondeo cada minuto), seguridad por ruta y de la zona.            |
| `contexts/clima`       | Open-Meteo con sensacion, viento, humedad, UV y 16 horas de pronostico; un solo pedido compartido.     |

El chip de clima nunca muestra "--": esqueleto mientras carga, dato real o "Clima no disponible" con reintento.

La mejor ventana para correr es el tramo mas largo de horas de 5:00 a 20:00 con 8 a 21 grados y menos de 30 por ciento de lluvia.

## Fase 1: layout e Inicio

- Escritorio: rejilla de 12 columnas. Hero con lo que falta para la meta, cifras del mes y de la semana y el avatar 3D; clima para correr; ruta segura del dia con mini mapa; tu semana; reto del mes y proximos logros; actividad reciente; en ruta ahora; ranking semanal; proximos eventos; seguridad en tu zona.
- Movil: orden de la maqueta `inicio-movil`, con cabecera propia (fecha, saludo, buscar, clima y campana).
- Cada panel que carga datos muestra un esqueleto y un estado vacio honesto.

## Fase 2: Rutas y Perfil

- Rutas: cabecera con conteo (oficiales y de la comunidad), pestanas Explorar, Mis rutas y Guardadas, cuatro destacadas con criterio real (segura del dia, mejor valorada, para tu ritmo y la mas llana), filtros en columna desde 1024 px, orden (recomendadas, mas seguras, mas cortas, mas largas, menos desnivel) y tarjetas con mini mapa, nivel de seguridad y perfil de elevacion. Desde 1280 px se suma el panel de la ruta elegida.
- Perfil: cabecera con nivel por km y progreso, contadores y accesos (avatar 3D, editar perfil, configuracion). Pestanas Resumen, Actividades, Logros y Reportes.
  - Resumen: cifras del periodo (4 semanas, 12 semanas, ano o todo), volumen por semana o por mes, records, constancia (rachas, mapa de 26 semanas y carreras por dia), medallas, las 5 actividades mas recientes y rutas favoritas con la hora en que sueles correr.
  - Actividades: tabla paginada de 10 en 10. Desnivel y visibilidad solo desde 1400 px; en movil cada fila es una tarjeta.
  - La pagina pasa de unos 4000 px a unos 1750 px en escritorio.

## QA

Capturas a 1440, 1280 y 390 px de cada pantalla y de los modales (busqueda, crear evento, publicar), antes y despues, sin desborde horizontal ni contenido cortado.
