# Opciones de intro Gacelas/Runcity, cuadro por cuadro con skia. Uso: python render_opciones.py A|B|C|D|D2|D3|E outdir  (D2MAPA archivo.png: mapa solo)
import sys, os, math, random, re, numpy as np, skia
from multiprocessing import Pool
from PIL import Image
VAR=sys.argv[1]; OUT=sys.argv[2]
if VAR!='D2MAPA': os.makedirs(OUT,exist_ok=True)
W,H,FPS=1080,1920,60; DUR={'A':2.5,'B':2.5,'C':2.3,'D':2.6,'D2':2.8,'D3':2.15,'E':3.0,'D2MAPA':0}[VAR]; SUB=3
def C(h,a=1.0): h=h.lstrip('#'); return skia.Color(int(h[0:2],16),int(h[2:4],16),int(h[4:6],16),int(cl(a)*255))
FONDO='#0C1322'; MOR='#A855F7'; MORC='#C084FC'; MORO='#7B1FA2'; LILA='#F3E8FF'; VERDE='#10B981'
cl=lambda v,a=0,b=1:max(a,min(b,v))
def eo(t,p=3): return 1-(1-cl(t))**p
def eio(t): x=cl(t); return 4*x*x*x if x<.5 else 1-(-2*x+2)**3/2
def esin(t): return .5-.5*math.cos(math.pi*cl(t))
def eback(t,s=1.3): x=cl(t)-1; return 1+(s+1)*x**3+s*x**2
def lerp(a,b,u): return a+(b-a)*u
def svgpath(d):
    p=skia.Path(); tok=re.findall(r'[A-Za-z]|-?\d*\.?\d+',d); i=0; cmd=None; cx=cy=0; lc=None
    def n():
        nonlocal i; v=float(tok[i]); i+=1; return v
    while i<len(tok):
        if tok[i].isalpha(): cmd=tok[i]; i+=1
        if cmd=='M': cx,cy=n(),n(); p.moveTo(cx,cy); cmd='L'
        elif cmd=='L': cx,cy=n(),n(); p.lineTo(cx,cy)
        elif cmd=='H': cx=n(); p.lineTo(cx,cy)
        elif cmd=='C': a,b,c2,d2,cx,cy=[n() for _ in range(6)]; p.cubicTo(a,b,c2,d2,cx,cy); lc=(c2,d2)
        elif cmd=='S':
            rx,ry=(2*cx-lc[0],2*cy-lc[1]) if lc else (cx,cy)
            c2,d2,cx,cy=[n() for _ in range(4)]; p.cubicTo(rx,ry,c2,d2,cx,cy); lc=(c2,d2)
        elif cmd=='Z': p.close()
    return p
# Silueta real de la gacela (assets/marca/primer_plano.svg)
GAZ="M28 60 C33 52 46 49 60 50 L66 49 C69 45 72 40 75 36 L73 27 C72 23 70 20 67 18 C71 19 74 22 76 27 L78 33 C80 33 83 35 86 38 L83 41 L79 41 C77 45 74 50 72 55 C77 56 83 57 90 55 L91 58 C84 61 77 62 69 61 C64 64 58 66 52 66 C45 71 36 75 24 78 L22 75 C31 72 38 68 42 63 C37 63 32 64 28 66 L22 62 Z"
gaz=svgpath(GAZ); lineas=svgpath("M10 52 H24 M6 60 H22 M12 68 H20")
anton=skia.Typeface.MakeFromFile('/usr/share/fonts/truetype/sand-box/google/Anton/Anton-Regular.ttf')
def grad_mor(): return skia.GradientShader.MakeLinear([skia.Point(22,18),skia.Point(91,78)],[C(LILA),C(MORC),C(MOR),C(MORO)],[0,.35,.75,1])
def glow(s): return skia.ImageFilters.Blur(s,s)
def mat(cx,cy,s,rot=0):
    m=skia.Matrix(); m.setTranslate(cx,cy); m.preRotate(rot); m.preScale(s,s); m.preTranslate(-56,-48); return m
def gacela(c,m,a=1,brillo=0,sheen=None,lin=True):
    if a<=0: return
    c.save(); c.concat(m)
    if lin: c.drawPath(lineas,skia.Paint(Color=C(MORC,.7*a),Style=skia.Paint.kStroke_Style,StrokeWidth=2.4,StrokeCap=skia.Paint.kRound_Cap,AntiAlias=True))
    if brillo>0: c.drawPath(gaz,skia.Paint(Color=C(MOR,brillo*a),ImageFilter=glow(3),AntiAlias=True))
    c.drawPath(gaz,skia.Paint(Shader=grad_mor(),Alphaf=a,AntiAlias=True))
    if sheen is not None:
        c.save(); c.clipPath(gaz,doAntiAlias=True); x=-10+sheen*120
        sh=skia.GradientShader.MakeLinear([skia.Point(x-14,0),skia.Point(x+14,30)],[C('#FFFFFF',0),C('#FFFFFF',.7*a),C('#FFFFFF',0)],[0,.5,1])
        c.drawRect(skia.Rect(0,0,110,90),skia.Paint(Shader=sh)); c.restore()
    c.drawCircle(80,37,1.1,skia.Paint(Color=C(FONDO,a),AntiAlias=True)); c.restore()
def fondo(c,glow_a=1):
    c.clear(C(FONDO))
    g=skia.GradientShader.MakeRadial(skia.Point(W/2,H*.43),W*.95,[C(MORO,.30*glow_a),C(MORO,0)])
    c.drawRect(skia.Rect(0,0,W,H),skia.Paint(Shader=g))
FONT=skia.Font(anton,190); FONT.setSkewX(-.18)  # Anton cursiva
TXT="RUNCITY"; WS=[FONT.measureText(ch) for ch in TXT]; TRK=.04*190
TOT=sum(WS)+TRK*6
def xs_letras(): x0=W/2-TOT/2; return [x0+sum(WS[:i])+TRK*i for i in range(7)]
def wordmark(c,y,alphas,dys=None,glow_a=.5):
    for i,(ch,x) in enumerate(zip(TXT,xs_letras())):
        a=alphas[i]; 
        if a<=0: continue
        dy=dys[i] if dys else 0
        c.drawString(ch,x,y+dy,FONT,skia.Paint(Color=C(MOR,glow_a*a),ImageFilter=glow(16),AntiAlias=True))
        c.drawString(ch,x,y+dy,FONT,skia.Paint(Color=C('#FFFFFF',a),AntiAlias=True))
