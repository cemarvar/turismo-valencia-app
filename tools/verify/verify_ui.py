"""Pruebas de interfaz en el emulador (780x1688 px / 320 dpi): navegación, contenido de la web y fotos cargadas.
Uso: python verify_ui.py [grupo ...]   (grupos: chips cats tiles contacto guias detalles paginas busqueda persist mapa avisos)
Cada arranque restablece los datos de demostración (`--ez resetData true`) para que las pruebas no dependan de lo que
guardó la anterior; `start(..., reset=False)` reabre la app conservando lo guardado."""
import io
import json
import os
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET
from datetime import datetime, timedelta, timezone

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
        out.append({
            "t": n.get("text") or "",
            "d": n.get("content-desc") or "",
            "b": tuple(map(int, m.groups())),
            "chk": n.get("checkable") == "true",
            "on": n.get("checked") == "true",
        })
    return out


def texts(ns=None):
    return [n["t"] for n in (ns if ns is not None else nodes()) if n["t"]]


def shot():
    return Image.open(io.BytesIO(adb("exec-out", "screencap", "-p", binary=True))).convert("RGB")


def start(route=None, *extra, reset=True):
    adb("shell", "am", "force-stop", PKG)
    # Un diálogo de permisos que quedara abierto de la prueba anterior taparía la app.
    adb("shell", "am", "force-stop", "com.google.android.permissioncontroller")
    args = ["shell", "am", "start", "-n", PKG + "/.MainActivity"]
    if reset:
        args += ["--ez", "resetData", "true"]
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


def accept_location_dialog():
    """La pantalla de permiso de la bienvenida abre el diálogo real del sistema (en inglés en el emulador): lo acepta."""
    if wait_for("While using the app", 5):
        tap_text("While using the app")
    time.sleep(1.5)


def tap_last(text):
    """Pulsa el último nodo con ese texto (el de la capa de encima, p. ej. el botón de un diálogo)."""
    hits = [n for n in nodes() if n["t"] == text or n["d"] == text]
    if hits:
        tap(hits[-1])
    return bool(hits)


def switch_near(label):
    """Interruptor de la fila cuyo texto es `label`."""
    ns = nodes()
    row = find(label, ns=ns)
    if not row:
        return None
    cy = (row["b"][1] + row["b"][3]) // 2
    return next((n for n in ns if n["chk"] and n["b"][1] - 40 <= cy <= n["b"][3] + 40), None)


def bell_dot():
    """El aviso rojo/naranja de la campana de Inicio (hay avisos sin leer)."""
    img = shot()
    box = img.crop((700, 112, 748, 150))
    orange = sum(1 for r, g, b in getattr(box, "get_flattened_data", box.getdata)() if r > 200 and 60 < g < 140 and b < 60)
    return orange > 20


