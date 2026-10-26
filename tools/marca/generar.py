"""Genera los iconos de Gacelas (propuesta A, Gacela en ruta) para Android y web.

Fuente: assets/marca/{icono,fondo,primer_plano}.svg (lienzo 108 x 108, zona segura 66).
Salidas:
  Android: drawable/ic_launcher_{background,foreground,monochrome}.xml (VectorDrawable),
           mipmap-<densidad>/ic_launcher(_round).png, drawable/splash_icono.xml,
           assets/marca/play_store_512.png
  Web:     public/favicon.svg, favicon.ico, favicon-32.png, apple-touch-icon.png,
           icon-192.png, icon-512.png, icon-maskable-512.png
Uso (desde la raiz del repo): python3 tools/marca/generar.py
Requiere: python3, Pillow y Playwright (chromium) para rasterizar los SVG.
"""
import io
import math
import pathlib

from PIL import Image
from playwright.sync_api import sync_playwright

RAIZ = pathlib.Path(__file__).resolve().parents[2]
MARCA = RAIZ / "assets/marca"
RES = RAIZ / "apps/android/app/src/main/res"
WEB = RAIZ / "apps/web/public"

GAZ = ("M28,60 C33,52 46,49 60,50 L66,49 C69,45 72,40 75,36 L73,27 C72,23 70,20 67,18 C71,19 74,22 76,27 "
       "L78,33 C80,33 83,35 86,38 L83,41 L79,41 C77,45 74,50 72,55 C77,56 83,57 90,55 L91,58 C84,61 77,62 69,61 "
       "C64,64 58,66 52,66 C45,71 36,75 24,78 L22,75 C31,72 38,68 42,63 C37,63 32,64 28,66 L22,62 Z")
ESTELAS = "M10,52 H24 M6,60 H22 M12,68 H20"
BRILLO = "M60,50 C66,52 70,56 72,55"
OJO = "M78.9,37 a1.1,1.1 0 1,0 2.2,0 a1.1,1.1 0 1,0 -2.2,0 Z"
NS = 'xmlns:android="http://schemas.android.com/apk/res/android" xmlns:aapt="http://schemas.android.com/aapt"'


def circulo(cx, cy, r):
    return f"M{cx - r:.2f},{cy:.2f} a{r:.2f},{r:.2f} 0 1,0 {2 * r:.2f},0 a{r:.2f},{r:.2f} 0 1,0 {-2 * r:.2f},0 Z"


def bezier(p0, p1, p2, p3, t):
    u = 1 - t
    return tuple(u ** 3 * a + 3 * u * u * t * b + 3 * u * t * t * c + t ** 3 * d for a, b, c, d in zip(p0, p1, p2, p3))


def puntos_ruta(paso=5.5):
    """Puntos de la ruta GPS punteada (M20 86 C34 80 44 90 58 82 S80 70 90 74), uno cada `paso`."""
    curvas = [((20, 86), (34, 80), (44, 90), (58, 82)), ((58, 82), (72, 74), (80, 70), (90, 74))]
    muestras = [bezier(*c, i / 400) for c in curvas for i in range(401)]
    out, acum, prev = [muestras[0]], 0.0, muestras[0]
    for p in muestras[1:]:
        acum += math.dist(p, prev)
        prev = p
        if acum >= paso:
            out.append(p)
            acum = 0.0
    return out


def mezcla(c1, c2, t):
    a = [int(c1[i:i + 2], 16) for i in (0, 2, 4)]
    b = [int(c2[i:i + 2], 16) for i in (0, 2, 4)]
    return "".join(f"{round(x + (y - x) * t):02X}" for x, y in zip(a, b))


def color_ruta(x):
    """Gradiente horizontal de la ruta: 7B1FA2 transparente -> A855F7 -> E9D5FF."""
    t = (x - 20) / 70
    if t < 0.5:
        return mezcla("7B1FA2", "A855F7", t / 0.5), t / 0.5
    return mezcla("A855F7", "E9D5FF", (t - 0.5) / 0.5), 1.0


def grad_lineal(x1, y1, x2, y2, paradas):
    items = "".join(f'<item android:offset="{o}" android:color="#{a}{c}"/>' for o, c, a in paradas)
    return (f'<aapt:attr name="android:fillColor"><gradient android:type="linear" android:startX="{x1}" android:startY="{y1}" '
            f'android:endX="{x2}" android:endY="{y2}">{items}</gradient></aapt:attr>')


