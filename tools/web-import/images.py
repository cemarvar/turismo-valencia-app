"""Descarga las fotos de la web referenciadas por las páginas (work/web/images_needed.txt) y las guarda reducidas
(900 px, WebP) en app/src/main/assets/img. Necesita Pillow: pip install pillow."""
import concurrent.futures as cf
import io
import os
import sys
import urllib.parse
import urllib.request

from PIL import Image

T = os.environ.get("WEB_IMPORT_WORK", os.path.abspath("work"))
RAW = "https://raw.githubusercontent.com/cemarvar/TurismoValencia/main/public/img/"
OUT = os.path.abspath(os.path.join("..", "..", "app", "src", "main", "assets", "img"))
MAX_W = 900
QUALITY = 68

names = [n for n in open(os.path.join(T, "web", "images_needed.txt"), encoding="utf-8").read().split("\n") if n.strip()]


def fetch(name):
    dst = os.path.join(OUT, name.replace("/", os.sep))
    if os.path.exists(dst):
        return name, os.path.getsize(dst), "cache"
    os.makedirs(os.path.dirname(dst), exist_ok=True)
    url = RAW + urllib.parse.quote(name)
    for attempt in range(3):
        try:
            data = urllib.request.urlopen(url, timeout=60).read()
            break
        except Exception as ex:  # noqa: BLE001
            if attempt == 2:
                return name, 0, "ERROR descarga: %s" % ex
    try:
        im = Image.open(io.BytesIO(data))
        im.load()
        has_alpha = im.mode in ("RGBA", "LA") or (im.mode == "P" and "transparency" in im.info)
        im = im.convert("RGBA" if has_alpha else "RGB")
        if im.width > MAX_W:
            im = im.resize((MAX_W, round(im.height * MAX_W / im.width)), Image.LANCZOS)
        ext = os.path.splitext(name)[1].lower()
        if ext == ".webp":
            im.save(dst, "WEBP", quality=QUALITY, method=5)
        elif ext in (".jpg", ".jpeg"):
            im.convert("RGB").save(dst, "JPEG", quality=78, optimize=True)
        else:
            im.save(dst, "WEBP" if not has_alpha else "PNG", quality=QUALITY)
        return name, os.path.getsize(dst), "ok"
    except Exception as ex:  # noqa: BLE001
        return name, 0, "ERROR imagen: %s" % ex


with cf.ThreadPoolExecutor(8) as ex:
    results = list(ex.map(fetch, names))

errors = [r for r in results if r[2].startswith("ERROR")]
total = sum(r[1] for r in results)
print("imagenes: %d, errores: %d, tamaño final: %.1f MB" % (len(results), len(errors), total / 1e6))
for r in errors[:20]:
    print(r)
