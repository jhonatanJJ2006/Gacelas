import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import path from 'path'

export default defineConfig({
  plugins: [
    react(),
    {
      // Lista de recursos con hash para que el service worker los guarde al instalarse y la
      // app funcione sin red tambien en pantallas que aun no se visitaron.
      name: 'lista-precache',
      apply: 'build',
      generateBundle(_, bundle) {
        const archivos = Object.keys(bundle)
          .filter((f) => f.startsWith('assets/') && /\.(js|css)$/.test(f))
          .map((f) => '/' + f)
        this.emitFile({
          type: 'asset',
          fileName: 'precache.json',
          source: JSON.stringify(archivos),
        })
      },
    },
  ],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  build: {
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (id.includes('node_modules/three') || id.includes('@react-three')) return 'three'
          if (id.includes('@supabase/supabase-js')) return 'supabase'
          if (id.includes('@vis.gl/react-google-maps') || id.includes('@googlemaps')) return 'mapas'
        },
      },
    },
  },
})
