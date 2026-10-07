# Reglas para Agentes de IA

Este documento contiene reglas específicas para agentes de IA que trabajan en el proyecto Gacelas.

## Documentación

### No usar iconos ni emojis en archivos Markdown

**Regla**: No usar iconos, emojis ni símbolos Unicode decorativos en ningún archivo de documentación (.md, READMEs, ADRs, descripciones de PR).

**Razón**: Mejora la legibilidad, facilita la búsqueda de texto y mantiene la consistencia en toda la documentación del proyecto.

**Aplica a**:

- Todos los archivos .md en docs/
- README.md en raíz y en subcarpetas (apps/, backend/, etc.)
- ADRs en docs/adr/
- Descripciones de Pull Requests
- Mensajes de commit (preferible, pero no obligatorio)

**Excepciones**:

- Diagramas mermaid pueden usar flechas de texto ( - , , etc.) si son parte de la sintaxis
- Código fuente y ejemplos de código no se ven afectados

**Reemplazo**:

- En lugar de checkmarks/cruces: usar "Sí"/"No" o "Completado"/"Pendiente"
- En lugar de íconos de sección: usar títulos claros
- En lugar de emojis de estado: usar texto descriptivo

**Ejemplos**:

```markdown
Incorrecto:
Feature completada
Bug encontrado
Documentación
Despliegue

Correcto:

- Completado: Feature X
- Pendiente: Bug Y
- Ver documentación en docs/
- Listo para despliegue
```

### No agregar agentes como coautores

**Regla**: No agregar al agente de Cursor (ni a ningún agente de IA) como contribuidor, coautor o colaborador en commits, documentación o créditos del proyecto.

**Razón**: Los agentes son herramientas que asisten al desarrollador. El crédito y la responsabilidad del código pertenecen al desarrollador humano que dirige el trabajo.

**Aplica a**:

- Trailers de commit (`Co-authored-by: Cursor Agent`)
- Archivos CONTRIBUTORS o AUTHORS
- Sección de equipo en README
- Descripciones de Pull Requests
- Comentarios de código que acrediten al agente

**Configuración**: Desactivar cualquier configuración de Git o del editor que agregue automáticamente trailers de coautoría de agentes.

## Verificación

El script `pnpm lint:docs` verifica automáticamente que no haya emojis en la documentación. Este check se ejecuta en CI.
