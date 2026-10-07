# Seguridad y Privacidad - Gacelas

## Principios de Seguridad

1. **Privacy by default**: Todas las features sensibles (live tracking, ubicación de casa) requieren opt-in explícito
2. **Principle of least privilege**: RLS policies garantizan acceso solo a datos autorizados
3. **Defense in depth**: Validación en cliente, servidor y base de datos
4. **Transparency**: Usuario sabe en todo momento qué datos se comparten y con quién

## Row Level Security (RLS)

Supabase RLS proporciona seguridad a nivel de fila en PostgreSQL. Todas las políticas usan `auth.uid()` para identificar al usuario actual.

### Políticas de Seguridad por Tabla

#### `profiles`

```sql
-- Política: Cualquiera puede ver perfiles públicos
CREATE POLICY "Public profiles are viewable by everyone"
ON profiles FOR SELECT
USING (true);  -- Todos los perfiles son visibles (info básica)

-- Política: Solo el dueño puede actualizar su perfil
CREATE POLICY "Users can update own profile"
ON profiles FOR UPDATE
USING (auth.uid() = id);

-- Política: Solo el dueño puede ver campos sensibles
-- Implementado en la query del cliente: filtrar home_location
```

#### `activities`

```sql
-- Política: Ver actividades propias siempre
CREATE POLICY "Users can view own activities"
ON activities FOR SELECT
USING (user_id = auth.uid());

-- Política: Ver actividades públicas de otros
CREATE POLICY "Anyone can view public activities"
ON activities FOR SELECT
USING (is_public = true);

-- Política: Ver actividades de seguidos si su perfil es 'followers' o 'public'
CREATE POLICY "View activities of followed users"
ON activities FOR SELECT
USING (
    user_id IN (
        SELECT followed_id FROM follows
        WHERE follower_id = auth.uid()
        AND status = 'accepted'
    )
    AND user_id IN (
        SELECT id FROM profiles
        WHERE privacy_level IN ('public', 'followers')
    )
);

-- Política: Solo el dueño puede insertar actividades
CREATE POLICY "Users can insert own activities"
ON activities FOR INSERT
WITH CHECK (user_id = auth.uid());

-- Política: Solo el dueño puede actualizar sus actividades
CREATE POLICY "Users can update own activities"
ON activities FOR UPDATE
USING (user_id = auth.uid());

-- Política: Solo el dueño puede eliminar sus actividades
CREATE POLICY "Users can delete own activities"
ON activities FOR DELETE
USING (user_id = auth.uid());
```

#### `track_points`

```sql
-- Política: Ver track_points de actividades accesibles
CREATE POLICY "View track_points of accessible activities"
ON track_points FOR SELECT
USING (
    activity_id IN (
        SELECT id FROM activities
        WHERE user_id = auth.uid()
        OR is_public = true
        OR user_id IN (
            SELECT followed_id FROM follows
            WHERE follower_id = auth.uid()
            AND status = 'accepted'
        )
    )
);

-- Política: Solo el dueño de la actividad puede insertar track_points
CREATE POLICY "Insert track_points for own activities"
ON track_points FOR INSERT
WITH CHECK (
    activity_id IN (
        SELECT id FROM activities WHERE user_id = auth.uid()
    )
);
```

#### `routes`

```sql
-- Política: Ver rutas propias
CREATE POLICY "Users can view own routes"
ON routes FOR SELECT
USING (user_id = auth.uid());

-- Política: Ver rutas públicas
CREATE POLICY "Anyone can view public routes"
ON routes FOR SELECT
USING (is_public = true);

-- Política: Insertar/actualizar/eliminar solo rutas propias
CREATE POLICY "Users can manage own routes"
ON routes FOR ALL
USING (user_id = auth.uid());
```

#### `follows`

```sql
-- Política: Ver follows donde eres follower o followed
CREATE POLICY "View follows where you are involved"
ON follows FOR SELECT
USING (
    follower_id = auth.uid()
    OR followed_id = auth.uid()
);

-- Política: Crear follow si eres el follower
CREATE POLICY "Users can create follows as follower"
ON follows FOR INSERT
WITH CHECK (follower_id = auth.uid());

-- Política: Actualizar follow (aceptar/rechazar) si eres el followed
CREATE POLICY "Users can update follows if they are followed"
ON follows FOR UPDATE
USING (followed_id = auth.uid());

-- Política: Eliminar follow si eres el follower (unfollow)
CREATE POLICY "Users can delete follows if they are follower"
ON follows FOR DELETE
USING (follower_id = auth.uid());
```

#### `comments`

