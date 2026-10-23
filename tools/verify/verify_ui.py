"""Pruebas de interfaz en el emulador (780x1688 px / 320 dpi): navegación, contenido de la web y fotos cargadas.
Uso: python verify_ui.py [grupo ...]   (grupos: chips cats tiles contacto guias detalles paginas busqueda persist mapa avisos ofertas reservas cancelar refresco privacidad idiomas oscuro tema)
Los grupos `reservas` y `cancelar` necesitan el backend local (cd backend && npm start, con Docker Desktop) y la app instalada con su
dirección; si no lo encuentran, se saltan con un aviso.
Cada arranque restablece los datos de demostración (`--ez resetData true`) para que las pruebas no dependan de lo que
guardó la anterior; `start(..., reset=False)` reabre la app conservando lo guardado.
Todos los grupos menos `idiomas` esperan la app en español: `start` le pide el idioma con `--es lang ES` (`lang=None` no lo toca) y
al empezar se fija el idioma de la app en el sistema (`cmd locale set-app-locales`), porque el emulador está en inglés."""
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
    # `uiautomator dump` falla si la pantalla no llega a estar quieta (el cursor parpadeando de un campo de texto, una animación): se
    # borra el volcado anterior, para no leer sin querer el de la pantalla de antes, y se reintenta.
    root = None
    for attempt in range(4):
        adb("shell", "rm -f /sdcard/ui.xml; uiautomator dump /sdcard/ui.xml")
        xml = adb("exec-out", "cat", "/sdcard/ui.xml", binary=True).decode("utf-8", "replace")
        try:
            root = ET.fromstring(xml)
            break
        except ET.ParseError:
            time.sleep(0.4)
    out = []
    if root is None:
        return out
    for n in root.iter("node"):
        m = re.match(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", n.get("bounds") or "")
        if not m:
            continue
        out.append({
            "t": n.get("text") or "",
            "d": n.get("content-desc") or "",
            "c": n.get("class") or "",
            "b": tuple(map(int, m.groups())),
            "chk": n.get("checkable") == "true",
            "on": n.get("checked") == "true",
            "en": n.get("enabled") == "true",
            "clk": n.get("clickable") == "true",
        })
    return out


def texts(ns=None):
    return [n["t"] for n in (ns if ns is not None else nodes()) if n["t"]]


def edit_fields(ns=None):
    """Los campos de texto de la pantalla (también los vacíos, que no tienen texto por el que buscarlos), de arriba abajo."""
    return sorted((n for n in (ns if ns is not None else nodes()) if n["c"].endswith("EditText")), key=lambda n: n["b"][1])


def clear_field(field, chars=40):
    """Vacía un campo de texto: lo pulsa, va al final y borra hacia atrás."""
    tap(field)
    adb("shell", "input", "keyevent", "KEYCODE_MOVE_END")
    adb("shell", "input", "keyevent", *["KEYCODE_DEL"] * chars)
    time.sleep(0.5)


def shot():
    return Image.open(io.BytesIO(adb("exec-out", "screencap", "-p", binary=True))).convert("RGB")


def start(route=None, *extra, reset=True, lang="ES"):
    adb("shell", "am", "force-stop", PKG)
    # Un diálogo de permisos que quedara abierto de la prueba anterior taparía la app.
    adb("shell", "am", "force-stop", "com.google.android.permissioncontroller")
    args = ["shell", "am", "start", "-n", PKG + "/.MainActivity"]
    if reset:
        args += ["--ez", "resetData", "true"]
    if route:
        args += ["--es", "route", route]
    if lang:
        args += ["--es", "lang", lang]
    adb(*args, *extra)
    time.sleep(3.2)
    for _ in range(8):  # espera a que la pantalla esté pintada
        if len(nodes()) > 12:
            break
        time.sleep(0.8)


def type_text(text, chunk=3, pause=0.18):
    """Escribe en el campo enfocado (`input text`) en trozos pequeños: los campos de la app guardan el texto en un `ViewModel` y, si llegan
    las letras todas de golpe, alguna se pierde (el emulador no es tan rápido como para devolver el estado entre una letra y la siguiente)."""
    time.sleep(0.9)  # a que el campo tenga el foco y el teclado esté abierto: las letras que llegan antes se pierden
    text = text.replace(" ", "%s")
    parts, i = [], 0
    while i < len(text):
        step = chunk + (2 if text[i:i + 2] == "%s" else 0)  # no partir «%s» (el espacio) entre dos trozos
        parts.append(text[i:i + step])
        i += step
    for part in parts:
        adb("shell", "input", "text", part)
        time.sleep(pause)


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


def control(text, ns=None):
    """El control pulsable (botón, chip) que contiene el texto. Su estado (activado, marcado) vive en él y no en el texto."""
    ns = ns if ns is not None else nodes()
    label = find(text, ns=ns)
    if not label:
        return None
    cx, cy = (label["b"][0] + label["b"][2]) // 2, (label["b"][1] + label["b"][3]) // 2
    # Un chip ya elegido deja de ser pulsable para Android, pero sigue siendo «marcable».
    around = [n for n in ns if (n["clk"] or n["chk"]) and n["b"][0] <= cx <= n["b"][2] and n["b"][1] <= cy <= n["b"][3]]
    return min(around, key=lambda n: (n["b"][2] - n["b"][0]) * (n["b"][3] - n["b"][1])) if around else None


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
        # Escribir con `input text` pierde alguna letra de vez en cuando (el emulador tiene sus altibajos): hasta dos intentos.
        for attempt in range(2):
            start("busqueda")
            type_text(q)
            time.sleep(1.4)
            ns = nodes()
            if want is None or find(want, ns=ns) is not None:
                break
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

    # Reservas (las de demostración: crear una de verdad necesita el servidor y lo prueba el grupo `reservas`)
    start("reservas")
    check("demo: 3 reservas · 1 pendiente", find("3 reservas · 1 pendiente") is not None)
    start("reservas", reset=False)
    check("tras reiniciar siguen las 3 reservas · 1 pendiente", find("3 reservas · 1 pendiente") is not None)
    start("perfil", reset=False)
    check("Perfil cuenta 3 reservas", find("3 reservas") is not None)

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
    type_text("bioparc")
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
    # Cerrar sesión borra lo del usuario: terminada la bienvenida, la app está como en el primer arranque (con la demostración).
    tap_text("Saltar")
    time.sleep(1.2)
    tap_text("Mientras se usa la app")
    accept_location_dialog()
    time.sleep(1.0)
    start("favoritos", reset=False)
    check("cerrar sesión borró lo del usuario: los favoritos vuelven a ser los 6 de la demostración", find("6 lugares guardados") is not None)
    start("perfil", reset=False)
    check("…y el tamaño del texto vuelve al del sistema (ya no es «Grande»)", find("Grande") is None)
    start("busqueda", reset=False)
    ts = texts()
    check("…y la búsqueda «Bioparc» ya no está entre las recientes", "Bioparc" not in ts and "bioparc" not in ts, str(ts[:8]))
    start("inicio", reset=False)
    check("…y la campana vuelve a tener avisos sin leer", bell_dot())
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
    adb("reverse", "tcp:54321", "tcp:54321")  # el reinicio borra el puente al backend local (si la app apunta a él)
    alarms = wait_alarms(1, 90)
    check("tras reiniciar, sin abrir la app, la alarma se ha vuelto a programar (BootReceiver)", len(alarms) == 1 and is_mascleta_window(alarms[0]), str(alarms))
    # Justo después de arrancar el sistema aún está ocupado (actualiza e indexa): pantalla encendida y sin bloqueo, el lanzador en
    # primer plano y unos segundos de calma antes de volver a pedir permisos.
    adb("shell", "input", "keyevent", "KEYCODE_WAKEUP")
    adb("shell", "wm", "dismiss-keyguard")
    wait_focus("nexuslauncher", 60)
    time.sleep(20)

    # --- Ubicación: si el sistema ya no pregunta, la app lleva a Ajustes
    # El diálogo del sistema es lo único que depende de un servicio ajeno a la app: si no sale a la primera se repite (hasta 3 veces)
    # y cada intento fallido cuenta qué había en pantalla.
    opened = False
    for attempt in range(1, 4):
        set_location_permission(False)
        start("perfil")
        tap_text("Ubicación")
        time.sleep(0.8)
        tap_last("Mientras se usa la app")
        opened = wait_system_dialog("access this device", 8)
        if opened:
            break
        print("   intento %d sin diálogo: foco %s · textos %s" % (attempt, focused_window().strip(), texts()[:8]), flush=True)
    check("Perfil → Ubicación abre el diálogo del sistema", opened)
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
    opened = wait_focus("com.android.settings", 20) and wait_for("Turismo Valencia", 12, exact=False)  # los Ajustes arrancan en frío tras el reinicio del emulador
    check("«Abrir ajustes» abre los ajustes de la app", opened, "" if opened else "foco %s · textos %s" % (focused_window().strip(), texts()[:8]))
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


def offers_log(want, seconds=12.0):
    """Línea del registro de la app sobre el catálogo de ofertas («actualizado», «sin cambios (304)» o «No se pudo…») que contiene
    `want`; espera a que salga (al abrir con `resetData` hay dos peticiones seguidas, la del arranque y la que sigue al borrado, y
    según lo rápido que conteste el servidor la 304 puede salir antes o después de la descarga) y, si no sale, devuelve la última."""
    end = time.time() + seconds
    last = ""
    while time.time() < end:
        lines = [l.split("TurismoDb:")[-1].strip() for l in adb("shell", "logcat", "-d", "-s", "TurismoDb:V").splitlines() if "ofertas" in l]
        if lines:
            last = lines[-1]
            hit = next((l for l in lines if want in l), None)
            if hit:
                return hit
        time.sleep(0.8)
    return last


def app_db(*queries):
    """Resultado de cada consulta sobre la base de datos de la app (build de depuración, con run-as), como lista de filas."""
    import sqlite3
    import tempfile

    folder = tempfile.mkdtemp(prefix="tv-db-")
    for suffix in ("", "-wal", "-shm"):
        data = adb("exec-out", "run-as", PKG, "cat", "databases/turismo.db" + suffix, binary=True)
        if data:
            with open(os.path.join(folder, "turismo.db" + suffix), "wb") as f:
                f.write(data)
    con = sqlite3.connect(os.path.join(folder, "turismo.db"))
    try:
        return [con.execute(query).fetchall() for query in queries]
    finally:
        con.close()


def app_offers_db():
    """(ofertas, versión) de la caché de ofertas guardada en la app; la versión es None si no hay nada guardado."""
    offers, meta = app_db("SELECT id, priceFromCents FROM offers ORDER BY position", "SELECT etag, fetchedAt FROM cache_meta WHERE name = 'offers'")
    return offers, (meta[0] if meta else None)


def g_ofertas():
    print("\n## Ofertas del servidor: caché en Room y respaldo sin conexión")
    # Si la app de depuración apunta al backend local, necesita el puente de `adb reverse` (un reinicio del emulador, como el de `avisos`,
    # lo borra); con la nube no hace ninguna falta y no estorba.
    adb("reverse", "tcp:54321", "tcp:54321")
    set_network(True)
    # Primer arranque con conexión: pide el catálogo al servidor y lo guarda con su versión.
    adb("shell", "logcat", "-c")
    start("entradas?tab=experiencias")
    line = offers_log("actualizado")
    check("con conexión se pide el catálogo y se guarda", "actualizado" in line, line)
    offers, meta = app_offers_db()
    check("la caché tiene las ofertas del servidor, en su orden y con su versión", len(offers) >= 16 and offers[0][0] == "albufera-barca" and bool(meta and meta[0]), "%d ofertas · %s" % (len(offers), meta and meta[0]))
    ts = texts()
    check("la lista pinta las ofertas del servidor", "Paseo en barca por la Albufera" in ts and "18 €" in ts and "Taller de paella valenciana" in ts)
    first_check = meta[1] if meta else 0

    # Segundo arranque conservando lo guardado: el servidor responde 304 (el ETag débil de la pasarela vuelve tal cual).
    adb("shell", "logcat", "-c")
    start("entradas?tab=experiencias", reset=False)
    line = offers_log("sin cambios")
    check("al reabrir, el servidor confirma que sigue vigente (304)", "sin cambios" in line, line)
    offers_again, meta_again = app_offers_db()
    check("sigue el mismo catálogo y se anota la comprobación", offers_again == offers and bool(meta_again) and meta_again[0] == meta[0] and meta_again[1] > first_check)

    # Sin conexión y con la caché guardada: la lista sale igual.
    set_network(False)
    start("entradas?tab=experiencias", reset=False)
    ts = texts()
    check("sin conexión se ve el catálogo guardado", "Paseo en barca por la Albufera" in ts and "Taller de paella valenciana" in ts)
    check("sin conexión no se pierde la caché", app_offers_db()[0] == offers)

    # Sin conexión y sin nada guardado (primer arranque): el catálogo incluido en la app.
    start("entradas?tab=experiencias")
    empty, no_meta = app_offers_db()
    check("sin nada guardado no hay caché", empty == [] and no_meta is None)
    ts = texts()
    check("pero se ve el catálogo incluido en la app (Experiencias)", "Paseo en barca por la Albufera" in ts and "Taller de paella valenciana" in ts)
    check("el catálogo incluido en la app tiene sus tres pestañas", tap_text("Tours") and "Ruta guiada Ciutat Vella" in texts() and tap_text("Entradas") and "Valencia Tourist Card 72 h" in texts())

    # Vuelve la conexión: el catálogo del servidor llega solo, sin reiniciar la app.
    adb("shell", "logcat", "-c")
    set_network(True)
    line = offers_log("actualizado", seconds=20)
    check("al volver la conexión se pide el catálogo", "actualizado" in line, line)
    check("y se guarda", len(app_offers_db()[0]) >= 16)
    start(None)  # deja la app como un primer arranque


LOCAL_API = "http://127.0.0.1:54321/functions/v1/api-v1"
LOCAL_APP_HINT = "instala la app con ./gradlew installDebug -PapiBaseUrl=" + LOCAL_API + "/ y deja `adb reverse tcp:54321 tcp:54321`"


def local_sql(sql):
    """Ejecuta una consulta en la base de datos del backend local (contenedor de Docker `supabase_db_<project_id>`) y devuelve
    las líneas que imprime, con las columnas separadas por «|». None si no se pudo (Docker parado o backend sin arrancar)."""
    config = open(os.path.join(REPO, "backend", "supabase", "config.toml"), encoding="utf-8").read()
    container = "supabase_db_" + re.search(r'^project_id\s*=\s*"([^"]+)"', config, re.M).group(1)
    try:
        r = subprocess.run(["docker", "exec", container, "psql", "-U", "postgres", "-t", "-A", "-F", "|", "-c", sql], capture_output=True, timeout=30)
    except (OSError, subprocess.TimeoutExpired):
        return None
    if r.returncode != 0:
        return None
    return [line for line in r.stdout.decode("utf-8", "replace").splitlines() if line.strip()]


def local_api_up():
    import urllib.request

    try:
        with urllib.request.urlopen(LOCAL_API + "/offers", timeout=5) as response:
            return response.status == 200
    except OSError:
        return False


def clean_local_backend():
    """Deja el backend local como lo necesitan las pruebas: sin contadores de límites de peticiones y sin las reservas que dejaron
    ellas mismas (las del correo de demostración de la app, las precargadas y todas las de la oferta de pruebas), que irían
    llenando el aforo de cada horario (la barca de la Albufera tiene 12 plazas por horario y día)."""
    local_sql("delete from app.rate_limits")
    local_sql("delete from app.reservations where email in ('laura.garcia@example.com', 'precarga@example.com', 'enlace@example.com') or offer_id = 'prueba-aforo'")


def app_reservations_db():
    """(reservas, claves de intentos pendientes, deviceId) guardados en la base de datos de la app. Una reserva es
    (localizador, oferta, hora, adultos, niños, correo, céntimos, estado), la más reciente primero."""
    rows, attempts, device = app_db(
        "SELECT locator, offerId, time, adults, children, email, totalCents, status FROM reservations ORDER BY createdAt DESC, locator DESC",
        "SELECT idempotencyKey FROM reservation_attempts",
        "SELECT deviceId FROM settings",
    )
    return rows, [a[0] for a in attempts], (device[0][0] if device else None)


def locator_on_screen(seconds=8.0):
    """Localizador (TV-año-número) de la pantalla de confirmación; espera a que el servidor conteste y la app navegue."""
    end = time.time() + seconds
    while time.time() < end:
        found = next((t for t in texts() if re.fullmatch(r"TV-\d{4}-\d{4,}", t)), "")
        if found:
            return found
        time.sleep(0.7)
    return ""


def ready_local_app():
    """Deja el backend local limpio y la app hablando con él, abierta en Reservar la barca de la Albufera. False (con un aviso
    «SKIP») si el backend no está en marcha o la app no se instaló con su dirección."""
    if not local_api_up() or local_sql("select 1") is None:
        print("SKIP - hace falta el backend local en marcha (cd backend && npm start, con Docker Desktop) y la app: " + LOCAL_APP_HINT)
        return False
    set_network(True)
    adb("reverse", "tcp:54321", "tcp:54321")
    clean_local_backend()
    start("reservar/albufera-barca")
    # La app habla con el backend local si su catálogo trae la oferta de pruebas, que solo existe allí.
    end = time.time() + 12
    while time.time() < end and "prueba-aforo" not in [o[0] for o in app_offers_db()[0]]:
        time.sleep(1.0)
    if "prueba-aforo" not in [o[0] for o in app_offers_db()[0]]:
        print("SKIP - la app no habla con el backend local (su catálogo no trae «prueba-aforo»): " + LOCAL_APP_HINT)
        return False
    start("reservar/albufera-barca", reset=False)
    return True


def g_reservas():
    print("\n## Reservas reales: confirma el servidor, aforo, sin conexión y reintento con la misma clave")
    if not ready_local_app():
        return

    # Reservar con conexión: confirma el servidor y la app guarda lo que él contesta.
    ns = nodes()
    confirm = control("Confirmar reserva", ns)
    check("con conexión no sale el aviso de «necesita conexión» y se puede confirmar", find("Reservar necesita conexión a Internet", ns=ns) is None and bool(confirm and confirm["en"]))
    check("el correo viene rellenado con el de la última reserva", find("laura.garcia@example.com", ns=ns) is not None)
    tap_text("Confirmar reserva")
    locator = locator_on_screen()
    ts = texts()
    check("el servidor confirma y la pantalla enseña su localizador", bool(locator), locator)
    check("con el total (36 €) y el correo", any("36 €" in t for t in ts) and any("laura.garcia@example.com" in t for t in ts))
    server = (local_sql("select total_cents, status, email, adults, children, slot from app.reservations where locator = '%s'" % locator) or [""])[0]
    check("el servidor la tiene guardada: 36 €, confirmada, 2 adultos, 18:30", server == "3600|confirmed|laura.garcia@example.com|2|0|18:30", server)
    rows, attempts, device = app_reservations_db()
    check("la app la guarda con ese localizador y sin intento pendiente", bool(rows) and rows[0][0] == locator and rows[0][6] == 3600 and attempts == [], str(rows[:1]))
    check("el móvil se identifica con un UUID propio", bool(re.fullmatch(r"[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}", device or "")), str(device))
    check("y el servidor la asocia a ese móvil", local_sql("select count(*) from app.reservations where device_id = '%s'" % device) == ["1"])
    start("reservas", reset=False)
    check("Mis reservas: las 3 de demostración y la nueva (4 · 1 pendiente), también tras reiniciar", find("4 reservas · 1 pendiente") is not None)
    start("perfil", reset=False)
    check("Perfil cuenta 4 reservas", find("4 reservas") is not None)

    # Aforo: la oferta de pruebas tiene 3 plazas a las 10:00.
    start("reservar/prueba-aforo", reset=False)
    tap_text("10:00")
    check("se elige el horario de las 10:00", bool((control("10:00") or {}).get("on")))
    tap_text("Confirmar reserva")
    check("aforo 3: la primera reserva de 2 personas entra", bool(locator_on_screen()))
    start("reservar/prueba-aforo", reset=False)
    tap_text("10:00")
    tap_text("Confirmar reserva")
    check("la segunda de 2 personas (solo queda 1 plaza) sale «Ya no quedan plazas»", wait_for("Ya no quedan plazas para ese día y horario. Prueba con otro.", 8))
    rows, attempts, _ = app_reservations_db()
    confirm = control("Confirmar reserva")
    check("se queda en Reservar con el botón activo; no se guarda nada y no queda intento (el fallo es definitivo)", bool(confirm and confirm["en"]) and len(rows) == 5 and attempts == [], "%d reservas, %d intentos" % (len(rows), len(attempts)))
    tap_text("12:00")
    check("al cambiar de horario desaparece el aviso", find("Ya no quedan plazas para ese día y horario. Prueba con otro.") is None)
    tap_text("Confirmar reserva")
    check("a las 12:00 (sin aforo) sí entra", bool(locator_on_screen()))

    # Sin conexión: se pueden ver las reservas guardadas pero no reservar.
    set_network(False)
    start("reservar/albufera-barca", reset=False)
    ns = nodes()
    confirm = control("Confirmar reserva", ns)
    check("sin conexión sale «Reservar necesita conexión a Internet»", find("Reservar necesita conexión a Internet", ns=ns) is not None)
    check("y «Confirmar reserva» está desactivado", bool(confirm) and confirm["en"] is False)
    start("reservas", reset=False)
    check("sin conexión se ven las 6 reservas guardadas", find("6 reservas · 1 pendiente") is not None)
    start("reservar/albufera-barca", reset=False)
    set_network(True)
    time.sleep(2.0)
    ns = nodes()
    confirm = control("Confirmar reserva", ns)
    check("al volver la conexión desaparece el aviso y se puede confirmar sin reabrir", find("Reservar necesita conexión a Internet", ns=ns) is None and bool(confirm and confirm["en"]))

    # Servidor inalcanzable: el intento guarda su clave y el reintento la reutiliza (una sola reserva en el servidor). Se reabre la app
    # con el puente cortado porque una conexión ya abierta (keep-alive) seguiría viva a través de `adb reverse`.
    adb("reverse", "--remove", "tcp:54321")
    start("reservar/albufera-barca", reset=False)
    tap_text("Confirmar reserva")
    check("sin llegar al servidor sale «No hemos podido conectar con el servidor»", wait_for("No hemos podido conectar con el servidor. Comprueba tu conexión e inténtalo de nuevo.", 10))
    rows, attempts, _ = app_reservations_db()
    check("la app guarda la clave del intento y no la reserva", len(attempts) == 1 and len(rows) == 6, "%d reservas, %d intentos" % (len(rows), len(attempts)))
    adb("reverse", "tcp:54321", "tcp:54321")
    tap_text("Confirmar reserva")
    retried = locator_on_screen()
    check("al reintentar con red, la reserva se confirma", bool(retried), retried)
    key = attempts[0] if attempts else ""
    check("con la misma clave: el servidor tiene una sola reserva con ella", local_sql("select count(*) from app.reservations where idempotency_key = '%s'" % key) == ["1"])
    rows, attempts, _ = app_reservations_db()
    check("hecha la reserva, el intento se borra", attempts == [] and len(rows) == 7, "%d reservas, %d intentos" % (len(rows), len(attempts)))

    # El servidor manda: si cancela una reserva, la app la enseña como cancelada al abrir.
    local_sql("update app.reservations set status = 'cancelled' where locator = '%s'" % retried)
    start("reservas", reset=False)
    time.sleep(2.5)
    check("una reserva cancelada en el servidor sale en «Canceladas»", scroll_to("CANCELADAS") is not None)
    check("con su estado «Cancelada»", scroll_to("Cancelada") is not None)
    check("y la app la guarda como cancelada", [r[7] for r in app_reservations_db()[0] if r[0] == retried] == ["Cancelled"])
    start(None)  # deja la app como un primer arranque


def server_status(locator):
    """Estado de una reserva en la base de datos del backend local (confirmed, cancelled…), o None si no existe."""
    rows = local_sql("select status from app.reservations where locator = '%s'" % locator) or []
    return rows[0] if rows else None


def reservation_id(locator):
    """Identificador (UUID) con el que la app guardó la reserva de ese localizador."""
    rows = app_db("SELECT id FROM reservations WHERE locator = '%s'" % locator)[0]
    return rows[0][0] if rows else None


def open_cancel_button():
    """Baja hasta «Cancelar reserva» (queda debajo del billete) y devuelve su control, o None si no hay."""
    scroll_to("Cancelar reserva")
    return control("Cancelar reserva")


def g_cancelar():
    print("\n## Cancelar y recuperar reservas: el servidor libera las plazas, sin conexión y desde otro móvil")
    if not ready_local_app():
        return

    # 1) Reservar y cancelar desde la confirmación
    tap_text("Confirmar reserva")
    locator = locator_on_screen()
    check("hay una reserva confirmada para cancelar", bool(locator), locator)
    check("la confirmación enseña el código (QR) de la reserva", find("Muestra este código en el embarcadero. Disponible sin conexión.") is not None)
    button = open_cancel_button()
    check("ofrece «Cancelar reserva», activo", bool(button and button["en"]))
    tap_text("Cancelar reserva")
    check("pide confirmación antes de cancelar", wait_for("¿Cancelar esta reserva?", 5))
    check("y dice qué se libera", any("Se liberarán tus plazas de «Paseo en barca por la Albufera»" in t for t in texts()))
    tap_text("No, mantenerla")
    time.sleep(1.0)
    check("«No, mantenerla» cierra el diálogo y no cancela nada", find("¿Cancelar esta reserva?") is None and server_status(locator) == "confirmed")
    tap_text("Cancelar reserva")
    wait_for("¿Cancelar esta reserva?", 5)
    tap_text("Sí, cancelar")
    check("el servidor cancela y la pantalla lo dice", wait_for("Reserva cancelada", 10))
    ts = texts()
    check("con las plazas liberadas y el estado «Cancelada»", any("Hemos liberado tus plazas" in t for t in ts) and "Cancelada" in ts)
    check("el código ya no se enseña: se avisa de que no vale", find("Esta reserva está cancelada: el código ya no vale.") is not None and find("Muestra este código en el embarcadero. Disponible sin conexión.") is None)
    check("ya no se ofrece cancelarla otra vez", find("Cancelar reserva") is None)
    check("la barra de abajo ofrece «Reservar de nuevo» y no el calendario", control("Reservar de nuevo") is not None and find("Calendario") is None)
    check("el servidor la tiene cancelada", server_status(locator) == "cancelled")
    check("la app la guarda cancelada", [r[7] for r in app_reservations_db()[0] if r[0] == locator] == ["Cancelled"])
    rid = reservation_id(locator)

    # 2) Mis reservas la pone en «Canceladas»; «Reservar de nuevo» abre la misma actividad
    start("reservas", reset=False)
    check("Mis reservas la enseña en «Canceladas»", scroll_to("CANCELADAS") is not None)
    start("reserva/" + (rid or "x"), reset=False)
    check("reabierta sigue cancelada, sin botón de cancelar", wait_for("Reserva cancelada", 6) and find("Cancelar reserva") is None)
    tap_text("Reservar de nuevo")
    check("«Reservar de nuevo» abre Reservar con la misma actividad", wait_for("Paso 1 de 2 · Fecha y personas", 6) and find("Paseo en barca por la Albufera", exact=False) is not None)

    # 3) Sin conexión no se cancela una reserva del servidor (pero se ve su código); al volver la conexión se activa solo
    tap_text("Confirmar reserva")
    second = locator_on_screen()
    rid2 = reservation_id(second)
    check("una segunda reserva confirmada", bool(second) and second != locator, second)
    set_network(False)
    start("reserva/" + (rid2 or "x"), reset=False)
    check("sin conexión sigue viéndose el código de la reserva", find("Muestra este código en el embarcadero. Disponible sin conexión.") is not None)
    button = open_cancel_button()
    check("sin conexión «Cancelar reserva» está desactivado", bool(button) and button["en"] is False)
    check("y explica por qué", find("Para cancelar la reserva necesitas conexión a Internet.") is not None)
    set_network(True)
    time.sleep(2.5)
    button = control("Cancelar reserva")
    check("al volver la conexión se activa sin reabrir", bool(button and button["en"]) and find("Para cancelar la reserva necesitas conexión a Internet.") is None)

    # 4) Las de demostración solo existen en el móvil: se cancelan aunque no haya conexión y el servidor no se entera
    set_network(False)
    start("reserva/seed-2")
    demo = open_cancel_button()
    check("una reserva de demostración se puede cancelar sin conexión", bool(demo and demo["en"]) and find("Para cancelar la reserva necesitas conexión a Internet.") is None)
    tap_text("Cancelar reserva")
    wait_for("¿Cancelar esta reserva?", 5)
    tap_text("Sí, cancelar")
    check("y queda cancelada", wait_for("Reserva cancelada", 8))
    demo_locator = next((r[0] for r in app_reservations_db()[0] if r[7] == "Cancelled"), "")
    check("solo en el móvil: el servidor no tiene esa reserva", bool(demo_locator) and server_status(demo_locator) is None, demo_locator)
    start("reserva/seed-3")
    check("una reserva ya completada no se puede cancelar", wait_for("Completada", 6) and find("Cancelar reserva") is None)
    set_network(True)

    # 5) Recuperar la reserva en otro móvil (sin reservas ni identificador): localizador y correo
    adb("shell", "pm", "clear", PKG)
    start("reservas")
    check("en el otro móvil no está la reserva del servidor", locator not in [r[0] for r in app_reservations_db()[0]] and second not in [r[0] for r in app_reservations_db()[0]])
    scroll_to("¿Reservaste desde otro móvil?")
    check("Mis reservas ofrece recuperar una reserva", find("¿Reservaste desde otro móvil?") is not None)
    tap_text("¿Reservaste desde otro móvil?")
    check("se abre «Recuperar una reserva»", wait_for("Recuperar una reserva", 5))
    check("con el aviso de lo que se envía al servidor", find("El localizador y el correo se envían a nuestro servidor solo para buscar la reserva.") is not None)
    confirm = control("Recuperar")
    check("«Recuperar» está desactivado hasta rellenar los dos datos", bool(confirm) and confirm["en"] is False)
    fields = edit_fields()
    check("hay dos campos: localizador y correo", len(fields) == 2, str(len(fields)))
    if len(fields) == 2:
        # Al enfocar un campo sale el teclado y el diálogo sube: cada paso mira de nuevo dónde están los campos (un toque con las
        # coordenadas de antes caería fuera del diálogo y lo cerraría).
        tap(edit_fields()[0])
        type_text("tv-2099-9999")
        tap(edit_fields()[1])
        type_text("laura.garcia@example.com")
        confirm = control("Recuperar")
        check("con los dos datos se activa", bool(confirm and confirm["en"]))
        tap_text("Recuperar")
        check("un localizador que no existe: «No hay ninguna reserva con ese localizador y ese correo»", wait_for("No hay ninguna reserva con ese localizador y ese correo. Revisa los dos datos.", 10))
        check("el diálogo sigue abierto para corregirlo", find("Recuperar una reserva") is not None)
        clear_field(edit_fields()[0])
        type_text(second.lower())
        check("al corregir un dato desaparece el error", find("No hay ninguna reserva con ese localizador y ese correo. Revisa los dos datos.") is None)
        tap_text("Recuperar")
        shown = locator_on_screen(12)
        check("con el localizador (en minúsculas) y el correo correctos se abre la reserva", shown == second, shown)
        rows, _, _ = app_reservations_db()
        check("y la app la guarda", second in [r[0] for r in rows])
        check("sigue confirmada, con su código", find("Muestra este código en el embarcadero. Disponible sin conexión.") is not None)

        # 6) Y se puede cancelar desde ese otro móvil: no la hizo él, pero tiene su localizador y su correo
        button = open_cancel_button()
        check("la reserva recuperada se puede cancelar", bool(button and button["en"]))
        tap_text("Cancelar reserva")
        wait_for("¿Cancelar esta reserva?", 5)
        tap_text("Sí, cancelar")
        check("desde el otro móvil, con localizador y correo", wait_for("Reserva cancelada", 10))
        check("el servidor la tiene cancelada", server_status(second) == "cancelled")
        device_here = app_reservations_db()[2]
        owner = (local_sql("select device_id from app.reservations where locator = '%s'" % second) or [""])[0]
        check("y el móvil que la hizo no es este", bool(device_here) and bool(owner) and owner != device_here, "%s / %s" % (owner, device_here))

    # 6b) Una reserva recuperada se pone al día sola: el servidor la apunta a este móvil y su lista de reservas la trae
    linked = (local_sql("select app.create_reservation(gen_random_uuid(), gen_random_uuid(), 'albufera-barca', (now() at time zone 'Europe/Madrid')::date + 3, '18:30', 2, 0, 'enlace@example.com')->'reservation'->>'locator'") or [""])[0]
    start("reservas", reset=False)
    scroll_to("¿Reservaste desde otro móvil?")
    tap_text("¿Reservaste desde otro móvil?")
    wait_for("Recuperar una reserva", 5)
    if len(edit_fields()) == 2:
        tap(edit_fields()[0])
        type_text(linked.lower())
        tap(edit_fields()[1])
        type_text("enlace@example.com")
        tap_text("Recuperar")
        shown = locator_on_screen(12)
        check("recuperar otra reserva, hecha en otro móvil, con sus dos datos", bool(linked) and shown == linked, "%s / %s" % (linked, shown))
        count = (local_sql("select count(*) from app.reservation_devices where reservation_id = (select id from app.reservations where locator = '%s')" % linked) or [""])[0]
        check("el servidor la apunta a este móvil (una vez)", count == "1", count)
        local_sql("update app.reservations set status = 'cancelled' where locator = '%s'" % linked)  # la cancela el otro móvil
        start("reservas", reset=False)
        time.sleep(3.0)
        check("al abrir la app, la que se canceló desde el otro móvil sale cancelada sin recuperarla otra vez", [r[7] for r in app_reservations_db()[0] if r[0] == linked] == ["Cancelled"], str([r[7] for r in app_reservations_db()[0] if r[0] == linked]))
    else:
        check("el diálogo de recuperar tiene sus dos campos", False)

    # 7) Recuperar exige conexión
    set_network(False)
    start("reservas")
    scroll_to("¿Reservaste desde otro móvil?")
    tap_text("¿Reservaste desde otro móvil?")
    wait_for("Recuperar una reserva", 5)
    check("sin conexión el diálogo avisa de que recuperar necesita conexión", find("Recuperar una reserva necesita conexión a Internet.") is not None)
    confirm = control("Recuperar")
    check("y «Recuperar» está desactivado", bool(confirm) and confirm["en"] is False)
    set_network(True)
    start(None)  # deja la app como un primer arranque


def app_job():
    """El trabajo nocturno de WorkManager según `dumpsys jobscheduler`, o None si no hay: su id, si exige una red sin tarifa
    (`unmetered`) o cualquier conexión (`connected`), si pide batería no baja, los segundos que le faltan para empezar (`delay`) y
    cuántos trabajos tiene la app (`count`; el id cambia cada vez que WorkManager lo reprograma)."""
    lines = adb("shell", "dumpsys", "jobscheduler").splitlines()
    pattern = r"\s*JOB \S+:u0a\d+/(\d+): .*" + re.escape(PKG)
    found = [(i, re.match(pattern, line)) for i, line in enumerate(lines) if re.match(pattern, line)]
    if not found:
        return None
    i, m = found[0]
    block = []
    for nxt in lines[i + 1:]:
        if re.match(r"^\s{0,3}JOB |^\S|^  \S", nxt):  # el siguiente trabajo o una sección nueva
            break
        block.append(nxt)
    network = next((b for b in block if b.strip().startswith("Network type:")), "")
    latency = re.search(r"Minimum latency: \+(?:(\d+)d)?(?:(\d+)h)?(?:(\d+)m)?(?:(\d+)s)?", "\n".join(block))
    seconds = None
    if latency:
        d, h, mi, s = (int(g or 0) for g in latency.groups())
        seconds = d * 86400 + h * 3600 + mi * 60 + s
    return {
        "id": int(m.group(1)),
        "count": len(found),
        "unmetered": "NOT_METERED" in network,
        "connected": "INTERNET" in network,
        "battery_not_low": "batteryNotLow=true" in "\n".join(block),
        "delay": seconds,
    }


def wait_job(present=True, seconds=12.0, until=None):
    """Espera a que el trabajo nocturno exista (o no) y, si se da `until`, a que cumpla esa condición; devuelve lo último que vio."""
    end = time.time() + seconds
    job = app_job()
    while time.time() < end and ((job is not None) != present or (present and until is not None and not until(job))):
        time.sleep(0.8)
        job = app_job()
    return job


def refresh_log_lines():
    """Líneas del registro de la app que dejó el trabajo nocturno al ejecutarse («Refresco en segundo plano (intento N): …»)."""
    return [l.split("TurismoDb:")[-1].strip() for l in adb("shell", "logcat", "-d", "-s", "TurismoDb:V").splitlines() if "Refresco en segundo plano" in l]


def run_night_job(job, seconds=30.0):
    """Ejecuta ya el trabajo nocturno: adelanta el reloj hasta pasadas las 3:00 (WorkManager no deja ejecutar un trabajo periódico antes
    de su hora, ni forzándolo), pide a JobScheduler que lo lance y devuelve la línea que deja en el registro (o ''). Restaura el reloj."""
    before = len(refresh_log_lines())
    line = ""
    try:
        set_clock(datetime.now(timezone.utc) + timedelta(seconds=(job["delay"] if job and job["delay"] else 86400) + 120))
        adb("shell", "cmd", "jobscheduler", "run", "-f", "-n", "androidx.work.systemjobscheduler", PKG, str(job["id"]) if job else "0")
        end = time.time() + seconds
        while time.time() < end and not line:
            lines = refresh_log_lines()
            line = lines[-1] if len(lines) > before else ""
            time.sleep(1.0)
    finally:
        restore_clock()
    return line


def seconds_until_night(hour=3):
    """Segundos que faltan, según el reloj del emulador (su zona horaria), para las 3:00 de la próxima noche."""
    now = [int(x) for x in adb("shell", "date", "+%H:%M:%S").strip().split(":")]
    seconds = now[0] * 3600 + now[1] * 60 + now[2]
    target = hour * 3600
    return target - seconds if target > seconds else target + 86400 - seconds


def g_refresco():
    print("\n## Refresco nocturno: WorkManager, «Solo Wi-Fi» y «Actualizar automáticamente»")
    set_network(True)
    # Desde cero, con la base de datos de WorkManager vacía: un trabajo que ya ha corrido conserva su calendario (cada 24 h desde la
    # última vez) y no empezaría a las 3:00.
    adb("shell", "pm", "clear", PKG)
    start("descargas")  # con los ajustes de demostración: «Solo Wi-Fi» y «Actualizar automáticamente» activados
    job = wait_job(True)
    check("con los ajustes de siempre WorkManager programa el trabajo nocturno", job is not None)
    check("…que pide una red sin tarifa (Solo Wi-Fi) y batería no baja", bool(job and job["unmetered"] and job["battery_not_low"]))
    expected = seconds_until_night()
    check("…y empieza a las 3:00 de la hora del móvil (faltan %d s)" % expected, bool(job and job["delay"] is not None and abs(job["delay"] - expected) < 180), str(job and job["delay"]))
    first_delay = job["delay"] if job else None

    # «Descargar solo con Wi-Fi» apagado: basta cualquier conexión; encendido otra vez, vuelve a exigir Wi-Fi
    tap(switch_near("Descargar solo con Wi-Fi"))
    job = wait_job(True, until=lambda j: not j["unmetered"])
    check("sin «Solo Wi-Fi» el trabajo acepta cualquier conexión", bool(job and job["connected"] and not job["unmetered"]))
    check("…sigue habiendo un solo trabajo y su calendario no se reinicia (a las 3:00, no 24 h después)", bool(job and job["count"] == 1 and job["delay"] is not None and abs(job["delay"] - (first_delay or 0)) < 180), str(job and (job["count"], job["delay"], first_delay)))
    tap(switch_near("Descargar solo con Wi-Fi"))
    job = wait_job(True, until=lambda j: j["unmetered"])
    check("al volver a encender «Solo Wi-Fi» vuelve a pedir una red sin tarifa", bool(job and job["unmetered"] and job["count"] == 1))

    # «Actualizar automáticamente» apagado: sin trabajo, también tras cerrar y abrir la app; encendido, vuelve
    tap(switch_near("Actualizar automáticamente"))
    check("al apagar «Actualizar automáticamente» desaparece el trabajo", wait_job(False) is None)
    start("descargas", reset=False)
    time.sleep(2.0)
    check("tras cerrar y abrir la app sigue sin trabajo (el ajuste se guardó)", app_job() is None)
    tap(switch_near("Actualizar automáticamente"))
    job = wait_job(True)
    check("al encenderlo de nuevo se programa otra vez", job is not None)
    start("descargas", reset=False)
    check("tras cerrar y abrir la app el trabajo sigue programado", wait_job(True) is not None)

    # El trabajo se ejecuta con la app cerrada. Se mata el proceso con `kill -9` (no con «forzar detención», que borra los trabajos de
    # la app), se adelanta el reloj hasta pasadas las 3:00 (WorkManager no deja ejecutar un trabajo periódico antes de su hora, ni
    # forzándolo) y se pide a JobScheduler que lo lance: el sistema arranca la app, WorkManager se crea con la configuración de
    # `TurismoApp` y el trabajo pone todo al día.
    job = wait_job(True)
    adb("shell", "input", "keyevent", "KEYCODE_HOME")
    pid = adb("shell", "pidof", PKG).strip()
    if pid:
        adb("shell", "run-as", PKG, "kill", "-9", pid.split()[0])
    time.sleep(1.5)
    check("con la app cerrada no hay proceso de la app y el trabajo sigue programado", adb("shell", "pidof", PKG).strip() == "" and app_job() is not None)
    line = run_night_job(app_job())
    check("al ejecutarse arranca la app en segundo plano y pone todo al día", "al día" in line, line)
    again = wait_job(True)
    check("y deja programado el de la noche siguiente (24 h después)", bool(again and again["delay"] is not None and 23 * 3600 < again["delay"] <= 24 * 3600 + 60), str(again and again["delay"]))
    start(None)  # deja la app como un primer arranque


def g_privacidad():
    print("\n## Política de privacidad: la de la web y lo que añade la app")
    start("pagina/PoliticaPrivacidad")
    check("la política de la web sigue arriba", find("Política de Privacidad") is not None and find("1. Responsable del tratamiento") is not None)
    sections = ("La app en tu móvil", "Reservas", "Cancelar y recuperar reservas", "Identificador del móvil", "Copia de seguridad de Android", "Cerrar sesión", "Asistente ValBot", "Mapa, rutas y ubicación", "Actualización automática", "Lo que la app no incluye", "Tus derechos")
    for title in sections:
        check("debajo, lo que añade la app: «%s»" % title, scroll_to(title, tries=40) is not None)
        if title == "Reservas":
            swipe_up(1, 500)  # y un poco más, para ver los puntos de debajo del título
            check("…con lo que se envía («Un identificador aleatorio de tu móvil…»)", find("Un identificador aleatorio de tu móvil", exact=False) is not None)
    start("pagina/AvisoLegal")
    swipe_up(14, 900)
    check("el aviso legal no lleva ese añadido", find("La app en tu móvil") is None and find("Lo que la app no incluye") is None)


def app_locales():
    """Idiomas de la app que guarda el sistema (`cmd locale get-app-locales`), como texto: «en», «ca-ES-valencia»…"""
    m = re.search(r"are \[(.*)\]", adb("shell", "cmd", "locale", "get-app-locales", PKG))
    return m.group(1) if m else ""


def set_app_locale(tags=""):
    """Fija (o, sin etiquetas, quita) el idioma de la app en el sistema, como en Ajustes → Aplicaciones → Idioma de la app."""
    adb("shell", "cmd", "locale", "set-app-locales", PKG, *(["--locales", tags] if tags else []))


def room_language():
    rows = app_db("SELECT language FROM settings")[0]
    return rows[0][0] if rows else None


def ask_valbot(question, reply_text, attempts=2):
    """Escribe una pregunta en ValBot (sin asistente en línea: contesta con las reglas) y espera a que salga `reply_text`.
    Con el emulador cargado la pregunta puede no llegar a enviarse (el campo aún sin foco, el teclado a medio abrir): se reintenta una
    vez, que el comportamiento que se prueba (qué contesta ValBot) no depende de ese intento."""
    for attempt in range(attempts):
        adb("shell", "input", "tap", "200", "1447")  # el campo de texto, en el diseño de 780x1688
        time.sleep(0.8)
        type_text(question)
        time.sleep(0.6)
        send = next((n for n in nodes() if n["d"] in ("Enviar", "Send", "Envoyer")), None)  # el control pulsable es su padre: se pulsa en su centro
        if send:
            tap(send)
        else:
            adb("shell", "input", "tap", "696", "1447")
        if wait_for(reply_text, 10, exact=False):
            return True
        print("   intento %d sin respuesta a «%s»: textos %s" % (attempt + 1, question, texts()[:8]), flush=True)
    return False


def g_idiomas():
    print("\n## Idiomas: español, valenciano, inglés y francés")
    # ---- Cambiar desde la hoja de idioma de Perfil
    start("perfil", "--ez", "chatDown", "true")
    check("Perfil en español", find("Perfil y ajustes") is not None and find("Español") is not None)
    tap_text("Idioma")
    check("la hoja de idioma ofrece los cuatro idiomas", wait_for("Valencià", 4) and all(find(t) for t in ("Español", "English", "Français")))
    tap_text("English")
    tap_text("Aplicar")
    check("«Aplicar» recrea la pantalla en inglés", wait_for("Profile & settings", 8))
    check("el sistema guarda el idioma de la app", app_locales() == "en", app_locales())
    check("Perfil: la fila Language dice English", find("Language") is not None and find("English") is not None)
    check("Room anota el idioma (EN)", room_language() == "EN", str(room_language()))

    # ---- Inglés: contenido, formatos y avisos
    start("inicio", reset=False, lang=None)
    check("Inicio en inglés (buscador, secciones y «Cerca de ti»)", all(find(t) for t in ("Search Valencia…", "What to see", "Near you")))
    check("…y la barra inferior", all(find(t) for t in ("Home", "Map", "Favourites", "Bookings")))
    check("las fichas salen traducidas (Central Market) y con el formato inglés (4.8 · 2.3k)", find("Central Market") is not None and any("(2.3k)" in t for t in texts()))
    start("detalle/mercado-central", reset=False, lang=None)
    check("la ficha traducida: título, información práctica y cifras", find("Valencia Central Market") is not None and find("Practical information") is not None and find("Free admission") is not None)
    check("…y la guía completa de la web, traducida y sin el aviso de «solo en español»", scroll_to("Full guide", tries=20) is not None and find("This content from the website is only available in Spanish.") is None and scroll_to("The Dome and Modernist Architecture", tries=20) is not None)
    start("reservas", reset=False, lang=None)
    check("Mis reservas en inglés", find("My bookings") is not None and find("3 bookings · 1 pending") is not None)
    check("…con el título de la oferta traducido (Boat trip on La Albufera)", find("Boat trip on La Albufera") is not None)
    check("…y las fechas en inglés («Sat 24 Oct · 18:30»)", any(re.fullmatch(r"(Mon|Tue|Wed|Thu|Fri|Sat|Sun) \d{1,2} [A-Z][a-z]{2} · \d{2}:\d{2}", t) for t in texts()), str([t for t in texts() if "·" in t][:3]))
    check("…y el precio con el símbolo delante (2 people · €36)", "2 people · €36" in texts())
    start("notificaciones", reset=False, lang=None)
    check("Notificaciones: las de demostración se traducen", all(find(t) is not None for t in ("Mascletà at 14:00", "Booking confirmed", "Guides updated", "ValBot has news")))
    start("entradas?tab=experiencias", reset=False, lang=None)
    check("Tickets & Tours en inglés (Book, from €18, Valencian paella workshop)", find("Book") is not None and find("from") is not None and "€18" in texts() and find("Valencian paella workshop") is not None)
    start("busqueda", reset=False, lang=None)
    type_text("central market")
    found = wait_for("Central Market", 8)  # el resultado sale tras la última letra: con el emulador cargado tarda más que un tiempo fijo
    check("el buscador encuentra por el nombre en inglés", found, "" if found else str(texts()[:8]))
    start("valbot", "--ez", "chatDown", "true", reset=False, lang=None)
    check("ValBot saluda en inglés", find("Hello! I’m ValBot, your tourism assistant in Valencia. How can I help you?") is not None)
    check("ValBot contesta en inglés a una pregunta en inglés", ask_valbot("hello", "Ask me about places"))
    check("…y avisa de que es una respuesta básica", find("Basic reply · online ValBot unavailable") is not None)
    start("valbot", "--ez", "chatDown", "true", reset=False, lang=None)
    check("ValBot entiende una pregunta en español aunque la app esté en inglés y contesta en inglés", ask_valbot("como llego a la lonja", "on foot from your location"))

    # ---- Persistencia: tras cerrar y abrir sigue en inglés
    start("perfil", reset=False, lang=None)
    check("tras cerrar y abrir la app sigue en inglés", find("Profile & settings") is not None)

    # ---- El idioma que se elige en Ajustes del móvil (Aplicaciones → Idioma de la app)
    set_app_locale("ca-ES-valencia")
    check("desde los ajustes del móvil: valenciano (ca-ES-valencia)", wait_for("Perfil i configuració", 8))
    check("Room sigue al sistema (VA)", room_language() == "VA", str(room_language()))
    start("inicio", reset=False, lang=None)
    check("Inicio en valencià (Cerca a València…, Què veure, Prop de tu, Mercat Central)", all(find(t) for t in ("Cerca a València…", "Què veure", "Prop de tu", "Mercat Central")))
    start("reservas", reset=False, lang=None)
    check("Reserves: fechas valencianas («Ds 24 oct · 18:30») y «2 persones · 36 €»", any(re.fullmatch(r"(Dl|Dt|Dc|Dj|Dv|Ds|Dg) \d{1,2} [a-zç]+ · \d{2}:\d{2}", t) for t in texts()) and "2 persones · 36 €" in texts(), str([t for t in texts() if "·" in t][:3]))
    start("valbot", "--ez", "chatDown", "true", reset=False, lang=None)
    check("ValBot en valencià", find("Hola! Sóc ValBot, el teu assistent de turisme a València. En què et puc ajudar?") is not None and ask_valbot("hola", "Pregunta’m per llocs"))

    # ---- Francés: con el extra de depuración
    start("perfil", reset=False, lang="FR")
    check("francés con el extra de depuración (--es lang FR)", wait_for("Profil et réglages", 8))
    check("el sistema guarda «fr»", app_locales() == "fr", app_locales())
    start("inicio", reset=False, lang=None)
    check("Inicio en français (Rechercher à Valence…, À voir, Près de vous, Marché central)", all(find(t) for t in ("Rechercher à Valence…", "À voir", "Près de vous", "Marché central")))
    start("reservas", reset=False, lang=None)
    check("Mes réservations: fechas francesas y «2 personnes · 36 €»", any(re.fullmatch(r"(Lun|Mar|Mer|Jeu|Ven|Sam|Dim) \d{1,2} [a-zéû.]+ · \d{2}:\d{2}", t) for t in texts()) and any(t.replace(" ", " ") == "2 personnes · 36 €" for t in texts()), str([t for t in texts() if "·" in t][:3]))
    start("valbot", "--ez", "chatDown", "true", reset=False, lang=None)
    check("ValBot en français", ask_valbot("bonjour", "Posez-moi des questions"))
    start("pagina/Navidad", reset=False, lang=None)
    check("una guía de la web: título y texto traducidos (Le Noël le plus lumineux, Marchés, crèches…) y sin el aviso de «solo en español»", find("Le Noël le plus lumineux") is not None and any("Marchés, crèches, concerts" in t for t in texts()) and find("Ce contenu du site web n’est disponible qu’en espagnol.") is None, str(texts()[:6]))
    start("pagina/AvisoLegal", reset=False, lang=None)
    check("…y una página legal también (Mentions légales, Données d’identification)", scroll_to("1. Données d’identification", tries=14) is not None, str(texts()[:6]))

    # ---- Volver al español desde la hoja
    start("perfil", reset=False, lang=None)
    tap_text("Langue")
    wait_for("Español", 4)
    tap_text("Español")
    tap_text("Appliquer")
    check("desde la hoja, de vuelta al español", wait_for("Perfil y ajustes", 8))
    check("el sistema guarda «es»", app_locales() == "es", app_locales())
    check("Room anota ES", room_language() == "ES", str(room_language()))
    start("pagina/Navidad", reset=False, lang=None)
    check("en español no sale el aviso de «solo en español»", find("Este contenido de la web solo está disponible en español.") is None and find("La Navidad más luminosa") is not None)
    start("reservas", reset=False, lang=None)
    check("en español las fechas y los precios son los de siempre", any(re.fullmatch(r"(Lun|Mar|Mié|Jue|Vie|Sáb|Dom) \d{1,2} [a-z]{3} · \d{2}:\d{2}", t) for t in texts()) and "2 pers. · 36 €" in texts())

    # ---- Primer arranque: manda el idioma del móvil si es uno de los cuatro (inglés en el emulador)…
    adb("shell", "pm", "clear", PKG)
    set_app_locale("")
    start("perfil", reset=False, lang=None)
    check("primer arranque en un móvil en inglés: la app sale en inglés", wait_for("Profile & settings", 8))
    check("…y Room lo anota (EN)", room_language() == "EN", str(room_language()))
    # …y, si no lo es (alemán), el español.
    device = adb("shell", "cmd", "locale", "get-device-locale").strip()
    try:
        adb("shell", "cmd", "locale", "set-device-locale", "de-DE")
        time.sleep(2.0)
        adb("shell", "pm", "clear", PKG)
        set_app_locale("")
        start("perfil", reset=False, lang=None)
        check("primer arranque en un móvil en alemán (que la app no ofrece): sale en español", wait_for("Perfil y ajustes", 8))
        check("…y Room anota ES", room_language() == "ES", str(room_language()))
    finally:
        adb("shell", "cmd", "locale", "set-device-locale", device or "en-US")
        time.sleep(2.0)
    # Los demás grupos esperan la app en español.
    set_app_locale("es")
    start(None)  # deja la app como un primer arranque


# ---------------------------------------------------------------------------------------------------------------------------------
# Modo oscuro: la app sigue el modo del sistema (`cmd uimode night yes|no`). Se comprueba con píxeles (la luminosidad de las zonas que
# se sabe que son fondo, tarjeta o texto en la pantalla de 780x1688) porque los textos no cambian y uiautomator no ve colores.
# ---------------------------------------------------------------------------------------------------------------------------------
def set_night(on):
    adb("shell", "cmd", "uimode", "night", "yes" if on else "no")
    time.sleep(2.0)


def luma(p):
    return 0.2126 * p[0] + 0.7152 * p[1] + 0.0722 * p[2]


def region_luma(img, box):
    """Luminosidad media (0-255) de una zona de la captura."""
    return luma(ImageStat.Stat(img.crop(box)).mean)


def region_extremes(img, box):
    """Luminosidad del píxel más oscuro y del más claro de una zona."""
    return img.crop(box).convert("L").getextrema()


def g_oscuro():
    print("\n## Modo oscuro: la app sigue el modo del sistema (fondos, texto, QR, mapa, barras y cambio en caliente)")
    try:
        # ---- Inicio en oscuro
        set_night(True)
        start("inicio")
        img = shot()
        check("Inicio en oscuro: el fondo de la pantalla es casi negro", region_luma(img, (0, 900, 24, 1500)) < 30, "%.0f" % region_luma(img, (0, 900, 24, 1500)))
        check("…y la barra superior, una superficie oscura", region_luma(img, (0, 100, 30, 190)) < 45, "%.0f" % region_luma(img, (0, 100, 30, 190)))
        lo, hi = region_extremes(img, (20, 30, 130, 66))
        check("…con los iconos de la barra de estado claros (la hora se lee en blanco)", hi > 200, "%.0f" % hi)
        chip = img.getpixel((62, 770))
        check("…y el chip seleccionado en naranja de relleno (AF3C02, con texto blanco)", abs(chip[0] - 0xAF) < 22 and abs(chip[1] - 0x3C) < 22 and chip[2] < 30, str(chip))
        check("«Cerca de ti» y el resto de textos están en la pantalla", find("Cerca de ti") is not None and find("Qué ver") is not None)

        # ---- El mismo Inicio en claro
        set_night(False)
        start("inicio")
        img = shot()
        check("Inicio en claro: el fondo vuelve a ser claro", region_luma(img, (0, 900, 24, 1500)) > 230, "%.0f" % region_luma(img, (0, 900, 24, 1500)))
        lo, hi = region_extremes(img, (20, 30, 130, 66))
        check("…y la hora de la barra de estado, oscura", lo < 90, "%.0f" % lo)
        chip = img.getpixel((62, 770))
        check("…y el chip seleccionado, en C44D00 como siempre", abs(chip[0] - 0xC4) < 22 and abs(chip[1] - 0x4D) < 22 and chip[2] < 30, str(chip))

        # ---- Reserva confirmada: el QR es tinta oscura sobre papel blanco también en oscuro
        set_night(True)
        start("reserva/seed-1")
        img = shot()
        paper = region_luma(img, (70, 1190, 78, 1360))
        lo, hi = region_extremes(img, (100, 1190, 260, 1360))
        check("Reserva confirmada en oscuro: el papel del QR es blanco", paper > 235, "%.0f" % paper)
        check("…y su tinta, oscura (el lector del acceso lo lee igual que en claro)", lo < 40 and hi > 235, "%.0f–%.0f" % (lo, hi))

        # ---- Perfil en oscuro: texto claro sobre superficies oscuras
        start("perfil")
        img = shot()
        title_lo, title_hi = region_extremes(img, (200, 120, 500, 175))
        check("Perfil en oscuro: el título es claro sobre la barra oscura", title_hi > 200 and region_luma(img, (560, 100, 760, 190)) < 45, "%.0f" % title_hi)

        # ---- Mapa: las teselas de OpenStreetMap se oscurecen con un filtro y conservan la atribución
        start("mapa")
        time.sleep(5.0)
        img = shot()
        mean = region_luma(img, (0, 560, 780, 1000))
        check("Mapa en oscuro: las teselas se ven oscuras (luminosidad media baja)", mean < 85, "%.0f" % mean)
        check("…pero con detalle (calles y nombres), no un rectángulo negro", map_detail() > 14, "%.1f" % map_detail())
        check("…y con la atribución de OpenStreetMap", wait_for("© Colaboradores de OpenStreetMap", 6))
        set_night(False)
        start("mapa")
        time.sleep(5.0)
        mean = region_luma(shot(), (0, 560, 780, 1000))
        check("Mapa en claro: las teselas son las de siempre, claras", mean > 140, "%.0f" % mean)

        # ---- Bienvenida: la pantalla de arranque (A1) en el naranja de relleno de cada modo
        for night, target, label in ((True, (0xAF, 0x3C, 0x02), "AF3C02"), (False, (0xC4, 0x4D, 0x00), "C44D00")):
            set_night(night)
            adb("shell", "am", "force-stop", PKG)
            adb("shell", "pm", "clear", PKG)
            adb("shell", "cmd", "locale", "set-app-locales", PKG, "--locales", "es")
            adb("shell", "am", "start", "-n", PKG + "/.MainActivity")
            time.sleep(2.2)
            px = shot().getpixel((60, 260))
            check("Bienvenida en %s: la pantalla de arranque es del naranja de relleno (%s)" % ("oscuro" if night else "claro", label), all(abs(px[i] - target[i]) < 24 for i in range(3)), str(px))

        # ---- Cambiar el modo con la app abierta: AppCompat recrea la actividad y la app sigue viva
        set_night(False)
        start("favoritos")
        pid_before = adb("shell", "pidof", PKG).strip()
        set_night(True)
        time.sleep(2.5)
        img = shot()
        check("Cambio en caliente a oscuro: la pantalla se repinta oscura sin cerrar la app", region_luma(img, (0, 900, 24, 1500)) < 30 and adb("shell", "pidof", PKG).strip() == pid_before, "%.0f" % region_luma(img, (0, 900, 24, 1500)))
        check("…y sigue en la misma pantalla (Favoritos)", find("Favoritos") is not None)
        set_night(False)
        time.sleep(2.5)
        check("Cambio en caliente a claro: vuelve a pintarse clara", region_luma(shot(), (0, 900, 24, 1500)) > 230)
    finally:
        set_night(False)
    start(None)  # deja la app como un primer arranque


def tema_subtitle():
    """El modo que enseña la fila «Tema» de Perfil (el texto de debajo de su título), o None si no se ve (por ejemplo, con el diálogo
    abierto, que tapa la pantalla de debajo)."""
    ns = nodes()
    label = next((n for n in ns if n["t"] == "Tema"), None)
    if not label:
        return None
    below = [n for n in ns if n["t"] and abs(n["b"][0] - label["b"][0]) < 6 and 0 <= n["b"][1] - label["b"][3] < 60]
    return below[0]["t"] if below else None


def room_theme():
    """El modo claro u oscuro guardado en Room (Light, Dark o System) y en la copia rápida de `TurismoApp` (`shared_prefs/theme.xml`)."""
    rows = app_db("SELECT themeMode FROM settings")[0]
    prefs = adb("shell", "run-as", PKG, "cat", "shared_prefs/theme.xml")
    saved = re.search(r'name="mode">([A-Za-z]+)<', prefs)
    return (rows[0][0] if rows else None), (saved.group(1) if saved else None)


def system_night():
    """El modo oscuro del sistema (`cmd uimode night`): «yes» o «no»."""
    return adb("shell", "cmd", "uimode", "night").strip().rsplit(" ", 1)[-1]


def app_is_dark():
    """La app se ve oscura: el fondo de la pantalla (una franja del borde izquierdo, entre las tarjetas) es casi negro."""
    return region_luma(shot(), (0, 900, 24, 1500)) < 30


def app_is_light():
    return region_luma(shot(), (0, 900, 24, 1500)) > 230


def g_tema():
    print("\n## Selector de tema (Perfil → Tema): claro, oscuro o el del móvil, con independencia del modo del sistema")
    try:
        set_night(False)
        start("perfil")
        check("Perfil tiene la fila «Tema» y, por defecto, «Sigue al del sistema»", tema_subtitle() == "Sigue al del sistema", str(tema_subtitle()))
        check("al principio, Room y la copia rápida dicen el del móvil (System)", room_theme() == ("System", "System"), str(room_theme()))
        tap_text("Tema")
        check("pulsarla abre el diálogo con las tres opciones", wait_for("Cancelar", 4) and find("Claro") is not None and find("Oscuro") is not None and find("Sigue al del sistema") is not None)

        # ---- Oscuro con el móvil en claro
        tap_text("Oscuro")
        time.sleep(2.8)  # AppCompat recrea la actividad con el modo nuevo
        img = shot()
        lo, hi = region_extremes(img, (20, 30, 130, 66))
        check("«Oscuro» con el móvil en claro: la app se pinta oscura", region_luma(img, (0, 900, 24, 1500)) < 30, "%.0f" % region_luma(img, (0, 900, 24, 1500)))
        check("…con los iconos de la barra de estado claros (la hora en blanco)", hi > 200, "%.0f" % hi)
        check("…sin salir de Perfil, con «Oscuro» en la fila", tema_subtitle() == "Oscuro", str(tema_subtitle()))
        check("…mientras el sistema sigue en claro", system_night() == "no", system_night())
        check("Room y la copia rápida guardan Dark", room_theme() == ("Dark", "Dark"), str(room_theme()))

        # ---- Se guarda: cerrar y abrir la app (sin restablecer los datos)
        start("perfil", reset=False)
        check("tras cerrar y abrir la app sigue oscura y con «Oscuro» en la fila", app_is_dark() and tema_subtitle() == "Oscuro", str(tema_subtitle()))

        # ---- Claro con el móvil en oscuro
        set_night(True)
        time.sleep(1.5)
        check("con el móvil en oscuro y la app en «Oscuro», sigue oscura", app_is_dark())
        tap_text("Tema")
        wait_for("Cancelar", 4)
        tap_text("Claro")
        time.sleep(2.8)
        img = shot()
        lo, hi = region_extremes(img, (20, 30, 130, 66))
        check("«Claro» con el móvil en oscuro: la app se pinta clara", region_luma(img, (0, 900, 24, 1500)) > 230, "%.0f" % region_luma(img, (0, 900, 24, 1500)))
        check("…con la hora de la barra de estado oscura", lo < 90, "%.0f" % lo)
        check("…con «Claro» en la fila y el sistema en oscuro", tema_subtitle() == "Claro" and system_night() == "yes", "%s / %s" % (tema_subtitle(), system_night()))
        check("Room y la copia rápida guardan Light", room_theme() == ("Light", "Light"), str(room_theme()))
        start("perfil", reset=False)
        check("tras cerrar y abrir la app sigue clara, aunque el móvil esté en oscuro", app_is_light() and tema_subtitle() == "Claro")

        # ---- Sigue al del sistema: la app vuelve a hacer lo que el móvil
        tap_text("Tema")
        wait_for("Cancelar", 4)
        tap_last("Sigue al del sistema")
        time.sleep(2.8)
        check("«Sigue al del sistema» con el móvil en oscuro: la app pasa a oscura", app_is_dark() and tema_subtitle() == "Sigue al del sistema", str(tema_subtitle()))
        check("Room y la copia rápida guardan System", room_theme() == ("System", "System"), str(room_theme()))
        set_night(False)
        time.sleep(2.8)
        check("…y al cambiar el móvil a claro, la app lo sigue, con la app abierta", app_is_light())
        set_night(True)
        time.sleep(2.8)
        check("…y al volver a oscuro, también", app_is_dark())
        set_night(False)

        # ---- El gancho de depuración `resetData` vuelve al del móvil
        tap_text("Tema")
        wait_for("Cancelar", 4)
        tap_text("Oscuro")
        time.sleep(2.5)
        start("perfil")  # restablece los datos
        time.sleep(1.5)
        check("restablecer los datos (depuración) vuelve a «Sigue al del sistema» y a claro", tema_subtitle() == "Sigue al del sistema" and app_is_light(), str(tema_subtitle()))
        check("Room y la copia rápida vuelven a System", room_theme() == ("System", "System"), str(room_theme()))
    finally:
        set_night(False)
    start(None)  # deja la app como un primer arranque


GROUPS = {"chips": g_chips, "cats": g_cats, "tiles": g_tiles, "contacto": g_contacto, "guias": g_guias, "detalles": g_detalles, "paginas": g_paginas, "busqueda": g_busqueda, "persist": g_persist, "mapa": g_mapa, "avisos": g_avisos, "ofertas": g_ofertas, "reservas": g_reservas, "cancelar": g_cancelar, "refresco": g_refresco, "privacidad": g_privacidad, "idiomas": g_idiomas, "oscuro": g_oscuro, "tema": g_tema}
if __name__ == "__main__":
    adb("shell", "logcat", "-c")
    # El emulador está en inglés y la app sigue el idioma del móvil: los grupos esperan el español.
    adb("shell", "cmd", "locale", "set-app-locales", PKG, "--locales", "es")
    set_night(False)  # los grupos esperan el modo claro; `oscuro` cambia el modo y lo deja otra vez en claro
    chosen = sys.argv[1:] or list(GROUPS)
    for g in chosen:
        GROUPS[g]()
    crash = adb("shell", "logcat", "-d", "-t", "2000")
    fatal = [l for l in crash.split("\n") if "FATAL EXCEPTION" in l and PKG in crash]
    check("sin cierres inesperados (logcat)", not fatal)
    print("\nRESULTADO: %d/%d correctas" % (sum(results), len(results)))
    sys.exit(0 if all(results) else 1)
