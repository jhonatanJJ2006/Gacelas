# Dibuja cuadros del vector animado intro_marca.xml en tiempos dados (evalua sus animadores) para
# revisar la intro sin dispositivo. Uso: previsualizar_intro_avd.py intro_marca.xml res_dir salida.png
import sys, re, xml.etree.ElementTree as ET, skia
from PIL import Image
A='{http://schemas.android.com/apk/res/android}'; AA='{http://schemas.android.com/aapt}'
src, res, out = sys.argv[1:4]
root = ET.parse(src).getroot()
def interp(ref):
    if 'linear' in ref: return lambda u: u
    if 'accelerate_quad' in ref: return lambda u: u*u
    if 'decelerate_quad' in ref: return lambda u: 1-(1-u)**2
    n = ref.split('/')[-1]; e = ET.parse(f'{res}/interpolator/{n}.xml').getroot()
    x1,y1,x2,y2 = [float(e.get(A+k)) for k in ('controlX1','controlY1','controlX2','controlY2')]
    def f(u):
        lo,hi=0.,1.
        for _ in range(40):
            m=(lo+hi)/2; x=3*(1-m)**2*m*x1+3*(1-m)*m*m*x2+m**3
            lo,hi=(m,hi) if x<u else (lo,m)
        m=(lo+hi)/2; return 3*(1-m)**2*m*y1+3*(1-m)*m*m*y2+m**3
    return f
anims = {}
for t in root.findall('target'):
    for o in t.iter('objectAnimator'):
        anims.setdefault(t.get(A+'name'), []).append((o.get(A+'propertyName'), float(o.get(A+'valueFrom')), float(o.get(A+'valueTo')), int(o.get(A+'startOffset')), int(o.get(A+'duration')), interp(o.get(A+'interpolator'))))
def valor(nombre, prop, base, t):
    v = base
    for p, a, b, ini, d, f in sorted(anims.get(nombre, []), key=lambda x: x[3]):
        if p == prop and t >= ini: v = a + (b - a) * f(min(1, (t - ini) / d))
    return v
def svg(d):
    tok = re.findall(r'[A-Za-z]|[-+]?(?:\d*\.\d+|\d+\.?)(?:[eE][-+]?\d+)?', d); i = 0; p = skia.Path(); x = y = 0; cmd = None; lc = (0, 0)
    def n():
        nonlocal i; i += 1; return float(tok[i-1])
    while i < len(tok):
        if tok[i].isalpha(): cmd = tok[i]; i += 1
        r = cmd.islower(); C = cmd.upper(); ox, oy = (x, y) if r else (0, 0)
        if C == 'Z': p.close(); continue
        if C == 'M': x, y = ox+n(), oy+n(); p.moveTo(x, y); cmd = 'l' if r else 'L'
        elif C == 'L': x, y = ox+n(), oy+n(); p.lineTo(x, y)
        elif C == 'H': x = ox+n(); p.lineTo(x, y)
        elif C == 'V': y = oy+n(); p.lineTo(x, y)
        elif C == 'C':
            a, b, c2, d2, x2, y2 = ox+n(), oy+n(), ox+n(), oy+n(), ox+n(), oy+n(); p.cubicTo(a, b, c2, d2, x2, y2); lc = (c2, d2); x, y = x2, y2
        elif C == 'S':
            a, b = (2*x-lc[0], 2*y-lc[1]); c2, d2, x2, y2 = ox+n(), oy+n(), ox+n(), oy+n(); p.cubicTo(a, b, c2, d2, x2, y2); lc = (c2, d2); x, y = x2, y2
        elif C == 'Q':
            a, b, x, y = ox+n(), oy+n(), ox+n(), oy+n(); p.quadTo(a, b, x, y)
        elif C == 'A':
            rx, ry, rot, la, sw = n(), n(), n(), n(), n(); x, y = ox+n(), oy+n()
            p.arcTo(rx, ry, rot, skia.Path.ArcSize.kLarge_ArcSize if la else skia.Path.ArcSize.kSmall_ArcSize, skia.PathDirection.kCW if sw else skia.PathDirection.kCCW, x, y)
        else: raise ValueError(cmd)
    return p