def vector(cuerpo, tam=108):
    return (f'<?xml version="1.0" encoding="utf-8"?>\n<!-- Generado por tools/marca/generar.py; no editar a mano. -->\n'
            f'<vector {NS} android:width="{tam}dp" android:height="{tam}dp" android:viewportWidth="108" android:viewportHeight="108">\n{cuerpo}\n</vector>\n')


def capa_fondo():
    return vector(
        f'<path android:pathData="M0,0 H108 V108 H0 Z">{grad_lineal(0, 0, 108, 108, [(0, "1A1030", "FF"), (0.55, "0C1322", "FF"), (1, "070E1D", "FF")])}</path>\n'
        f'<path android:pathData="{circulo(54, 50, 46)}"><aapt:attr name="android:fillColor"><gradient android:type="radial" '
        f'android:centerX="54" android:centerY="45.4" android:gradientRadius="55.2"><item android:offset="0" android:color="#BF7B1FA2"/>'
        f'<item android:offset="1" android:color="#007B1FA2"/></gradient></aapt:attr></path>\n'
        f'<path android:fillAlpha="0.9" android:pathData="M0,82 L22,62 L34,72 L52,50 L70,70 L82,60 L108,80 V108 H0 Z">'
        f'{grad_lineal(0, 50, 0, 108, [(0, "2E3545", "FF"), (1, "141B2B", "FF")])}</path>\n'
        f'<path android:fillColor="#070E1D" android:fillAlpha="0.8" android:pathData="M0,92 L30,76 L50,86 L76,72 L108,88 V108 H0 Z"/>')


def gacela(mono=False, sombra=True):
    """Grupo de la gacela con estelas (transform del SVG: translate(54 54) scale(.82) translate(-56 -48))."""
    g = '<group android:scaleX="0.82" android:scaleY="0.82" android:translateX="8.08" android:translateY="14.64">\n'
    if mono:
        g += f'<path android:strokeColor="#FFFFFF" android:strokeWidth="2.4" android:strokeLineCap="round" android:pathData="{ESTELAS}"/>\n'
        g += f'<path android:fillColor="#FFFFFF" android:pathData="{GAZ}"/>\n'
        return g + "</group>"
    if sombra:
        g += f'<group android:translateY="1.9"><path android:fillColor="#000000" android:fillAlpha="0.35" android:pathData="{GAZ}"/></group>\n'
    g += f'<path android:strokeColor="#C084FC" android:strokeAlpha="0.7" android:strokeWidth="2.4" android:strokeLineCap="round" android:pathData="{ESTELAS}"/>\n'
    g += (f'<path android:pathData="{GAZ}">' + grad_lineal(22, 18, 91, 78, [(0, "F3E8FF", "FF"), (0.35, "C084FC", "FF"), (0.75, "A855F7", "FF"), (1, "7B1FA2", "FF")]) + '</path>\n')
    g += f'<path android:strokeColor="#F3E8FF" android:strokeAlpha="0.7" android:strokeWidth="0.9" android:pathData="{BRILLO}"/>\n'
    g += f'<path android:fillColor="#0C1322" android:pathData="{OJO}"/>\n'
    return g + "</group>"


def ruta(mono=False):
    capas = []
    for x, y in puntos_ruta():
        col, a = color_ruta(x)
        if mono:
            capas.append(f'<path android:fillColor="#FFFFFF" android:pathData="{circulo(x, y, 1.5)}"/>')
        else:
            capas.append(f'<path android:fillColor="#{col}" android:fillAlpha="{a * 0.35:.2f}" android:pathData="{circulo(x, y, 2.6)}"/>')
            capas.append(f'<path android:fillColor="#{col}" android:fillAlpha="{a:.2f}" android:pathData="{circulo(x, y, 1.5)}"/>')
    if mono:
        capas.append(f'<path android:fillColor="#FFFFFF" android:pathData="{circulo(90, 74, 3.2)}"/>')
    else:
        capas.append(f'<path android:fillColor="#10B981" android:fillAlpha="0.35" android:pathData="{circulo(90, 74, 5)}"/>')
        capas.append(f'<path android:fillColor="#10B981" android:pathData="{circulo(90, 74, 3.2)}"/>')
        capas.append(f'<path android:fillColor="#A855F7" android:pathData="{circulo(20, 86, 2)}"/>')
    return "\n".join(capas)


