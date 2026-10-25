import json,urllib.request,time,math
R=[
("jipiro","Parque Jipiro","parque","pavimento",[(-3.9705,-79.2045),(-3.9676,-79.1995),(-3.9654,-79.1964),(-3.9686,-79.1989),(-3.9705,-79.2045)]),
("la-tebaida","Parque Lineal La Tebaida","parque","mixta",[(-4.0122,-79.2046),(-4.0230,-79.2030),(-4.0122,-79.2046)]),
("cascarilla","Ruta de la Cascarilla","sendero","tierra",[(-4.0145,-79.2027),(-4.0191,-79.2021),(-4.0274,-79.2010)]),
("puear","Parque Universitario La Argelia (PUEAR)","sendero","tierra",[(-4.0365,-79.1955),(-4.0471,-79.1779)]),
("pucara","Parque Pucará Podocarpus","parque","mixta",[(-4.0073,-79.1972),(-4.0091,-79.1948),(-4.0089,-79.1961),(-4.0073,-79.1972)]),
("el-churo","Mirador El Churo","montana","mixta",[(-3.9950,-79.2020),(-3.9980,-79.1948)]),
("villonaco","Cerro Villonaco","montana","tierra",[(-3.9950,-79.2400),(-3.9885,-79.2684)]),
("cajanuma-miradores","Cajanuma: Los Miradores","montana","tierra",[(-4.1152,-79.1724),(-4.1182,-79.1642),(-4.1171,-79.1630)]),
("cajanuma-oso","Cajanuma: Oso de Anteojos","sendero","tierra",[(-4.1135,-79.1708),(-4.1156,-79.1730),(-4.1122,-79.1769)]),
("zamora-huayco","Zamora Huayco","sendero","mixta",[(-4.0040,-79.1940),(-4.0077,-79.1867),(-4.0100,-79.1750)]),
("mandango","Vilcabamba: Cerro Mandango","montana","tierra",[(-4.2607,-79.2227),(-4.2652,-79.2326),(-4.2658,-79.2384)]),
("rumi-wilco","Vilcabamba: Rumi Wilco","sendero","tierra",[(-4.2588,-79.2183),(-4.2582,-79.2173),(-4.2636,-79.2157)]),
("mirador-san-jose","Vilcabamba: Mirador San José","montana","mixta",[(-4.2607,-79.2227),(-4.2694,-79.2123)]),
("la-banda","Sendero Ecológico La Banda","sendero","tierra",[(-3.9603,-79.2131),(-3.9413,-79.2219),(-3.9308,-79.2233)]),
("orillas-zamora","Orillas del río Zamora","urbana","pavimento",[(-4.0040,-79.1960),(-3.9750,-79.2020)]),
("orillas-malacatos","Orillas del río Malacatos","urbana","pavimento",[(-4.0120,-79.2050),(-3.9800,-79.2050)]),
("centro-historico","Centro histórico de Loja","urbana","pavimento",[(-3.9910,-79.2045),(-3.9967,-79.2017),(-3.9984,-79.2016),(-4.0016,-79.2013)]),
("las-penas","Mirador Las Peñas","urbana","pavimento",[(-3.9967,-79.2017),(-3.9982,-79.2100)]),
("daniel-alvarez","Parque Daniel Álvarez","parque","tierra",[(-4.0180,-79.2105),(-4.0195,-79.2119),(-4.0210,-79.2125)]),
("malacatos-sendero","Malacatos: Sendero Ecológico","sendero","tierra",[(-4.2133,-79.2418),(-4.2164,-79.2432)]),
("altavista","Mirador Altavista","montana","pavimento",[(-3.9950,-79.2020),(-3.9950,-79.1924)]),
("piedra-bola","Mirador Piedra Bola","montana","tierra",[(-4.0750,-79.2150),(-4.1027,-79.2246)]),
("uritusinga","Cerro Uritusinga","montana","tierra",[(-4.0400,-79.2100),(-4.0565,-79.2311)]),
("chaupi","Vilcabamba: Sendero Chaupi","sendero","tierra",[(-4.2607,-79.2227),(-4.2472,-79.2466)]),
("yunanga","Malacatos: Sendero Yunanga","sendero","tierra",[(-4.2194,-79.2586),(-4.1915,-79.2282)]),
]
out=[]
for slug,n,cat,sup,wp in R:
  u="https://routing.openstreetmap.de/routed-foot/route/v1/foot/"+";".join(f"{lo},{la}" for la,lo in wp)+"?overview=full&geometries=geojson&steps=true"
  try: d=json.load(urllib.request.urlopen(u,timeout=60))
  except Exception as e: print(slug,"ERR",e); continue
  r=d["routes"][0]; snaps=[round(w["distance"]) for w in d["waypoints"]]
  names=sorted({s["name"] for l in r["legs"] for s in l["steps"] if s["name"]})
  out.append(dict(slug=slug,nombre=n,categoria=cat,superficie=sup,wp=wp,coords=r["geometry"]["coordinates"],dist=r["distance"],snaps=snaps,calles=names))
  print(slug,round(r["distance"]),snaps,names[:8]); time.sleep(1)
json.dump(out,open("osrm.json","w"))
