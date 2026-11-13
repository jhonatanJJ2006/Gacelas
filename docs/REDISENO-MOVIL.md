# Rediseño móvil (Android 0.9.16)

Implementación en Jetpack Compose y KMP de la propuesta aprobada de rediseño móvil. Todas las cifras salen de datos reales (historial local de carreras, ajustes, rutas y la ruta sugerida). Si falta un dato, el bloque se oculta o se muestra su equivalente real. No hay valores de relleno.

## Piezas comunes (`core/ui`, `RedisenoC2.kt`)

- **Chips:** `ChipParalelogramo` y `ChipEstadoC2`.
- **Selectores:** `SelectorSegmentadoC2` y `OpcionTarjetaC2`.
- **Cifras:** `CifraC2`, `TiraCifrasC2` y `CeldaCifraC2`, en Anton cursiva.
- **Contenedores:** `TarjetaSeccionC2` y `HojaInferiorC2`. La hoja va pegada al borde, con tirador, y respeta `WindowInsets.navigationBars`.
- **Botón principal:** `BotonGrandeC2`, con gradiente `#7B1FA2` a `#A855F7` y esquina cortada.
- **Gráficas en Canvas** (sin cargar el mapa en cada tarjeta): `MiniTrazoC2`, `BarrasC2`, `LineaC2`, `AnilloC2`, `MapaCalorC2` y `BarraProgresoC2`.
- **`HojaC2`:** admite un borde, que es rojo en la hoja SOS.
- **Barra inferior:** pestaña activa con cápsula, y botón Correr central más grande, con halo y etiqueta.

## Pantallas

- **Correr (pre-carrera)**
  - Mapa a pantalla completa con flecha de volver y estado del GPS (± m).
  - Hoja inferior con:
    - la ruta segura del día, o carrera libre;
    - el objetivo (Libre, Distancia, Tiempo o Ritmo), con ajuste fino;
    - el efecto real del objetivo sobre la meta semanal en km y sobre la racha;
    - el estado de Voz, En vivo y SOS;
    - el aviso de que compartir en vivo se eligió una sola vez;
    - el botón EMPEZAR.
  - El objetivo se guarda en `Ajustes.objetivoTipo` y `Ajustes.objetivoValor`.
- **Carrera en curso**
  - Chips "GRABANDO · GPS ± N m" y "EN VIVO" (solo si se comparte).
  - Grupos MAPA y CARRERA con etiquetas.
  - Selector 3D | 2D con la vista actual. La última vista se recuerda en `Ajustes.vistaMapa3d`.
  - Reportar con etiqueta al costado del mapa.
  - SOS separado, abajo a la izquierda, con "MANTÉN 3 S".
  - Hoja de métricas pegada al borde con:
    - distancia;
    - barra del objetivo y lo que falta;
    - 4 celdas: tiempo, ritmo, promedio y desnivel;
    - parciales segmentados con el mejor km;
    - Bloquear, PAUSAR y Terminar con etiqueta.
- **Hoja SOS**
  - Borde rojo.
  - Estado del aviso por contacto: "Avisado por SMS hace 8 s" o "Sin avisar". Solo cuenta lo que se envió desde la hoja.
  - Llamar o WhatsApp a cada contacto.
- **Resumen**
  - Mapa del recorrido arriba con flecha de volver, y chips de completada, objetivo cumplido y récord.
  - Distancia y ritmo medio comparado con la media de tus últimas 10 carreras.
  - Tiempo, desnivel y calorías. Las calorías salen del peso si está guardado; si no, se marcan como estimación.
  - Meta semanal con anillo, parciales en barras con el mejor km, elevación y ritmo.
  - Sección "Cuéntalo", historia para Instagram y Descartar.
  - Barra fija con Compartir y GUARDAR Y PUBLICAR.
- **Inicio**
  - El hero no cambia.
  - Debajo: tira de 4 cifras de la semana, meta en km con anillo, ruta segura del día con su trazo y "Correr esta ruta", y semana atlética con barras de km por día.
  - El chip de clima se oculta sin dato, en lugar de mostrar "LOJA --".
