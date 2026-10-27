# Intro v2 de Gacelas, render cuadro por cuadro (skia). Uso: render_v2.py [a|b] salida_dir
from PIL import Image
import sys, math, random, os, numpy as np, skia
VAR = sys.argv[1]; OUT = sys.argv[2]; os.makedirs(OUT, exist_ok=True)
W, H, FPS = 1080, 1920, 60
DUR = {'a':2.6,'b':2.2,'c':2.0}[VAR]
SUB = 4  # submuestras por cuadro (motion blur)
def C(h, a=1.0): h=h.lstrip('#'); return skia.Color(int(h[0:2],16),int(h[2:4],16),int(h[4:6],16),int(a*255))
FONDO='#0C1322'; MOR='#A855F7'; MORC='#C084FC'; MORO='#7B1FA2'; LILA='#F3E8FF'; VERDE='#10B981'
cl=lambda v,a=0,b=1:max(a,min(b,v))
def eo(t,p=3): return 1-(1-cl(t))**p
def eio(t): x=cl(t); return 4*x*x*x if x<.5 else 1-(-2*x+2)**3/2
def eexpo(t): x=cl(t); return 1 if x>=1 else 1-2**(-10*x)
def eback(t,s=1.4): x=cl(t)-1; return 1+(s+1)*x**3+s*x**2
import re
def svgpath(d):
    p=skia.Path(); tok=re.findall(r'[A-Za-z]|-?\d*\.?\d+',d); i=0; cmd=None; cx=cy=0; lc=None
    def n():
        nonlocal i; v=float(tok[i]); i+=1; return v
    while i<len(tok):
        if tok[i].isalpha(): cmd=tok[i]; i+=1
        if cmd=='M': cx,cy=n(),n(); p.moveTo(cx,cy); cmd='L'
        elif cmd=='L': cx,cy=n(),n(); p.lineTo(cx,cy)
        elif cmd=='H': cx=n(); p.lineTo(cx,cy)
        elif cmd=='C':
            a,b,c2,d2,cx,cy=[n() for _ in range(6)]; p.cubicTo(a,b,c2,d2,cx,cy); lc=(c2,d2)
        elif cmd=='S':
            rx,ry=(2*cx-lc[0],2*cy-lc[1]) if lc else (cx,cy)
            c2,d2,cx,cy=[n() for _ in range(4)]; p.cubicTo(rx,ry,c2,d2,cx,cy); lc=(c2,d2)
        elif cmd=='Z': p.close()
    return p
GAZ="M28 60 C33 52 46 49 60 50 L66 49 C69 45 72 40 75 36 L73 27 C72 23 70 20 67 18 C71 19 74 22 76 27 L78 33 C80 33 83 35 86 38 L83 41 L79 41 C77 45 74 50 72 55 C77 56 83 57 90 55 L91 58 C84 61 77 62 69 61 C64 64 58 66 52 66 C45 71 36 75 24 78 L22 75 C31 72 38 68 42 63 C37 63 32 64 28 66 L22 62 Z"
gaz=svgpath(GAZ)
lineas=svgpath("M10 52 H24 M6 60 H22 M12 68 H20")
ojo=(80,37)
anton=skia.Typeface.MakeFromFile('/usr/share/fonts/truetype/sand-box/google/Anton/Anton-Regular.ttf')
# Ruta: entra por abajo-izquierda, serpentea y termina en el centro del logo.
ruta=svgpath("M -80 1560 C 180 1500 260 1300 470 1330 S 760 1240 820 1080 S 700 860 540 900")
pm=skia.ContourMeasure if False else None
meas=skia.PathMeasure(ruta,False); L=meas.getLength()
def punto(d):
    r=meas.getPosTan(cl(d)*L); p,tan=r[0],r[1]
    return (p.x(),p.y()) if hasattr(p,"x") else tuple(p), (tan.x(),tan.y()) if hasattr(tan,"x") else tuple(tan)
CX,CY=540,880  # centro del logo
# Escala de la gacela en el splash de Android (288dp contenedor, 108 unidades, densidad ~2.625)
SPLASH_U=7.0
def mat_splash():
    m=skia.Matrix(); m.setTranslate(W/2,H/2); m.preScale(SPLASH_U,SPLASH_U); m.preTranslate(-54,-54)
    m.preTranslate(8.08,14.64); m.preScale(.82,.82); return m
