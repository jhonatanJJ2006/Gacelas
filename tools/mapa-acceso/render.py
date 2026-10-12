import json,math
from PIL import Image,ImageDraw,ImageFilter,ImageFont
W,H=1080,1920;S=2
CLAT,CLON=-3.9718,-79.2030
DEG_W=0.0155;DEG_H=DEG_W*H/W
top=CLAT+DEG_H*0.40; left=CLON-DEG_W/2
def P(lat,lon): return ((lon-left)/DEG_W*W*S,(top-lat)/DEG_H*H*S)
d=json.load(open(__import__('sys').argv[1] if len(__import__('sys').argv)>1 else 'osm.json'))['elements']
bg=Image.new('RGB',(W*S,H*S),'#0B1120');dr=ImageDraw.Draw(bg)
def pts(e): return [P(g['lat'],g['lon']) for g in e.get('geometry',[])]
for e in d:
  t=e['tags']
  if t.get('leisure')=='park' or t.get('natural')=='water':
    p=pts(e)
    if len(p)>2: dr.polygon(p,fill='#0A2A22' if t.get('leisure') else '#12224A')
for e in d:
  t=e['tags'];w=t.get('waterway')
  if w: dr.line(pts(e),fill='#1E3A8A',width=S*(10 if w=='river' else 4),joint='curve')
wid={'trunk':9,'primary':8,'primary_link':5,'secondary':7,'secondary_link':4,'tertiary':6,'residential':4,'unclassified':4,'living_street':3,'service':2,'pedestrian':2}
col={'trunk':'#334155','primary':'#334155','secondary':'#2B3647','tertiary':'#263142'}
for lvl in ['minor','major']:
  for e in d:
    h=e['tags'].get('highway')
    if h not in wid: continue
    major=h in col
    if (lvl=='major')!=major: continue
    dr.line(pts(e),fill=col.get(h,'#1C2536'),width=S*wid[h],joint='curve')
# route (real trace "1" from Mapas.trazos), Chaikin smoothing
R=[(-3.9746,-79.2017),(-3.9712,-79.2001),(-3.969,-79.203),(-3.9718,-79.2058)]
p=[P(*x) for x in R]
for _ in range(4):
  q=[]
  for i in range(len(p)):
    a,b=p[i],p[(i+1)%len(p)]
    q+= [(a[0]*.75+b[0]*.25,a[1]*.75+b[1]*.25),(a[0]*.25+b[0]*.75,a[1]*.25+b[1]*.75)]
  p=q
p.append(p[0])
glow=Image.new('RGBA',bg.size,(0,0,0,0));g=ImageDraw.Draw(glow)
g.line(p,fill=(214,51,138,190),width=S*26,joint='curve')
glow=glow.filter(ImageFilter.GaussianBlur(S*22))
bg=bg.convert('RGBA');bg.alpha_composite(glow)
ln=Image.new('RGBA',bg.size,(0,0,0,0));l=ImageDraw.Draw(ln)
l.line(p,fill=(171,6,96,255),width=S*14,joint='curve');l.line(p,fill=(214,51,138,255),width=S*8,joint='curve')
bg.alpha_composite(ln.filter(ImageFilter.GaussianBlur(S*1)))
dr=ImageDraw.Draw(bg)
sx,sy=p[0];r=S*14
dr.ellipse([sx-r*1.8,sy-r*1.8,sx+r*1.8,sy+r*1.8],outline=(214,51,138,200),width=S*3)
dr.ellipse([sx-r,sy-r,sx+r,sy+r],fill='#FFFFFF',outline='#AB0660',width=S*4)
f=ImageFont.truetype('/workspace/gacelas/apps/android/app/src/main/res/font/inter.ttf',S*24)
try: f.set_variation_by_axes([14,600])
except Exception as ex: print(ex)
def lab(lat,lon,txt,c=(148,163,184,255)):
  x,y=P(lat,lon);dr.text((x,y),txt,font=f,fill=c,spacing=4)
lab(-3.9722,-79.1992,'P A R Q U E   J I P I R O')
# river label along Zamora: find a point
lab(-3.9690,-79.2104,'R Í O   Z A M O R A',(96,130,200,255))
lab(-3.9808,-79.2018,'A V .   O R I L L A S   D E L   Z A M O R A')
# vignette bottom fade
v=Image.new('L',(1,H*S))
for y in range(H*S): v.putpixel((0,y),int(255*max(0,min(1,(y/(H*S)-0.55)/0.45))**1.3))
fade=Image.new('RGBA',bg.size,(17,24,39,255));fade.putalpha(v.resize(bg.size))
bg.alpha_composite(fade)
bg.convert('RGB').resize((W,H),Image.LANCZOS).save('mapa_loja_jipiro.webp',quality=82)