- **Rutas:** pestañas segmentadas, buscador con el botón Filtros (hoja inferior), una sola fila de chips rápidos, y mini mapa del trazo y perfil de elevación en cada tarjeta.
- **Detalle de ruta**
  - Mapa a todo el ancho con volver y compartir.
  - Chips y título en Anton.
  - Tira de cifras: km, desnivel, tu mejor tiempo y carreras. Tu mejor tiempo sale de tus carreras que empiezan a menos de 250 m del inicio y tienen una distancia parecida.
  - Barra fija con el tiempo estimado a tu ritmo y "Correr esta ruta".
- **Comunidad, Eventos y Clubes**
  - Comunidad: métricas de cada publicación en 4 celdas.
  - Eventos: el evento principal lleva cuenta regresiva y barra de cupos.
  - Clubes: cada tarjeta lleva una franja de marca y barra de cupo.
- **Perfil:** selector de periodo (4 semanas, 12 semanas, Año y Todo), 4 indicadores con su tendencia frente al periodo anterior, volumen semanal de 12 semanas y mini mapa en las actividades recientes.
- **Ajustes**
  - Tarjeta de cuenta con gradiente y tres cifras: meta semanal, versión y vista del mapa.
  - Fila "Vista del mapa al correr".
  - Nueva sección "Seguridad y privacidad": compartir en vivo (decidido una vez), ocultar zona de casa, alertas de zona y contactos de emergencia.
  - Se quitó el texto "Club Gacelas Loja".

## Datos

No hizo falta una migración nueva:

- La meta en km usa `Ajustes.metaKmSemana`, que ya existe en `user_settings.prefs`.
- El objetivo y la vista del mapa también viajan en `prefs`.
- La migración `040_rediseno_web.sql` del proceso web (`weekly_goal_km`, `seguridad_rutas()`, `km_grupos()`, `event_results`) se usará desde Android cuando esté en `main`.

## QA

- `RedisenoMovilQaTest` y `RedisenoModalesQaTest` capturan las pantallas y los modales en 393x873, 360x800 y con fuente al 130 %.
- Solo corren con `-Pgacelas.shots=<dir> -Proborazzi.test.record=true`.

## Pendiente

- **Actividad Caminata o Trail en Correr:** falta el tipo de actividad en `activities`.
- **Seguridad en Inicio y Detalle:**
  - Inicio: clima por horas, corredores en vivo y seguridad de la zona con el ECU 911.
  - Detalle de ruta: nota de seguridad 0 a 100.
  - Dependen de la migración 040 y de Open-Meteo `hourly`.
- **Clubes y Comunidad:**
  - Clubes: pestaña Agenda, próxima salida con "Voy", y km del grupo, que depende de `km_grupos()`.
  - Comunidad: historias y ranking semanal.
- **Eventos:** resultados recientes, que dependen de `event_results`.
- **Carrera en curso y Resumen:**
  - Carrera en curso: el número de espectadores en vivo.
  - Resumen: los pasos.

## Android 0.9.17: dirección A "Tablero"

- Dock flotante (Hoy, Rutas, Club, Tú) con CORRER a la derecha; la barra nunca tapa botones.
- Inicio: mapa de Loja como protagonista y rejilla de widgets (semana, racha, clima por hora, ritmo, club y seguridad de la zona).
- Correr: hoja de modos (Ruta del día, Objetivo, Libre). Carrera y Resumen en mosaicos. Rutas: mapa con carrusel de tarjetas.
- Datos reales: clima por hora de Open-Meteo y seguridad de las RPC de la migración 040. Sin servidor no hay notificaciones,
  eventos ni incidentes de ejemplo; los bloques sin fuente se ocultan.
- QA: `TableroQaTest` (capturas de todas las pantallas y modales) y `TableroInteraccionesTest` (toques y navegación).