```sql
-- Política: Ver comentarios de contenido accesible
CREATE POLICY "View comments on accessible content"
ON comments FOR SELECT
USING (
    (commentable_type = 'activity' AND commentable_id IN (
        SELECT id FROM activities WHERE is_public = true OR user_id = auth.uid()
    ))
    OR (commentable_type = 'route' AND commentable_id IN (
        SELECT id FROM routes WHERE is_public = true OR user_id = auth.uid()
    ))
    OR (commentable_type = 'post' AND commentable_id IN (
        SELECT id FROM posts  -- asume que posts tienen su propia política
    ))
);

-- Política: Crear comentario si estás autenticado
CREATE POLICY "Authenticated users can create comments"
ON comments FOR INSERT
WITH CHECK (user_id = auth.uid());

-- Política: Actualizar/eliminar solo tus comentarios
CREATE POLICY "Users can manage own comments"
ON comments FOR UPDATE
USING (user_id = auth.uid());

CREATE POLICY "Users can delete own comments"
ON comments FOR DELETE
USING (user_id = auth.uid());
```

#### `notifications`

```sql
-- Política: Solo ver tus notificaciones
CREATE POLICY "Users can view own notifications"
ON notifications FOR SELECT
USING (user_id = auth.uid());

-- Política: Solo actualizar tus notificaciones (marcar como leída)
CREATE POLICY "Users can update own notifications"
ON notifications FOR UPDATE
USING (user_id = auth.uid());
```

## Autenticación

### Métodos Soportados

#### Email + Password (MVP)

```typescript
const { data, error } = await supabase.auth.signUp({
  email: 'user@example.com',
  password: 'securePassword123!',
  options: {
    data: {
      full_name: 'Juan Pérez',
      username: 'juanperez',
    },
  },
})
```

**Validaciones**:

- Password: mínimo 8 caracteres, al menos 1 número
- Email: formato válido
- Username: único, 3-20 caracteres, alfanumérico + guion bajo

#### OAuth (Post-MVP)

- Google Sign-In
- Apple Sign-In
- Facebook Sign-In

### Sesiones

**JWT tokens** con refresh automático:

- Access token: 1 hora de validez
- Refresh token: 30 días de validez
- Auto-refresh: 5 minutos antes de expirar

```typescript
// Supabase maneja esto automáticamente
supabase.auth.onAuthStateChange((event, session) => {
  if (event === 'TOKEN_REFRESHED') {
    console.log('Token refreshed')
  }
})
```

### Reset de Password

```typescript
// 1. Solicitar reset
await supabase.auth.resetPasswordForEmail('user@example.com', {
  redirectTo: 'https://gacelas.app/reset-password',
})

// 2. Usuario recibe email con link
// 3. Confirmar nueva contraseña
await supabase.auth.updateUser({
  password: 'newSecurePassword123!',
})
```

## Privacidad de Ubicación

### Home Zone Fuzzing

**Implementación en backend** (Edge Function):

```typescript
// backend/functions/fuzz-location.ts
export function fuzzHomeLocation(
  point: { lat: number; lon: number },
  homeLocation: { lat: number; lon: number },
  radiusMeters: number = 300
): { lat: number; lon: number } {
  const distance = calculateDistance(point, homeLocation)

  if (distance < radiusMeters) {
    // Punto está dentro de zona de casa
    const bearing = calculateBearing(homeLocation, point)
    const fuzzedPoint = movePoint(homeLocation, radiusMeters, bearing)
    return fuzzedPoint
  }

  return point // Fuera de zona, no modificar
}

// Aplicar al query de track_points
export async function getActivityTrackPoints(activityId: string) {
  const { data: activity } = await supabase
    .from('activities')
    .select('user_id')
    .eq('id', activityId)
    .single()

  const { data: profile } = await supabase
    .from('profiles')
    .select('home_location, home_radius_meters')
    .eq('id', activity.user_id)
    .single()

  const { data: points } = await supabase
    .from('track_points')
    .select('*')
    .eq('activity_id', activityId)
    .order('timestamp', { ascending: true })

  // Fuzz primeros y últimos puntos
  const fuzzedPoints = points.map((point, index) => {
    if (index < 10 || index > points.length - 10) {
      // Primeros/últimos 10 puntos
      const [lon, lat] = point.location.coordinates
      const fuzzed = fuzzHomeLocation(
        { lat, lon },
        profile.home_location,
        profile.home_radius_meters
      )
      return { ...point, location: { ...point.location, coordinates: [fuzzed.lon, fuzzed.lat] } }
    }
    return point
  })

  return fuzzedPoints
}
```

