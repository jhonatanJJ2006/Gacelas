# Fondo de mapa de las pantallas de acceso

`mapa_stitch_bienvenida.webp` y `mapa_stitch_login.webp` son la capa de mapa del export de Stitch (variante C4 aprobada).
Se generan desde los HTML de esta carpeta ocultando la cabecera y la tarjeta y capturando a 390x884 con escala 3:

```sh
for v in bienvenida login; do
  sed 's#</head>#<style>header,section,body>div>div.z-10{visibility:hidden!important} *{animation:none!important}</style></head>#' C4-$v.html > /tmp/mapa-$v.html
  google-chrome --headless=new --hide-scrollbars --force-device-scale-factor=3 --window-size=390,884 \
    --virtual-time-budget=8000 --screenshot=mapa-$v.png file:///tmp/mapa-$v.html
done
```
Después se recortan 2 px por lado (borde del contenedor de Stitch) y se convierten a WebP.