def g_persist():
    print("\n## Persistencia (Room): lo guardado sigue ahí al cerrar y abrir la app")

    # Favoritos
    start("favoritos")
    check("demo: 6 lugares guardados", find("6 lugares guardados") is not None)
    tap_text("Quitar de favoritos")
    check("quitar un favorito: quedan 5", find("5 lugares guardados") is not None)
    start("favoritos", reset=False)
    ns = nodes()
    check("tras cerrar y abrir siguen siendo 5", find("5 lugares guardados", ns=ns) is not None)
    check("el quitado no vuelve (Mercado Central)", find("Mercado Central", ns=ns) is None)

    # Descargas y sus ajustes
    start("descargas")
    sw = switch_near("Descargar solo con Wi-Fi")
    check("demo: «solo Wi-Fi» activado", sw is not None and sw["on"])
    if sw:
        tap(sw)
    check("«solo Wi-Fi» se desactiva al tocarlo", (switch_near("Descargar solo con Wi-Fi") or {}).get("on") is False)
    tap_text("Eliminar descarga")
    left = len([t for t in texts() if t == "DESCARGADO"])
    start("descargas", reset=False)
    sw = switch_near("Descargar solo con Wi-Fi")
    check("tras reiniciar «solo Wi-Fi» sigue desactivado", sw is not None and not sw["on"])
    check("tras reiniciar «Actualizar automáticamente» sigue activado", (switch_near("Actualizar automáticamente") or {}).get("on") is True)
    check("la descarga eliminada no vuelve", len([t for t in texts() if t == "DESCARGADO"]) <= left, "quedan %d" % len([t for t in texts() if t == "DESCARGADO"]))

    # Reservas
    start("reservas")
    check("demo: 3 reservas · 1 pendiente", find("3 reservas · 1 pendiente") is not None)
    start("reservar/albufera-barca", reset=False)
    tap_text("Confirmar reserva")
    time.sleep(0.8)
    locator = next((t for t in texts() if re.fullmatch(r"TV-\d{4}-\d{4}", t)), "")
    check("la reserva nueva sale con su localizador (TV-…-0416)", locator.endswith("0416"), locator)
    start("reservas", reset=False)
    check("tras reiniciar hay 4 reservas", find("4 reservas · 1 pendiente") is not None)
    start("perfil", reset=False)
    check("Perfil cuenta 4 reservas", find("4 reservas") is not None)

    # Ajustes
    tap_text("Tamaño del texto")
    time.sleep(0.6)
    tap_text("Grande")
    time.sleep(0.8)
    check("tamaño del texto: Grande", find("Grande") is not None)
    start("perfil", reset=False)
    check("tras reiniciar el tamaño del texto sigue en Grande", find("Grande") is not None)

    # Búsquedas recientes
    start("busqueda")
    check("demo: recientes «Mercado Central» y «Fallas 2027»", find("Mercado Central") is not None and find("Fallas 2027") is not None)
    adb("shell", "input", "text", "bioparc")
    time.sleep(1.2)
    tap_text("Bioparc")
    time.sleep(1.0)
    start("busqueda", reset=False)
    ts = texts()
    check("la búsqueda hecha aparece la primera en recientes", "Bioparc" in ts or "bioparc" in ts, str([t for t in ts[:6]]))

    # Avisos sin leer
    start("inicio")
    check("demo: la campana tiene avisos sin leer", bell_dot())
    start("notificaciones", reset=False)
    tap_text("Mascletà a las 14:00")  # abre la ficha de Fallas y lo marca como leído
    start("inicio", reset=False)
    check("leído 1 de 2 avisos y tras reiniciar la campana sigue con punto", bell_dot())
    start("notificaciones", reset=False)
    tap_text("Reserva confirmada")  # abre Mis reservas y lo marca como leído
    start("inicio", reset=False)
    check("leídos los 2 avisos y tras reiniciar no hay punto en la campana", not bell_dot())

    # Bienvenida y cerrar sesión
    start(None)
    time.sleep(2.2)
    check("primer arranque: se muestra la bienvenida", find("Saltar") is not None)
    tap_text("Saltar")
    time.sleep(1.2)
    tap_text("Mientras se usa la app")
    accept_location_dialog()
    time.sleep(1.0)
    check("terminada la bienvenida se entra en Inicio", find("Buscar en Valencia…") is not None)
    start(None, reset=False)
    time.sleep(1.5)
    ns = nodes()
    check("al reabrir no se repite la bienvenida", find("Buscar en Valencia…", ns=ns) is not None and find("Saltar", ns=ns) is None)
    start("perfil", reset=False)
    swipe_up(2)
    tap_text("Cerrar sesión")
    time.sleep(0.8)
    tap_last("Cerrar sesión")  # el botón del diálogo, no la fila de debajo
    time.sleep(3.0)
    check("cerrar sesión vuelve a la bienvenida", find("Saltar") is not None)
    start(None, reset=False)
    time.sleep(2.2)
    check("y al reabrir sigue la bienvenida (se guardó)", find("Saltar") is not None)
    start(None)  # deja la app como un primer arranque


