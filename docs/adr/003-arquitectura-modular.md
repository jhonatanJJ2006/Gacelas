# ADR 003: Arquitectura modular unificada

## Estado

Aceptado (octubre 2026).

## Contexto

Gacelas tenia logica compartida en un unico modulo KMP `shared` y la web con `lib/` plano y un store monolitico. Se necesitaba escalar compilacion, fronteras claras y el mismo vocabulario DDD en web y movil.

## Decision

- **Movil**: modulos Gradle por capa y bounded context (`core:*`, `contextos:<ctx>:domain|data|presentation`) mas `:shared` paraguas con DI y navegacion. Convenciones en `build-logic`.
- **Web**: features por contexto con capas `domain`, `data`, `application`, `ui`; entrada en `src/app/`; utilidades en `src/shared/`.
- **Presentacion**: MVVM (ViewModel en KMP, hooks ViewModel en React).
- **Backend**: migraciones en `supabase/migrations/`; seeds de referencia en `backend/seeds/`; tests RLS en `backend/tests/rls/`.

## Consecuencias

- Compilacion incremental en Android/iOS al tocar un solo contexto.
- Reglas ESLint `eslint-plugin-boundaries` en web (domain sin React ni supabase-js).
- CI `db.yml` valida migraciones en `supabase/migrations/` y tipos generados.
- JDK 17-21 recomendado para Gradle local (JDK 26 puede fallar con Kotlin DSL).