FT=skia.Font(skia.Typeface('DejaVu Sans'),40)
def tagline(c,y,a,trk=0):
    if a<=0: return
    s="LOJA · ECUADOR"; ws=[FT.measureText(ch) for ch in s]; sp=10+trk
    tot=sum(ws)+sp*(len(s)-1); x=W/2-tot/2
    for i,ch in enumerate(s):
        c.drawString(ch,x,y,FT,skia.Paint(Color=C(MORC,a*.9),AntiAlias=True)); x+=ws[i]+sp
def fade_out(c,t,ini):
    k=esin((t-ini)/(DUR-ini))
    if k>0: c.drawRect(skia.Rect(0,0,W,H),skia.Paint(Color=C(FONDO,k)))
def stroke(col,a,w,blur=0):
    p=skia.Paint(Color=C(col,a),Style=skia.Paint.kStroke_Style,StrokeWidth=w,StrokeCap=skia.Paint.kRound_Cap,StrokeJoin=skia.Paint.kRound_Join,AntiAlias=True)
    if blur: p.setImageFilter(glow(blur))
    return p
def seg(path,d0,d1):
    m=skia.PathMeasure(path,False); L=m.getLength(); out=skia.Path()
    if d1>d0: m.getSegment(L*cl(d0),L*cl(d1),out,True)
    return out
def postan(path,d):
    m=skia.PathMeasure(path,False); r=m.getPosTan(m.getLength()*cl(d)); p,t=r[0],r[1]; return (p.x(),p.y()),(t.x(),t.y())
WY=1250  # linea base del wordmark
# ---------------- A: gacela corre y su estela dibuja el wordmark ----------------
x0s=xs_letras(); XL,XR=x0s[0]-30,x0s[-1]+WS[-1]+30
RUTA_A=svgpath(f"M -40 1330 C 160 1330 {XL-40} 1300 {XL} 1290 L {XR} 1290 C {XR+120} 1290 {XR+60} 980 {W/2+40} 880")
def escena_A(c,t):
    fondo(c,eo(t/1.2))
    run=1-(1-cl(t/1.45))**2.4 if False else (lambda u: u*u*(3-2*u)*.35+ (1-(1-u)**2.2)*.65)(cl(t/1.45))  # 0..1.45 s recorre la ruta
    (px,py),(tx,ty)=postan(RUTA_A,run)
    # estela
    tail=max(0,run-.30)
    c.drawPath(seg(RUTA_A,tail*.0,run),stroke(MOR,.0,1))
    trail=seg(RUTA_A,max(0,run-.55),run)
    fadeT=1-eo((t-1.45)/.5)
    c.drawPath(trail,stroke(MOR,.55*fadeT,26,14)); c.drawPath(trail,stroke(MORC,.95*fadeT,7))
    # wordmark revelado por la estela (clip hasta la gacela)
    rev=px if run<.999 else W
    c.save(); c.clipRect(skia.Rect(0,0,min(rev,W) if t<1.5 else W,H))
    wordmark(c,WY,[1]*7,glow_a=.55); c.restore()
    # chispa en la cabeza
    if t<1.55:
        c.drawCircle(px,py,26,skia.Paint(Color=C(LILA,.6),ImageFilter=glow(18),AntiAlias=True))
    # gacela: corre con saltos (rebote seno) y al final se asienta como logo
    hop=abs(math.sin(t*math.pi*4.2))*38*(1-eo((t-1.1)/.4))
    ang=math.degrees(math.atan2(ty,tx))*.35*(1-eo((t-1.2)/.4))
    s=lerp(2.6,6.2,eio((t-1.15)/.55))
    gx=lerp(px,W/2,eio((t-1.25)/.45)); gy=lerp(py-90-hop,860,eio((t-1.25)/.45))
    sheen=cl((t-1.7)/.45) if 1.7<t<2.15 else None
    gacela(c,mat(gx,gy,s,ang),eo(t/.15),brillo=.6,sheen=sheen,lin=True)
    tagline(c,WY+110,eo((t-1.6)/.4),trk=lerp(14,0,eo((t-1.6)/.6)))
    fade_out(c,t,2.2)
# ---------------- B: mapa oscuro de Loja con zoom y ruta ----------------
rnd=random.Random(11); CALLES=[]
for i in range(-14,15):   # cuadricula del centro de Loja, levemente girada y curvada
    CALLES.append(svgpath(f"M {-900} {i*110} C -300 {i*110+rnd.uniform(-20,20)} 300 {i*110+rnd.uniform(-20,20)} 900 {i*110+rnd.uniform(-15,15)}"))
    CALLES.append(svgpath(f"M {i*95} -1400 C {i*95+rnd.uniform(-20,20)} -400 {i*95+rnd.uniform(-20,20)} 400 {i*95+rnd.uniform(-15,15)} 1400"))