PERMISSIONS = ("android.permission.ACCESS_FINE_LOCATION", "android.permission.ACCESS_COARSE_LOCATION")
# Valencia (Plaza de la Virgen) y la bandeja del emulador en California: con esto se prueba la ubicación real y la de ejemplo.
GEO_VALENCIA = ("-0.3755", "39.4745")


def set_location_permission(granted):
    for permission in PERMISSIONS:
        adb("shell", "pm", "grant" if granted else "revoke", PKG, permission)
    if not granted:
        reset_permission_flags(*PERMISSIONS)


def has_location_permission():
    return "ACCESS_FINE_LOCATION: granted=true" in adb("shell", "dumpsys", "package", PKG)


def set_network(on):
    """Modo avión del sistema: corta o devuelve la conexión del emulador."""
    adb("shell", "cmd", "connectivity", "airplane-mode", "disable" if on else "enable")
    time.sleep(9 if on else 3)


def pix(image):
    return list(getattr(image, "get_flattened_data", image.getdata)())


def wait_for(text, seconds=8.0, exact=True):
    end = time.time() + seconds
    while time.time() < end:
        if find(text, exact=exact):
            return True
        time.sleep(0.7)
    return False


def map_detail(box=(0, 520, 780, 1000)):
    """Desviación típica de la luminosidad de la zona del mapa: un mapa con teselas y marcadores tiene mucha; en blanco, casi nada."""
    return ImageStat.Stat(shot().crop(box).convert("L")).stddev[0]


def count_pixels(img, box, predicate, exclude=None):
    """Píxeles de la zona que cumplen la condición, sin contar los de `exclude` (otra zona dentro de ella)."""
    total = sum(1 for r, g, b in pix(img.crop(box)) if predicate(r, g, b))
    if exclude:
        total -= sum(1 for r, g, b in pix(img.crop(exclude)) if predicate(r, g, b))
    return total


def is_orange(r, g, b):
    return r > 215 and 60 < g < 130 and b < 60


def is_user_blue(r, g, b):
    return b > 190 and r < 90 and 100 < g < 190


def pin_nodes(title, ns=None):
    return [n for n in (ns if ns is not None else nodes()) if n["d"] == title]


