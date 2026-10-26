import js from '@eslint/js'
import globals from 'globals'
import reactHooks from 'eslint-plugin-react-hooks'
import reactRefresh from 'eslint-plugin-react-refresh'
import tseslint from 'typescript-eslint'
import boundaries from 'eslint-plugin-boundaries'

export default tseslint.config(
  { ignores: ['dist', 'coverage', 'playwright-report', 'test-results'] },
  {
    extends: [js.configs.recommended, ...tseslint.configs.recommended],
    files: ['**/*.{ts,tsx}'],
    languageOptions: {
      ecmaVersion: 2020,
      globals: globals.browser,
    },
    plugins: {
      'react-hooks': reactHooks,
      'react-refresh': reactRefresh,
      boundaries,
    },
    settings: {
      'boundaries/elements': [
        { type: 'app', pattern: 'src/app/**' },
        { type: 'shared', pattern: 'src/shared/**' },
        { type: 'domain', pattern: 'src/{features,contexts}/*/domain/**' },
        { type: 'data', pattern: 'src/{features/*/data,contexts/*/infrastructure}/**' },
        { type: 'application', pattern: 'src/{features,contexts}/*/application/**' },
        { type: 'ui', pattern: 'src/{features,contexts}/*/ui/**' },
        // Fachada publica de cada contexto (docs/ARQUITECTURA-DDD.md, 4.2).
        { type: 'api', pattern: 'src/contexts/*/api.ts', mode: 'file' },
        { type: 'legacy', pattern: 'src/{lib,store,data,components}/**' },
      ],
      'boundaries/ignore': ['**/*.test.ts', '**/*.test.tsx', '**/__tests__/**', 'src/test/**'],
    },
    rules: {
      ...reactHooks.configs.recommended.rules,
      'react-refresh/only-export-components': ['warn', { allowConstantExport: true }],
      'boundaries/element-types': [
        'error',
        {
          default: 'disallow',
          rules: [
            { from: 'domain', allow: ['shared', 'domain'] },
            { from: 'data', allow: ['domain', 'shared', 'data'] },
            { from: 'application', allow: ['domain', 'data', 'application', 'shared'] },
            { from: 'ui', allow: ['application', 'domain', 'shared', 'ui', 'legacy', 'api'] },
            { from: 'app', allow: ['ui', 'application', 'shared', 'legacy', 'app', 'api'] },
            { from: 'api', allow: ['domain', 'application', 'data', 'shared'] },
            { from: 'legacy', allow: ['*'] },
            { from: 'shared', allow: ['shared'] },
          ],
        },
      ],
    },
  },
  // Reglas DDD en modo aviso (docs/adr/004-arquitectura-ddd.md): Supabase solo en infrastructure.
  {
    files: ['src/contexts/*/{domain,application,ui}/**/*.{ts,tsx}', 'src/features/**/*.{ts,tsx}'],
    ignores: ['**/*.test.ts', '**/*.test.tsx', '**/__tests__/**'],
    rules: {
      'no-restricted-imports': [
        'warn',
        {
          patterns: [
            {
              group: ['@supabase/*', '**/shared/supabase/*', '**/lib/supabase'],
              message: 'Supabase solo se usa desde infrastructure (ADR 004).',
            },
          ],
        },
      ],
    },
  }
)