def png(pagina, svg, tam, radio="0"):
    pagina.set_viewport_size({"width": tam, "height": tam})
    s = svg.replace('width="1024" height="1024"', f'width="{tam}" height="{tam}"')
    pagina.set_content(f'<body style="margin:0;background:transparent"><div style="width:{tam}px;height:{tam}px;border-radius:{radio};overflow:hidden">{s}</div></body>')
    return Image.open(io.BytesIO(pagina.screenshot(omit_background=True))).convert("RGBA")


def main():
    dr = RES / "drawable"
    dr.mkdir(parents=True, exist_ok=True)
    (dr / "ic_launcher_background.xml").write_text(capa_fondo())
    (dr / "ic_launcher_foreground.xml").write_text(vector(ruta() + "\n" + gacela()))
    (dr / "ic_launcher_monochrome.xml").write_text(vector(ruta(mono=True) + "\n" + gacela(mono=True)))
    # Icono del SplashScreen API: solo la gacela (Android lo recorta a un circulo de 2/3 del lienzo).
    (dr / "splash_icono.xml").write_text(vector(gacela(sombra=False)))

    icono = (MARCA / "icono.svg").read_text()
    with sync_playwright() as p:
        b = p.chromium.launch()
        pg = b.new_page()
        for nombre, tam in {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}.items():
            d = RES / f"mipmap-{nombre}"
            d.mkdir(exist_ok=True)
            png(pg, icono, tam, "22%").save(d / "ic_launcher.png", optimize=True)
            png(pg, icono, tam, "50%").save(d / "ic_launcher_round.png", optimize=True)
        png(pg, icono, 512).convert("RGB").save(MARCA / "play_store_512.png", optimize=True)
        (WEB / "favicon.svg").write_text(icono.replace('width="1024" height="1024"', 'width="64" height="64"')
                                         .replace('<rect width="108" height="108"', '<rect width="108" height="108" rx="24"', 1)
                                         if '<rect width="108" height="108"' in icono else icono)
        png(pg, icono, 32, "22%").save(WEB / "favicon-32.png", optimize=True)
        grande = png(pg, icono, 256, "22%")
        grande.save(WEB / "favicon.ico", sizes=[(16, 16), (32, 32), (48, 48)])
        png(pg, icono, 180).convert("RGB").save(WEB / "apple-touch-icon.png", optimize=True)
        png(pg, icono, 192, "22%").save(WEB / "icon-192.png", optimize=True)
        png(pg, icono, 512, "22%").save(WEB / "icon-512.png", optimize=True)
        png(pg, icono, 512).convert("RGB").save(WEB / "icon-maskable-512.png", optimize=True)
        # Logo interno para imagenes de compartir (Android y web) y tarjeta Open Graph.
        logo = png(pg, icono, 512, "22%")
        logo.save(RES / "drawable-nodpi/compartir_logo.png", optimize=True)
        (WEB / "marca").mkdir(exist_ok=True)
        logo.save(WEB / "marca/logo-compartir.png", optimize=True)
        pg.set_viewport_size({"width": 1200, "height": 630})
        s = icono.replace('width="1024" height="1024"', 'width="300" height="300"')
        pg.set_content(
            '<body style="margin:0;width:1200px;height:630px;display:flex;align-items:center;justify-content:center;gap:56px;'
            'background:radial-gradient(circle at 50% 45%,#1A1030,#0C1322 60%,#070E1D);font-family:Anton,Impact,sans-serif">'
            f'<div style="width:300px;height:300px;border-radius:22%;overflow:hidden;box-shadow:0 0 80px #A855F755">{s}</div>'
            '<div><div style="color:#fff;font-size:120px;letter-spacing:10px;font-weight:800">GACELAS</div>'
            '<div style="color:#C084FC;font:600 36px Inter,sans-serif">Rutas para correr en Loja</div></div></body>')
        Image.open(io.BytesIO(pg.screenshot())).convert("RGB").save(WEB / "og-default.png", optimize=True)
        b.close()
    print("Iconos generados.")


if __name__ == "__main__":
    main()