def g_mapa():
    print("\n## Mapa real (osmdroid), ubicación y rutas")
    set_location_permission(False)

    # Teselas y marcadores
    start("mapa")
    wait_for("© Colaboradores de OpenStreetMap", 10)
    time.sleep(4)  # teselas
    detail = map_detail()
    check("el mapa muestra teselas de OpenStreetMap (no está en blanco)", detail > 28, "desviación %.1f" % detail)
    check("atribución obligatoria de OpenStreetMap", find("© Colaboradores de OpenStreetMap") is not None)
    ns = nodes()
    titles = ["Lonja de la Seda", "Catedral y Santo Cáliz", "Mercado Central", "Barrio del Carmen", "Iglesia de San Nicolás"]
    found = [t for t in titles if pin_nodes(t, ns)]
    check("los marcadores del centro están sobre el mapa (accesibles por su nombre)", len(found) >= 4, str(found))

    # Seleccionar un marcador
    candidates = pin_nodes("Lonja de la Seda", ns)
    lonja = min(candidates, key=lambda n: n["b"][3] - n["b"][1]) if candidates else None
    if lonja:
        tap(lonja)
        time.sleep(1.5)
        sheet = [n for n in nodes() if n["t"] == "Lonja de la Seda" and n["b"][1] > 1050]
        check("tocar un marcador lo selecciona y abre su hoja", bool(sheet))
    else:
        check("tocar un marcador lo selecciona y abre su hoja", False, "no hay marcador de la Lonja")

    # Zoom
    before = shot().crop((0, 520, 780, 1000))
    tap_text("Acercar")
    time.sleep(2.5)
    after = shot().crop((0, 520, 780, 1000))
    changed = sum(abs(a - b) for a, b in zip(pix(before.convert("L").resize((40, 20))), pix(after.convert("L").resize((40, 20)))))
    check("los botones de zoom cambian el mapa", changed > 2500, "diferencia %d" % changed)

    # Hojas con distancias reales: desde el centro de Valencia (emulador fuera de Valencia)
    start("mapa")
    time.sleep(4)
    meters = [int(m.group(1)) for t in texts() for m in [re.match(r"^(\d+) m · \d+ min$", t)] if m]
    check("hoja del Mercado Central: distancia real desde el centro de Valencia", bool(meters) and 400 <= meters[0] <= 520, str(meters))

    # Permiso de ubicación desde el botón del mapa (diálogo real del sistema)
    start("mapa")
    time.sleep(3)
    tap_text("Centrar en mi ubicación")
    check("«Centrar en mi ubicación» sin permiso abre el diálogo del sistema", wait_for("While using the app", 6))
    tap_text("While using the app")
    time.sleep(2)
    check("al aceptar queda concedido el permiso de ubicación", has_location_permission())

    # Ubicación real dentro de Valencia
    adb("emu", "geo", "fix", *GEO_VALENCIA)
    start("mapa", reset=False)
    time.sleep(4)
    tap_text("Centrar en mi ubicación")
    time.sleep(3.5)
    img = shot()
    blue = count_pixels(img, (330, 700, 450, 830), is_user_blue)
    check("con ubicación real el mapa se centra en el punto azul del usuario", blue > 40, "%d píxeles azules" % blue)
    meters = [int(m.group(1)) for t in texts() for m in [re.match(r"^(\d+) m · \d+ min$", t)] if m]
    check("las distancias se recalculan desde la ubicación real", bool(meters) and 200 <= meters[0] <= 400, str(meters))

    # Ruta real (servicio de rutas de OpenStreetMap)
    start("ruta/mercado-central", reset=False)
    wait_for("Has llegado: Mercado Central", 10)
    time.sleep(3)
    ts = texts()
    summary = next((t for t in ts if re.match(r"^\d+ m · llegada \d\d:\d\d$", t)), "")
    check("ruta real: distancia y hora de llegada", bool(summary), summary)
    check("ruta real: pasos con calles de verdad", any(w in t for t in ts for w in ("Plaça", "Avinguda", "Carrer", "Gira", "Sigue")), str([t for t in ts if "Gira" in t or "Sigue" in t][:3]))
    check("ruta real: termina con la llegada", "Has llegado: Mercado Central" in ts)
    destination = pin_nodes("Mercado Central")
    pin_box = tuple(destination[0]["b"]) if destination else None
    orange = count_pixels(shot(), (0, 470, 780, 860), is_orange, exclude=pin_box)
    check("la ruta se dibuja sobre el mapa (puntos naranjas fuera del marcador)", orange > 150, "%d píxeles naranjas" % orange)
    walk = next((t for t in ts if re.match(r"^\d+ min$", t)), "")
    bike_tab = [n for n in nodes() if re.match(r"^\d+ min$", n["t"]) and 250 < n["b"][0] < 330 and n["b"][1] < 450]
    if bike_tab:
        tap(bike_tab[0])
        bike_big = next((n["t"] for n in nodes() if re.match(r"^\d+ min$", n["t"]) and n["b"][1] > 600), "")
        check("la pestaña de bici muestra su propia duración", bool(bike_big) and bike_big == bike_tab[0]["t"], "%s / %s" % (walk, bike_big))
    else:
        check("la pestaña de bici muestra su propia duración", False, "sin pestaña")

    # Sin conexión: el mapa y la ruta no se rompen y la ruta pasa a ser aproximada
    set_network(False)
    start("ruta/mercado-central", reset=False)
    time.sleep(6)
    check("sin conexión la ruta es aproximada y avisa", find("Ruta aproximada: el servicio de rutas no responde") is not None)
    check("sin conexión se sigue viendo el recorrido y los pasos", find("Has llegado: Mercado Central") is not None)
    start("mapa", reset=False)
    time.sleep(3)
    check("sin conexión el mapa sigue funcionando", find("© Colaboradores de OpenStreetMap") is not None and len(pin_nodes("Mercado Central")) > 0)
    set_network(True)

    # Perfil: ubicación (diálogo y ajuste)
    set_location_permission(False)
    start("perfil")
    tap_text("Ubicación")
    tap_text("No permitir")
    check("Perfil: elegir «No permitir» se guarda", find("No permitir") is not None)
    start("perfil", reset=False)
    check("…y sigue así al reiniciar", find("No permitir") is not None)
    tap_text("Ubicación")
    tap_text("Solo esta vez")
    check("Perfil: elegir «Solo esta vez» sin permiso abre el diálogo del sistema", wait_for("Only this time", 6))
    tap_text("Only this time")
    time.sleep(2)
    check("…y al aceptar queda «Solo esta vez»", find("Solo esta vez") is not None and has_location_permission())

    # Bienvenida: la pantalla de permiso lanza la petición real
    set_location_permission(False)
    start(None)
    time.sleep(2.5)
    tap_text("Saltar")
    time.sleep(1.2)
    tap_text("Mientras se usa la app")
    check("Bienvenida: «Mientras se usa la app» abre el diálogo del sistema", wait_for("While using the app", 6))
    tap_text("While using the app")
    time.sleep(2)
    check("…y al aceptar se entra en Inicio con el permiso concedido", wait_for("Buscar en Valencia…", 6) and has_location_permission())
    start(None)  # deja la app como un primer arranque


