import { htmlRuta, idValido, SITIO } from './_og.js'

export default async function handler(req, res) {
  const id = String(req.query.id ?? '')
  if (!idValido(id)) {
    res.statusCode = 302
    res.setHeader('Location', `${SITIO}/rutas`)
    return res.end()
  }
  const url = process.env.VITE_SUPABASE_URL
  const clave = process.env.VITE_SUPABASE_ANON_KEY
  let ruta = null
  let imagen = `${SITIO}/og-default.png`
  if (url && clave) {
    const cab = { apikey: clave, Authorization: `Bearer ${clave}` }
    try {
      const r = await fetch(
        `${url}/rest/v1/routes?id=eq.${encodeURIComponent(id)}&select=name,description,distance_km,elevation_m`,
        { headers: cab }
      )
      ruta = r.ok ? ((await r.json())[0] ?? null) : null
      const png = `${url}/storage/v1/object/public/share-images/rutas/${encodeURIComponent(id)}.png`
      if (ruta && (await fetch(png, { method: 'HEAD' })).ok) imagen = png
    } catch {
      /* sin vista previa: se usa la imagen por defecto */
    }
  }
  res.statusCode = 200
  res.setHeader('Content-Type', 'text/html; charset=utf-8')
  res.setHeader('Cache-Control', 'public, s-maxage=600, stale-while-revalidate=86400')
  res.end(htmlRuta({ id, ruta, imagen }))
}
