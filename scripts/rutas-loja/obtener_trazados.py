import json,math,urllib.request,time
o={r["slug"]:r for r in json.load(open("osrm.json"))}
W={e["id"]:[[p["lon"],p["lat"]] for p in e["geometry"]] for e in json.load(open("r3.json"))["elements"]}
def chain(ids):
  c=[]
  for i in ids:
    g=W[abs(i)][::-1] if i<0 else W[i]
    if c and c[-1]!=g[0]:
      if c[-1]==g[-1]: g=g[::-1]
    c+= g if not c or c[-1]!=g[0] else g[1:]
  return c
def osrm(wp):
  u="https://routing.openstreetmap.de/routed-foot/route/v1/foot/"+";".join(f"{lo},{la}" for la,lo in wp)+"?overview=full&geometries=geojson"
  return json.load(urllib.request.urlopen(u,timeout=60))["routes"][0]["geometry"]["coordinates"]
def hav(a,b):
  la1,la2=math.radians(a[1]),math.radians(b[1]);dl=la2-la1;dg=math.radians(b[0]-a[0])
  return 2*6371000*math.asin(math.sqrt(math.sin(dl/2)**2+math.cos(la1)*math.cos(la2)*math.sin(dg/2)**2))
S=[]
def add(slug,n,cat,sup,coords,fuente,desc):
  S.append(dict(slug=slug,nombre=n,categoria=cat,superficie=sup,coords=coords,fuente=fuente,desc=desc))
OS="OpenStreetMap (ODbL), trazado con OSRM perfil a pie sobre datos OSM"
D={
"la-tebaida":("Ida y vuelta por el Parque Lineal La Tebaida junto al río Malacatos.","parque"),
"cascarilla":("Sendero de la Ruta de la Cascarilla entre La Tebaida y La Argelia.","sendero"),
"pucara":("Vuelta corta por el Parque Pucará Podocarpus y el sendero del teleférico.","parque"),
"el-churo":("Subida desde Plaza San Francisco hasta el Mirador El Churo.","montana"),
"villonaco":("Subida por la vía antigua a Catamayo hasta la cumbre del Villonaco.","montana"),
"zamora-huayco":("Desde el Parque Lozano hacia el barrio Zamora Huayco por el río Zamora.","sendero"),
"rumi-wilco":("Sendero de la reserva Rumi Wilco en Vilcabamba.","sendero"),
"mirador-san-jose":("Desde el parque central de Vilcabamba hasta el Mirador San José.","montana"),
"la-banda":("Sendero Ecológico La Banda en el norte de Loja.","sendero"),
"orillas-zamora":("Recorrido urbano por la avenida junto al río Zamora.","urbana"),
"orillas-malacatos":("Recorrido urbano por las orillas del río Malacatos.","urbana"),
"centro-historico":("Puerta de la Ciudad, Plaza Central, Santo Domingo y San Sebastián.","urbana"),
"las-penas":("Desde la Plaza Central hasta el Mirador Las Peñas.","urbana"),
"daniel-alvarez":("Sendero del Parque Daniel Álvarez Burneo.","parque"),
"altavista":("Escalinatas hasta el Mirador Altavista.","montana"),
"piedra-bola":("Caminos rurales hasta el mirador Piedra Bola, al sur de Loja.","montana"),
"uritusinga":("Subida por caminos rurales al Cerro Uritusinga.","montana"),
"chaupi":("De Vilcabamba al puente peatonal Chaupi.","sendero"),
"yunanga":("De Malacatos por el sendero Yunanga.","sendero"),
}
for slug,(desc,_) in D.items():
  r=o[slug]
  coords=r["coords"]
  if slug=="zamora-huayco": coords=osrm(r["wp"][:2])
  add(slug,r["nombre"],r["categoria"],r["superficie"],coords,OS,desc)
OW="OpenStreetMap (ODbL), vías "
add("jipiro","Parque Jipiro","parque","pavimento",chain([170924331,943824894,943824893,694942246,1182173722,1182173720,1182173721,694942244]),OW+"170924331, 943824893, 694942244 y tramos","Vuelta por los senderos del Parque Recreacional Jipiro.")
add("cajanuma-miradores","Podocarpus Cajanuma: Los Miradores","montana","tierra",chain([137870253,-40298120,137870273,137870248,137870250]),OW+"137870253, 40298120, 137870273, 137870248 y 137870250","Circuito Los Miradores del Parque Nacional Podocarpus, sector Cajanuma.")
add("cajanuma-bosque-nublado","Podocarpus Cajanuma: Bosque Nublado","sendero","tierra",W[40298122],OW+"40298122","Sendero corto por el bosque nublado del sector Cajanuma.")
add("puear-oso","PUEAR: Sendero del Oso","sendero","tierra",W[946998708],OW+"946998708","Sendero del Parque Universitario La Argelia (PUEAR).")
m=osrm([(-4.2607,-79.2227),(-4.2664,-79.2258)])+chain([297211965,280173286])
add("mandango","Vilcabamba: Cerro Mandango","montana","tierra",m,OS+"; vías 297211965 y 280173286","Desde el parque central de Vilcabamba hasta la cima del Mandango.")
add("vilcabamba-malacatos","Sendero Ecológico Malacatos","sendero","tierra",W[658584534],OW+"658584534","Sendero ecológico entre los valles de Malacatos.")
add("pista-utpl","Pista de atletismo UTPL","pista","tartan",W[650288229],OW+"650288229","Pista de atletismo del campus de la UTPL.")
for s in S:
  c=s["coords"]; d=sum(hav(a,b) for a,b in zip(c,c[1:])); s["dist"]=round(d)
  # sample <=100
  step=max(1,len(c)//99); smp=c[::step]
  if smp[-1]!=c[-1]: smp.append(c[-1])
  smp=smp[:100]
  u="https://api.opentopodata.org/v1/srtm30m?locations="+"|".join(f"{p[1]:.6f},{p[0]:.6f}" for p in smp)
  for t in range(3):
    try: el=[x["elevation"] for x in json.load(urllib.request.urlopen(u,timeout=60))["results"]];break
    except Exception as e: print("retry",e);time.sleep(3)
  el=[e if e is not None else el[0] for e in el]
  sm=[sum(el[max(0,i-1):i+2])/len(el[max(0,i-1):i+2]) for i in range(len(el))]
  s["gain"]=round(sum(max(0,b-a) for a,b in zip(sm,sm[1:]))); s["loss"]=round(sum(max(0,a-b) for a,b in zip(sm,sm[1:])))
  s["elev_min"]=round(min(el));s["elev_max"]=round(max(el))
  k=d/1000+s["gain"]/100
  s["dificultad"]="facil" if k<5 else "moderada" if k<12 else "dificil"
  print(s["slug"],s["dist"],s["gain"],s["elev_min"],s["elev_max"],s["dificultad"],len(c)); time.sleep(1.2)
json.dump(S,open("final.json","w"),ensure_ascii=False)