NOTIFICATION_PERMISSION = "android.permission.POST_NOTIFICATIONS"
REMINDER_ROW = "Avisarme de la mascletà"


def reset_permission_flags(*permissions):
    """`pm revoke` cuenta como una decisión del usuario: con esa marca el primer «No permitir» ya es el segundo para Android y
    el sistema deja de preguntar. Se borran las marcas para volver al estado de un permiso que nunca se ha pedido."""
    for permission in permissions:
        adb("shell", "pm", "clear-permission-flags", PKG, permission, "user-set", "user-fixed")


def set_notification_permission(granted):
    adb("shell", "pm", "grant" if granted else "revoke", PKG, NOTIFICATION_PERMISSION)
    if not granted:
        reset_permission_flags(NOTIFICATION_PERMISSION)


def has_notification_permission():
    return "POST_NOTIFICATIONS: granted=true" in adb("shell", "dumpsys", "package", PKG)


def app_alarms():
    """Alarmas pendientes de la app (hora de inicio de la ventana en UTC), de `dumpsys alarm`."""
    out = adb("shell", "dumpsys", "alarm")
    pattern = r"Alarm\{\w+ type \d+ origWhen (\d+) whenElapsed -?\d+ " + re.escape(PKG)
    return sorted(datetime.fromtimestamp(int(m.group(1)) / 1000, timezone.utc) for m in re.finditer(pattern, out))


def wait_alarms(count, seconds=8.0):
    """Espera a que haya `count` alarmas de la app (se programan en segundo plano al cambiar un ajuste o arrancar la app)."""
    end = time.time() + seconds
    alarms = app_alarms()
    while len(alarms) != count and time.time() < end:
        time.sleep(0.6)
        alarms = app_alarms()
    return alarms


def is_mascleta_window(alarm, now=None):
    """Ventana de la mascletà: del 1 al 19 de marzo a las 13:20 de Valencia (12:20 UTC, es invierno), dentro del próximo año."""
    now = now or datetime.now(timezone.utc)
    return alarm.month == 3 and 1 <= alarm.day <= 19 and (alarm.hour, alarm.minute, alarm.second) == (12, 20, 0) and now < alarm < now + timedelta(days=400)


def reminder_notification_posted():
    return "tag=event-reminder" in adb("shell", "dumpsys", "notification", "--noredact")


def set_clock(moment):
    """Cambia la hora del emulador (UTC). Siempre hay que llamar después a restore_clock()."""
    adb("shell", "settings", "put", "global", "auto_time", "0")
    adb("shell", "settings", "put", "global", "auto_time_zone", "0")
    adb("shell", "cmd", "alarm", "set-time", str(int(moment.timestamp() * 1000)))


