// Service worker de Runcity: deja la app y el corredor 3D disponibles sin red despues de la
// primera visita. Solo atiende navegacion y recursos estaticos del mismo origen; Supabase, /api y
// el resto de peticiones van directo a la red.
const VERSION = 'runcity-v2'
const PRECACHE = [
  '/',
  '/index.html',
  '/models/gacelas-runner-animated.glb',
  '/models/gacelas-runner-poster.png',
  '/manifest.webmanifest',
  '/icon-192.png',
]
const FUENTES = ['https://fonts.googleapis.com', 'https://fonts.gstatic.com']
const ESTATICO = /^\/(assets|models|marca|images)\/|\.(png|svg|ico|webmanifest|woff2?)$/

self.addEventListener('install', (e) => {
  e.waitUntil(
    caches
      .open(VERSION)
      .then(async (c) => {
        await c.addAll(PRECACHE)
        // Chunks del build (pantallas con carga diferida); si falta la lista, se sigue igual.
        const lista = await fetch('/precache.json', { cache: 'no-store' })
          .then((r) => (r.ok ? r.json() : []))
          .catch(() => [])
        await Promise.all(lista.map((u) => c.add(u).catch(() => undefined)))
      })
      .then(() => self.skipWaiting())
  )
})

self.addEventListener('activate', (e) => {
  e.waitUntil(
    caches
      .keys()
      .then((ks) => Promise.all(ks.filter((k) => k !== VERSION).map((k) => caches.delete(k))))
      .then(() => self.clients.claim())
  )
})

async function guardar(peticion, respuesta) {
  if (respuesta.ok || respuesta.type === 'opaque') {
    const c = await caches.open(VERSION)
    await c.put(peticion, respuesta.clone())
  }
  return respuesta
}

self.addEventListener('fetch', (e) => {
  const req = e.request
  if (req.method !== 'GET') return
  const url = new URL(req.url)
  if (FUENTES.includes(url.origin)) {
    // Fuentes de Google (texto e iconos): cache primero.
    e.respondWith(
      caches
        .match(req, { ignoreVary: true })
        .then((hit) => hit || fetch(req).then((r) => guardar(req, r)))
    )
    return
  }
  if (url.origin !== self.location.origin || url.pathname.startsWith('/api/')) return

  if (req.mode === 'navigate') {
    // Red primero (version nueva), y sin red la app guardada.
    e.respondWith(
      fetch(req)
        .then((r) => guardar('/index.html', r))
        .catch(
          async () => (await caches.match('/index.html', { ignoreVary: true })) || Response.error()
        )
    )
    return
  }
  if (ESTATICO.test(url.pathname)) {
    // Cache primero: los /assets llevan hash y el GLB cambia de nombre si cambia el modelo.
    e.respondWith(
      caches
        .match(req, { ignoreSearch: url.pathname.startsWith('/models/'), ignoreVary: true })
        .then((hit) => hit || fetch(req).then((r) => guardar(req, r)))
    )
  }
})
