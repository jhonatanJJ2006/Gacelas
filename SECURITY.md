# Seguridad

Cómo reportar una vulnerabilidad de Runcity (repositorio Gacelas).

## Cómo reportar

No abras un issue público, un pull request ni un mensaje en Discussions con el detalle. Eso deja el problema a la vista antes de que exista una corrección.

Usa el reporte privado de GitHub:

<https://github.com/jhonatanJJ2006/Gacelas/security/advisories/new>

Si ese enlace no acepta el reporte, el reporte privado todavía no está activado en el repositorio. En ese caso contacta en privado a quien mantiene el proyecto y no publiques los pasos.

Incluye, en la medida de lo posible:

- Qué se puede hacer con el fallo y a quién afecta
- Pasos para reproducirlo
- Commit, versión o superficie (web, Android, iOS, Supabase, workflows)
- Si hace falta, un escenario de corrección, sin datos reales de otras personas

No hay programa de recompensas. El mantenedor confirma el reporte cuando lo revisa, valora el impacto y prepara la corrección. Un aviso público, si hace falta, sale cuando la corrección ya está disponible.

## Qué entra y qué no

Entra el código de este repositorio: la web, Android, iOS, las migraciones y funciones de Supabase, y los workflows.

No hace falta reportar, porque son públicas por diseño:

- La anon key de Supabase (el JWT con `role` `anon`)
- La clave publishable (`sb_publishable_`)

Esas claves van en el navegador y en las apps. La protección de los datos está en Row Level Security, no en esconderlas. El escaneo de secretos las ignora a propósito; la explicación está en `.gitleaks.toml`.

Siguen siendo secretos: `service_role`, `sb_secret_`, un token personal `sbp_` (`SUPABASE_ACCESS_TOKEN`), `DATABASE_URL`, el secreto OAuth de Google y cualquier clave privada. No los pegues en un issue.

La seguridad del producto (privacidad, ubicación, políticas de acceso) está en [docs/SECURITY_PRIVACY.md](docs/SECURITY_PRIVACY.md).
