#!/usr/bin/env python3
"""Genera las instancias estaticas por peso de las fuentes variables de Android para iOS.

Compose Multiplatform 1.7 en iOS no aplica FontVariation, asi que iOS carga una instancia
estatica por peso de los MISMOS archivos (resto de ejes en su valor por defecto, como Android).
Uso: python3 tools/fuentes-ios.py   (requiere fonttools)
"""
from fontTools.ttLib import TTFont
from fontTools.varLib import instancer

ORIGEN = "apps/android/shared/src/androidMain/res/font"
DESTINO = "apps/ios/iosApp/iosApp/Fuentes"
PESOS = {"inter": [400, 500, 600, 700], "roboto_flex": [400, 500, 600, 700, 900], "space_grotesk": [500, 600, 700]}

for nombre, pesos in PESOS.items():
    for w in pesos:
        f = TTFont(f"{ORIGEN}/{nombre}.ttf")
        loc = {a.axisTag: a.defaultValue for a in f["fvar"].axes}
        loc["wght"] = w
        instancer.instantiateVariableFont(f, loc, updateFontNames=False).save(f"{DESTINO}/{nombre}_{w}.ttf")
