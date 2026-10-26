# ADR 004: Arquitectura DDD por bounded contexts

## Estado

Aceptado (25 de octubre de 2026). Aprobadas las decisiones D1 a D8 de [ARQUITECTURA-DDD.md](../ARQUITECTURA-DDD.md). Amplía el [ADR 003](003-arquitectura-modular.md).

## Contexto

La app suma contextos nuevos (mapa en vivo, seguridad de rutas, moderación, analítica). La base modular del ADR 003 cubría cuatro contextos y dejaba lógica de negocio y llamadas a Supabase repartidas en la UI, `src/lib`, `src/store` y el módulo `:shared`.

## Decisión

- Trece bounded contexts con lenguaje ubicuo en español, descritos en `docs/ARQUITECTURA-DDD.md`.
- Capas `domain`, `application`, `infrastructure` y `presentation` (en la web `ui`). Supabase es solo infraestructura.
- Los contextos se comunican por identificadores, la fachada `api` de cada contexto y eventos de dominio. Los contratos que cruzan fronteras viven en `packages/contratos` (D2).
- Nombres (D1): `carreras` pasa a `actividad`, `cuenta` pasa a `identidad` y se separa `legal`, `data` pasa a `infrastructure`.
- Verificación (D5): `eslint-plugin-boundaries`, `no-restricted-imports` y `dependency-cruiser` en la web; tarea Gradle `verificarArquitectura` en Android/KMP; pruebas de arquitectura con Konsist en una fase posterior.
- Esquemas PostgreSQL por contexto en la fase F7 (D3), publicación como entidad propia (D4), backend propio en TypeScript en F8 (D6), alcance de seguridad según datos públicos (D7) y congelamiento de funciones grandes durante F2 y F7 (D8).

## Reglas en modo aviso

Las reglas nuevas se activaron como aviso en F0 y pasaron a error al terminar F6, con el tipo `legacy` ya eliminado.

| Plataforma  | Comando                                                                | Qué revisa                                                                |
| ----------- | ---------------------------------------------------------------------- | ------------------------------------------------------------------------- |
| Web         | `pnpm lint`                                                            | Capas con boundaries; Supabase fuera de infrastructure (aviso)            |
| Web         | `pnpm arch`                                                            | Ciclos, imports entre contextos sin `api`, dominio puro, Supabase (aviso) |
| Android/KMP | `./gradlew verificarArquitectura`                                      | Dependencias entre módulos de contextos y capas (aviso)                   |
| Android/KMP | `./gradlew verificarArquitectura -Pgacelas.arquitectura.estricta=true` | Igual, pero falla si hay violaciones                                      |

## Consecuencias

- Renombres de módulos y paquetes en F1: ramas abiertas antes del cambio necesitan rebase.
- Mientras dure la migración conviven `src/features` y `src/contexts` en la web; el código nuevo va en `src/contexts`.
- El paquete Kotlin `com.gacelas.legal` se reparte entre `:contextos:legal:domain` y `:contextos:legal:infrastructure`.