def grad_mor(m):
    return skia.GradientShader.MakeLinear([skia.Point(22,18),skia.Point(91,78)],[C(LILA),C(MORC),C(MOR),C(MORO)],[0,.35,.75,1])
rnd=random.Random(7)
PART=[(rnd.random(),rnd.uniform(-1,1),rnd.uniform(.4,1.2),rnd.uniform(2,6)) for _ in range(70)]
def glow(sig): return skia.ImageFilters.Blur(sig,sig)
def dib_gacela(c,m,alpha,brillo=0,sheen=None,lin=True):
    c.save(); c.concat(m)
    if lin:
        c.drawPath(lineas,skia.Paint(Color=C(MORC,.7*alpha),Style=skia.Paint.kStroke_Style,StrokeWidth=2.4,StrokeCap=skia.Paint.kRound_Cap,AntiAlias=True))
    if brillo>0:
        c.drawPath(gaz,skia.Paint(Color=C(MOR,brillo*alpha),ImageFilter=glow(3.0),AntiAlias=True))
    c.drawPath(gaz,skia.Paint(Shader=grad_mor(m),Alphaf=alpha,AntiAlias=True))
    if sheen is not None:  # barrido especular
        c.save(); c.clipPath(gaz,doAntiAlias=True)
        x=-10+sheen*120
        sh=skia.GradientShader.MakeLinear([skia.Point(x-14,0),skia.Point(x+14,30)],[C('#FFFFFF',0),C('#FFFFFF',.75*alpha),C('#FFFFFF',0)],[0,.5,1])
        c.drawRect(skia.Rect(0,0,110,90),skia.Paint(Shader=sh)); c.restore()
    c.drawCircle(ojo[0],ojo[1],1.1,skia.Paint(Color=C(FONDO,alpha),AntiAlias=True))
    c.restore()
def lerp(a,b,u): return a+(b-a)*u
def mat_logo(s,cx=CX,cy=CY):
    m=skia.Matrix(); m.setTranslate(cx,cy); m.preScale(s,s); m.preTranslate(-56,-48); return m
def interp_mat(m1,m2,u):
    a=[m1.get(i) for i in range(9)]; b=[m2.get(i) for i in range(9)]
    return skia.Matrix.MakeAll(*[lerp(a[i],b[i],u) for i in range(9)])
def palabra(c,t0,t,alpha,track_ini=.45,track_fin=.16,size=168,y=1300):
    f=skia.Font(anton,size); txt="RUNCITY"
    ws=[f.measureText(ch) for ch in txt]
    for i,ch in enumerate(txt):
        u=cl((t-t0-i*.045)/.42); k=eo(u,4)
        tr=lerp(track_ini,track_fin,eo(cl((t-t0)/.7),3))*size
        tot=sum(ws)+tr*(len(txt)-1); x=W/2-tot/2+sum(ws[:i])+tr*i
        a=alpha*k
        if a<=0: continue
        dy=(1-k)*40
        c.drawString(ch,x,y+dy,f,skia.Paint(Color=C(MOR,.55*a),ImageFilter=glow(14),AntiAlias=True))
        c.drawString(ch,x,y+dy,f,skia.Paint(Color=C('#FFFFFF',a),AntiAlias=True))
