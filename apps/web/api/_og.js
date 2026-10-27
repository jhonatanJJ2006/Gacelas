// Pagina publica /r/:id con metadatos Open Graph para que WhatsApp, Instagram, X, etc.
// muestren la vista previa de la ruta. Luego redirige a la app (/rutas/:id).

const esc = (s) =>
  String(s ?? '').replace(
    /[&<>"']/g,
    (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[c]
  )

export const SITIO = 'https://gacelas.vercel.app'
export const idValido = (id) => typeof id === 'string' && /^[\w-]{1,64}$/.test(id)

export function htmlRuta({ id, ruta, imagen }) {
  const titulo = ruta ? `${ruta.name} · Runcity` : 'Runcity · Rutas para correr en Loja'
  const km = ruta?.distance_km != null ? `${Number(ruta.distance_km).toFixed(1)} km` : ''
  const desnivel =
    ruta?.elevation_m != null ? ` · ${Math.round(ruta.elevation_m)} m de desnivel` : ''
  const descripcion = ruta
    ? `${km}${desnivel}. ${ruta.description || 'Córrela con Gacelas.'}`.slice(0, 200)
    : 'Descubre y comparte rutas para correr.'
  const destino = `${SITIO}/rutas/${encodeURIComponent(id)}`
  return `<!doctype html>
<html lang="es"><head><meta charset="utf-8">
<title>${esc(titulo)}</title>
<meta name="description" content="${esc(descripcion)}">
<meta property="og:type" content="website">
<meta property="og:site_name" content="Runcity">
<meta property="og:title" content="${esc(titulo)}">
<meta property="og:description" content="${esc(descripcion)}">
<meta property="og:url" content="${esc(`${SITIO}/r/${encodeURIComponent(id)}`)}">
<meta property="og:image" content="${esc(imagen)}">
<meta property="og:image:width" content="1080"><meta property="og:image:height" content="1080">
<meta name="twitter:card" content="summary_large_image">
<meta name="twitter:image" content="${esc(imagen)}">
<meta name="theme-color" content="#0b1220">
<meta http-equiv="refresh" content="0;url=${esc(destino)}">
<link rel="canonical" href="${esc(destino)}">
</head><body style="background:#0b1220;color:#fff;font-family:sans-serif">
<p>Abriendo <a href="${esc(destino)}" style="color:#F59E0B">${esc(titulo)}</a>…</p>
<script>location.replace(${JSON.stringify(destino)})</script>
</body></html>`
}
