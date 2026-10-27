import React from 'react'
import ReactDOM from 'react-dom/client'
import '../index.css'
import { cerrarTecladoAlTocarFuera } from '../shared/lib/teclado'
import App from './App.tsx'
import { activarVistaPreviaSiCorresponde } from './modoVistaPrevia'

activarVistaPreviaSiCorresponde()

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
)

cerrarTecladoAlTocarFuera()

// Los archivos de esta primera carga llegaron antes de que el service worker controlara la pagina:
// se guardan aqui en la misma cache para que la siguiente apertura funcione sin red.
async function guardarRecursosCargados() {
  await document.fonts.ready
  const cache = await caches.open('runcity-v1')
  const urls = performance
    .getEntriesByType('resource')
    .map((e) => new URL(e.name))
    .filter(
      (u) =>
        (u.origin === location.origin && /^\/(assets|models|marca|images)\//.test(u.pathname)) ||
        u.origin === 'https://fonts.googleapis.com' ||
        u.origin === 'https://fonts.gstatic.com'
    )
    .map((u) => u.href)
  // Las fuentes de Google se piden solo cuando se usan: se leen del CSS para guardarlas todas.
  for (const l of Array.from(
    document.querySelectorAll<HTMLLinkElement>('link[href^="https://fonts.googleapis.com"]')
  )) {
    const css = await fetch(l.href, { mode: 'cors' })
      .then((r) => r.text())
      .catch(() => '')
    urls.push(l.href, ...(css.match(/https:\/\/fonts\.gstatic\.com\/[^)'"\s]+/g) ?? []))
  }
  await Promise.all(
    [...new Set(urls)].map(async (u) => {
      if (await cache.match(u, { ignoreVary: true })) return
      const propio = u.startsWith(location.origin)
      const r = await fetch(u, propio ? {} : { mode: 'cors' }).catch(() => null)
      if (r && (r.ok || r.type === 'opaque')) await cache.put(u, r)
    })
  )
}

// Sin red: el service worker sirve la app y el modelo 3D guardados en la primera visita.
if (import.meta.env.PROD && 'serviceWorker' in navigator) {
  window.addEventListener('load', () => {
    navigator.serviceWorker
      .register('/sw.js')
      .then(() => navigator.serviceWorker.ready)
      .then(guardarRecursosCargados)
      .catch(() => undefined)
  })
}
