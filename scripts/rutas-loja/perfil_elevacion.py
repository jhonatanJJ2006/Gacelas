import json,math,urllib.request,time
S=json.load(open("final.json"))
def hav(a,b):
  la1,la2=math.radians(a[1]),math.radians(b[1]);dl=la2-la1;dg=math.radians(b[0]-a[0])
  return 2*6371000*math.asin(math.sqrt(math.sin(dl/2)**2+math.cos(la1)*math.cos(la2)*math.sin(dg/2)**2))
def muestrear(c,n=60):
  acc=[0]
  for a,b in zip(c,c[1:]): acc.append(acc[-1]+hav(a,b))
  L=acc[-1];out=[];j=0
  for i in range(n):
    t=L*i/(n-1)
    while j<len(c)-2 and acc[j+1]<t: j+=1
    seg=acc[j+1]-acc[j] or 1;f=(t-acc[j])/seg
    out.append([c[j][0]+(c[j+1][0]-c[j][0])*f,c[j][1]+(c[j+1][1]-c[j][1])*f])
  return out
for s in S:
  smp=muestrear(s["coords"])
  u="https://api.opentopodata.org/v1/srtm30m?locations="+"|".join(f"{p[1]:.6f},{p[0]:.6f}" for p in smp)
  el=[x["elevation"] for x in json.load(urllib.request.urlopen(u,timeout=60))["results"]]
  s["perfil"]=[round(e) for e in el]
  sm=[sum(el[max(0,i-1):i+2])/len(el[max(0,i-1):i+2]) for i in range(len(el))]
  s["gain"]=round(sum(max(0,b-a) for a,b in zip(sm,sm[1:]))); s["loss"]=round(sum(max(0,a-b) for a,b in zip(sm,sm[1:])))
  k=s["dist"]/1000+s["gain"]/100
  s["dificultad"]="facil" if k<5 else "moderada" if k<10 else "dificil"
  print(s["slug"],s["dist"],s["gain"],s["dificultad"]);time.sleep(1.1)
json.dump(S,open("final.json","w"),ensure_ascii=False)
