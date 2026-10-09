import { chromium } from 'playwright';
import { mkdir } from 'fs/promises';
import { join } from 'path';

const screenshots = [
  { section: 'acceso', name: 'bienvenida', url: '/bienvenida' },
  { section: 'acceso', name: 'inicio-sesion', url: '/inicio-sesion' },
  { section: 'acceso', name: 'registro', url: '/registro' },
  { section: 'acceso', name: 'recuperar-contrasena', url: '/recuperar-contrasena' },
  { section: 'acceso', name: 'permisos-ubicacion', url: '/permisos-ubicacion' },
  { section: 'inicio', name: 'inicio', url: '/inicio' },
  { section: 'correr', name: 'correr-preparacion', url: '/correr' },
  { section: 'correr', name: 'carrera-en-curso', url: '/correr/en-curso' },
  { section: 'correr', name: 'resumen-post-carrera', url: '/correr/resumen' },
  { section: 'rutas', name: 'lista-rutas', url: '/rutas' },
  { section: 'comunidad', name: 'feed', url: '/comunidad' },
  { section: 'perfil', name: 'perfil', url: '/perfil' },
  { section: 'ajustes', name: 'ajustes-preferencias', url: '/ajustes' },
];

async function captureScreenshots() {
  const outputDir = '/opt/cursor/artifacts/screens';
  await mkdir(outputDir, { recursive: true });

  const browser = await chromium.launch();
  const context = await browser.newContext({
    viewport: { width: 390, height: 844 },
    deviceScaleFactor: 2,
  });

  const page = await context.newPage();

  for (const screenshot of screenshots) {
    try {
      console.log(`Capturando ${screenshot.section}-${screenshot.name}...`);
      await page.goto(`http://localhost:5173${screenshot.url}`, { 
        waitUntil: 'networkidle',
        timeout: 10000 
      });
      await page.waitForTimeout(1500);
      
      const outputPath = join(outputDir, `${screenshot.section}-${screenshot.name}.png`);
      await page.screenshot({ path: outputPath, fullPage: false });
      console.log(`Guardado: ${outputPath}`);
    } catch (error) {
      console.error(`Error capturando ${screenshot.section}-${screenshot.name}:`, error.message);
    }
  }

  await browser.close();
  console.log('Capturas completadas');
}

captureScreenshots();
