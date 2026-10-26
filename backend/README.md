# Backend / Database

Configuración de Supabase. El schema versionado vive en `supabase/migrations/` y se aplica con Supabase CLI. Esta carpeta solo guarda variables de entorno y notas. El camino caliente de GPS no pasa por Edge Functions.

## Setup de Supabase

1. **Crear proyecto en Supabase**:
   - Ir a https://app.supabase.com
   - Create New Project
   - Anotar:
     - Project URL: `https://[project-id].supabase.co`
     - Anon Key: `eyJhbGc...`

2. **Ejecutar migraciones**:
   - Opción 1: Usar SQL Editor en dashboard de Supabase
     - SQL Editor - New Query
     - Copiar, en orden, los archivos de `supabase/migrations/`
     - Run

   - Opción 2: Usar Supabase CLI (recomendado para producción)
     ```bash
     supabase db push
     ```

3. **Verificar**:
   - Table Editor - Ver tablas creadas
   - Database - Extensions - Verificar PostGIS habilitado

## Estructura de Migraciones

Las migraciones se ejecutan en orden numérico:

```
supabase/migrations/
├── 001_initial_schema.sql       # Schema base (profiles, activities, track_points)
├── 015_capacity.sql             # Lotes, resumenes y bloqueo de insert directo
└── ...                          # Migraciones siguientes, en orden numerico
```

## Environment Variables

Crear archivo `.env` (NO commitear):

```bash
cp .env.example .env
```

Completar con valores de tu proyecto Supabase:

```
SUPABASE_URL=https://your-project.supabase.co
SUPABASE_ANON_KEY=your-anon-key
SUPABASE_SERVICE_ROLE_KEY=your-service-role-key
```

**Importante**: `SERVICE_ROLE_KEY` tiene permisos totales. NUNCA exponerlo en cliente.

## Edge Functions

Las funciones serverless irán en `functions/`:

```
functions/
├── calculate-activity-stats/   # Calcular métricas post-actividad
├── send-notification/          # Enviar notificaciones
└── compress-track-points/      # Comprimir puntos viejos
```

Deployment:

```bash
supabase functions deploy function-name
```

## Queries Útiles

### Buscar actividades cercanas

```sql
SELECT
    a.*,
    ST_Distance(
        a.started_location,
        ST_MakePoint(-78.550341, 0.315653)::geography
    ) as distance_meters
FROM activities a
WHERE
    a.is_public = true
    AND ST_DWithin(
        a.started_location,
        ST_MakePoint(-78.550341, 0.315653)::geography,
        10000  -- 10km radius
    )
ORDER BY distance_meters
LIMIT 20;
```

### Estadísticas de usuario

```sql
SELECT
    COUNT(*) as total_activities,
    SUM(distance_meters) / 1000.0 as total_km,
    SUM(duration_seconds) / 3600.0 as total_hours
FROM activities
WHERE user_id = 'user-uuid-here'
    AND start_time >= NOW() - INTERVAL '1 year';
```

## Documentación

Ver [../docs/DATA_MODEL.md](../docs/DATA_MODEL.md) para el modelo de datos completo.

## Backend propio por contextos (F8)

Esqueleto del servicio TypeScript descrito en `docs/ARQUITECTURA-DDD.md` (4.5). Por ahora solo contiene casos de uso y adaptadores sobre Supabase; no tiene servidor HTTP ni se despliega (el proveedor queda para el ADR de la decisión D6).

```
backend/src/
  shared/supabase.ts            cliente con la clave de servicio
  contexts/seguridad/
    domain/                     índice, niveles de riesgo y puertos
    application/consultas.ts    ConsultarSeguridadDeRuta, SugerirHorarioSeguro, incidentes recientes
    application/importar-ecu911.ts  importador existente del ECU 911
    infrastructure/supabase.ts  RPC seguridad_zona_ruta y vista incidentes_publicos
  contexts/analitica/
    domain/                     estadísticas, periodo y puerto ConsultaAnalitica
    application/panel.ts        VerPanelAdmin y exportación CSV
    infrastructure/supabase.ts  RPC admin_estadisticas
backend/tests/contexts/         pruebas con un doble del cliente de Supabase
```

Reglas: `domain` no importa nada de fuera del contexto; `application` solo usa `domain`; Supabase solo aparece en `infrastructure` y en `shared/supabase.ts`.

### Cómo correrlo

Desde la raíz del repositorio, con Node 20 y pnpm:

```bash
pnpm install
pnpm --filter backend type-check   # tsc sin emitir
pnpm --filter backend test:unit    # vitest, sin red ni Supabase
```

Las pruebas no necesitan credenciales. Para usar los adaptadores contra un proyecto real hay que definir `SUPABASE_URL` y `SUPABASE_SERVICE_ROLE_KEY` en el entorno (por ejemplo en `backend/.env`, que no se versiona) y crear el cliente con `crearClienteServicio()`. Los avisos y el consumidor del outbox se añadirán cuando exista la tabla `kernel.eventos_salida` (F7).
