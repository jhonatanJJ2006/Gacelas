# @gacelas/contratos

Contratos publicados entre bounded contexts: eventos de dominio y DTO que cruzan fronteras (web, backend y móvil). Ver [docs/ARQUITECTURA-DDD.md](../../docs/ARQUITECTURA-DDD.md), sección 4.3, y la decisión D2.

- Los esquemas zod de `src/index.ts` son la fuente de verdad.
- Cada evento tiene un nombre `<contexto>.<HechoEnPasado>`, `ocurridoEn` en ISO 8601 y `datos`.
- Un cambio incompatible exige un nombre nuevo o una versión nueva del evento.
