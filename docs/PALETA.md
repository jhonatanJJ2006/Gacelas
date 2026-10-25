# Paleta de colores Gacelas (Social Run Loja)

Paleta definida por Sebastian en Stitch (proyecto Gacelas, pantallas Inicio, Rutas, Comunidad y Perfil). Sustituye la paleta naranja "amanecer andino". Los valores se tomaron de la configuracion Tailwind y de las clases usadas en las cuatro pantallas principales.

Nota: el `designTheme` del proyecto y el sistema de diseno "Andean Dawn Athletics" en Stitch todavia muestran la paleta ambar anterior; la fuente de verdad son las pantallas.

## Tokens primarios (morado)

| Token                                    | Valor     | Uso                                              |
| ---------------------------------------- | --------- | ------------------------------------------------ |
| `primary` / `primary-400` / surface-tint | `#C084FC` | Acentos, textos destacados, iconos activos, ruta |
| `primary-200` / `inverse-primary`        | `#E9D5FF` | Fondos suaves, chips                             |
| `primary-500` / `primary-fixed-dim`      | `#A855F7` | Estados intermedios                              |
| `primary-600` / `primary-container`      | `#7B1FA2` | Botones principales, FAB                         |
| `primary-700`                            | `#5E1781` | Hover, inicio del degradado de botones           |
| `primary-800` / on-primary-fixed-variant | `#4A154B` | Bordes y fondos oscuros                          |
| `primary-fixed`                          | `#F3E8FF` | Fondos claros                                    |
| `on-primary` / `on-primary-container`    | `#FFFFFF` | Texto sobre morado                               |
| `on-primary-fixed`                       | `#240038` | Texto sobre `primary-fixed`                      |

Degradado de botones: `#5E1781` a `#7B1FA2`.

## Tokens sin cambios

| Grupo            | Valores                                       |
| ---------------- | --------------------------------------------- |
| Superficies      | `#0C1322`, `#191F2F`, `#232A3A` (modo oscuro) |
| Secundario verde | `#68DBA9`, `#10B981`, `#059669`, `#047857`    |
| Terciario        | `#96CCFF`                                     |
| Peligro          | `#DC2626`                                     |
| Advertencia      | `#F59E0B`                                     |
| Informacion      | `#3B82F6`                                     |

Modo claro: Stitch no lo define; se mantienen las superficies claras existentes con los mismos tokens primarios.

## Implementacion

- Web: `apps/web/tailwind.config.js` (escala `primary`, `on-primary*`, `inverse-primary`, `surface-tint`).
- Android: `apps/android/core/ui/src/commonMain/kotlin/com/gacelas/ui/theme/Color.kt` (`GColors`).
- Mapas: color de ruta y acento `#C084FC` (`mapaEstilo.ts` en web, `MapaComun` en Android).
