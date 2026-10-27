# Genera apps/android/app/src/main/res/drawable/intro_marca.xml: la intro v2b "Ruta y destello"
# como AnimatedVectorDrawable (docs/MARCA.md). Solo usa propiedades que Android anima en el
# RenderThread (trimPath, fillAlpha, strokeAlpha, scale, translate), asi la animacion sigue a
# 60 fps aunque el hilo principal este ocupado montando la app.
# Uso: python tools/marca/generar_intro_avd.py ruta/Anton-Regular.ttf salida.xml
import sys
from fontTools.ttLib import TTFont
from fontTools.pens.svgPathPen import SVGPathPen
from fontTools.pens.transformPen import TransformPen
W, H = 1080, 1920; CX, CY = 540, 880
FONDO, MOR, MORC, MORO, LILA, VERDE = '#0C1322', '#A855F7', '#C084FC', '#7B1FA2', '#F3E8FF', '#10B981'
RUTA = "M-80,1560 C180,1500 260,1300 470,1330 S760,1240 820,1080 S700,860 540,900"
GAZ = "M28 60 C33 52 46 49 60 50 L66 49 C69 45 72 40 75 36 L73 27 C72 23 70 20 67 18 C71 19 74 22 76 27 L78 33 C80 33 83 35 86 38 L83 41 L79 41 C77 45 74 50 72 55 C77 56 83 57 90 55 L91 58 C84 61 77 62 69 61 C64 64 58 66 52 66 C45 71 36 75 24 78 L22 75 C31 72 38 68 42 63 C37 63 32 64 28 66 L22 62 Z"
LINEAS = "M10 52 H24 M6 60 H22 M12 68 H20"
EIO = "M0,0 C0.65,0 0.35,1 1,1"; EO = "M0,0 C0.2,0.8 0.4,1 1,1"; BACK = "M0,0 C0.3,1.5 0.55,1 1,1"
DUR = 2200

def glifos(ttf, texto="RUNCITY", size=168, y=1300, track=.16):
    f = TTFont(ttf); gs = f.getGlyphSet(); cmap = f.getBestCmap(); k = size / f['head'].unitsPerEm
    nombres = [cmap[ord(c)] for c in texto]; anchos = [gs[n].width * k for n in nombres]
    tot = sum(anchos) + track * size * (len(texto) - 1); x = W / 2 - tot / 2; out = []
    for n, a in zip(nombres, anchos):
        pen = SVGPathPen(gs); gs[n].draw(TransformPen(pen, (k, 0, 0, -k, x, y))); out.append(pen.getCommands())
        x += a + track * size
    return out

A = []  # (target, [animadores])
def anim(prop, desde, hasta, ini, dur, interp=EIO):
    return f'<objectAnimator android:propertyName="{prop}" android:valueFrom="{desde}" android:valueTo="{hasta}" android:startOffset="{ini}" android:duration="{dur}" android:valueType="floatType" android:interpolator="@{interp}"/>'

