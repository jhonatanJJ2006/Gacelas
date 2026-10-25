# Incidentes del ECU 911

Gacelas muestra en el detalle de cada ruta una tarjeta "Seguridad de la zona". Se calcula con la base de emergencias abierta del SIS ECU 911 y con los reportes verificados de los corredores (ver `docs/INCIDENTES.md`).

## Fuente y licencia

- Conjunto de datos: "Base de emergencias" en el portal de datos abiertos del Ecuador, https://www.datosabiertos.gob.ec/dataset/base-de-emergencias
- Publicador: Servicio Integrado de Seguridad ECU 911.
- Licencia: CC-BY. Hay que citar la fuente siempre que se muestren los datos. La tarjeta muestra "SIS ECU 911 (CC-BY)" y el mes hasta el que llegan los datos.
- Se publica un archivo por mes (CSV o XLSX), con meses de retraso.

Columnas que usa el importador: `Fecha` (dd/mm/aaaa), `Provincia`, `Canton`, `Cod_Parroquia`, `Parroquia`, `Servicio`, `Subtipo`. Los nombres se reconocen sin importar tildes, mayúsculas ni espacios.

## Limitaciones

- No trae hora ni coordenadas: el nivel más fino es la parroquia y el día de la semana.
- Son llamadas de emergencia, no delitos confirmados. Incluye tránsito, salud, incendios y otros servicios.
- Llega con retraso. El indicador es orientativo y la tarjeta lo dice: no garantiza la seguridad de nadie.

## Qué se guarda

No se guarda ninguna fila original. El importador filtra el cantón Loja (códigos de parroquia que empiezan por `1101`) y guarda solo conteos en `incidentes_agregados`, por parroquia, servicio, subtipo, año, mes y día de la semana. Cada carga queda registrada en `importaciones_ecu911`.

Al importar un mes, la función `reemplazar_incidentes_ecu911` borra los conteos de ese mismo periodo e inserta los nuevos en una sola transacción. Repetir la importación no duplica datos.

Tablas y funciones (migración `030_incidentes.sql`):

- `parroquias`: las 14 parroquias del cantón Loja con población y geometría (INEC, DPA 2020).
- `incidentes_agregados` e `importaciones_ecu911`: lectura pública, escritura solo con `service_role` o administrador.
- `nivel_seguridad_ruta(ruta, dia_semana, hora)`: nivel bajo, medio o alto por tramo de la ruta.
- `seguridad_zona_ruta(ruta)`: resumen para la tarjeta (nivel por día, tipos más frecuentes, mes de los datos).

El índice de una parroquia es su tasa de emergencias por habitante en los últimos 12 meses cargados, para ese día de la semana, dividida para la tasa del cantón. Bajo es menos de 0,75, medio hasta 1,5 y alto desde 1,5. Si no hay datos se muestra medio.

## Importar a mano

Descarga el archivo del mes desde el portal y ejecuta desde la raíz del repositorio:

```bash
SUPABASE_URL=https://<proyecto>.supabase.co \
SUPABASE_SERVICE_ROLE_KEY=<clave de servicio> \
pnpm --filter backend incidentes:importar -- ruta/al/archivo.xlsx
```

También acepta CSV. Con `--simular` solo lee y agrega el archivo, sin escribir en Supabase. Con `--descargar` busca en el portal el recurso más reciente y lo importa.

Las credenciales se leen de variables de entorno y nunca se imprimen.

Códigos de salida:

- `0`: importación correcta.
- `1`: error (archivo ilegible, columnas faltantes, error de Supabase).
- `2`: uso incorrecto.
- `3`: el portal respondió 403 (bloqueado).

## Bloqueo del portal

Desde algunos servidores fuera de Ecuador el portal responde 403, tanto a la API CKAN como a la página del conjunto de datos. En ese caso el script termina con el código 3 y hay que usar la ruta manual: descargar el archivo desde un navegador en Ecuador e importarlo con el comando anterior, o enviarlo a la Edge Function.

## Edge Function mensual

`supabase/functions/importar-ecu911` hace lo mismo que el script. No está desplegada; para desplegarla:

```bash
supabase functions deploy importar-ecu911 --no-verify-jwt
```

Exige la cabecera `Authorization: Bearer <clave de servicio>`. Sin cuerpo descarga el recurso más reciente; con un archivo en el cuerpo (cabecera `x-archivo` con el nombre) lo importa. Si el portal responde 403 devuelve `bloqueado: true`.

Programación con pg_cron y pg_net el día 15 de cada mes a las 06:00 (hora de Ecuador, 11:00 UTC). La clave se guarda en Vault, no en el texto del job:

```sql
select cron.schedule(
  'importar-ecu911-mensual',
  '0 11 15 * *',
  $$
  select net.http_post(
    url := 'https://<proyecto>.supabase.co/functions/v1/importar-ecu911',
    headers := jsonb_build_object(
      'Authorization', 'Bearer ' || (select decrypted_secret from vault.decrypted_secrets where name = 'service_role_key'),
      'Content-Type', 'application/json'
    ),
    body := '{}'::jsonb
  );
  $$
);
```

Revisa `importaciones_ecu911` después de cada ejecución: una fila con estado `error` indica que hubo que importar a mano.

## Aviso al usuario

Texto que muestran la web y Android bajo la tarjeta:

> Indicador orientativo con datos agregados del ECU 911 por parroquia (sin hora exacta) y reportes de corredores. Llega con meses de retraso y no garantiza tu seguridad: mantente atento y, ante una emergencia, llama al 911.

## Pruebas

- `pnpm --filter backend test:unit`: lectura de CSV y XLSX, columnas con tildes, filtro de Loja y agregación, con datos ficticios en `backend/tests/ecu911/`.
- La vista previa (`?vista-previa` en la web, o la app sin servidor) muestra datos de ejemplo marcados como tales.