def escena(c,t):
    c.clear(C(FONDO))
    A = VAR=='a'
    o = 0.0 if A else -0.35  # la b arranca directo en la ruta
    # Viñeta / niebla violeta que aparece despacio (empieza en el mismo color plano del splash)
    v=eo((t-.15-o)/.9)*(1-eio((t-(DUR-.35))/.35))
    if v>0:
        sh=skia.GradientShader.MakeRadial(skia.Point(CX,CY),1150,[C('#1A1030',v),C('#1A1030',0)],[0,1])
        c.drawPaint(skia.Paint(Shader=sh))
    # 1) Splash: gacela fija igual al splash, se carga de luz y colapsa a un punto
    if A and t<.62:
        u=eio((t-.18)/.42)
        m0=mat_splash()
        if u<=0:
            dib_gacela(c,m0,1)
        else:
            p0,_=punto(0.0)
            # colapsa hacia el punto de salida de la ruta mientras se aclara
            mt=skia.Matrix(); mt.setTranslate(lerp(W/2,p0[0]+120,u),lerp(H/2,p0[1]-60,u)); s=SPLASH_U*.82*(1-u*.97)
            mt.preScale(s,s); mt.preTranslate(-56,-48)
            dib_gacela(c,mt,1-u*.6,brillo=.9*u,lin=u<.3)
            c.drawCircle(mt.mapXY(56,48).x(),mt.mapXY(56,48).y(),30*u,skia.Paint(Color=C(LILA,u),ImageFilter=glow(18*u+1),AntiAlias=True))
    # 2) Cometa por la ruta
    r0,r1=(.5+o if A else .0),(1.32+o if A else .95)
    d=eio((t-r0)/(r1-r0))*.85+eo((t-r0)/(r1-r0),2)*.15
    fade=1-cl((t-(r1+.25))/.35)
    if t>r0 and fade>0:
        seg=skia.Path(); meas.getSegment(0,d*L,seg,True)
        for w,a,b in [(60,.10,26),(30,.22,10),(9,1,0)]:
            p=skia.Paint(Color=C(MOR,a*fade),Style=skia.Paint.kStroke_Style,StrokeWidth=w,StrokeCap=skia.Paint.kRound_Cap,AntiAlias=True)
            if b: p.setImageFilter(glow(b))
            c.drawPath(seg,p)
        # nucleo brillante cerca de la cabeza
        seg2=skia.Path(); meas.getSegment(max(0,d*L-260),d*L,seg2,True)
        c.drawPath(seg2,skia.Paint(Shader=None,Color=C(LILA,.9*fade),Style=skia.Paint.kStroke_Style,StrokeWidth=5,StrokeCap=skia.Paint.kRound_Cap,AntiAlias=True))
        if d<.999:
            (hx,hy),_=punto(d)
            c.drawCircle(hx,hy,70,skia.Paint(Color=C(MOR,.35),ImageFilter=glow(30),AntiAlias=True))
            c.drawCircle(hx,hy,24,skia.Paint(Color=C(VERDE,.9),ImageFilter=glow(8),AntiAlias=True))
            c.drawCircle(hx,hy,11,skia.Paint(Color=C('#FFFFFF'),AntiAlias=True))
            # chispas que se desprenden
            for (pd,off,sp,rad) in PART:
                age=d-pd*d
                if pd>d or age>.18: continue
                (px,py),(tx,ty)=punto(pd*d); k=age/.18
                px+= -ty*off*90*k; py+= tx*off*90*k+ 60*k*sp
                c.drawCircle(px,py,rad*(1-k),skia.Paint(Color=C(MORC,(1-k)*.9),AntiAlias=True))
    # 3) Destello anamorfico al llegar
    tf=r1+.02
    f=cl(1-abs(t-tf)/.22)**1.6
    if f>0:
        c.drawCircle(CX,CY,700*f+1,skia.Paint(Shader=skia.GradientShader.MakeRadial(skia.Point(CX,CY),700*f+1,[C(LILA,f),C(MOR,.45*f),C(MOR,0)],[0,.35,1])))
        c.save(); c.translate(CX,CY); c.scale(1,.035)
        c.drawCircle(0,0,1100*f,skia.Paint(Shader=skia.GradientShader.MakeRadial(skia.Point(0,0),1100*f+1,[C('#FFFFFF',f),C(MORC,.5*f),C(MOR,0)],[0,.3,1])))
        c.restore()
    # 4) Logo + palabra
    tl=r1-.04
    if t>tl:
        u=cl((t-tl)/.55)
        s=lerp(5.2,8.2,1)*(1+.10*(1-eback(u,1.2)))
        s=8.2*lerp(.82,1,eback(u,1.3))
        al=eo(u*1.6)
        zo=0
        tz=DUR-.42
        if t>tz:
            z=eio((t-tz)/.42); s*=1+z*.35; al*=1-z; zo=z
        sheen=cl((t-(tl+.45))/.5) if t<tl+1.0 else None
        if al>0:
            dib_gacela(c,mat_logo(s),al,brillo=.5,sheen=sheen,lin=True)
            palabra(c,tl+.22,t,al*(1-zo))
    # Fundido final exacto al fondo de la app
    if t>DUR-.18: c.drawPaint(skia.Paint(Color=C(FONDO,eio((t-(DUR-.18))/.18))))

