"""Genera la semilla SQL, el catalogo web y el catalogo Kotlin a partir de supabase/seed/rutas-loja.geojson.

Uso: python3 scripts/rutas-loja/generar.py
"""
import json, math, pathlib

RAIZ = pathlib.Path(__file__).resolve().parents[2]
datos = json.loads((RAIZ / "supabase/seed/rutas-loja.geojson").read_text())

DIF = {"facil": "Fácil", "moderada": "Moderado", "dificil": "Difícil"}
TAGS = {"parque": ["parque"], "sendero": ["sendero"], "montana": ["montana", "panoramica"], "urbana": ["urbana"], "pista": ["plana"]}
RIO = {"la-tebaida", "orillas-zamora", "orillas-malacatos", "zamora-huayco", "cascarilla"}


def hav(a, b):
    la1, la2 = math.radians(a[1]), math.radians(b[1])
    dl, dg = la2 - la1, math.radians(b[0] - a[0])
    return 2 * 6371000 * math.asin(math.sqrt(math.sin(dl / 2) ** 2 + math.cos(la1) * math.cos(la2) * math.sin(dg / 2) ** 2))


def seg(p, a, b):
    kx = 111320 * math.cos(math.radians(a[1])); ky = 110540
    px, py, bx, by = (p[0] - a[0]) * kx, (p[1] - a[1]) * ky, (b[0] - a[0]) * kx, (b[1] - a[1]) * ky
    l2 = bx * bx + by * by
    t = 0 if l2 == 0 else max(0, min(1, (px * bx + py * by) / l2))
    return math.hypot(px - t * bx, py - t * by)


def dp(c, tol):
    if len(c) <= 2: return c
    keep = [False] * len(c); keep[0] = keep[-1] = True; pila = [(0, len(c) - 1)]
    while pila:
        i, j = pila.pop(); mx, k = -1, -1
        for x in range(i + 1, j):
            d = seg(c[x], c[i], c[j])
            if d > mx: mx, k = d, x
        if mx > tol and k > 0: keep[k] = True; pila += [(i, k), (k, j)]
    return [p for p, q in zip(c, keep) if q]


def limitar(c, tol, maximo):
    r = dp(c, tol)
    while len(r) > maximo: tol *= 1.5; r = dp(c, tol)
    return r


PIE = """) AS v(id, slug, name, distance_km, difficulty, elevation_m, elevation_loss_m, description,
  path, tags, elevation_profile, category, surface, source, license)
ON CONFLICT (id) DO UPDATE SET
  slug = EXCLUDED.slug, name = EXCLUDED.name, distance_km = EXCLUDED.distance_km,
  difficulty = EXCLUDED.difficulty, elevation_m = EXCLUDED.elevation_m,
  elevation_loss_m = EXCLUDED.elevation_loss_m, description = EXCLUDED.description,
  path = EXCLUDED.path, tags = EXCLUDED.tags, elevation_profile = EXCLUDED.elevation_profile,
  category = EXCLUDED.category, surface = EXCLUDED.surface, source = EXCLUDED.source,
  license = EXCLUDED.license, is_official = true, origin = 'oficial', author_id = NULL, is_public = true;
"""

rutas = []
for f in datos["features"]:
    p = f["properties"]; c = f["geometry"]["coordinates"]
    tags = TAGS[p["categoria"]] + (["rio"] if p["slug"] in RIO else [])
    if p["desnivel_positivo_m"] < 40 and "plana" not in tags: tags.append("plana")
    rutas.append(dict(p, id="loja-" + p["slug"], tags=tags, km=round(p["distancia_m"] / 1000, 2),
                      sql=limitar(c, 2, 480), cli=limitar(c, 6, 160)))

q = lambda s: "'" + s.replace("'", "''") + "'"
filas = []
for r in rutas:
    path = json.dumps([{"lat": round(y, 6), "lng": round(x, 6)} for x, y in r["sql"]], separators=(",", ":"))
    filas.append("  (" + ", ".join([
        q(r["id"]), q(r["slug"]), q(r["nombre"]), str(r["km"]), q(DIF[r["dificultad"]]), str(r["desnivel_positivo_m"]),
        str(r["desnivel_negativo_m"]), q(r["descripcion"]), q(path) + "::jsonb", "ARRAY[" + ",".join(q(t) for t in r["tags"]) + "]::text[]",
        q(json.dumps(r["perfil_elevacion_m"])) + "::jsonb", q(r["categoria"]), q(r["superficie"]),
        q("OpenStreetMap: " + r["trazado"] + ". Elevación: " + r["elevacion"]), q(r["licencia"])]) + ")")
CABECERA = (RAIZ / "scripts/rutas-loja/022_cabecera.sql").read_text()
(RAIZ / "supabase/migrations/022_rutas_loja.sql").write_text(CABECERA + ",\n".join(filas) + "\n" + PIE)

