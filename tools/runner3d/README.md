# Corredor 3D en WebGL (Android)

`main.js` es la fuente del visor que usa la app cuando Filament no puede renderizar el corredor
(por ejemplo, en el emulador con SwiftShader). Carga el mismo `gacelas-runner-animated.glb` con three.js.

Para regenerar `apps/android/app/src/main/assets/runner3d/runner3d.js`:

```sh
cd tools/runner3d
ln -s ../../apps/web/node_modules node_modules
pnpm dlx esbuild@0.24.0 main.js --bundle --minify --format=iife --outfile=../../apps/android/app/src/main/assets/runner3d/runner3d.js
```