RIO1=svgpath("M -120 -1400 C -60 -700 -200 -200 -150 300 S -40 900 -90 1400")   # Malacatos
RIO2=svgpath("M 260 -1400 C 300 -800 180 -300 230 200 S 360 800 300 1400")      # Zamora
AV=svgpath("M -900 620 C -300 560 300 640 900 560")
RUTA_B=svgpath("M -520 760 L -330 650 L -330 430 L -140 330 L -140 110 L 50 20 L 50 -90 L 0 -40")
def escena_B(c,t):
    c.clear(C('#070E1D'))
    z=lerp(.55,1.55,eio(t/1.7)); rot=lerp(-14,0,eio(t/1.7))
    cam=eio(t/1.7)
    c.save(); c.translate(W/2,H*.47); c.scale(z,z); c.rotate(rot); c.translate(lerp(160,0,cam),lerp(-260,40,cam))
    mapa_a=1-eo((t-1.55)/.5)*.75
    for p in CALLES: c.drawPath(p,stroke('#1E293B',mapa_a,5/z+1))
    c.drawPath(AV,stroke('#2A3550',mapa_a,12))
    for r in (RIO1,RIO2): c.drawPath(r,stroke('#12324A',mapa_a,22))
    d=eio((t-.15)/1.25); tr=seg(RUTA_B,0,d)
    ra=1-eo((t-1.5)/.45)*.85
    c.drawPath(tr,stroke(MOR,.6*ra,30,16)); c.drawPath(tr,stroke(MORC,ra,9))
    (px,py),_=postan(RUTA_B,d)
    if t<1.5: c.drawCircle(px,py,16,skia.Paint(Color=C(LILA),ImageFilter=glow(6),AntiAlias=True))
    c.drawCircle(-520,760,12,skia.Paint(Color=C(MOR),AntiAlias=True))
    c.restore()
    # vineta
    v=skia.GradientShader.MakeRadial(skia.Point(W/2,H/2),H*.62,[C(FONDO,0),C(FONDO,.95)],[.45,1])
    c.drawRect(skia.Rect(0,0,W,H),skia.Paint(Shader=v))
    # llegada: pulso verde GPS -> logo
    k=cl((t-1.35)/.5)
    if 0<k:
        cx,cy=W/2,880
        r=lerp(20,420,eo(k,2)); c.drawCircle(cx,cy,r,stroke(VERDE,.6*(1-k),6))
    gacela(c,mat(W/2,860,lerp(3.5,6.2,eback((t-1.4)/.5,1.1))),eo((t-1.4)/.3),brillo=.6,sheen=cl((t-1.75)/.4) if 1.75<t<2.15 else None)
    al=[eo((t-1.6-i*.035)/.35,4) for i in range(7)]
    wordmark(c,WY,al,dys=[(1-a)*36 for a in al])
    tagline(c,WY+110,eo((t-1.85)/.35))
    fade_out(c,t,2.22)
# ---------------- C: minimalista, trazos + pulso + letra a letra ----------------
def escena_C(c,t):
    fondo(c,.6+.4*esin((t-1.0)/.6))
    m=mat(W/2,860,6.2*(1+.035*math.sin(math.pi*cl((t-1.05)/.45))))
    d=esin(t/1.05)
    c.save(); c.concat(m)
    tr=seg(gaz,0,d)
    c.drawPath(tr,stroke(MOR,.5*(1-eo((t-1.1)/.4)),2.2,2)); c.drawPath(tr,stroke(LILA,1-eo((t-1.1)/.4),.9))
    for j,y in enumerate((52,60,68)):  # estelas trazadas
        dd=esin((t-.25-j*.08)/.5); xa,xb=[(10,24),(6,22),(12,20)][j]
        if dd>0: c.drawLine(xb,y,lerp(xb,xa,dd),y,stroke(MORC,.7,2.4))
    if d<1:
        (px,py),_=postan(gaz,d); c.drawCircle(px,py,1.6,skia.Paint(Color=C('#FFFFFF'),ImageFilter=glow(1.2),AntiAlias=True))
    c.restore()
    fa=eo((t-.85)/.35)
    gacela(c,m,fa,brillo=.6*fa,sheen=cl((t-1.1)/.5) if 1.1<t<1.6 else None,lin=False)
    # anillo de pulso
    k=cl((t-1.05)/.7)
    if 0<k<1: c.drawCircle(W/2,880,lerp(260,520,eo(k,2)),stroke(MOR,.45*(1-k),3,2))
    al=[eo((t-1.15-i*.07)/.3,3) for i in range(7)]
    wordmark(c,WY,al,dys=[(1-a)*24 for a in al],glow_a=.4)
    tagline(c,WY+110,eo((t-1.7)/.35),trk=lerp(12,0,eo((t-1.7)/.5)))
    fade_out(c,t,2.05)
# ---------------- D: mapa de Loja (B) -> ruta se vuelve contorno de gacela (C) ----------------
# camara final: mapa (mx,my) -> pantalla (540+ZF*mx, 860+ZF*my). La ruta termina en el inicio del contorno (28,60)
ZF=1.3; GX0,GY0=((28-56)*6.2)/ZF,((60-48)*6.2)/ZF
RUTA_D=svgpath(f"M -560 820 L -380 700 L -380 470 L -200 360 L -200 200 C -200 150 {GX0-40} {GY0+60} {GX0} {GY0}")
def cam_D(t):
    k=eio(cl(t/1.05))
    return lerp(.62,ZF,k),lerp(-12,0,k),lerp(170,0,k),lerp(-230,0,k)
def prog_D(t):  # punto continuo: ruta (v<L1) y luego contorno; ease-in/out solo en extremos
    u=cl((t-.1)/1.75); L1=.52; A=.18
    v=(u*u/(2*A) if u<A else (u-A/2 if u<1-A else 1-A-(1-u)**2/(2*A)+A/2))/(1-A); v=cl(v)
    return (v/L1,0) if v<L1 else (1,(v-L1)/(1-L1))