# Web
web = ["// Generado por scripts/rutas-loja/generar.py a partir de supabase/seed/rutas-loja.geojson. No editar a mano.",
       "// Trazados (c) colaboradores de OpenStreetMap, licencia ODbL 1.0. Elevación SRTM 30 m vía OpenTopoData.",
       "import type { Ruta } from '../mocks/data'", "", "export const RUTAS_LOJA: Ruta[] = ["]
for r in rutas:
    pts = ",".join(f"[{round(y,5)},{round(x,5)}]" for x, y in r["cli"])
    web.append("  {")
    web.append(f"    id: {json.dumps(r['id'])},")
    web.append(f"    nombre: {json.dumps(r['nombre'], ensure_ascii=False)},")
    web.append(f"    distanciaKm: {r['km']},")
    web.append(f"    dificultad: {json.dumps(DIF[r['dificultad']], ensure_ascii=False)},")
    web.append(f"    elevacionM: {r['desnivel_positivo_m']},")
    web.append(f"    descripcion: {json.dumps(r['descripcion'], ensure_ascii=False)},")
    web.append("    autorId: 'gacelas',")
    web.append(f"    etiquetas: {json.dumps(r['tags'])},")
    web.append("    origen: 'oficial',")
    web.append("    oficial: true,")
    web.append(f"    categoria: {json.dumps(r['categoria'])},")
    web.append(f"    superficie: {json.dumps(r['superficie'])},")
    web.append(f"    fuente: {json.dumps('OpenStreetMap (ODbL 1.0), ' + r['trazado'], ensure_ascii=False)},")
    web.append(f"    perfilElevacion: {json.dumps(r['perfil_elevacion_m'])},")
    web.append(f"    puntos: ([{pts}] as [number, number][]).map(([lat, lng]) => ({{ lat, lng }})),")
    web.append("  },")
web.append("]")
(RAIZ / "apps/web/src/data/rutasLoja.ts").write_text("\n".join(web) + "\n")

# Kotlin (trazo como texto para no superar el limite de tamano de metodo de la JVM)
k = ["// Generado por scripts/rutas-loja/generar.py a partir de supabase/seed/rutas-loja.geojson. No editar a mano.",
     "// Trazados (c) colaboradores de OpenStreetMap, licencia ODbL 1.0. Elevación SRTM 30 m vía OpenTopoData.",
     "package com.gacelas.rutas.domain.modelo", "",
     "/** Catalogo de rutas oficiales de Loja (migracion 022). */", "object RutasLoja {",
     "    private fun trazo(s: String) = s.split(';').map { p -> p.split(',').let { it[0].toDouble() to it[1].toDouble() } }",
     "    private fun perfil(s: String) = s.split(',').map { it.toInt() }", "",
     "    val todas: List<Ruta> by lazy {", "        listOf("]
D = {"facil": "Dificultad.Facil", "moderada": "Dificultad.Moderado", "dificil": "Dificultad.Dificil"}
for r in rutas:
    pts = ";".join(f"{round(y,5)},{round(x,5)}" for x, y in r["cli"])
    k.append(f"            Ruta(")
    k.append(f"                id = {json.dumps(r['id'])}, nombre = {json.dumps(r['nombre'], ensure_ascii=False)}, distanciaKm = {r['km']}, dificultad = {D[r['dificultad']]},")
    k.append(f"                elevacionM = {r['desnivel_positivo_m']}, descripcion = {json.dumps(r['descripcion'], ensure_ascii=False)},")
    k.append(f"                etiquetas = listOf({', '.join(json.dumps(t) for t in r['tags'])}), oficial = true, categoria = CategoriaRuta.desdeClave({json.dumps(r['categoria'])}),")
    k.append(f"                superficie = {json.dumps(r['superficie'])}, fuente = {json.dumps('OpenStreetMap (ODbL 1.0), ' + r['trazado'], ensure_ascii=False)},")
    k.append(f"                perfilElevacion = perfil(\"{','.join(map(str, r['perfil_elevacion_m']))}\"),")
    k.append(f"                puntos = trazo(\"{pts}\"),")
    k.append("            ),")
k += ["        )", "    }", "", "    /** Agrega las oficiales que falten (por ejemplo, antes de aplicar la migracion 022). */",
      "    fun conOficiales(rutas: List<Ruta>): List<Ruta> {", "        val ids = rutas.mapTo(HashSet()) { it.id }",
      "        return rutas + todas.filter { it.id !in ids }", "    }", "}"]
(RAIZ / "apps/android/contextos/rutas/domain/src/commonMain/kotlin/com/gacelas/rutas/domain/modelo/RutasLoja.kt").write_text("\n".join(k) + "\n")
print(len(rutas), "rutas", sum(len(r["sql"]) for r in rutas), "puntos sql", sum(len(r["cli"]) for r in rutas), "cliente")
