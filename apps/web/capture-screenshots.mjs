import { chromium } from 'playwright'
import { mkdir } from 'fs/promises'
import { join } from 'path'

const screenshots = [
  // Acceso
  { section: 'acceso', name: 'bienvenida', url: '/bienvenida' },
  { section: 'acceso', name: 'inicio-sesion', url: '/inicio-sesion' },
  { section: 'acceso', name: 'registro', url: '/registro' },
  { section: 'acceso', name: 'recuperar-contrasena', url: '/recuperar-contrasena' },
  { section: 'acceso', name: 'permisos-ubicacion', url: '/permisos-ubicacion' },

  // Inicio
  { section: 'inicio', name: 'inicio', url: '/inicio' },

  // Correr
  { section: 'correr', name: 'correr-preparacion', url: '/correr' },
  { section: 'correr', name: 'carrera-en-curso', url: '/correr/en-curso' },
  { section: 'correr', name: 'cronometro', url: '/correr/cronometro' },
  { section: 'correr', name: 'resumen-post-carrera', url: '/correr/resumen' },

  // Rutas
  { section: 'rutas', name: 'lista-rutas', url: '/rutas' },
  { section: 'rutas', name: 'buscador-rutas', url: '/rutas/buscar' },
  { section: 'rutas', name: 'navegacion-ruta', url: '/rutas/navegacion' },

  // Comunidad
  { section: 'comunidad', name: 'feed', url: '/comunidad' },
  { section: 'comunidad', name: 'eventos', url: '/comunidad/eventos' },
  { section: 'comunidad', name: 'detalle-evento', url: '/comunidad/eventos/1' },

  // Perfil
  { section: 'perfil', name: 'perfil', url: '/perfil' },
  { section: 'perfil', name: 'editar-perfil', url: '/perfil/editar' },
  { section: 'perfil', name: 'avatar-3d', url: '/perfil/avatar' },

  // Ajustes
  { section: 'ajustes', name: 'ajustes-preferencias', url: '/ajustes' },
  { section: 'ajustes', name: 'privacidad-ubicacion', url: '/ajustes/privacidad' },
  { section: 'ajustes', name: 'configuracion-notificaciones', url: '/ajustes/notificaciones' },

  // Notificaciones
  { section: 'notificaciones', name: 'notificaciones', url: '/notificaciones' },
]

async function captureScreenshots() {
  const outputDir = '/opt/cursor/artifacts/screens'
  await mkdir(outputDir, { recursive: true })

  const browser = await chromium.launch()
  const context = await browser.newContext({
    viewport: { width: 390, height: 844 },
    deviceScaleFactor: 2,
  })

  const page = await context.newPage()

  console.log(`Capturando ${screenshots.length} pantallas...`)

  for (const screenshot of screenshots) {
    try {
      console.log(`Capturando ${screenshot.section}-${screenshot.name}...`)
      await page.goto(`http://localhost:5173${screenshot.url}`, {
        waitUntil: 'networkidle',
        timeout: 15000,
      })
      await page.waitForTimeout(2000)

      const outputPath = join(outputDir, `${screenshot.section}-${screenshot.name}.png`)
      await page.screenshot({ path: outputPath, fullPage: false })
      console.log(`  ✓ Guardado`)
    } catch (error) {
      console.error(`  ✗ Error: ${error.message}`)
    }
  }

  await browser.close()
  console.log(`\n✓ Capturas completadas: ${screenshots.length} pantallas`)
}

captureScreenshots()