def escena_D(c,t):
    c.clear(C(FONDO))
    z,rot,ox,oy=cam_D(t); dr,dg=prog_D(t)
    dim=esin((t-.95)/.75); bl=dim*9
    lay=skia.Paint()
    if bl>.05: lay.setImageFilter(glow(bl))
    c.saveLayer(None,lay)
    c.translate(W/2,860); c.scale(z,z); c.rotate(rot); c.translate(ox,oy)
    ma=1-dim*.8
    for p in CALLES: c.drawPath(p,stroke('#1E293B',ma,5/z+1))
    c.drawPath(AV,stroke('#2A3550',ma,12))
    for r in (RIO1,RIO2): c.drawPath(r,stroke('#12324A',ma,22))
    c.drawCircle(-560,820,11,skia.Paint(Color=C(MOR,ma),AntiAlias=True))
    c.restore()
    c.save(); c.translate(W/2,860); c.scale(z,z); c.rotate(rot); c.translate(ox,oy)
    ra=1-esin((t-1.0)/.7); tr=seg(RUTA_D,0,dr)
    if ra>0: c.drawPath(tr,stroke(MOR,.55*ra,31/z,12)); c.drawPath(tr,stroke(MORC,ra,10/z))
    hx=hy=0
    if dg==0: (px,py),_=postan(RUTA_D,dr); hx,hy=c.getTotalMatrix().mapXY(px,py)
    c.restore()
    v=skia.GradientShader.MakeRadial(skia.Point(W/2,H/2),H*.62,[C(FONDO,0),C(FONDO,.95)],[.45,1])
    c.drawRect(skia.Rect(0,0,W,H),skia.Paint(Shader=v))
    c.drawRect(skia.Rect(0,0,W,H),skia.Paint(Shader=skia.GradientShader.MakeRadial(skia.Point(W/2,H*.43),W*.95,[C(MORO,.30*dim),C(MORO,0)])))
    m=mat(W/2,860,6.2*(1+.035*math.sin(math.pi*cl((t-1.85)/.45))))
    oa=1-eo((t-1.85)/.4)
    if dg>0:
        c.save(); c.concat(m); og=seg(gaz,0,dg)
        c.drawPath(og,stroke(MOR,.5*oa,2.6,2)); c.drawPath(og,stroke(LILA,oa,1.1)); c.restore()
        (gx,gy),_=postan(gaz,dg); hx,hy=m.mapXY(gx,gy)
    for j,y in enumerate((52,60,68)):
        dd=esin((t-1.55-j*.07)/.4); xa,xb=[(10,24),(6,22),(12,20)][j]
        if dd>0: c.save(); c.concat(m); c.drawLine(xb,y,lerp(xb,xa,dd),y,stroke(MORC,.7,2.4)); c.restore()
    ha=1-esin((t-1.75)/.25)
    if ha>0:
        c.drawCircle(hx,hy,30,skia.Paint(Color=C(LILA,.45*ha),ImageFilter=glow(16),AntiAlias=True))
        c.drawCircle(hx,hy,11,skia.Paint(Color=C('#FFFFFF',ha),ImageFilter=glow(4),AntiAlias=True))
    fa=esin((t-1.6)/.4)
    gacela(c,m,fa,brillo=.6*fa,sheen=cl((t-1.9)/.45) if 1.9<t<2.35 else None,lin=False)
    k=cl((t-1.85)/.7)
    if 0<k<1: c.drawCircle(W/2,880,lerp(260,520,eo(k,2)),stroke(MOR,.45*(1-k),3,2))
    al=[eo((t-1.9-i*.06)/.3,3) for i in range(7)]
    wordmark(c,WY,al,dys=[(1-a)*24 for a in al],glow_a=.4)
    tagline(c,WY+110,eo((t-2.2)/.3),trk=lerp(12,0,eo((t-2.2)/.4)))
    fi=1-esin(t/.3)
    if fi>0: c.drawRect(skia.Rect(0,0,W,H),skia.Paint(Color=C(FONDO,fi)))
    fade_out(c,t,2.42)
