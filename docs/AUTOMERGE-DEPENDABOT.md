# Automerge de Dependabot

Este documento explica cómo se actualizan las dependencias de Runcity sin que los pull requests se acumulen, y qué protecciones tienen `main` y `staging`.

## Cómo funciona

1. Dependabot revisa cada lunes a las 09:00 (hora de Ecuador) tres ecosistemas: `npm` (workspace pnpm de la raíz), `gradle` (`apps/android`) y `github-actions`. La configuración está en `.github/dependabot.yml`.
2. Las actualizaciones minor y patch de cada ecosistema llegan juntas en un solo PR, del grupo `menores-y-parches`. Las major llegan como un PR por dependencia.
3. El workflow `.github/workflows/dependabot-automerge.yml` se ejecuta cuando Dependabot abre o actualiza un PR. Lee los metadatos con `dependabot/fetch-metadata` y, si todas las dependencias del PR son minor o patch, activa el auto-merge con `gh pr merge --auto --squash`.
4. GitHub espera a que los checks obligatorios terminen en verde y entonces fusiona. Si un check falla, el PR queda abierto y no se fusiona nada.
5. Las actualizaciones major nunca se fusionan solas: quedan abiertas para revisión manual.

El workflow usa `pull_request_target` para tener permisos de escritura sobre los PR de Dependabot. No hace checkout ni ejecuta código del PR; solo llama a la API de GitHub, por eso es seguro.

## Qué hacer con una actualización major

- Si la actualización es razonable, se revisa el PR, se corrige lo que rompa en una rama propia y se fusiona a mano.
- Si exige migrar otras piezas (por ejemplo, `@testing-library/jest-dom` 8 pide Vitest 5 o superior y el proyecto usa Vitest 1), se cierra el PR con un comentario y se agrega una entrada `ignore` en `.github/dependabot.yml` para esa versión mayor. Al retomar la migración se quita la entrada.

## Protecciones de main y staging

`main` es producción y `staging` es la rama de pruebas. Ambas están cubiertas por el ruleset `Proteger main y staging`:

- Pull request obligatorio, sin commits directos.
- Una aprobación obligatoria, con revisión de code owner (`.github/CODEOWNERS` asigna todo a `@jhonatanJJ2006`). Quien tiene permiso de escritura, como un colaborador, no puede fusionar sin esa aprobación.
- La aprobación anterior se descarta si se suben cambios nuevos al PR.
- Checks obligatorios: `Build`, `Format & Lint`, `Test`, `Dependencies`, `test` (pruebas web), `Android build`, `Android test` y `Gitleaks`.
- No se pueden borrar las ramas ni hacer force push.
- Pueden saltarse el ruleset el administrador del repositorio y la app Dependabot (solo a través de un PR).

Como los checks son obligatorios, los workflows `CI`, `Android` y `Web Tests` corren en todos los PR hacia `main`, `develop` y `staging`, sin filtro de rutas. Un check obligatorio que no se ejecuta deja el PR bloqueado para siempre.

## Ajustes del repositorio

- Auto-merge activado (`allow_auto_merge`).
- Las ramas se borran solas al fusionar un PR (`delete_branch_on_merge`).
- Un merge hecho por el `GITHUB_TOKEN` no dispara los workflows de `push` sobre `main`; el CI ya corrió en el PR, y Vercel despliega por su propia integración.

## Limitaciones

- Las major y los PR de otras personas se fusionan a mano.
- Si el auto-merge de Dependabot no puede avanzar porque falta la aprobación del code owner, el PR espera a que el administrador lo apruebe o lo fusione.