def restore_clock():
    adb("shell", "cmd", "alarm", "set-time", str(int(time.time() * 1000)))
    adb("shell", "settings", "put", "global", "auto_time", "1")
    adb("shell", "settings", "put", "global", "auto_time_zone", "1")


def focused_window():
    out = adb("shell", "dumpsys", "window")
    return next((l.strip() for l in out.split("\n") if "mCurrentFocus" in l), "")


def wait_focus(package, seconds=8.0):
    end = time.time() + seconds
    while time.time() < end:
        if package in focused_window():
            return True
        time.sleep(0.6)
    return False


def tap_system_button(*labels, seconds=6.0):
    """Pulsa un botón de un diálogo del sistema (en inglés en el emulador); el apóstrofo de «Don’t allow» no es el del teclado."""
    wanted = [l.replace("’", "'") for l in labels]
    end = time.time() + seconds
    while time.time() < end:
        for n in nodes():
            if n["t"].replace("’", "'") in wanted:
                tap(n)
                return True
        time.sleep(0.6)
    return False


def wait_system_dialog(text, seconds=6.0):
    end = time.time() + seconds
    while time.time() < end:
        if any(text in t for t in texts()):
            return True
        time.sleep(0.6)
    return False


def reminder_switch():
    """Interruptor del aviso de la ficha de Fallas (baja por la pantalla hasta verlo)."""
    scroll_to(REMINDER_ROW)
    return switch_near(REMINDER_ROW)


def reminder_on():
    return (reminder_switch() or {}).get("on")


def welcome_to_home():
    """Recorre la bienvenida (con el permiso de ubicación del sistema) hasta Inicio; deja `onboardingDone` guardado."""
    start(None)
    time.sleep(2.2)
    tap_text("Saltar")
    time.sleep(1.2)
    tap_text("Mientras se usa la app")
    accept_location_dialog()
    return wait_for("Buscar en Valencia…", 8)