LINEAS=[(rnd.uniform(200,1700),rnd.uniform(.6,1.6),rnd.uniform(2,7),rnd.uniform(0,.25)) for _ in range(46)]
def escena_c(c,t):
    # "Sprint": la gacela entra corriendo desde la izquierda con estelas de velocidad, frena en seco
    # con un destello, y GACELAS se revela con un barrido de luz. Corto y directo como v2b.
    c.clear(C(FONDO))
    v=eo(t/.5)*(1-eio((t-(DUR-.3))/.3))
    c.drawPaint(skia.Paint(Shader=skia.GradientShader.MakeRadial(skia.Point(CX,CY),1150,[C('#1A1030',v),C('#1A1030',0)],[0,1])))
    # estelas horizontales (warp) que cruzan y se frenan
    sp=1-eo((t-.55)/.35)
    for (y,vel,w,dl) in LINEAS:
        u=(t-dl)*vel*1.6
        if u<0 or sp<=0: continue
        x=-400+u*2600; ln=380*vel*sp
        a=.55*sp*cl(u*4)
        sh=skia.GradientShader.MakeLinear([skia.Point(x-ln,0),skia.Point(x,0)],[C(MOR,0),C(MORC,a)],[0,1])
        c.drawRect(skia.Rect(x-ln,y-w/2,x,y+w/2),skia.Paint(Shader=sh,AntiAlias=True))
    # gacela: entra con ease-out expo, estela fantasma detras
    u=eexpo(cl((t-.08)/.62))
    gx=lerp(-300,CX,u)
    s=8.2*(1+.06*math.sin(cl((t-.7)/.25)*math.pi)) if t>.7 else 8.2
    al=cl(t/.12)
    tz=DUR-.35
    if t>tz: z=eio((t-tz)/.35); s*=1+z*.3; al*=1-z
    for k,ga in [(3,.12),(2,.2),(1,.35)]:
        if u<.98: dib_gacela(c,mat_logo(s,gx-k*70*(1-u),CY),al*ga*(1-u),lin=False)
    sheen=cl((t-.85)/.45) if .85<t<1.4 else None
    dib_gacela(c,mat_logo(s,gx,CY),al,brillo=.5,sheen=sheen)
    # frenada: destello anamorfico + onda
    f=cl(1-abs(t-.72)/.2)**1.5
    if f>0:
        c.save(); c.translate(CX,CY); c.scale(1,.03)
        c.drawCircle(0,0,1200*f,skia.Paint(Shader=skia.GradientShader.MakeRadial(skia.Point(0,0),1200*f+1,[C('#FFFFFF',f),C(MORC,.5*f),C(MOR,0)],[0,.3,1])))
        c.restore()
        r=lerp(120,620,1-f)
        c.drawCircle(CX,CY,r,skia.Paint(Color=C(MORC,.5*f),Style=skia.Paint.kStroke_Style,StrokeWidth=6,ImageFilter=glow(6),AntiAlias=True))
    # palabra con revelado por mascara de izquierda a derecha y barrido de luz
    if t>.78:
        w=eo(cl((t-.78)/.45),3)
        c.save(); c.clipRect(skia.Rect(0,1100,W*w+40,1400))
        palabra(c,.70,t,al,track_ini=.30,track_fin=.16)
        c.restore()
        if w<1:
            xs=W*w
            c.drawRect(skia.Rect(xs-6,1150,xs+6,1330),skia.Paint(Color=C(LILA,.9*(1-w)),ImageFilter=glow(8),AntiAlias=True))
    if t>DUR-.15: c.drawPaint(skia.Paint(Color=C(FONDO,eio((t-(DUR-.15))/.15))))

if VAR=='c': escena=escena_c
N=int(round(DUR*FPS))
surf=skia.Surface(W,H)
for i in range(N+1):
    acc=np.zeros((H,W,4),np.float32)
    for s in range(SUB):
        t=(i+(s/SUB)-.5+.5/SUB)/FPS if i>0 else 0
        escena(surf.getCanvas(),max(0,min(DUR,t)))
        acc+=surf.makeImageSnapshot().toarray(colorType=skia.kRGBA_8888_ColorType).astype(np.float32)
    Image.fromarray(np.clip(acc/SUB+np.random.default_rng(i).normal(0,1.2,(H,W,1)),0,255).astype(np.uint8)[:,:,:3]).save(f"{OUT}/f{i:04d}.png")
print(N+1)
