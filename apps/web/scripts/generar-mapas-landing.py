#!/usr/bin/env python3
"""Genera los mapas nocturnos de la landing (public/images/landing-mapa-*.webp) con datos reales
de OpenStreetMap (© colaboradores de OpenStreetMap, ODbL) y escribe las posiciones de la ruta en
src/app/landing/mapasLanding.ts para que los puntos HTML queden sobre la ruta.

Uso (desde apps/web):  python3 scripts/generar-mapas-landing.py
Requiere: Python 3, Pillow y Playwright de apps/web (npx playwright install chromium).
Los datos crudos se descargan a un directorio temporal y no se versionan.
"""
import heapq, json, math, os, subprocess, tempfile, urllib.request
import xml.etree.ElementTree as ET
from PIL import Image

AQUI = os.path.dirname(os.path.abspath(__file__))
WEB = os.path.dirname(AQUI)
TMP = os.path.join(tempfile.gettempdir(), 'gacelas-osm')
os.makedirs(TMP, exist_ok=True)

# Paleta del mapa del inicio de sesion (diseño C4).
FONDO, MANZANA, PARQUE, AGUA = '#0B101C', '#111827', '#0F2A24', '#1E5A8A'
EDIF, EDIF_TECHO, EDIF_SOMBRA = '#1A2236', '#222C44', '#070B14'
CALLE = {'primary': (9, '#2A3350'), 'secondary': (7, '#283048'), 'tertiary': (6, '#252C42'),
         'residential': (4, '#20273A'), 'unclassified': (4, '#20273A'), 'pedestrian': (3, '#1E2536'),
         'service': (2.2, '#1C2233'), 'footway': (1.4, '#1C2233'), 'path': (1.4, '#1C2233'),
         'steps': (1.4, '#1C2233'), 'living_street': (3, '#20273A')}
for k in list(CALLE):
    CALLE[k + '_link'] = CALLE[k]

MAPAS = [
    {  # Hero: centro de Loja, desde la Puerta de la Ciudad hacia el sur.
        'id': 'hero', 'bbox': (-79.2120, -4.0060, -79.1960, -3.9900), 'ancho': 960, 'alto': 1200,
        'vista': (-79.2085, -3.9995, -79.1995, -3.9875),
        'ruta': [(-3.98985, -79.20425), (-3.99330, -79.20330), (-3.99700, -79.20200)],
        'etiquetas': ['Avenida Universitaria', 'Avenida Manuel Agustín Aguirre', 'Río Zamora',
                      'Río Malacatos', 'Puerta de la Ciudad', 'Plaza central de Loja'],
    },
    {  # Seccion "Que te sigan": Parque Jipiro y el río Zamora.
        'id': 'vivo', 'bbox': (-79.2080, -3.9850, -79.1960, -3.9740), 'ancho': 1280, 'alto': 640,
        'vista': (-79.2072, -3.9835, -79.1982, -3.9790),
        'ruta': [(-3.98250, -79.20600), (-3.98120, -79.20330), (-3.98050, -79.20000)],
        'etiquetas': ['Río Zamora', 'Parque Recreacional Jipiro', 'Avenida Orillas del Zamora'],
    },
]


def descargar(bbox):
    ruta = os.path.join(TMP, '%s.xml' % '_'.join(map(str, bbox)))
    if not os.path.exists(ruta):
        url = 'https://api.openstreetmap.org/api/0.6/map?bbox=%s' % ','.join(map(str, bbox))
        req = urllib.request.Request(url, headers={'User-Agent': 'gacelas-landing-script/1.0'})
        with urllib.request.urlopen(req, timeout=120) as r, open(ruta, 'wb') as f:
            f.write(r.read())
    return ET.parse(ruta).getroot()