if VAR in('D2','D3','D2MAPA'):
    # ---------------- D2: mapa real de Loja (OpenStreetMap) + ruta organica por calles reales -> gacela ----------------
    # Datos: osm_loja/*.json (cache de Overpass API, (c) colaboradores de OpenStreetMap, ODbL). 1 unidad de mapa = 2 m.
    import json, heapq, bisect
    OSM=os.path.join(os.path.dirname(os.path.abspath(__file__)),'osm_loja')
    LAT0,LON0=-3.9931,-79.2042; KM=0.5
    def proy(lat,lon): return ((lon-LON0)*111320*math.cos(math.radians(LAT0))*KM,-(lat-LAT0)*110540*KM)
    def _ways(f): return json.load(open(os.path.join(OSM,f)))['elements']
    def _poly(g,close=False):
        p=skia.Path(); x,y=proy(g[0]['lat'],g[0]['lon']); p.moveTo(x,y)
        for q in g[1:]: p.lineTo(*proy(q['lat'],q['lon']))
        if close: p.close()
        return p
    CLASE={'trunk':'av','primary':'av','primary_link':'sec','trunk_link':'sec','secondary':'sec','secondary_link':'sec','tertiary':'sec',
           'residential':'min','unclassified':'min','living_street':'min','service':'ser','pedestrian':'ped','footway':'ped','steps':'ped','path':'ped','track':'ped'}
    ROADS={}; GRAFO={}; NODO={}
    _vistos=set()
    for e in _ways('osm_calles.json')+_ways('osm_calles_ext.json'):
        if e['id'] in _vistos or 'geometry' not in e: continue
        _vistos.add(e['id']); h=e['tags'].get('highway'); k=CLASE.get(h)
        if not k: continue
        ROADS.setdefault(k,skia.Path()).addPath(_poly(e['geometry']))
        if k in('av','sec','min'):
            wgt={'av':1,'sec':1.05,'min':3}[k]
            for a,b,ga,gb in zip(e['nodes'],e['nodes'][1:],e['geometry'],e['geometry'][1:]):
                NODO[a]=proy(ga['lat'],ga['lon']); NODO[b]=proy(gb['lat'],gb['lon']); w=math.dist(NODO[a],NODO[b])*wgt
                GRAFO.setdefault(a,[]).append((b,w)); GRAFO.setdefault(b,[]).append((a,w))
    EDIF=skia.Path()
    for e in _ways('osm_edif.json'):
        if 'geometry' in e: EDIF.addPath(_poly(e['geometry'],True))
    VERDE_P=skia.Path(); RIOS=skia.Path(); QUEB=skia.Path(); LBL_RIO=[]
    for e in _ways('osm_agua.json'):
        if 'geometry' not in e: continue
        t=e['tags']
        if t.get('waterway')=='river': RIOS.addPath(_poly(e['geometry']))
        elif t.get('waterway'): QUEB.addPath(_poly(e['geometry']))
        else: VERDE_P.addPath(_poly(e['geometry'],True))
    def _dijkstra(s,t):
        D={s:0}; prev={}; q=[(0,s)]
        while q:
            dd,u=heapq.heappop(q)
            if u==t: break
            if dd>D[u]: continue
            for v,w in GRAFO.get(u,[]):
                if dd+w<D.get(v,1e18): D[v]=dd+w; prev[v]=u; heapq.heappush(q,(dd+w,v))
        out=[t]
        while out[-1]!=s: out.append(prev[out[-1]])
        return out[::-1]
    def _cerca(x,y): return min(NODO,key=lambda n:(NODO[n][0]-x)**2+(NODO[n][1]-y)**2)
    # Ruta real: sube por la Av. Universitaria (sur->norte), curva en S del redondel y vuelta amplia hacia el norte
    WP2=[(-82,719),(-199,445),(-223,172),(-326,152),(-336,94),(-322,-140)]
    _ns=[_cerca(*w) for w in WP2]; _pts=[]
    for a,b in zip(_ns,_ns[1:]):
        sg=_dijkstra(a,b); _pts+=[NODO[n] for n in (sg if not _pts else sg[1:])]
    def _remuestreo(pts,paso):
        P=np.array(pts,float); dl=np.r_[0,np.cumsum(np.hypot(*np.diff(P,axis=0).T))]
        s=np.arange(0,dl[-1],paso); return np.c_[np.interp(s,dl,P[:,0]),np.interp(s,dl,P[:,1])]
    _R=_remuestreo(_pts,2.0)
    _sig=14; _k=np.exp(-.5*(np.arange(-3*_sig,3*_sig+1)/_sig)**2); _k/=_k.sum()  # suavizado tipo spline (radio ~30 m)
    _pad=np.r_[np.repeat(_R[:1],3*_sig,0),_R,np.repeat(_R[-1:],3*_sig,0)]
    _R=np.c_[np.convolve(_pad[:,0],_k,'valid'),np.convolve(_pad[:,1],_k,'valid')]
    _R=_remuestreo(_R,2.0)
    RUTA_P=skia.Path(); RUTA_P.moveTo(*_R[0])
    for q in _R[1:]: RUTA_P.lineTo(*q)
    RL=float(np.sum(np.hypot(*np.diff(_R,axis=0).T)))
    _S=np.r_[0,np.cumsum(np.hypot(*np.diff(_R,axis=0).T))]
    _ang=np.unwrap(np.arctan2(*np.gradient(_R,axis=0)[:,::-1].T))  # rumbo (rad)
    _cur=np.abs(np.gradient(_ang)/2.0)
    _cur=np.convolve(np.r_[np.repeat(_cur[:1],40),_cur,np.repeat(_cur[-1:],40)],np.ones(81)/81,'valid')
    # perfil de velocidad: arranca, acelera en rectas, frena en curvas, frena al final
    _v=1/(1+55*_cur); _v*=np.clip(np.sqrt((_S+8)/140),0,1)*np.clip(((RL-_S)+60)/260,.45,1)
    _T=np.r_[0,np.cumsum(np.diff(_S)/_v[1:])]; _T/=_T[-1]
    _hs=np.convolve(np.r_[np.repeat(_ang[:1],150),_ang,np.repeat(_ang[-1:],150)],np.ones(301)/301,'valid')  # rumbo suavizado para la camara
    T_R0,T_R1,T_G1=.12,1.52,2.06
    GOAL=math.radians(math.degrees(math.atan2(-8,5)))   # direccion inicial del contorno de la gacela en pantalla
    ZF2=1.25; GXs,GYs=540+(28-56)*6.2,860+(60-48)*6.2
    def ruta_s(t):  # distancia recorrida (unidades de mapa)
        u=cl((t-T_R0)/(T_R1-T_R0)); return float(np.interp(u,_T,_S))
    def pos_s(s):
        s=cl(s,0,RL); return float(np.interp(s,_S,_R[:,0])),float(np.interp(s,_S,_R[:,1]))
    def cam_D2(t):
        s=ruta_s(t); u=cl((t-T_R0)/(T_R1-T_R0))
        lead=lerp(90,0,esin((t-1.1)/.42))
        hx,hy=pos_s(s+lead)
        hd=float(np.interp(min(s+lead*.5,RL),_S,_hs))
        rot_f=GOAL-hd                                        # rumbo arriba-derecha (heading-up parcial)
        rot_end=GOAL-float(_ang[-1])
        w=lerp(.4,.8,esin((t-.3)/1.0))
        rot=lerp(rot_f*w,rot_end,esin((t-1.15)/.37))
        z=lerp(.62,ZF2,eio(cl(t/1.35)))*(1+.07*math.sin(math.pi*cl((t-.25)/.9)))
        # pose final: el fin de la ruta cae exactamente donde empieza el contorno de la gacela
        ex,ey=float(_R[-1,0]),float(_R[-1,1]); dx,dy=(GXs-540)/ZF2,(GYs-860)/ZF2
        cr,sr=math.cos(-rot_end),math.sin(-rot_end); fx,fy=ex-(cr*dx-sr*dy),ey-(sr*dx+cr*dy)
        b=esin((t-1.0)/.52)
        return z,math.degrees(rot),lerp(hx,fx,b),lerp(hy,fy,b),s
    _PIC=None
    def mapa_pic():
        global _PIC
        if _PIC: return _PIC
        rec=skia.PictureRecorder(); c=rec.beginRecording(skia.Rect(-4000,-4000,4000,4000))
        def fill(col,a): return skia.Paint(Color=C(col,a),AntiAlias=True)
        c.drawPath(VERDE_P,fill('#0E2A27',.85)); c.drawPath(VERDE_P,stroke('#14423A',.5,1.2))
        c.drawPath(EDIF,fill('#141E33',.9)); c.drawPath(EDIF,stroke('#1B2742',.6,.6))
        c.drawPath(QUEB,stroke('#123A55',.7,2.4))
        p=stroke('#1D6A8F',.35,26); p.setMaskFilter(skia.MaskFilter.MakeBlur(skia.kNormal_BlurStyle,8)); c.drawPath(RIOS,p)
        c.drawPath(ROADS['ped'],stroke('#1A2337',.8,1)); c.drawPath(ROADS['ser'],stroke('#1B2539',.9,1.6))
        c.drawPath(ROADS['min'],stroke('#0A101C',1,4.2)); c.drawPath(ROADS['min'],stroke('#222D45',1,2.6))
        c.drawPath(ROADS['sec'],stroke('#0A101C',1,6.5)); c.drawPath(ROADS['sec'],stroke('#2C3A5A',1,4.4))
        g=stroke('#5B5FC7',.22,22); g.setMaskFilter(skia.MaskFilter.MakeBlur(skia.kNormal_BlurStyle,9)); c.drawPath(ROADS['av'],g)
        c.drawPath(ROADS['av'],stroke('#0A101C',1,9.5)); c.drawPath(ROADS['av'],stroke('#3A4874',1,7)); c.drawPath(ROADS['av'],stroke('#56659A',.55,2.2))
        c.drawPath(RIOS,stroke('#0B2236',1,9)); c.drawPath(RIOS,stroke('#145073',.95,6)); c.drawPath(RIOS,stroke('#2A86B0',.45,1.6))
        f=skia.Font(skia.Typeface('DejaVu Sans'),15)
        for txt,x,y,a in (('Río Malacatos',-262,-20,-84),('Río Zamora',300,-330,-62),('Av. Universitaria',-366,-40,-87),('Centro',-60,140,0),('Parque Jipiro',140,-860,0)):
            c.save(); c.translate(x,y); c.rotate(a); c.drawString(txt.upper(),0,0,f,skia.Paint(Color=C('#7C8DB5',.33),AntiAlias=True)); c.restore()
        _PIC=rec.finishRecordingAsPicture(); return _PIC
    def rastro(c,s,a,z):
        if s<=0 or a<=0: return
        full=seg(RUTA_P,0,s/RL)
        c.drawPath(full,stroke(MORO,.55*a,11/z+3))                     # recorrido ya hecho (tipo Strava)
        c.drawPath(full,stroke(MOR,.55*a,26/z+4,10))
        n=26; L=min(s,380)                                             # cola luminosa con degradado
        for i in range(n):
            s0=s-L*(1-i/n); s1=s-L*(1-(i+1)/n)+1.5
            if s1<=0: continue
            k=(i+1)/n; pp=seg(RUTA_P,max(0,s0)/RL,s1/RL)
            c.drawPath(pp,stroke(lerp_col(k),a*(.35+.65*k),(5+6*k)/z+2))
        pt=seg(RUTA_P,max(0,s-120)/RL,s/RL); c.drawPath(pt,stroke(LILA,.8*a,22/z+3,9))
    def lerp_col(k): return MORC if k<.6 else ('#E9D5FF' if k<.9 else LILA)
    FKM=skia.Font(skia.Typeface('DejaVu Sans'),13)
    def km_marcas(c,s,a,z,mat_):
        for j,sk in enumerate(np.arange(250,RL-60,250)):
            k=cl((s-sk)/40)
            if k<=0: continue
            x,y=pos_s(sk); X,Y=mat_.mapXY(x,y); r=lerp(2,6,eback(k,2))
            c.drawCircle(X,Y,r+5,stroke(LILA,.35*a,1.6)); c.drawCircle(X,Y,r*.55,skia.Paint(Color=C(LILA,.85*a),AntiAlias=True))
            lab=f"{(j+1)*0.5:.1f} km".replace('.',',')
            c.drawString(lab,X+14,Y+5,FKM,skia.Paint(Color=C('#D8C7F5',.55*a*k),AntiAlias=True))
    def escena_D2(c,t):
        c.clear(C(FONDO))
        z,rot,cx,cy,s=cam_D2(t)
        u_g=cl((t-T_R1)/(T_G1-T_R1)); dg=1-(1-u_g)**1.7 if t>T_R1 else 0
        dim=esin((t-1.18)/.75); bl=dim*9
        M=skia.Matrix(); M.setTranslate(W/2,860); M.preScale(z,z); M.preRotate(rot); M.preTranslate(-cx,-cy)
        lay=skia.Paint()
        if bl>.05: lay.setImageFilter(glow(bl))
        c.saveLayer(None,lay); c.concat(M)
        ma=1-dim*.82
        c.drawPicture(mapa_pic(),None,skia.Paint(Alphaf=ma))
        sx,sy=pos_s(0); c.drawCircle(sx,sy,9,skia.Paint(Color=C(VERDE,ma),AntiAlias=True)); c.drawCircle(sx,sy,15,stroke(VERDE,.5*ma,2))
        c.restore()
        ra=1-esin((t-1.3)/.6)
        c.save(); c.concat(M); rastro(c,s,ra,z); c.restore()
        km_marcas(c,s,ra*(1-dim),z,M)
        hx,hy=M.mapXY(*pos_s(s))
        # vineta + brillo morado final
        v=skia.GradientShader.MakeRadial(skia.Point(W/2,H*.47),H*.62,[C(FONDO,0),C(FONDO,.97)],[.42,1])
        c.drawRect(skia.Rect(0,0,W,H),skia.Paint(Shader=v))
        c.drawRect(skia.Rect(0,0,W,H),skia.Paint(Shader=skia.GradientShader.MakeRadial(skia.Point(W/2,H*.43),W*.95,[C(MORO,.30*dim),C(MORO,0)])))
        m=mat(W/2,860,6.2*(1+.035*math.sin(math.pi*cl((t-2.06)/.45))))
        oa=1-eo((t-2.06)/.4)
        if dg>0:
            c.save(); c.concat(m); og=seg(gaz,0,dg)
            c.drawPath(og,stroke(MOR,.5*oa,2.6,2)); c.drawPath(og,stroke(LILA,oa,1.1)); c.restore()
            (gx,gy),_=postan(gaz,dg); hx,hy=m.mapXY(gx,gy)
        for j,y in enumerate((52,60,68)):
            dd=esin((t-1.78-j*.07)/.4); xa,xb=[(10,24),(6,22),(12,20)][j]
            if dd>0: c.save(); c.concat(m); c.drawLine(xb,y,lerp(xb,xa,dd),y,stroke(MORC,.7,2.4)); c.restore()
        ha=1-esin((t-1.98)/.25)
        if ha>0:
            c.drawCircle(hx,hy,34,skia.Paint(Color=C(LILA,.45*ha),ImageFilter=glow(18),AntiAlias=True))
            c.drawCircle(hx,hy,11,skia.Paint(Color=C('#FFFFFF',ha),ImageFilter=glow(4),AntiAlias=True))
        fa=esin((t-1.82)/.4)
        gacela(c,m,fa,brillo=.6*fa,sheen=cl((t-2.1)/.45) if 2.1<t<2.55 else None,lin=False)
        k=cl((t-2.06)/.7)
        if 0<k<1: c.drawCircle(W/2,880,lerp(260,520,eo(k,2)),stroke(MOR,.45*(1-k),3,2))
        al=[eo((t-2.1-i*.06)/.3,3) for i in range(7)]
        wordmark(c,WY,al,dys=[(1-a)*24 for a in al],glow_a=.4)
        tagline(c,WY+110,eo((t-2.4)/.3),trk=lerp(12,0,eo((t-2.4)/.4)))
        fi=1-esin(t/.3)
        if fi>0: c.drawRect(skia.Rect(0,0,W,H),skia.Paint(Color=C(FONDO,fi)))
        k=esin((t-2.6)/.17)
        if k>0: c.drawRect(skia.Rect(0,0,W,H),skia.Paint(Color=C(FONDO,k)))
    # ---------------- D3: D2 comprimido (mapa ~0,45 s) y logo protagonista (~1,7 s) ----------------
    T3M=.45   # fin del mapa / inicio del contorno (sin pausa)
    def warp3(t):  # tiempo D3 -> tiempo D2 de la ruta: arranque rapido, sin frenar al entrar al contorno
        u=cl(t/T3M); return T_R1*(.55*u+.45*(1-(1-u)**2))
    def escena_D3(c,t):
        c.clear(C(FONDO))
        z,rot,cx,cy,s=cam_D2(warp3(t))
        u_g=cl((t-T3M)/.55); dg=1-(1-u_g)**1.6 if t>T3M else 0
        dim=esin((t-.28)/.3); bl=dim*10
        M=skia.Matrix(); M.setTranslate(W/2,860); M.preScale(z,z); M.preRotate(rot); M.preTranslate(-cx,-cy)
        lay=skia.Paint()
        if bl>.05: lay.setImageFilter(glow(bl))
        c.saveLayer(None,lay); c.concat(M)
        ma=1-dim*.85
        c.drawPicture(mapa_pic(),None,skia.Paint(Alphaf=ma))
        c.restore()
        ra=1-esin((t-.45)/.3)
        if ra>0: c.save(); c.concat(M); rastro(c,s,ra,z); c.restore()
        hx,hy=M.mapXY(*pos_s(s))
        v=skia.GradientShader.MakeRadial(skia.Point(W/2,H*.47),H*.62,[C(FONDO,0),C(FONDO,.97)],[.42,1])
        c.drawRect(skia.Rect(0,0,W,H),skia.Paint(Shader=v))
        c.drawRect(skia.Rect(0,0,W,H),skia.Paint(Shader=skia.GradientShader.MakeRadial(skia.Point(W/2,H*.43),W*.95,[C(MORO,.32*dim),C(MORO,0)])))
        m=mat(W/2,860,6.2*(1+.045*math.sin(math.pi*cl((t-1.0)/.4))))
        oa=1-eo((t-1.0)/.35)
        if dg>0:
            c.save(); c.concat(m); og=seg(gaz,0,dg)
            c.drawPath(og,stroke(MOR,.55*oa,3,3)); c.drawPath(og,stroke(LILA,oa,1.2))
            tl=seg(gaz,max(0,dg-.12),dg); c.drawPath(tl,stroke('#E9D5FF',.8*oa*(1-u_g*.5),2.2,1.5))  # estela
            c.restore()
            (gx,gy),_=postan(gaz,dg); hx,hy=m.mapXY(gx,gy)
        for j,y in enumerate((52,60,68)):
            dd=esin((t-.82-j*.05)/.25); xa,xb=[(10,24),(6,22),(12,20)][j]
            if dd>0: c.save(); c.concat(m); c.drawLine(xb,y,lerp(xb,xa,dd),y,stroke(MORC,.7,2.4)); c.restore()
        ha=1-esin((t-.98)/.18)
        if ha>0:
            c.drawCircle(hx,hy,36,skia.Paint(Color=C(LILA,.5*ha),ImageFilter=glow(18),AntiAlias=True))
            c.drawCircle(hx,hy,11,skia.Paint(Color=C('#FFFFFF',ha),ImageFilter=glow(4),AntiAlias=True))
        fa=esin((t-.85)/.3)
        gacela(c,m,fa,brillo=.7*fa,sheen=cl((t-1.12)/.4) if 1.12<t<1.52 else None,lin=False)
        k=cl((t-1.0)/.6)
        if 0<k<1: c.drawCircle(W/2,880,lerp(260,540,eo(k,2)),stroke(MOR,.5*(1-k),3.5,2))
        ks=[cl((t-1.12-i*.05)/.34) for i in range(7)]
        wordmark(c,WY,[eo(x/.6,2) for x in ks],dys=[(1-eback(x,1.6))*34 for x in ks],glow_a=.45)
        tagline(c,WY+110,eo((t-1.45)/.28),trk=lerp(12,0,eo((t-1.45)/.35)))
        fi=1-esin(t/.1)
        if fi>0: c.drawRect(skia.Rect(0,0,W,H),skia.Paint(Color=C(FONDO,fi)))
        k=esin((t-1.98)/.15)
        if k>0: c.drawRect(skia.Rect(0,0,W,H),skia.Paint(Color=C(FONDO,k)))
    def mapa_solo(path,S=2600,z=1.35):
        surf=skia.Surface(S,S); c=surf.getCanvas(); c.clear(C(FONDO))
        c.save(); c.translate(S/2,S/2); c.scale(z,z); c.translate(-(-200),-(300)); c.drawPicture(mapa_pic())
        rastro(c,RL,1,z); sx,sy=pos_s(0); c.drawCircle(sx,sy,9,skia.Paint(Color=C(VERDE),AntiAlias=True)); c.restore()
        v=skia.GradientShader.MakeRadial(skia.Point(S/2,S/2),S*.72,[C(FONDO,0),C(FONDO,.9)],[.55,1]); c.drawRect(skia.Rect(0,0,S,S),skia.Paint(Shader=v))
        a=surf.makeImageSnapshot().toarray().astype(np.float32); a+=np.random.default_rng(0).normal(0,1.6,a.shape).astype(np.float32)
        Image.fromarray(np.clip(a,0,255).astype(np.uint8)[:,:,[2,1,0]]).save(path)

