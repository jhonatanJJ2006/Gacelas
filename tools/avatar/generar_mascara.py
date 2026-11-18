"""
Genera la mascara de zonas del corredor 3D (apps/android/app/src/main/assets/models/runner-mascara.png).

El GLB tiene una sola malla con una sola textura: piel, camiseta, shorts y zapatillas estan pintados
en la misma imagen y la camiseta y los shorts tienen el mismo morado. Por eso cada texel se clasifica
con dos datos:
  - su color en la textura base (piel, ropa, zapatillas, pelo);
  - la altura del vertice del modelo al que pertenece (rasterizando cada triangulo en el espacio UV),
    que separa la camiseta (arriba de la cintura) de los shorts (entre cintura y rodilla).

Salida: PNG en escala de grises de 512x512 con un valor por zona:
  0 sin cambio (pelo, ojos, logos), 1 piel, 2 camiseta, 3 shorts, 4 zapatillas.

Uso: python -I tools/avatar/generar_mascara.py [ruta.glb] [salida.png]
Requiere Pillow.
"""
import colorsys
import io
import json
import struct
import sys

from PIL import Image

LADO = 512
# Alturas relativas (0 = pies, 1 = cabeza) de la pose de reposo.
CINTURA = 0.5
RODILLA = 0.26
TOBILLO = 0.09

GLB = sys.argv[1] if len(sys.argv) > 1 else 'apps/android/app/src/main/assets/models/gacelas-runner-animated.glb'
SALIDA = sys.argv[2] if len(sys.argv) > 2 else 'apps/android/app/src/main/assets/models/runner-mascara.png'

TIPOS = {5120: 'b', 5121: 'B', 5122: 'h', 5123: 'H', 5125: 'I', 5126: 'f'}
COMPONENTES = {'SCALAR': 1, 'VEC2': 2, 'VEC3': 3, 'VEC4': 4}


def leer_glb(ruta):
    d = open(ruta, 'rb').read()
    assert d[:4] == b'glTF', 'no es un GLB'
    largo = struct.unpack('<I', d[12:16])[0]
    j = json.loads(d[20:20 + largo])
    o = 20 + largo
    bl = struct.unpack('<I', d[o:o + 4])[0]
    return j, d[o + 8:o + 8 + bl]


def accesor(j, binario, i):
    a = j['accessors'][i]
    vista = j['bufferViews'][a['bufferView']]
    fmt = TIPOS[a['componentType']]
    n = COMPONENTES[a['type']]
    tam = struct.calcsize(fmt)
    paso = vista.get('byteStride', tam * n)
    base = vista.get('byteOffset', 0) + a.get('byteOffset', 0)
    salida = []
    for k in range(a['count']):
        ini = base + k * paso
        v = struct.unpack('<' + fmt * n, binario[ini:ini + tam * n])
        if a.get('normalized') and fmt != 'f':
            v = tuple(x / float((1 << (8 * tam)) - 1) for x in v)
        salida.append(v)
    return salida


def zona_color(r, g, b):
    """Clase por color: piel, ropa (morado/magenta), zapatilla (blanco/azul), pelo u otro."""
    h, s, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
    h *= 360
    if (h >= 255 or h <= 5) and s > 0.25 and v > 0.08:
        return 'ropa'
    if v < 0.12:
        return 'ropa'  # sombras muy oscuras de la camiseta
    if 8 <= h <= 40 and 0.15 <= s <= 0.6 and v > 0.55:
        return 'piel'
    if 15 <= h <= 45 and v <= 0.55:
        return 'pelo'
    if (190 <= h <= 235 and s > 0.3) or (s < 0.15 and v > 0.7):
        return 'zapatilla'
    return 'otro'


def main():
    j, binario = leer_glb(GLB)
    prim = j['meshes'][0]['primitives'][0]
    pos = accesor(j, binario, prim['attributes']['POSITION'])
    uv = accesor(j, binario, prim['attributes']['TEXCOORD_0'])
    idx = [x[0] for x in accesor(j, binario, prim['indices'])]
    ys = [p[1] for p in pos]
    y0, y1 = min(ys), max(ys)
    altura = [(y - y0) / (y1 - y0) for y in ys]

    # Altura media por texel: se rasteriza cada triangulo en el espacio UV.
    suma = [0.0] * (LADO * LADO)
    cuenta = [0] * (LADO * LADO)
    for t in range(0, len(idx), 3):
        a, b, c = idx[t], idx[t + 1], idx[t + 2]
        pts = [(uv[k][0] * LADO, uv[k][1] * LADO) for k in (a, b, c)]
        h = (altura[a] + altura[b] + altura[c]) / 3
        xmin = max(int(min(p[0] for p in pts)), 0); xmax = min(int(max(p[0] for p in pts)) + 1, LADO - 1)
        ymin = max(int(min(p[1] for p in pts)), 0); ymax = min(int(max(p[1] for p in pts)) + 1, LADO - 1)
        (ax, ay), (bx, by), (cx, cy) = pts
        den = (by - cy) * (ax - cx) + (cx - bx) * (ay - cy)
        if abs(den) < 1e-9:
            continue
        for py in range(ymin, ymax + 1):
            for px in range(xmin, xmax + 1):
                x, y = px + 0.5, py + 0.5
                w1 = ((by - cy) * (x - cx) + (cx - bx) * (y - cy)) / den
                w2 = ((cy - ay) * (x - cx) + (ax - cx) * (y - cy)) / den
                if w1 >= -0.02 and w2 >= -0.02 and w1 + w2 <= 1.04:
                    suma[py * LADO + px] += h
                    cuenta[py * LADO + px] += 1

    textura = j['textures'][j['materials'][0]['pbrMetallicRoughness']['baseColorTexture']['index']]['source']
    vista = j['bufferViews'][j['images'][textura]['bufferView']]
    img = Image.open(io.BytesIO(binario[vista.get('byteOffset', 0):vista.get('byteOffset', 0) + vista['byteLength']])).convert('RGB').resize((LADO, LADO))
    px = img.load()
    mascara = Image.new('L', (LADO, LADO), 0)
    mp = mascara.load()
    alto = {}
    ropa = set()
    for y in range(LADO):
        for x in range(LADO):
            n = cuenta[y * LADO + x]
            if n == 0:
                continue
            h = suma[y * LADO + x] / n
            alto[(x, y)] = h
            z = zona_color(*px[x, y])
            if z == 'piel':
                mp[x, y] = 1
            elif z == 'zapatilla' and h < TOBILLO:
                mp[x, y] = 4
            elif z == 'ropa':
                ropa.add((x, y))
    # La camiseta termina en la cadera (altura relativa 0,5): por debajo, hasta la rodilla, es el short.
    # Se decide por texel porque en el atlas algunas islas de camiseta y short se tocan.
    for t in ropa:
        h = alto[t]
        mp[t] = 2 if h >= CINTURA else 3 if h >= RODILLA else 0
    totales = {}
    for v in mascara.getdata():
        totales[v] = totales.get(v, 0) + 1
    mascara.save(SALIDA, optimize=True)
    print('mascara', SALIDA, {k: totales[k] for k in sorted(totales)})


if __name__ == '__main__':
    main()
