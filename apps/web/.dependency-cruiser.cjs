/**
 * Reglas de arquitectura DDD (docs/adr/004-arquitectura-ddd.md), en modo aviso.
 * Ejecutar: pnpm --filter @gacelas/web arch
 */
module.exports = {
  forbidden: [
    {
      name: 'sin-ciclos',
      severity: 'warn',
      from: { path: '^src' },
      to: { circular: true },
    },
    {
      name: 'contexto-solo-por-api',
      comment: 'Otro contexto solo se importa por su api.ts (o index.ts mientras se migra).',
      severity: 'warn',
      from: { path: '^src/contexts/([^/]+)/' },
      to: {
        path: '^src/contexts/([^/]+)/.+',
        pathNot: ['^src/contexts/$1/', '^src/contexts/[^/]+/(api|index)\\.ts$'],
      },
    },
    {
      name: 'dominio-puro',
      comment: 'domain no depende de React, Supabase ni de otras capas.',
      severity: 'warn',
      from: { path: '^src/contexts/[^/]+/domain/' },
      to: {
        path: [
          'node_modules/(react|react-dom|@supabase)/',
          '^src/contexts/[^/]+/(application|infrastructure|ui)/',
          '^src/(lib|store|data|components)/',
        ],
      },
    },
    {
      name: 'supabase-solo-en-infraestructura',
      severity: 'warn',
      from: {
        path: '^src',
        pathNot: [
          '^src/contexts/[^/]+/infrastructure/',
          '^src/shared/supabase/',
          '^src/(lib|data|store)/',
          '__tests__',
          '\\.test\\.tsx?$',
        ],
      },
      to: { path: 'node_modules/@supabase/' },
    },
  ],
  options: {
    doNotFollow: { path: 'node_modules' },
    tsConfig: { fileName: 'tsconfig.json' },
    tsPreCompilationDeps: true,
    exclude: { path: '(__tests__|\\.test\\.tsx?$|src/test/)' },
  },
}
