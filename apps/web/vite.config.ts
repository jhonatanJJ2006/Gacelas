import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import path from 'path'

export default defineConfig({
  plugins: [react()],
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