def main(ttf, salida):
    letras = glifos(ttf)
    v = []; t = {}
    def tgt(nombre, *a): t.setdefault(nombre, []).extend(a)
    # Vineta violeta que aparece despacio.
    v.append(f'''<path android:name="vineta" android:fillAlpha="0" android:pathData="M0,0 H{W} V{H} H0 Z"><aapt:attr name="android:fillColor"><gradient android:type="radial" android:centerX="{CX}" android:centerY="{CY}" android:gradientRadius="1150" android:startColor="#FF1A1030" android:endColor="#001A1030"/></aapt:attr></path>''')
    tgt("vineta", anim("fillAlpha", 0, 1, 0, 600, EO))
    # Cometa por la ruta: tres trazos (halo, medio, nucleo) + nucleo claro + cabeza.
    for i, (w, a) in enumerate([(60, .10), (30, .22), (9, 1)]):
        v.append(f'<path android:name="ruta{i}" android:pathData="{RUTA}" android:strokeColor="{MOR}" android:strokeAlpha="{a}" android:strokeWidth="{w}" android:strokeLineCap="round" android:trimPathEnd="0"/>')
        tgt(f"ruta{i}", anim("trimPathEnd", 0, 1, 0, 950), anim("strokeAlpha", a, 0, 1200, 350))
    v.append(f'<path android:name="nucleo" android:pathData="{RUTA}" android:strokeColor="{LILA}" android:strokeAlpha=".9" android:strokeWidth="5" android:strokeLineCap="round" android:trimPathEnd="0"/>')
    tgt("nucleo", anim("trimPathEnd", 0, 1, 0, 950), anim("trimPathStart", 0, .82, 0, 950), anim("strokeAlpha", .9, 0, 1200, 350))
    for i, (w, c, a) in enumerate([(140, MOR, .18), (48, VERDE, .9), (22, '#FFFFFF', 1)]):
        v.append(f'<path android:name="cabeza{i}" android:pathData="{RUTA}" android:strokeColor="{c}" android:strokeAlpha="{a}" android:strokeWidth="{w}" android:strokeLineCap="round" android:trimPathStart="0" android:trimPathEnd="0.004"/>')
        tgt(f"cabeza{i}", anim("trimPathStart", 0, .996, 0, 950), anim("trimPathEnd", .004, 1, 0, 950), anim("strokeAlpha", a, 0, 930, 80, "android:interpolator/linear"))
    # Logo (gacela) con rebote suave y zoom final.
    v.append(f'''<group android:name="logoZoom" android:translateX="{CX}" android:translateY="{CY}"><group android:name="logo" android:scaleX="0" android:scaleY="0"><group android:translateX="-56" android:translateY="-48">
<path android:name="lineas" android:pathData="{LINEAS}" android:strokeColor="{MORC}" android:strokeAlpha="0" android:strokeWidth="2.4" android:strokeLineCap="round"/>
<path android:name="gazHalo" android:pathData="{GAZ}" android:strokeColor="{MOR}" android:strokeAlpha="0" android:strokeWidth="5" android:strokeLineJoin="round"/>
<path android:name="gaz" android:pathData="{GAZ}" android:fillAlpha="0"><aapt:attr name="android:fillColor"><gradient android:type="linear" android:startX="22" android:startY="18" android:endX="91" android:endY="78"><item android:offset="0" android:color="{LILA}"/><item android:offset="0.35" android:color="{MORC}"/><item android:offset="0.75" android:color="{MOR}"/><item android:offset="1" android:color="{MORO}"/></gradient></aapt:attr></path>
<path android:name="ojo" android:pathData="M78.9,37 a1.1,1.1 0 1,0 2.2,0 a1.1,1.1 0 1,0 -2.2,0" android:fillColor="{FONDO}" android:fillAlpha="0"/>
</group></group></group>''')
    s0, s1 = 8.2 * .82, 8.2
    tgt("logo", anim("scaleX", s0, s1, 910, 550, BACK), anim("scaleY", s0, s1, 910, 550, BACK))
    tgt("logoZoom", anim("scaleX", 1, 1.35, 1780, 420), anim("scaleY", 1, 1.35, 1780, 420))
    for n, a, p in [("lineas", .7, "strokeAlpha"), ("gazHalo", .35, "strokeAlpha"), ("gaz", 1, "fillAlpha"), ("ojo", 1, "fillAlpha")]:
        tgt(n, anim(p, 0, a, 910, 340, EO), anim(p, a, 0, 1780, 420))
    # Palabra RUNCITY (Anton, con tracking) letra por letra.
    for i, d in enumerate(letras):
        ini = 1130 + i * 45
        v.append(f'<group android:name="letra{i}" android:translateY="40"><path android:name="brillo{i}" android:pathData="{d}" android:strokeColor="{MOR}" android:strokeAlpha="0" android:strokeWidth="14" android:strokeLineJoin="round"/><path android:name="texto{i}" android:pathData="{d}" android:fillColor="#FFFFFF" android:fillAlpha="0"/></group>')
        tgt(f"letra{i}", anim("translateY", 40, 0, ini, 420, EO))
        tgt(f"brillo{i}", anim("strokeAlpha", 0, .35, ini, 420, EO), anim("strokeAlpha", .35, 0, 1780, 300))
        tgt(f"texto{i}", anim("fillAlpha", 0, 1, ini, 420, EO), anim("fillAlpha", 1, 0, 1780, 300))
    # Destello anamorfico al llegar (centro del logo, t = 0,97 s).
    v.append(f'''<group android:name="destello" android:translateX="{CX}" android:translateY="{CY}" android:scaleX="0" android:scaleY="0">
<path android:pathData="M-700,0 a700,700 0 1,0 1400,0 a700,700 0 1,0 -1400,0"><aapt:attr name="android:fillColor"><gradient android:type="radial" android:centerX="0" android:centerY="0" android:gradientRadius="700"><item android:offset="0" android:color="{LILA}"/><item android:offset="0.35" android:color="#73A855F7"/><item android:offset="1" android:color="#00A855F7"/></gradient></aapt:attr></path>
<group android:scaleY="0.035"><path android:pathData="M-1100,0 a1100,1100 0 1,0 2200,0 a1100,1100 0 1,0 -2200,0"><aapt:attr name="android:fillColor"><gradient android:type="radial" android:centerX="0" android:centerY="0" android:gradientRadius="1100"><item android:offset="0" android:color="#FFFFFFFF"/><item android:offset="0.3" android:color="#80C084FC"/><item android:offset="1" android:color="#00A855F7"/></gradient></aapt:attr></path></group></group>''')
    tgt("destello", anim("scaleX", 0, 1, 750, 220, "android:interpolator/accelerate_quad"), anim("scaleY", 0, 1, 750, 220, "android:interpolator/accelerate_quad"),
        anim("scaleX", 1, 0, 970, 220, "android:interpolator/decelerate_quad"), anim("scaleY", 1, 0, 970, 220, "android:interpolator/decelerate_quad"))
    # Fundido final exacto al fondo de la app.
    v.append(f'<path android:name="cierre" android:pathData="M0,0 H{W} V{H} H0 Z" android:fillColor="{FONDO}" android:fillAlpha="0"/>')
    tgt("cierre", anim("fillAlpha", 0, 1, DUR - 180, 180))
    targets = "\n".join(f'<target android:name="{n}"><aapt:attr name="android:animation"><set android:ordering="together">{"".join(a)}</set></aapt:attr></target>' for n, a in t.items())
    xml = f'''<?xml version="1.0" encoding="utf-8"?>
<!-- Generado por tools/marca/generar_intro_avd.py; no editar a mano. Intro v2b Runcity (docs/MARCA.md). -->
<animated-vector xmlns:android="http://schemas.android.com/apk/res/android" xmlns:aapt="http://schemas.android.com/aapt">
<aapt:attr name="android:drawable"><vector android:width="360dp" android:height="640dp" android:viewportWidth="{W}" android:viewportHeight="{H}">
<path android:pathData="M0,0 H{W} V{H} H0 Z" android:fillColor="{FONDO}"/>
{chr(10).join(v)}
</vector></aapt:attr>
{targets}
</animated-vector>
'''
    # Interpoladores propios como recursos aparte (pathInterpolator).
    xml = xml.replace(f'@{EIO}', '@interpolator/intro_eio').replace(f'@{EO}', '@interpolator/intro_eo').replace(f'@{BACK}', '@interpolator/intro_back').replace('@android:interpolator/', '@android:interpolator/')
    open(salida, 'w').write(xml)

if __name__ == '__main__':
    main(sys.argv[1], sys.argv[2])
