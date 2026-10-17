import { readFileSync } from 'node:fs'
import { resolve, dirname } from 'node:path'
import { fileURLToPath } from 'node:url'

const raiz = resolve(dirname(fileURLToPath(import.meta.url)), '..')
const parse = (archivo) =>
  Object.fromEntries(
    readFileSync(resolve(raiz, archivo), 'utf8')
      .split('\n')
      .filter((l) => /^[A-Z_]+=/.test(l))
      .map((l) => {
        const i = l.indexOf('=')
        return [l.slice(0, i), l.slice(i + 1).trim()]
      })
  )

const archivo = process.argv[2]
if (!archivo) {
  console.error('Uso: node scripts/apply-migration.mjs supabase/migrations/NNN.sql')
  process.exit(1)
}

const back = parse('backend/.env')
const web = parse('apps/web/.env.local')
const token = back.SUPABASE_ACCESS_TOKEN
const ref = new URL(web.VITE_SUPABASE_URL).hostname.split('.')[0]
if (!token) {
  console.error('Falta SUPABASE_ACCESS_TOKEN en backend/.env')
  process.exit(1)
}

const sql = readFileSync(resolve(raiz, archivo), 'utf8')
const r = await fetch(`https://api.supabase.com/v1/projects/${ref}/database/query`, {
  method: 'POST',
  headers: {
    Authorization: `Bearer ${token}`,
    'Content-Type': 'application/json',
  },
  body: JSON.stringify({ query: sql }),
})
const body = await r.text()
if (!r.ok) {
  console.error(`Error ${r.status}:`, body.slice(0, 800))
  process.exit(1)
}
console.log(`OK: ${archivo}`)
console.log(body.slice(0, 300))
