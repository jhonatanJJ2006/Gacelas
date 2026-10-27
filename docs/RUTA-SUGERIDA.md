# Ruta sugerida para hoy

Tarjeta de Inicio en web y Android. Desde la migracion 035 la decide Supabase con la funcion
`public.ruta_sugerida_hoy(p_lat, p_lng)`; las apps solo la muestran.

## Reglas

1. Siempre segura. Para cada ruta se calcula `nivel_seguridad_ruta` (datos ECU 911 por parroquia y
   reportes de corredores verificados, la misma fuente que la tarjeta "Seguridad de la zona") con el
   dia de la semana y la hora actuales de Ecuador. Una ruta es segura si su indice es 1.0 o menos, es
   decir, nivel bajo, o nivel medio sin superar el promedio del canton. Nivel alto o sin dato de
   seguridad nunca se sugiere.
2. Sin repetir. Se excluyen las rutas corridas o sugeridas en los dias de Ecuador (America/Guayaquil,
   UTC-5) hoy, ayer, anteayer y hace 3 dias. Ejemplo: el lunes 26 no vuelve nada del jueves 23 al
   lunes 26; lo del miercoles 22 ya puede volver.
3. Orden. Puntaje = |distancia de la ruta - objetivo| / objetivo, mas la distancia del usuario al
   inicio de la ruta dividida para 10 km si la web tiene permiso de ubicacion. El objetivo es la media
   de las ultimas 5 actividades (5 km sin historial). Empates: hash de id y fecha, que rota cada dia.

Si nada cumple 1 y 2 la tarjeta muestra un estado vacio honesto con el motivo ("ninguna ruta tiene
un nivel de seguridad suficiente para esta hora" o "ya corriste o te sugerimos todas las rutas
seguras en los ultimos 3 dias"). Nunca se relaja la seguridad para llenar la tarjeta.

## Estable durante el dia

La primera llamada del dia elige la ruta y la guarda en `rutas.route_suggestions` (una fila por
usuario y dia, clave primaria `(user_id, dia)`). Las llamadas siguientes de ese dia devuelven la
misma ruta, aunque cambie la hora o la ubicacion. La seguridad se evalua con la hora de esa primera
consulta. Sin sesion se calcula la sugerencia pero no se registra.

## Que cuenta como "corrida"

Las actividades no guardan `route_id`. Una ruta cuenta como corrida si:

- hay una fila en `route_runs` (carrera iniciada con "Correr esta ruta"), o
- una actividad empezo a 300 m o menos del primer punto de la ruta y midio entre 70 % y 130 % de la
  ruta.

Limitacion: una carrera que recorre la ruta empezando por otro punto, o mucho mas larga, no se
detecta.

## Datos

- Tabla `rutas.route_suggestions` con vista `public.route_suggestions` (security_invoker, ADR 005).
- RLS: cada usuario solo lee sus filas. Nadie escribe directo; solo la funcion, que es
  SECURITY DEFINER con `search_path = public` y usa siempre `auth.uid()`.
- Migracion `supabase/migrations/035_ruta_sugerida.sql`, reversion en
  `supabase/rollback/035_ruta_sugerida.down.sql`, prueba en `backend/tests/rls/035_ruta_sugerida.sql`.

## Sin conexion

Web (`contexts/rutas/domain/sugerida.ts`) y Android (`rutas/domain/reglas/RutaSugerida.kt`) tienen
la misma regla en codigo puro, con pruebas. Cada respuesta del servidor guarda en el dispositivo la
lista de rutas seguras y el historial de sugerencias. Sin red se aplica la regla local con esa
lista y la tarjeta dice "Segura segun la ultima consulta". Si nunca hubo respuesta del servidor, la
tarjeta pide conectarse en vez de adivinar.

## Estado de los datos (oct 2026)

`incidentes_agregados` todavia no tiene filas del ECU 911, asi que todas las rutas dan indice 1.0
(promedio) salvo las que tengan reportes de corredores cerca. Al cargar los datos mensuales del ECU 911
el filtro empieza a distinguir zonas sin cambiar codigo.