def g_avisos():
    print("\n## Avisos: permiso de notificaciones, aviso de la mascletà, alarmas, notificación y ajustes del sistema")
    set_location_permission(True)

    # --- Sin permiso: el aviso sale apagado y pide el permiso al encenderlo
    set_notification_permission(False)
    start("detalle/fallas")
    check("sin permiso de notificaciones el aviso sale apagado", reminder_on() is False)
    check("…y no hay ninguna alarma programada", app_alarms() == [])
    tap(reminder_switch())
    check("al encenderlo se abre el diálogo del sistema", wait_system_dialog("send you notifications"))
    check("«No permitir»", tap_system_button("Don't allow"))
    time.sleep(1.2)
    check("tras decir que no sigue apagado y sin alarma", reminder_on() is False and app_alarms() == [])
    tap(reminder_switch())
    check("el sistema deja preguntar una segunda vez", wait_system_dialog("send you notifications"))
    tap_system_button("Don't allow")
    check("tras el segundo no, la app explica cómo activarlas en Ajustes", wait_for("Notificaciones desactivadas", 5))
    check("…con el botón «Abrir ajustes»", find("Abrir ajustes") is not None)
    tap_text("Abrir ajustes")
    check("«Abrir ajustes» abre los ajustes de notificaciones de la app", wait_focus("com.android.settings") and wait_for("Turismo Valencia", 5, exact=False))
    # El usuario las activa allí (se simula con `pm grant`) y vuelve a la app: el aviso que pidió se enciende solo.
    set_notification_permission(True)
    back()
    time.sleep(1.5)
    check("al volver con el permiso dado el aviso pedido está encendido", reminder_on() is True)
    alarms = wait_alarms(1)
    check("…y la alarma de la mascletà queda programada (13:20 de Valencia)", len(alarms) == 1 and is_mascleta_window(alarms[0]), str(alarms))
    check("la alarma es una ventana de 10 minutos", "window=+10m0s0ms" in adb("shell", "dumpsys", "alarm"))

    # --- Con el diálogo del sistema: permitir
    set_notification_permission(False)
    start("detalle/fallas")
    check("sin permiso (de nuevo) el aviso sale apagado", reminder_on() is False)
    tap(reminder_switch())
    wait_system_dialog("send you notifications")
    tap_system_button("Allow")
    time.sleep(1.5)
    check("al permitir se concede el permiso del sistema", has_notification_permission())
    check("…y el aviso queda encendido", reminder_on() is True)
    alarms = wait_alarms(1)
    check("…con la alarma de la próxima mascletà", len(alarms) == 1 and is_mascleta_window(alarms[0]), str(alarms))

    # --- Persistencia
    start("detalle/fallas", reset=False)
    check("al cerrar y abrir la app el aviso sigue encendido", reminder_on() is True)
    check("…y la alarma se vuelve a programar al arrancar (el cierre forzoso las borra)", len(wait_alarms(1)) == 1)
    tap(reminder_switch())
    time.sleep(1.2)
    check("apagar el aviso lo apaga", reminder_on() is False)
    check("…y cancela la alarma", len(wait_alarms(0)) == 0)
    start("detalle/fallas", reset=False)
    check("apagado, tras reiniciar sigue apagado y sin alarma", reminder_on() is False and app_alarms() == [])
    tap(reminder_switch())
    time.sleep(1.2)
    check("con el permiso ya dado, encenderlo no pregunta más", reminder_on() is True and not any("send you notifications" in t for t in texts()))
    check("…y programa la alarma", len(wait_alarms(1)) == 1)

    # --- Perfil: el ajuste general de Notificaciones manda sobre todos los avisos
    start("perfil", reset=False)
    check("Perfil: Notificaciones está activado", (switch_near("Notificaciones") or {}).get("on") is True)
    tap(switch_near("Notificaciones"))
    time.sleep(1.2)
    check("Perfil: al apagarlo se cancela la alarma", (switch_near("Notificaciones") or {}).get("on") is False and len(wait_alarms(0)) == 0)
    start("detalle/fallas", reset=False)
    check("…y en la ficha de Fallas el aviso sale apagado", reminder_on() is False)
    start("perfil", reset=False)
    tap(switch_near("Notificaciones"))
    time.sleep(1.2)
    check("Perfil: al volver a encenderlo se programa otra vez", (switch_near("Notificaciones") or {}).get("on") is True and len(wait_alarms(1)) == 1)
    set_notification_permission(False)
    start("perfil", reset=False)
    check("Perfil: sin el permiso del sistema Notificaciones sale apagado", (switch_near("Notificaciones") or {}).get("on") is False)
    tap(switch_near("Notificaciones"))
    check("…y al encenderlo pide el permiso", wait_system_dialog("send you notifications"))
    tap_system_button("Allow")
    time.sleep(1.5)
    check("…y al permitir queda encendido con su alarma", (switch_near("Notificaciones") or {}).get("on") is True and len(wait_alarms(1)) == 1)

    # --- El aviso salta de verdad: se adelanta el reloj al final de la ventana de la alarma
    check("bienvenida completa hasta Inicio", welcome_to_home())
    alarms = wait_alarms(1, 10)
    first = alarms[0] if alarms else None
    check("tras la bienvenida la mascletà vuelve a tener su alarma (la demo trae el aviso activado)", first is not None and is_mascleta_window(first), str(alarms))
    posted = False
    if first:
        try:
            set_clock(first + timedelta(minutes=9, seconds=40))
            end = time.time() + 70
            while time.time() < end and not posted:
                posted = reminder_notification_posted()
                time.sleep(2)
            check("la alarma salta con la ventana y publica la notificación", posted)
            nxt = wait_alarms(1, 10)
            check("…y programa el aviso del día siguiente", len(nxt) == 1 and nxt[0] == first + timedelta(days=1), str(nxt))
            adb("shell", "cmd", "statusbar", "expand-notifications")
            time.sleep(2)
            ts = texts()
            check("la notificación dice «Mascletà a las 14:00»", "Mascletà a las 14:00" in ts)
            check("…con la plaza y la antelación", any("Plaza del Ayuntamiento" in t and "30 min" in t for t in ts))
            adb("shell", "cmd", "statusbar", "collapse")
            time.sleep(1)
            # Arranque en frío tocando la notificación: la app abre Inicio y encima la ficha de Fallas.
            adb("shell", "input", "keyevent", "KEYCODE_HOME")
            time.sleep(1)
            adb("shell", "am", "kill", PKG)
            time.sleep(1)
            adb("shell", "cmd", "statusbar", "expand-notifications")
            time.sleep(2)
            note = next((n for n in nodes() if n["t"] == "Mascletà a las 14:00"), None)
            check("la notificación sigue en la bandeja con la app cerrada", note is not None)
            if note:
                tap(note)
                time.sleep(3)
                check("tocarla abre la ficha de Fallas", wait_for("Les Falles de València", 6))
                check("…y la notificación se quita al tocarla", not reminder_notification_posted())
                back()
                check("«Atrás» lleva a Inicio (no sale de la app)", wait_for("Buscar en Valencia…", 5))
        finally:
            restore_clock()

    # --- Al reiniciar el móvil las alarmas se vuelven a programar sin abrir la app
    start("detalle/fallas", reset=False)  # la app queda abierta (no «detenida»), como la deja el usuario
    check("antes de reiniciar hay una alarma", len(wait_alarms(1)) == 1)
    adb("shell", "input", "keyevent", "KEYCODE_HOME")
    adb("reboot")
    time.sleep(15)
    end = time.time() + 240
    booted = False
    while time.time() < end and not booted:
        booted = adb("shell", "getprop", "sys.boot_completed").strip() == "1"
        time.sleep(3)
    check("el emulador ha vuelto a arrancar", booted)
    adb("shell", "wm", "size", "780x1688")
    adb("shell", "wm", "density", "320")
    alarms = wait_alarms(1, 90)
    check("tras reiniciar, sin abrir la app, la alarma se ha vuelto a programar (BootReceiver)", len(alarms) == 1 and is_mascleta_window(alarms[0]), str(alarms))

    # --- Ubicación: si el sistema ya no pregunta, la app lleva a Ajustes
    set_location_permission(False)
    start("perfil")
    tap_text("Ubicación")
    time.sleep(0.8)
    tap_last("Mientras se usa la app")
    check("Perfil → Ubicación abre el diálogo del sistema", wait_system_dialog("access this device"))
    tap_system_button("Don't allow")
    time.sleep(1.2)
    check("tras el primer no no hay aviso de Ajustes", find("Ubicación desactivada") is None)
    tap_text("Ubicación")
    time.sleep(0.8)
    tap_last("Mientras se usa la app")
    wait_system_dialog("access this device")
    tap_system_button("Don't allow")
    check("tras el segundo no la app explica cómo permitir la ubicación", wait_for("Ubicación desactivada", 5))
    tap_text("Abrir ajustes")
    check("«Abrir ajustes» abre los ajustes de la app", wait_focus("com.android.settings") and wait_for("Turismo Valencia", 5, exact=False))
    back()
    time.sleep(1)
    start("mapa", reset=False)
    wait_for("© Colaboradores de OpenStreetMap", 8)
    tap_text("Centrar en mi ubicación")
    check("Mapa: «Centrar en mi ubicación» sin poder preguntar lleva también a Ajustes", wait_for("Ubicación desactivada", 5))
    tap_text("Ahora no")
    check("«Ahora no» cierra el aviso", wait_for("Ubicación desactivada", 1.5) is False)
    set_location_permission(True)
    start(None)  # deja la app como un primer arranque


GROUPS = {"chips": g_chips, "cats": g_cats, "tiles": g_tiles, "contacto": g_contacto, "guias": g_guias, "detalles": g_detalles, "paginas": g_paginas, "busqueda": g_busqueda, "persist": g_persist, "mapa": g_mapa, "avisos": g_avisos}
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
