"""Pruebas de interfaz en el emulador (780x1688 px / 320 dpi): navegación, contenido de la web y fotos cargadas.
Uso: python verify_ui.py [grupo ...]   (grupos: chips cats tiles contacto guias detalles paginas busqueda)"""
import io
import json
import os
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET

from PIL import Image, ImageStat

ADB = os.environ.get("ADB") or os.path.expandvars(r"%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe")
PKG = "com.cmarcillovargas.turismovalencia"
REPO = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
ASSETS = os.path.join(REPO, "app", "src", "main", "assets", "web")
index = json.load(open(os.path.join(ASSETS, "index.json"), encoding="utf-8"))
W, H = 780, 1688
BAR_TOP, BAR_BOTTOM = 210, 1525  # zona de contenido entre la cabecera y la barra inferior

results = []


def check(name, ok, detail=""):
    results.append(ok)
    print(("PASS" if ok else "FAIL"), "-", name, ("· " + detail) if detail else "", flush=True)


def adb(*args, binary=False):
    r = subprocess.run([ADB, *args], capture_output=True)
    return r.stdout if binary else r.stdout.decode("utf-8", "replace")


def nodes():
    adb("shell", "uiautomator", "dump", "/sdcard/ui.xml")
    xml = adb("exec-out", "cat", "/sdcard/ui.xml", binary=True).decode("utf-8", "replace")
    out = []
    try:
        root = ET.fromstring(xml)
    except ET.ParseError:
        return out
    for n in root.iter("node"):
        m = re.match(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", n.get("bounds") or "")
        if not m:
            continue
        out.append({"t": n.get("text") or "", "d": n.get("content-desc") or "", "b": tuple(map(int, m.groups()))})
    return out


def texts(ns=None):
    return [n["t"] for n in (ns if ns is not None else nodes()) if n["t"]]


def shot():
    return Image.open(io.BytesIO(adb("exec-out", "screencap", "-p", binary=True))).convert("RGB")


def start(route=None, *extra):
    adb("shell", "am", "force-stop", PKG)
    args = ["shell", "am", "start", "-n", PKG + "/.MainActivity"]
    if route:
        args += ["--es", "route", route]
    adb(*args, *extra)
    time.sleep(3.2)
    for _ in range(8):  # espera a que la pantalla esté pintada
        if len(nodes()) > 12:
            break
        time.sleep(0.8)


def swipe_up(n=1, dist=700):
    for _ in range(n):
        adb("shell", "input", "swipe", "390", "1350", "390", str(1350 - dist), "900")
        time.sleep(0.6)


def swipe_down(n=1, dist=700):
    for _ in range(n):
        adb("shell", "input", "swipe", "390", "500", "390", str(500 + dist), "900")
        time.sleep(0.6)


def back():
    adb("shell", "input", "keyevent", "KEYCODE_BACK")
    time.sleep(0.9)


def tap(n):
    x1, y1, x2, y2 = n["b"]
    adb("shell", "input", "tap", str((x1 + x2) // 2), str((y1 + y2) // 2))
    time.sleep(1.3)


def tap_chip(text):
    """Pulsa un chip de la portada; si el carrusel de chips lo dejó fuera de pantalla, lo desliza hasta verlo."""
    for direction in (None, "right", "left", "left"):
        n = find(text)
        if n and n["b"][2] - n["b"][0] > 60 and n["b"][0] >= 0 and 740 < n["b"][1] < 800:
            tap(n)
            return True
        if direction:
            a, b = ("100", "700") if direction == "right" else ("700", "100")
            adb("shell", "input", "swipe", a, "770", b, "770", "600")
            time.sleep(0.6)
    return False


def find(text, exact=True, ns=None):
    for n in ns if ns is not None else nodes():
        for label in (n["t"], n["d"]):
            if label and ((label == text) if exact else (text.lower() in label.lower())):
                return n
    return None


def scroll_to(text, exact=True, tries=14):
    for i in range(tries):
        n = find(text, exact)
        if n and BAR_TOP < n["b"][1] < BAR_BOTTOM - 60:
            return n
        swipe_up(1)
    return None


def tap_text(text, exact=True):
    n = find(text, exact)
    if n:
        tap(n)
    return n is not None


def is_photo(img, box, min_std=9.0):
    x1, y1, x2, y2 = box
    x1, y1, x2, y2 = max(0, x1), max(BAR_TOP, y1), min(W, x2), min(BAR_BOTTOM, y2)
    if x2 - x1 < 40 or y2 - y1 < 40:
        return None
    st = ImageStat.Stat(img.crop((x1, y1, x2, y2)).convert("L"))
    return st.stddev[0] > min_std


def photo_report(ns, titles):
    """Devuelve (con_foto, sin_foto) para las tarjetas visibles cuya foto lleva como descripción su título."""
    img = shot()
    ok, bad = [], []
    for n in ns:
        if n["d"] and n["d"].strip() in titles:
            r = is_photo(img, n["b"])
            if r is None:
                continue
            (ok if r else bad).append(n["d"])
    return ok, bad


ALIAS = {"Lonja de Seda": "Lonja de la Seda", "Oceanografic": "Oceanogràfic", "Albufera": "Parque Natural de La Albufera", "Fallas": "Fallas 2027", "Paella Valenciana": "Paella valenciana"}
CATS = {"quever": ("que-ver", "Espacios únicos"), "gastronomia": ("gastronomia", "Tradición culinaria"), "alojamientos": ("alojamientos", "Opciones de alojamiento"), "eventos": ("eventos", "Eventos destacados")}
CHIP = {"quever": "Qué ver", "gastronomia": "Gastronomía", "alojamientos": "Alojamientos", "eventos": "Eventos"}
SOST = {"MueveteValencia": "Muévete por Valencia", "Ecoturismo": "Ecoturismo", "ComercioLocal": "Comercio local", "Consejos": "Consejos para viajar mejor", "ViajeSostenible": "Ciudad sostenible e inteligente", "ViajeResponsable": "Viaje responsable"}


def expected_titles(key):
    return [ALIAS.get(t["title"], t["title"]) for t in index["categories"][key]["tiles"]]


# ---------------------------------------------------------------- grupos
def g_chips():
    print("\n## Portada: chips de sección")
    start("inicio")
    for key, chip in CHIP.items():
        tap_chip(chip)
        ns = nodes()
        cat = index["categories"][key]
        exp = expected_titles(key)
        if key == "quever":
            ok = find("Cerca de ti", ns=ns) is not None
            check("chip %s: título «Cerca de ti» (diseño)" % chip, ok)
            first = "Mercado Central"
        else:
            ok = find(cat["title"], ns=ns) is not None and find(cat["subtitle"], ns=ns) is not None
            check("chip %s: título y texto de la web" % chip, ok, cat["title"])
            first = exp[0]
            check("chip %s: carrusel empieza por «%s»" % (chip, first), find(first, ns=ns) is not None)
        titles = set(exp) | {"Mercado Central", "Albufera en barca", "Paella valenciana", "Playas de Valencia"}
        ok_img, bad_img = photo_report(ns, titles)
        check("chip %s: fotos de las tarjetas visibles cargadas" % chip, len(ok_img) >= 2 and not bad_img, "con foto %s · sin foto %s" % (ok_img, bad_img))


def collect_titles(expected, scrolls=14):
    seen = []
    for _ in range(scrolls):
        ns = nodes()
        for n in ns:
            if n["t"] in expected and n["t"] not in seen and 660 < n["b"][1] < BAR_BOTTOM:
                seen.append(n["t"])
        if len(seen) == len(expected):
            break
        swipe_up(1, 650)
    return seen


def g_cats():
    print("\n## Secciones: contenido frente a la web")
    for key, (route, banner) in CATS.items():
        start("categoria/" + route)
        for _ in range(8):
            ns = nodes()
            if find(banner, ns=ns):
                break
            time.sleep(0.8)
        cat = index["categories"][key]
        check("sección %s: cabecera «%s» y texto de la web" % (key, banner), find(banner, ns=ns) is not None and find(cat["subtitle"], ns=ns) is not None)
        exp = expected_titles(key)
        ok_img, bad_img = photo_report(ns, set(exp))
        check("sección %s: fotos de las primeras tarjetas cargadas" % key, len(ok_img) >= 2 and not bad_img, "sin foto %s" % bad_img)
        seen = collect_titles(set(exp))
        if key == "quever":
            check("sección %s: las %d fichas de la web" % (key, len(exp)), set(seen) == set(exp), "faltan %s" % (set(exp) - set(seen)))
        else:
            check("sección %s: %d fichas en el orden de la web" % (key, len(exp)), seen == exp, "obtenido %s" % seen)


def g_tiles():
    print("\n## Portada: las 21 tarjetas abren su página")
    start("inicio")
    swipe_up(2, 800)
    for group in ("esencial", "planes", "sostenible"):
        for t in index["home"][group]:
            title = t["title"].strip()
            n = scroll_to(title)
            if n is None:
                swipe_down(6, 800)
                n = scroll_to(title)
            if n is None:
                check("tarjeta «%s»: visible en la portada" % title, False)
                continue
            tap(n)
            ns = nodes()
            route = t["route"]
            if route == "Entradas":
                ok = find("Tickets & Tours", ns=ns) is not None
                want = "Tickets & Tours"
            elif route in index["pages"] and group != "sostenible":
                want = index["pages"][route]["title"]
                ok = find(want, ns=ns) is not None
            else:
                want = SOST.get(route, title)
                ok = find(want, ns=ns) is not None
            check("tarjeta «%s» → «%s»" % (title, want), ok)
            back()


def g_contacto():
    print("\n## Contacto y textos legales")
    start("contacto")
    ns = nodes()
    for s in ("Lun - Vie: 9:00 - 18:00", "+34 963 000 000", "info@valencia.com", "Valencia, España"):
        check("contacto: «%s»" % s, find(s, ns=ns) is not None)
    swipe_up(3, 800)
    ns = nodes()
    for s in ("LinkedIn", "Facebook", "Instagram", "X"):
        check("contacto: red «%s»" % s, find(s, ns=ns) is not None)
    for link, page in (("Política de privacidad", "PoliticaPrivacidad"), ("Aviso legal", "AvisoLegal")):
        n = find(link)
        if n:
            tap(n)
            check("legal: «%s» abre su página" % link, find(index["pages"][page]["title"]) is not None)
            back()
        else:
            check("legal: enlace «%s»" % link, False)


def g_guias():
    print("\n## Tickets & Tours: guías de la web")
    guide = {"entradas": ["ValenciaCard", "BusTuristico"], "tours": ["CentroHistorico", "CienciasArtes", "Bici", "PaseoMaritimo", "PrivadoGrupo"], "experiencias": ["Excursiones", "ActividadesGastronomicas", "Nauticas"]}
    for tab, pages in guide.items():
        start("entradas?tab=" + tab)
        swipe_up(8, 900)
        ns = nodes()
        missing = [p for p in pages if find(index["pages"][p]["title"], ns=ns) is None]
        # algunas guías quedan fuera de pantalla: se sube hasta verlas todas
        if missing:
            swipe_down(2, 500)
            ns = nodes()
            missing = [p for p in pages if find(index["pages"][p]["title"], ns=ns) is None]
        check("pestaña %s: guías %s" % (tab, ", ".join(pages)), not missing, "no vistas %s" % missing)
    n = find("Valencia Tourist Card") or find("Valencia Tourist Card", exact=False)
    start("entradas?tab=entradas")
    swipe_up(8, 900)
    n = find(index["pages"]["ValenciaCard"]["title"])
    if n:
        tap(n)
        check("guía «Valencia Tourist Card» se abre", find(index["pages"]["ValenciaCard"]["title"]) is not None and find("Atrás") is not None)


def g_detalles():
    print("\n## Fichas: guía completa de la web y foto de cabecera")
    for item, page in (("mercado-central", "MercadoCentral"), ("paella", "Paella"), ("aloj-centro", "Centro"), ("fallas", "Fallas"), ("bioparc", "Bioparc"), ("cac", "CAC"), ("deportes", "Deportes"), ("ecoturismo", "Ecoturismo")):
        start("detalle/" + item)
        img = shot()
        hero = is_photo(img, (0, 220, W, 600))
        guide = scroll_to("Guía completa", tries=14)
        check("ficha %s: foto de cabecera y «Guía completa»" % item, bool(hero) and guide is not None, "foto=%s guía=%s" % (hero, guide is not None))


def g_paginas():
    print("\n## Páginas de la web: foto de cabecera cargada")
    sample = ["Navidad", "VisitasImprescindibles", "ValenciaTresDias", "CiudadAmurallada", "PatrimonioHumanidad", "MonumentosMuseos", "CAC", "Bioparc", "Espectaculo", "Familia", "Naturaleza", "FiestasTradicionales", "Deportes", "Exposicion", "ValenciaCard", "Nauticas"]
    for p in sample:
        start("pagina/" + p)
        img = shot()
        title = index["pages"][p]["title"]
        check("página %s: título «%s» y foto de cabecera" % (p, title), find(title) is not None and bool(is_photo(img, (0, 220, W, 560))))


def g_busqueda():
    print("\n## Buscador")
    for q, want in (("bioparc", "Bioparc"), ("navidad", "La Navidad más luminosa"), ("deporte", "Deportes"), ("alojamiento barato", None)):
        start("busqueda")
        adb("shell", "input", "text", q.replace(" ", "%s"))
        time.sleep(1.4)
        ns = nodes()
        if want:
            check("buscar «%s» → «%s»" % (q, want), find(want, ns=ns) is not None)
        else:
            check("buscar «%s» no se rompe" % q, find("Atrás", ns=ns) is not None)


GROUPS = {"chips": g_chips, "cats": g_cats, "tiles": g_tiles, "contacto": g_contacto, "guias": g_guias, "detalles": g_detalles, "paginas": g_paginas, "busqueda": g_busqueda}
if __name__ == "__main__":
    adb("shell", "logcat", "-c")
    chosen = sys.argv[1:] or list(GROUPS)
    for g in chosen:
        GROUPS[g]()
    crash = adb("shell", "logcat", "-d", "-t", "2000")
    fatal = [l for l in crash.split("\n") if "FATAL EXCEPTION" in l and PKG in crash]
    check("sin cierres inesperados (logcat)", not fatal)
    print("\nRESULTADO: %d/%d correctas" % (sum(results), len(results)))
    sys.exit(0 if all(results) else 1)