### Live Tracking Privacy

**Control granular de visibilidad**:

```typescript
// Tabla: live_sharing_settings
CREATE TABLE live_sharing_settings (
    activity_id UUID PRIMARY KEY REFERENCES activities(id),
    visibility_type TEXT CHECK (visibility_type IN ('all_followers', 'selected_users', 'club', 'event', 'public_link')),
    allowed_user_ids UUID[],  -- Si visibility_type = 'selected_users'
    club_id UUID,  -- Si visibility_type = 'club'
    event_id UUID,  -- Si visibility_type = 'event'
    public_link_token TEXT UNIQUE,  -- Si visibility_type = 'public_link'
    expires_at TIMESTAMP WITH TIME ZONE
);
```

**Verificar acceso a live tracking**:

```typescript
export async function canViewLiveTracking(viewerId: string, activityId: string): Promise<boolean> {
  const { data: activity } = await supabase
    .from('activities')
    .select('user_id, is_live_shared')
    .eq('id', activityId)
    .single()

  if (!activity.is_live_shared) return false
  if (activity.user_id === viewerId) return true // Dueño siempre puede ver

  const { data: settings } = await supabase
    .from('live_sharing_settings')
    .select('*')
    .eq('activity_id', activityId)
    .single()

  switch (settings.visibility_type) {
    case 'all_followers':
      const { count } = await supabase
        .from('follows')
        .select('*', { count: 'exact', head: true })
        .eq('follower_id', viewerId)
        .eq('followed_id', activity.user_id)
        .eq('status', 'accepted')
      return count > 0

    case 'selected_users':
      return settings.allowed_user_ids.includes(viewerId)

    case 'club':
      const { count: memberCount } = await supabase
        .from('club_members')
        .select('*', { count: 'exact', head: true })
        .eq('club_id', settings.club_id)
        .eq('user_id', viewerId)
      return memberCount > 0

    case 'event':
      const { count: participantCount } = await supabase
        .from('event_participants')
        .select('*', { count: 'exact', head: true })
        .eq('event_id', settings.event_id)
        .eq('user_id', viewerId)
      return participantCount > 0

    case 'public_link':
      return true // Cualquiera con el link

    default:
      return false
  }
}
```

## Seguridad de Comunicación

### HTTPS Everywhere

- **Web**: Forzar HTTPS en producción (Supabase lo hace automáticamente)
- **API**: Todas las requests a Supabase usan HTTPS
- **WebSockets**: Realtime usa WSS (WebSocket Secure)

### API Keys

**Supabase Anon Key** (público, seguro con RLS):

```typescript
// Puede estar en código cliente
const supabaseUrl = 'https://xxxxx.supabase.co'
const supabaseAnonKey = 'eyJhbGc...' // Público, seguro con RLS
```

**Supabase Service Role Key** (privado, solo backend):

```typescript
// NUNCA exponer en cliente, solo en Edge Functions
const supabaseServiceKey = process.env.SUPABASE_SERVICE_ROLE_KEY
```

### Rate Limiting

**Implementación con Edge Function**:

```typescript
// backend/functions/_shared/rate-limit.ts
export async function checkRateLimit(
  userId: string,
  action: string,
  limit: number,
  windowSeconds: number
): Promise<{ allowed: boolean; remaining: number }> {
  const key = `ratelimit:${userId}:${action}`

  // Usar Upstash Redis o similar
  const count = await redis.incr(key)
  if (count === 1) {
    await redis.expire(key, windowSeconds)
  }

  const allowed = count <= limit
  const remaining = Math.max(0, limit - count)

  return { allowed, remaining }
}

// Uso en Edge Function
const { allowed, remaining } = await checkRateLimit(
  userId,
  'create_activity',
  10, // 10 actividades
  86400 // por día
)

if (!allowed) {
  return new Response(JSON.stringify({ error: 'Rate limit exceeded' }), { status: 429 })
}
```

## Protección contra Ataques

### SQL Injection

**Protegido por Supabase**: Todos los queries usan prepared statements.

```typescript
// SEGURO: Supabase maneja escaping automáticamente
const { data } = await supabase.from('activities').select('*').eq('user_id', userId) // Parámetro escapado automáticamente
```

### XSS (Cross-Site Scripting)

**Sanitización de input** en cliente:

```typescript
import DOMPurify from 'dompurify'

// Sanitizar HTML en descripciones
function sanitizeContent(content: string): string {
  return DOMPurify.sanitize(content, {
    ALLOWED_TAGS: ['b', 'i', 'em', 'strong', 'a', 'br'],
    ALLOWED_ATTR: ['href'],
  })
}
```