vec = root.find(AA+'attr').find('vector')
def col(h, alpha=1.):
    h = h.lstrip('#'); h = ('FF' + h) if len(h) == 6 else h
    a = int(h[0:2],16)/255*alpha; return skia.Color(int(h[2:4],16), int(h[4:6],16), int(h[6:8],16), int(a*255))
def shader(g, alpha):
    items = [(float(i.get(A+'offset')), i.get(A+'color')) for i in g.findall('item')] or [(0, g.get(A+'startColor')), (1, g.get(A+'endColor'))]
    cs = [col(c, alpha) for _, c in items]; ps = [p for p, _ in items]
    if g.get(A+'type') == 'radial':
        return skia.GradientShader.MakeRadial(skia.Point(float(g.get(A+'centerX')), float(g.get(A+'centerY'))), float(g.get(A+'gradientRadius')), cs, ps)
    return skia.GradientShader.MakeLinear([skia.Point(float(g.get(A+'startX')), float(g.get(A+'startY'))), skia.Point(float(g.get(A+'endX')), float(g.get(A+'endY')))], cs, ps)
def dibujar(c, nodo, t):
    for e in nodo:
        n = e.get(A+'name')
        if e.tag == 'group':
            c.save()
            c.translate(valor(n,'translateX',float(e.get(A+'translateX',0)),t), valor(n,'translateY',float(e.get(A+'translateY',0)),t))
            c.scale(valor(n,'scaleX',float(e.get(A+'scaleX',1)),t), valor(n,'scaleY',float(e.get(A+'scaleY',1)),t))
            dibujar(c, e, t); c.restore()
        elif e.tag == 'path':
            p = svg(e.get(A+'pathData'))
            fa = valor(n,'fillAlpha',float(e.get(A+'fillAlpha',1)),t)
            g = e.find(AA+'attr')
            if (e.get(A+'fillColor') or g is not None) and fa > 0:
                pt = skia.Paint(AntiAlias=True)
                if g is not None: pt.setShader(shader(g.find('gradient'), fa))
                else: pt.setColor(col(e.get(A+'fillColor'), fa))
                c.drawPath(p, pt)
            if e.get(A+'strokeColor'):
                sa = valor(n,'strokeAlpha',float(e.get(A+'strokeAlpha',1)),t)
                ts = valor(n,'trimPathStart',float(e.get(A+'trimPathStart',0)),t); te = valor(n,'trimPathEnd',float(e.get(A+'trimPathEnd',1)),t)
                if sa <= 0 or te <= ts: continue
                m = skia.PathMeasure(p, False); L = m.getLength(); seg = skia.Path(); m.getSegment(ts*L, te*L, seg, True)
                cap = skia.Paint.kRound_Cap if e.get(A+'strokeLineCap') == 'round' else skia.Paint.kButt_Cap
                c.drawPath(seg, skia.Paint(Color=col(e.get(A+'strokeColor'), sa), Style=skia.Paint.kStroke_Style, StrokeWidth=float(e.get(A+'strokeWidth')), StrokeCap=cap, StrokeJoin=skia.Paint.kRound_Join, AntiAlias=True))
tiempos = [0, 300, 600, 900, 1000, 1200, 1500, 1800, 2100]
W, H = 1080, 1920; k = .25; hoja = Image.new('RGB', (int(W*k)*len(tiempos), int(H*k)))
for i, t in enumerate(tiempos):
    s = skia.Surface(W, H); c = s.getCanvas(); dibujar(c, vec, t)
    im = Image.fromarray(s.makeImageSnapshot().toarray(colorType=skia.kRGBA_8888_ColorType)).convert('RGB').resize((int(W*k), int(H*k)))
    hoja.paste(im, (i*int(W*k), 0))
hoja.save(out); print(out)
