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

## Android 0.9.18: corredor 3D en Inicio

- Inicio: el corredor 3D vuelve a pararse sobre el mapa de Loja, entre los controles de arriba y la tarjeta de la ruta del día.
- Usa el mismo Filament translúcido (sobreFondo) que se usaba en Inicio desde 0.9.5.
- Barra inferior: vuelve la barra clásica (Inicio, Rutas, Correr al centro, Comunidad y Perfil) en lugar del dock flotante.
- Rutas: selector Lista | Mapa siempre visible arriba en los dos modos; sustituye al botón "Ver como lista".
- Comunidad y Perfil no cambian.

## Android 0.9.19: escenario en Inicio, Rutas en Lista y pulido

- Inicio: detrás del corredor 3D vuelve el escenario de 0.9.16 (fondo morado con rayas y "CORRE LOJA"), en lugar del mapa.
- Rutas abre en Lista. El Mapa muestra tu posición (punto azul) y la incluye en el encuadre si estás cerca de la ruta; "atrás" desde el Mapa vuelve a la Lista.
- Rutas en Lista reserva la barra de estado y deja aire arriba; la columna de controles del Mapa ya no choca con los chips.
- Rótulos de tarjetas con mayúscula inicial a 12 sp, coma decimal en Rutas, barra del Resumen sin solaparse y "Sin tiempo" en Detalle de ruta.

## Android 0.9.20: cada bloque con su vista, estados vacíos y avatar editable

- Inicio: corredor 3D más grande con saludo y accesos a la racha y a la semana. "Esta semana", Clima (7 días atrás y 7 adelante,
  con rutas recomendadas según el clima) y Seguridad en tu zona (mapa de incidentes) tienen pantalla propia. La racha abre
  una hoja propia con la semana en llamas, hitos y mapa de calor. El buscador sugiere mientras escribes y sus tarjetas miden igual.
- Rutas: la lista muestra fotos de cada ruta en una galería y el detalle abre con fotos y mapa. Lista paginada de 20 en 20 sin
  quedar bajo la barra inferior. Estados vacíos en Mis rutas (con Crear ruta) y Guardadas. El modo Mapa pinta cada ruta con el
  color de su dificultad (fácil verde, media ámbar, difícil roja) y ya no tiene 3D/2D ni Reportar.
- Ubicación: Cerca de mí, Centrar, Seguridad y Correr piden permiso y abren el diálogo nativo para activar el GPS.
- Comunidad: publicaciones con galería (ruta y fotos), estados vacíos en Siguiendo (con corredores sugeridos), feed y clubes,
  y comentarios en una hoja inferior al estilo de Instagram.
- Correr: vuelve "¿Listo para correr?" antes del mapa de modos y la cuenta regresiva tiene anillo, animación y vibración.
  El mapa muestra edificios en 3D y relieve sombreado.
- Perfil: 20 logros y 12 medallas, cada uno con su detalle, progreso y cómo conseguirlo, y pantallas propias. El avatar 3D
  cambia color de camiseta, piel, shorts y zapatillas (máscara de zonas en `tools/avatar`) y el estilo cambia la animación.
- Supabase: migraciones 047 (fotos de demostración de rutas y publicaciones) y 048 (códigos de logros nuevos).

## Android 0.9.21: hojas que se arrastran, carrera con más mapa, fotos y 3D más rápidos

- Hojas y modales (`HojaC2`, `DialogoC2`, `ModalC2`): la manija de arriba ahora arrastra. Si se baja más de un cuarto del alto
  o se lanza hacia abajo, la hoja se cierra; si no, vuelve a su sitio. Los modales obligatorios (términos, actualización
  obligatoria) vuelven solos. Con lector de pantalla, la manija es el botón "Cerrar".
- Carrera en curso: la hoja de datos va plegada por defecto (unos 200 dp en vez de 410) con distancia, ritmo, tiempo y los
  botones a 52 dp. Tocar o subir el tirador despliega el objetivo, el promedio, el desnivel, el km en curso y los parciales.
- Navegación: corregido el error por el que, tras salir de una carrera abierta desde Comunidad, al pulsar Comunidad se abría
  la carrera. La pila de la carrera ya no se guarda como estado de la pestaña (`Destinos.flujoCarrera`).
- Fotos:
  - Las fotos de Supabase Storage se piden a Vercel Image Optimization (`/_vercel/image`, WebP de 256, 640 o 1080 px y caché
    de 30 días, configurado en `apps/web/vercel.json`). Si Vercel falla, se carga la original.
  - Mientras cargan se ve un esqueleto que late; si no cargan, un icono.
  - Las fotos nuevas del feed y de rutas se suben reescaladas a 1600 px en JPEG al 80 % y sin EXIF, igual que las de incidentes.
  - La foto de perfil se decodifica fuera del hilo principal.
- Corredor 3D:
  - Las texturas del GLB bajan de 2048 a 1024 px (de unos 45 MB a 11 MB de memoria de GPU).
  - La textura del avatar tenida se guarda en el teléfono (`filesDir/avatar3d`), así que el tenido solo se hace la primera vez.
  - El GLB se lee una sola vez por proceso.

## Android 1.0.0: historias en una galería

Misma entrega que 0.9.23, publicada como versión estable. Compartir abre la cuadrícula de plantillas, la foto queda detrás y se ajustan brillo, contraste, saturación y opacidad.

## Android 0.9.23: historias en una galería

- Compartir una carrera o una ruta abre una cuadrícula de plantillas 9:16. La foto elegida queda detrás de todas.
- Plantillas nuevas: distancia, columna lateral y tarjeta con la fecha. La historia de marca Runcity es una tarjeta más y no usa la foto.
- Al abrir una plantilla se arrastra, se pellizca y se ajusta el brillo, el contraste, la saturación y la opacidad de la foto.
- La barra inferior manda la historia a Instagram, copia la imagen, la guarda o copia el enlace. Más abre WhatsApp y el selector del sistema.

## Android 0.9.22: fotos de demostración más ligeras

- Las fotos de demostración de rutas y del feed (picsum.photos, migración 047) se piden al ancho de la vista (256, 640 o
  1080 px) y en WebP, en vez del JPEG de 1200 px: unos 19 KB por foto en lugar de unos 86 KB. Las fotos de Supabase
  Storage siguen pasando por Vercel Image Optimization.