def generar(m):
    raiz = descargar(m['bbox'])
    nodos = {n.get('id'): (float(n.get('lat')), float(n.get('lon'))) for n in raiz.iter('node')}
    nodos_tags = {n.get('id'): {t.get('k'): t.get('v') for t in n.iter('tag')} for n in raiz.iter('node')}
    oeste, sur, este, norte = m['vista']
    W, H = m['ancho'], m['alto']
    kx = math.cos(math.radians((sur + norte) / 2))
    escala = max(W / ((este - oeste) * kx), H / (norte - sur))
    cx, cy = (oeste + este) / 2, (sur + norte) / 2

    def xy(p):
        lat, lon = p
        return (W / 2 + (lon - cx) * kx * escala, H / 2 - (lat - cy) * escala)

    def pts(ids):
        return ' '.join('%.1f,%.1f' % xy(nodos[i]) for i in ids if i in nodos)

    capas = {'area': [], 'agua': [], 'calle_borde': [], 'calle': [], 'sombra': [], 'edif': [], 'texto': []}
    grafo = {}
    for w in raiz.iter('way'):
        t = {x.get('k'): x.get('v') for x in w.iter('tag')}
        ids = [nd.get('ref') for nd in w.iter('nd')]
        if len(ids) < 2:
            continue
        cerrado = ids[0] == ids[-1]
        if t.get('leisure') in ('park', 'pitch', 'garden', 'track') or t.get('landuse') in ('grass', 'recreation_ground', 'forest') or t.get('natural') in ('wood', 'scrub'):
            if cerrado:
                capas['area'].append('<polygon points="%s" fill="%s"/>' % (pts(ids), PARQUE))
        elif t.get('natural') == 'water' and cerrado:
            capas['agua'].append('<polygon points="%s" fill="%s" fill-opacity="0.5"/>' % (pts(ids), AGUA))
        elif 'waterway' in t:
            ancho = 7 if t['waterway'] == 'river' else 3
            capas['agua'].append('<polyline points="%s" fill="none" stroke="%s" stroke-width="%s" stroke-opacity="0.75" stroke-linecap="round" stroke-linejoin="round"/>' % (pts(ids), AGUA, ancho))
        elif 'building' in t and cerrado:
            nivel = int(t.get('building:levels', '2').split(';')[0] or 2) if t.get('building:levels', '2').split(';')[0].isdigit() else 2
            dz = min(2 + nivel * 1.2, 9)
            p = [xy(nodos[i]) for i in ids if i in nodos]
            capas['sombra'].append('<polygon points="%s" fill="%s"/>' % (' '.join('%.1f,%.1f' % (x + dz * 0.6, y + dz) for x, y in p), EDIF_SOMBRA))
            capas['edif'].append('<polygon points="%s" fill="%s" stroke="%s" stroke-width="0.8"/>' % (' '.join('%.1f,%.1f' % q for q in p), EDIF, EDIF_TECHO))
        elif t.get('highway') in CALLE:
            a, color = CALLE[t['highway']]
            a *= W / 960
            p = pts(ids)
            capas['calle_borde'].append('<polyline points="%s" fill="none" stroke="#070B14" stroke-width="%.1f" stroke-linecap="round" stroke-linejoin="round"/>' % (p, a + 2))
            capas['calle'].append('<polyline points="%s" fill="none" stroke="%s" stroke-width="%.1f" stroke-linecap="round" stroke-linejoin="round"/>' % (p, color, a))
            if t['highway'] not in ('steps', 'service') and t.get('access') != 'private':
                for u, v in zip(ids, ids[1:]):
                    if u in nodos and v in nodos:
                        d = math.dist(xy(nodos[u]), xy(nodos[v]))
                        grafo.setdefault(u, []).append((v, d))
                        grafo.setdefault(v, []).append((u, d))
        nombre = t.get('name')
        if nombre in m['etiquetas'] and ('highway' in t or 'waterway' in t) and len(ids) > 3:
            capas['texto'].append((nombre, ids))

    # Solo la red conectada mas grande, para que siempre exista camino entre paradas.
    sin_ver, mayor = set(grafo), set()
    while sin_ver:
        pila, comp = [sin_ver.pop()], set()
        while pila:
            u = pila.pop()
            comp.add(u)
            for v, _ in grafo[u]:
                if v in sin_ver:
                    sin_ver.discard(v)
                    pila.append(v)
        if len(comp) > len(mayor):
            mayor = comp

    def cercano(p):
        q = xy(p)
        return min(mayor, key=lambda n: math.dist(xy(nodos[n]), q))

    def camino(a, b):
        dist, prev, cola = {a: 0}, {}, [(0, a)]
        while cola:
            d, u = heapq.heappop(cola)
            if u == b:
                break
            if d > dist.get(u, 1e18):
                continue
            for v, w in grafo.get(u, []):
                if d + w < dist.get(v, 1e18):
                    dist[v], prev[v] = d + w, u
                    heapq.heappush(cola, (d + w, v))
        r = [b]
        while r[-1] != a:
            r.append(prev[r[-1]])
        return r[::-1]

    paradas = [cercano(p) for p in m['ruta']]
    ruta = []
    for a, b in zip(paradas, paradas[1:]):
        ruta += camino(a, b)[(1 if ruta else 0):]
    rxy = [xy(nodos[i]) for i in ruta]
    largo = [0.0]
    for p, q in zip(rxy, rxy[1:]):
        largo.append(largo[-1] + math.dist(p, q))

    def en(f):
        obj = f * largo[-1]
        for i in range(1, len(largo)):
            if largo[i] >= obj:
                k = (obj - largo[i - 1]) / max(largo[i] - largo[i - 1], 1e-9)
                x = rxy[i - 1][0] + (rxy[i][0] - rxy[i - 1][0]) * k
                y = rxy[i - 1][1] + (rxy[i][1] - rxy[i - 1][1]) * k
                return {'x': round(100 * x / W, 1), 'y': round(100 * y / H, 1)}
        return {'x': round(100 * rxy[-1][0] / W, 1), 'y': round(100 * rxy[-1][1] / H, 1)}

    ruta_pts = ' '.join('%.1f,%.1f' % p for p in rxy)
    textos = []
    vistos = set()
    for nombre, ids in capas['texto']:
        if nombre in vistos:
            continue
        p = [xy(nodos[i]) for i in ids if i in nodos]
        dentro = [q for q in p if 40 < q[0] < W - 40 and 40 < q[1] < H - 40]
        if len(dentro) < 3:
            continue
        vistos.add(nombre)
        a, b = dentro[0], dentro[-1]
        if a[0] > b[0]:
            a, b = b, a
        ang = math.degrees(math.atan2(b[1] - a[1], b[0] - a[0]))
        mx, my = dentro[len(dentro) // 2]
        color = '#5B8DB8' if nombre.startswith('Río') else '#6B7491'
        textos.append('<text x="%.1f" y="%.1f" transform="rotate(%.1f %.1f %.1f)" fill="%s" font-family="Archivo, Arial, sans-serif" font-size="%d" font-weight="600" letter-spacing="1.5" text-anchor="middle">%s</text>' % (mx, my - 6, ang, mx, my, color, int(13 * W / 960), nombre.upper()))
    for nid, t in nodos_tags.items():
        if 'Puerta De La Ciudad' in t.get('name', '') and 'Puerta de la Ciudad' in m['etiquetas']:
            x, y = xy(nodos[nid])
            textos.append('<text x="%.1f" y="%.1f" fill="#9AA3BF" font-family="Archivo, Arial, sans-serif" font-size="14" font-weight="700" letter-spacing="1.5">PUERTA DE LA CIUDAD</text>' % (x + 22, y + 5))

    svg = '''<svg xmlns="http://www.w3.org/2000/svg" width="{W}" height="{H}" viewBox="0 0 {W} {H}">
<defs>
 <radialGradient id="tinte" cx="45%" cy="45%" r="70%"><stop offset="0" stop-color="#4A154B" stop-opacity="0.35"/><stop offset="1" stop-color="#0B101C" stop-opacity="0"/></radialGradient>
 <filter id="brillo" x="-20%" y="-20%" width="140%" height="140%"><feGaussianBlur stdDeviation="9"/></filter>
</defs>
<rect width="{W}" height="{H}" fill="{MANZANA}"/>
{area}{agua}{borde}{calle}{sombra}{edif}
<rect width="{W}" height="{H}" fill="url(#tinte)"/>
{textos}
<polyline points="{ruta}" fill="none" stroke="#DB2777" stroke-opacity="0.6" stroke-width="18" stroke-linecap="round" stroke-linejoin="round" filter="url(#brillo)"/>
<polyline points="{ruta}" fill="none" stroke="#F472B6" stroke-width="6" stroke-linecap="round" stroke-linejoin="round"/>
</svg>'''.format(W=W, H=H, MANZANA=MANZANA, area=''.join(capas['area']), agua=''.join(capas['agua']),
                 borde=''.join(capas['calle_borde']), calle=''.join(capas['calle']), sombra=''.join(capas['sombra']),
                 edif=''.join(capas['edif']), textos=''.join(textos), ruta=ruta_pts)
    svg_ruta = os.path.join(TMP, m['id'] + '.svg')
    with open(svg_ruta, 'w') as f:
        f.write(svg)
    png = os.path.join(TMP, m['id'] + '.png')
    js = ("import { chromium } from '%s/node_modules/playwright/index.mjs'\n"
          "const b = await chromium.launch(); const p = await b.newPage({ viewport: { width: %d, height: %d } })\n"
          "await p.goto('file://%s'); await p.screenshot({ path: '%s' }); await b.close()\n") % (WEB, W, H, svg_ruta, png)
    mjs = os.path.join(TMP, 'render.mjs')
    with open(mjs, 'w') as f:
        f.write(js)
    subprocess.run(['node', mjs], check=True)
    salida = os.path.join(WEB, 'public', 'images', 'landing-mapa-%s.webp' % m['id'])
    Image.open(png).convert('RGB').save(salida, 'WEBP', quality=72, method=6)
    print(salida, os.path.getsize(salida) // 1024, 'KB')
    return {'inicio': en(0), 'fin': en(1), 'manada': [en(0.3), en(0.55)], 'vivo': en(0.78)}


if __name__ == '__main__':
    datos = {m['id']: generar(m) for m in MAPAS}
    ts = ('// Generado por scripts/generar-mapas-landing.py. No editar a mano.\n'
          '// Posiciones en %% de cada imagen (public/images/landing-mapa-*.webp), sobre la ruta real.\n'
          'export const MAPAS_LANDING = %s as const\n') % json.dumps(datos, indent=2)
    with open(os.path.join(WEB, 'src', 'app', 'landing', 'mapasLanding.ts'), 'w') as f:
        f.write(ts)