# ---------------- E: sin mapa, 3,0 s. Punto de luz curvo -> contorno -> relleno -> pulso -> texto ----------------
GX,GY=mat(W/2,860,6.2).mapXY(28,60)
APR_E=svgpath(f"M -80 1500 C 260 1700 820 1420 700 1120 S {GX-260} {GY+40} {GX} {GY}")
T_A,T_D=.32,1.1   # fin aproximacion / fin contorno
def escena_E(c,t):
    fondo(c,.55+.45*esin((t-.9)/.7))
    pa=1+.05*math.sin(math.pi*cl((t-1.22)/.42))
    m=mat(W/2,860,6.2*pa)
    oa=1-eo((t-1.12)/.3)
    if t<T_A:  # entrada curva, acelera suave
        u=cl(t/T_A); d=.15*u+.85*esin(u)*u**.3; d=cl(d)
        c.drawPath(seg(APR_E,max(0,d-.35),d),stroke(MOR,.45,9,6)); c.drawPath(seg(APR_E,max(0,d-.22),d),stroke('#E9D5FF',.85,3.2,1.5))
        (hx,hy),_=postan(APR_E,d); dg=0
    else:
        ta=1-esin((t-T_A)/.25)
        if ta>0:
            c.drawPath(seg(APR_E,.65+.35*(1-ta),1),stroke(MOR,.45*ta,9,6)); c.drawPath(seg(APR_E,.78+.22*(1-ta),1),stroke('#E9D5FF',.85*ta,3.2,1.5))
        u_g=cl((t-T_A)/(T_D-T_A)); dg=.6*u_g+.4*esin(u_g) if u_g<1 else 1
        c.save(); c.concat(m); og=seg(gaz,0,dg)
        c.drawPath(og,stroke(MOR,.55*oa,3,3)); c.drawPath(og,stroke(LILA,oa,1.2))
        c.drawPath(seg(gaz,max(0,dg-.14),dg),stroke('#E9D5FF',.85*oa*(1-u_g*.4),2.4,1.5))
        c.restore(); (gx,gy),_=postan(gaz,dg); hx,hy=m.mapXY(gx,gy)
    ha=(esin(t/.06))*(1-esin((t-1.06)/.16))
    if ha>0:
        c.drawCircle(hx,hy,38,skia.Paint(Color=C(LILA,.5*ha),ImageFilter=glow(18),AntiAlias=True))
        c.drawCircle(hx,hy,11,skia.Paint(Color=C('#FFFFFF',ha),ImageFilter=glow(4),AntiAlias=True))
    for j,y in enumerate((52,60,68)):
        dd=esin((t-.92-j*.06)/.28); xa,xb=[(10,24),(6,22),(12,20)][j]
        if dd>0: c.save(); c.concat(m); c.drawLine(xb,y,lerp(xb,xa,dd),y,stroke(MORC,.7,2.4)); c.restore()
    fa=esin((t-1.0)/.32)
    gacela(c,m,fa,brillo=.7*fa+.4*math.sin(math.pi*cl((t-1.22)/.42)),sheen=cl((t-1.25)/.45) if 1.25<t<1.7 else None,lin=False)
    k=cl((t-1.24)/.6)
    if 0<k<1: c.drawCircle(W/2,880,lerp(260,560,eo(k,2)),stroke(MOR,.5*(1-k),3.5,2))
    ks=[cl((t-1.5-i*.06)/.42) for i in range(7)]
    wordmark(c,WY,[eo(x/.5,2) for x in ks],dys=[(1-eback(x,1.4))*36 for x in ks],glow_a=.45)
    tagline(c,WY+110,eo((t-1.95)/.3),trk=lerp(16,0,eo((t-1.95)/.45)))
    fi=1-esin(t/.08)
    if fi>0: c.drawRect(skia.Rect(0,0,W,H),skia.Paint(Color=C(FONDO,fi)))
    k=esin((t-2.5)/.45)
    if k>0: c.drawRect(skia.Rect(0,0,W,H),skia.Paint(Color=C(FONDO,k)))
ESC={'A':escena_A,'B':escena_B,'C':escena_C,'D':escena_D,'D2':globals().get('escena_D2'),'D3':globals().get('escena_D3'),'E':escena_E,'D2MAPA':None}[VAR]
N=int(DUR*FPS)
def frame(i):
    acc=np.zeros((H,W,4),np.float32)
    for s in range(SUB):
        t=(i+s/SUB)/FPS
        surf=skia.Surface(W,H); c=surf.getCanvas(); ESC(c,t)
        acc+=surf.makeImageSnapshot().toarray()
    a=(acc/SUB); a+=np.random.default_rng(i).normal(0,1.6 if VAR in('D2','D3','E') else .8,a.shape).astype(np.float32)  # grano anti-banding
    img=np.clip(a,0,255).astype(np.uint8)[:,:,[2,1,0]]
    Image.fromarray(img).save(f"{OUT}/f{i:04d}.png")
if __name__=='__main__' and VAR=='D2MAPA': mapa_solo(OUT); sys.exit()
if __name__=='__main__':
    with Pool(8) as p: p.map(frame,range(N))
    print(N)
