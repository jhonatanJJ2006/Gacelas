import { readFileSync, createReadStream } from 'node:fs'
import { resolve, dirname } from 'node:path'
import { fileURLToPath } from 'node:url'
import { createHash } from 'node:crypto'

const raiz = resolve(dirname(fileURLToPath(import.meta.url)), '..')
const parse = (archivo) =>
  Object.fromEntries(
    readFileSync(resolve(raiz, archivo), 'utf8')
      .split('\n')
      .filter((l) => /^[A-Z_#]/.test(l) && l.includes('='))
      .filter((l) => !l.startsWith('#'))
      .map((l) => {
        const i = l.indexOf('=')
        return [l.slice(0, i), l.slice(i + 1).trim()]
      })
  )

const apkPath = process.argv[2]
const notas = process.argv[3]
if (!apkPath || !notas) {
  console.error('Uso: node scripts/publicar-apk-storage.mjs <ruta.apk> "Nota1; Nota2"')
  process.exit(1)
}

const local = parse('apps/android/local.properties')
const back = parse('backend/.env')
const url = local.SUPABASE_URL
const key = back.SUPABASE_SERVICE_ROLE_KEY
if (!url || !key) {
  console.error('Faltan SUPABASE_URL en local.properties o SUPABASE_SERVICE_ROLE_KEY en backend/.env')
  process.exit(1)
}

const gradle = readFileSync(resolve(raiz, 'apps/android/app/build.gradle.kts'), 'utf8')
const versionName = gradle.match(/versionName = "([^"]+)"/)?.[1]
if (!versionName) {
  console.error('No se encontro versionName en build.gradle.kts')
  process.exit(1)
}
const parts = versionName.split('.').map((x) => parseInt(x, 10) || 0)
const versionCode = parts[0] * 10000 + parts[1] * 100 + parts[2]

const buf = readFileSync(resolve(apkPath))
const sha256 = createHash('sha256').update(buf).digest('hex')
const destino = `android/runcity-${versionName}-arm64.apk`
const publica = `${url}/storage/v1/object/public/app-releases`

async function subir(rutaLocal, rutaBucket, tipo) {
  const r = await fetch(`${url}/storage/v1/object/app-releases/${rutaBucket}`, {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${key}`,
      apikey: key,
      'Content-Type': tipo,
      'x-upsert': 'true',
      'Cache-Control': 'max-age=60',
    },
    body: buf,
  })
  if (!r.ok) throw new Error(`Upload ${rutaBucket}: ${r.status} ${await r.text()}`)
}

await subir(apkPath, destino, 'application/vnd.android.package-archive')

const items = notas
  .split(';')
  .map((s) => s.trim())
  .filter(Boolean)
const latest = {
  versionCode,
  versionName,
  notes: items.join('. '),
  items,
  apkUrl: `${publica}/${destino}`,
  apkBytes: buf.length,
  sha256,
}

const latestBody = JSON.stringify(latest)
const r2 = await fetch(`${url}/storage/v1/object/app-releases/android/latest.json`, {
  method: 'POST',
  headers: {
    Authorization: `Bearer ${key}`,
    apikey: key,
    'Content-Type': 'application/json',
    'x-upsert': 'true',
    'Cache-Control': 'max-age=60',
  },
  body: latestBody,
})
if (!r2.ok) throw new Error(`latest.json: ${r2.status} ${await r2.text()}`)

console.log('Publicado:', versionName, versionCode)
console.log(JSON.stringify(latest, null, 2))