**Content Security Policy** (CSP):

```html
<meta
  http-equiv="Content-Security-Policy"
  content="default-src 'self'; 
               script-src 'self' https://maps.googleapis.com; 
               style-src 'self' 'unsafe-inline'; 
               img-src 'self' data: https:;"
/>
```

### CSRF (Cross-Site Request Forgery)

**Protegido por Supabase Auth**: JWT tokens en Authorization header, no cookies.

### DDoS

**Mitigación**:

1. Rate limiting por usuario (ver arriba)
2. Cloudflare o similar en frontend
3. Supabase maneja límites de conexiones

## Almacenamiento de Datos Sensibles

### Contraseñas

**Supabase Auth**: Usa bcrypt con salt automáticamente.

### Tokens

- Access tokens: JWT, almacenados en memoria (no localStorage)
- Refresh tokens: Almacenados en httpOnly cookies (web) o Keychain/KeyStore (móvil)

```typescript
// Web: Usar SecureStore de Supabase
// No guardar en localStorage (vulnerable a XSS)

// Android: Usar EncryptedSharedPreferences
val masterKey = MasterKey.Builder(context)
    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
    .build()

val sharedPreferences = EncryptedSharedPreferences.create(
    context,
    "gacelas_secure_prefs",
    masterKey,
    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
)
```

## Auditoría y Logs

### Eventos Auditables

```sql
CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES auth.users(id),
    action TEXT NOT NULL,  -- 'login', 'activity_created', 'profile_updated', etc.
    resource_type TEXT,
    resource_id UUID,
    ip_address INET,
    user_agent TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX audit_logs_user_id_idx ON audit_logs(user_id);
CREATE INDEX audit_logs_created_at_idx ON audit_logs(created_at DESC);
```

**Eventos registrados**:

- Login/logout
- Cambios de configuración de privacidad
- Inicio/fin de live tracking
- Eliminación de actividades
- Cambios de contraseña

### Retención de Logs

- Logs de auditoría: 1 año
- Logs de errores: 90 días
- Logs de acceso: 30 días

## Cumplimiento de Privacidad

### GDPR (si aplicable en futuro internacional)

**Derechos del usuario**:

1. **Derecho de acceso**: Exportar todos sus datos
2. **Derecho de rectificación**: Editar datos personales
3. **Derecho al olvido**: Eliminar cuenta y todos los datos
4. **Derecho de portabilidad**: Descargar datos en formato estándar (JSON)

**Implementación**:

```typescript
// Edge Function: Exportar datos de usuario
export async function exportUserData(userId: string) {
  const [profile, activities, routes, follows, comments] = await Promise.all([
    supabase.from('profiles').select('*').eq('id', userId).single(),
    supabase.from('activities').select('*').eq('user_id', userId),
    supabase.from('routes').select('*').eq('user_id', userId),
    supabase.from('follows').select('*').or(`follower_id.eq.${userId},followed_id.eq.${userId}`),
    supabase.from('comments').select('*').eq('user_id', userId),
  ])

  return {
    profile: profile.data,
    activities: activities.data,
    routes: routes.data,
    follows: follows.data,
    comments: comments.data,
    exported_at: new Date().toISOString(),
  }
}

// Edge Function: Eliminar cuenta
export async function deleteUserAccount(userId: string) {
  // Soft delete: marcar como deleted
  await supabase
    .from('profiles')
    .update({
      deleted_at: new Date().toISOString(),
      email: `deleted_${userId}@gacelas.app`,
      username: `deleted_${userId}`,
    })
    .eq('id', userId)

  // Hard delete después de 30 días (job programado)
}
```

## Checklist de Seguridad Pre-Launch

- [ ] Todas las tablas tienen RLS habilitado
- [ ] Políticas RLS testeadas con múltiples usuarios
- [ ] Service Role Key nunca expuesta en cliente
- [ ] Rate limiting implementado
- [ ] Home zone fuzzing testeado
- [ ] HTTPS forzado en producción
- [ ] CSP headers configurados
- [ ] Input sanitization en forms
- [ ] Logs de auditoría funcionando
- [ ] Backup de base de datos configurado (Supabase automático)
- [ ] Procedimiento de incident response documentado

## Contacto de Seguridad

**Reportar vulnerabilidades**: security@gacelas.app (cuando esté disponible)

**Bug bounty program**: Considerar en futuro si el proyecto crece.

---

Ver [BUSINESS_RULES.md](BUSINESS_RULES.md) para reglas de negocio que estas políticas implementan.
