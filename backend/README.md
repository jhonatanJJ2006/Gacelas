# Backend / Database

Configuración de Supabase y migraciones SQL.

## Setup de Supabase

1. **Crear proyecto en Supabase**:
   - Ir a https://app.supabase.com
   - Create New Project
   - Anotar:
     - Project URL: `https://[project-id].supabase.co`
     - Anon Key: `eyJhbGc...`

2. **Ejecutar migraciones**:
   - Opción 1: Usar SQL Editor en dashboard de Supabase
     - SQL Editor → New Query
     - Copiar contenido de `migrations/001_initial_schema.sql`
     - Run
   
   - Opción 2: Usar Supabase CLI (recomendado para producción)
     ```bash
     supabase db push
     ```

3. **Verificar**:
   - Table Editor → Ver tablas creadas
   - Database → Extensions → Verificar PostGIS habilitado

## Estructura de Migraciones

Las migraciones se ejecutan en orden numérico:

```
migrations/
├── 001_initial_schema.sql       # Schema base (profiles, activities, track_points)
├── 002_add_routes.sql          # (Futuro) Rutas compartidas
├── 003_add_social.sql          # (Futuro) Follows, comments, likes
├── 004_add_clubs_events.sql    # (Futuro) Clubes y eventos
└── ...
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

**⚠️ Importante**: `SERVICE_ROLE_KEY` tiene permisos totales. NUNCA exponerlo en cliente.

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
