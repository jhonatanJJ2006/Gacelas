# Modelos 3D

Este directorio contiene los modelos 3D optimizados para uso en la aplicación web.

## gacelas-runner-animated.glb

Avatar del corredor con el uniforme del grupo Gacelas Running Team de Loja, versión 1 con esqueleto y animaciones.

**Especificaciones técnicas:**

- Formato: glTF 2.0 Binary (.glb)
- Tamaño: 3,762,796 bytes (~3.59 MB)
- Triángulos: 75,516
- Texturas: 2048x2048 JPEG embebidas
- Escala: Real (1.75 m de altura)
- Origen: Pies en y=0, mirando hacia +Z
- Esqueleto: 53 huesos con nombres DEF-* (mismo esquema que Universal Animation Library)
- Animaciones incluidas:
  - **Idle**: 5.0 segundos, bucle continuo en el lugar
  - **Jog**: 0.917 segundos, bucle continuo en el lugar
  - **Run**: 0.667 segundos, bucle continuo en el lugar
- Root motion: Ninguno (animaciones en el lugar, sin desplazamiento)
- Compresión: Sin Draco (compatibilidad máxima)

**Atribución de animaciones:**

Las animaciones fueron retargetizadas desde la **Universal Animation Library** (versión Standard/free) por **Quaternius**.

- Fuente: https://quaternius.itch.io/universal-animation-library
- Licencia: CC0 1.0 Universal (dedicación al dominio público)
- Ver archivo ANIMATIONS_LICENSE.txt para detalles completos

**Limitaciones conocidas:**

- **Jog y Run similares**: La animación Jog es visualmente parecida a Run, diferenciándose principalmente en velocidad
- **Deslizamiento de pies**: Al estar en el lugar sin root motion, los pies pueden deslizarse si no se ajusta la velocidad de traslación del personaje
- **Puño tosco**: Los puños cerrados se ven algo toscos en primeros planos

**Uso:** Listo para cargar en three.js con soporte de animaciones. Reproduce los clips con `mixer.clipAction(clip).play()`.

**Próximos pasos:**

- Agregar root motion a las animaciones de desplazamiento
- Crear variantes de Jog más realistas y diferenciadas
- Refinar pose de manos/puños
- Considerar formato KTX2 + Draco para reducir tamaño

## gacelas-runner.glb

Avatar del corredor con el uniforme del grupo Gacelas Running Team de Loja.

**Especificaciones técnicas:**

- Formato: glTF 2.0 Binary (.glb)
- Tamaño: 2,521,936 bytes (~2.4 MB)
- Triángulos: 75,516
- Texturas: 2048x2048 JPEG embebidas
- Escala: Real (1.75 m de altura)
- Origen: Pies en y=0, mirando hacia +Z
- Rig: No incluido
- Animaciones: No incluidas
- Compresión: Sin Draco (compatibilidad máxima)
- Validación: gltf-validator (0 errores)

**Origen:** Exportado de Hi3D y optimizado para three.js y Android.

**Uso:** Este modelo está listo para cargar directamente en three.js o cualquier motor que soporte glTF 2.0.

**Nota:** Este modelo no incluye esqueleto ni animaciones. Para la versión animada, usar **gacelas-runner-animated.glb**.

**Pendientes:**

- Evaluar texturas 1024x1024 para dispositivos de gama baja
- Considerar formato KTX2 con compresión Basis para mayor optimización
- Agregar variantes con Draco para reducir tamaño de descarga

**Archivo fuente:** El modelo original sin optimizar está disponible en `/assets/3d/source/gacelas-runner-source.glb` (solo para desarrollo, no distribuir).
