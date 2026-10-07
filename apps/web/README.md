# Gacelas Web App

Aplicación web construida con React, TypeScript, Tailwind CSS y Vite.

## Inicio Rápido

1. **Instalar dependencias** (desde la raíz del monorepo):

   ```bash
   pnpm install
   ```

2. **Configurar variables de entorno**:

   ```bash
   cp .env.example .env.local
   ```

   Editar `.env.local` con tus keys:

   ```
   VITE_SUPABASE_URL=https://your-project.supabase.co
   VITE_SUPABASE_ANON_KEY=your-anon-key
   VITE_GOOGLE_MAPS_API_KEY=your-google-maps-key
   ```

3. **Iniciar servidor de desarrollo**:

   ```bash
   pnpm dev
   ```

   O desde esta carpeta:

   ```bash
   pnpm dev
   ```

4. **Abrir en navegador**:
   - http://localhost:5173

## Scripts Disponibles

```bash
pnpm dev        # Servidor de desarrollo
pnpm build      # Build para producción
pnpm preview    # Preview del build
pnpm lint       # Lint con ESLint
pnpm type-check # Verificar tipos TypeScript
pnpm test:unit  # Tests unitarios con Vitest
```

## Estructura

```
src/
├── components/      # Componentes reutilizables
│   ├── Layout.tsx
│   └── Navigation.tsx
├── pages/          # Páginas/rutas
│   ├── Home.tsx
│   ├── Routes.tsx
│   ├── Community.tsx
│   ├── Events.tsx
│   └── Profile.tsx
├── lib/            # Utilidades y clientes
│   └── supabase.ts
├── hooks/          # Custom hooks
├── stores/         # Zustand stores
├── types/          # TypeScript types
├── App.tsx         # App principal con routing
├── main.tsx        # Entry point
└── index.css       # Estilos globales + Tailwind
```

## Stack Tecnológico

- **React 18**: UI library
- **TypeScript**: Type safety
- **Vite**: Build tool y dev server
- **Tailwind CSS**: Utility-first CSS
- **React Router**: Client-side routing
- **Supabase Client**: Backend as a Service
- **Zustand**: State management (ligero)
- **@vis.gl/react-google-maps**: Google Maps React wrapper

## Estado de Desarrollo

Este es el scaffold inicial con placeholders para cada ruta. Las funcionalidades se implementarán según [../../docs/ROADMAP.md](../../docs/ROADMAP.md).

### Rutas Disponibles

- `/` - Home (bienvenida)
- `/routes` - Explorador de rutas (placeholder)
- `/community` - Feed social (placeholder)
- `/events` - Eventos (placeholder)
- `/profile` - Perfil de usuario (placeholder)

## Deployment

### Vercel (Recomendado)

1. Conectar repo en Vercel
2. Configurar Environment Variables:
   - `VITE_SUPABASE_URL`
   - `VITE_SUPABASE_ANON_KEY`
   - `VITE_GOOGLE_MAPS_API_KEY`
3. Build command: `pnpm build`
4. Output directory: `dist`

### Netlify

1. Conectar repo
2. Build settings:
   - Build command: `pnpm build`
   - Publish directory: `apps/web/dist`
3. Environment variables (igual que Vercel)

## Próximos Pasos

Ver [../../docs/ROADMAP.md](../../docs/ROADMAP.md) para el plan de implementación por fases.
