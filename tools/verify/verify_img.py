"""Regresión de fotos: tras cambiar de chip, cada tarjeta debe mostrar SU foto de la web (no la de la sección anterior)."""
import os
import sys
import time

from PIL import Image

import verify_ui as v

DESIGN = {"Paella valenciana", "Fallas 2027", "Mercado Central", "Albufera en barca"}  # llevan la foto del diseño (Figma), no la de la web
BASE = os.path.join(v.REPO, "app", "src", "main", "assets", "img")


def vec(img):
    g = img.convert("L").resize((48, 48))
    px = list(g.getdata())
    m = sum(px) / len(px)
    d = [p - m for p in px]
    n = sum(p * p for p in d) ** 0.5 or 1
    return [p / n for p in d]


def sim(a, b):
    return sum(x * y for x, y in zip(a, b))


def main():
    print("\n## Fotos: cada tarjeta muestra SU foto")
    v.start("inicio")
    for key in ("gastronomia", "alojamientos", "eventos", "gastronomia", "alojamientos"):
        v.tap_chip(v.CHIP[key])
        time.sleep(0.8)
        ns = v.nodes()
        img = v.shot()
        tiles = {v.ALIAS.get(t["title"], t["title"]): t for t in v.index["categories"][key]["tiles"]}
        good, bad = [], []
        for n in ns:
            t = tiles.get(n["d"].strip()) if n["d"] else None
            if not t:
                continue
            x1, y1, x2, y2 = n["b"]
            if y1 < 700 or y2 > 1525 or x2 - x1 < 200 or n["d"].strip() in DESIGN:
                continue
            ref = Image.open(os.path.join(BASE, t["image"].replace("/", os.sep))).convert("RGB")
            w, h = ref.size
            tw, th = x2 - x1, y2 - y1
            sc = max(tw / w, th / h)
            cw, ch = tw / sc, th / sc
            ref = ref.crop((int((w - cw) / 2), int((h - ch) / 2), int((w + cw) / 2), int((h + ch) / 2)))
            cut = int(th * 0.35)  # el corazón de favorito tapa la parte superior
            s = sim(vec(img.crop((x1, y1 + cut, x2, y2))), vec(ref.crop((0, int(ref.size[1] * 0.35), ref.size[0], ref.size[1]))))
            (good if s > 0.6 else bad).append("%s=%.2f" % (n["d"], s))
        v.check("chip %s: fotos de las tarjetas visibles son las suyas" % v.CHIP[key], bool(good) and not bad, "bien %s · mal %s" % (good, bad))
    print("\nRESULTADO: %d/%d correctas" % (sum(v.results), len(v.results)))
    sys.exit(0 if all(v.results) else 1)


main()
