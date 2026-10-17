# Modelos 3D Fuente

Este directorio contiene los archivos originales de modelos 3D exportados directamente de las herramientas de autoría. No están optimizados y son solo para referencia o procesamiento posterior.

**Importante:** Estos archivos NO deben usarse directamente en producción. Use las versiones optimizadas en `apps/web/public/models/` o `apps/android/assets/`.

## gacelas-runner-source.glb

Avatar original del corredor con el uniforme del grupo Gacelas Running Team de Loja.

**Especificaciones técnicas:**

- Formato: glTF 2.0 Binary (.glb)
- Tamaño: 60,982,140 bytes (~58 MB)
- Triángulos: ~2,000,000
- Texturas: 8192x8192 (8K) embebidas
- Contenido: Dos figuras en una sola malla
- Estado: Sin optimizar

**Origen:** Exportado directamente de Hi3D sin procesamiento.

**Versión optimizada:** Ver `apps/web/public/models/gacelas-runner.glb` para la versión lista para producción.

**Proceso de optimización aplicado:**

1. Separación de figuras (una sola figura por archivo)
2. Reducción de geometría a ~75K triángulos
3. Downscaling de texturas a 2048x2048
4. Conversión de texturas a JPEG para reducir tamaño
5. Ajuste de escala y orientación
6. Validación con gltf-validator

**Próximos pasos:**

- Agregar rig y animaciones con Mixamo
- Crear variantes con diferentes niveles de detalle (LOD)
- Evaluar texturas PBR mejoradas
