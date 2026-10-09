# Modelos 3D

Este directorio contiene los modelos 3D optimizados para uso en la aplicación web.

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

**Pendientes:**

- Agregar rig con Mixamo para animaciones de running
- Evaluar texturas 1024x1024 para dispositivos de gama baja
- Considerar formato KTX2 con compresión Basis para mayor optimización
- Agregar variantes con Draco para reducir tamaño de descarga

**Archivo fuente:** El modelo original sin optimizar está disponible en `/assets/3d/source/gacelas-runner-source.glb` (solo para desarrollo, no distribuir).
