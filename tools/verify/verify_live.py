"""Compara lo que muestra la web publicada (https://turismovalenciaa.netlify.app, leída con el navegador) con los datos de la app."""
import json
import sys

import os

REPO = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
ASSETS = os.path.join(REPO, "app", "src", "main", "assets", "web", "index.json")
RES = os.path.join(REPO, "app", "src", "main", "res", "values", "strings.xml")
index = json.load(open(ASSETS, encoding="utf-8"))
strings = open(RES, encoding="utf-8").read()

# Capturado de la web en directo (titulos, fotos y enlaces de cada índice)
LIVE = {
    "quever": ("Espacios únicos", ["Lonja de Seda", "Catedral y Santo Cáliz", "Barrio del Carmen", "Oceanografic", "Mercado Central", "Bioparc", "Ciudad de las Artes y las Ciencias", "La Marina", "Jardín del Turia", "Museo de Bellas Artes", "Ruzafa", "Playas", "Albufera", "Iglesia de San Nicolás", "Mestalla"],
               ["lonja", "catedral", "barri_carmen", "oceanografic", "mercado_central", "bioparc", "c_a_c", "marina", "jardi_turia", "m_b_a", "ruzafa", "playas", "albufera", "iglesia_nicolas", "mestalla"]),
    "eventos": ("Eventos destacados", ["Festivos", "Fallas", "Deportes", "Gran Feria de Valencia", "Festivales de Verano", "Corpus Christi"],
                ["valencia-festivos", "fallas", "deportes", "feria", "festivales", "corpus"]),
    "gastronomia": ("Tradición culinaria", ["Paella Valenciana", "Fideuà", "Arroz al horno", "Esgarraet", "Arroz Negro", "All i pebre", "Buñuelo de Calabaza", "Arroz del senyoret", "Horchata"],
                    ["paella", "fideua", "arroz-al-horno", "esgarraet", "Arroz-negro", "allipebre", "bunuelos", "arroz-senyoret", "horchata"]),
    "alojamientos": ("Opciones de alojamiento", ["Centro", "Ciudad de las Artes y las Ciencias", "Barrio Ruzafa", "Barrio Gran Vía", "Zona de playa y Paseo Marítimo", "Alojamiento Barato y Exclusivo"],
                     ["centro", "artes-ciencias", "ruzafa", "gran-via", "arenas", "alojamiento"]),
}
HOME = {
    "esencial": ["Navidad", "10 visitas imprescindibles", "Valencia en 3 días", "Valencia la ciudad amurallada", "Patrimonio de la humanidad", "Monumentos y Museos", "Ciudad de las Artes y las Ciencias", "Visitas y tours", "Bioparc"],
    "planes": ["Espectáculo", "En familia", "Naturaleza", "Fiestas Tradicionales", "Deportes", "Exposición"],
    "sostenible": ["Muévete por valencia", "Practica el ecoturismo", "Compra en comercio local", "Consejos para ir a tu aire", "Haz tu viaje más sostenible", "Haz un viaje responsable"],
}
HOME_TITLES = ["Lo esencial para ver en Valencia", "Planes que alimentan el alma", "Turismo Sostenible"]
FOOTER = ["Descubre la ciudad del sol, la paella y las fiestas. Tu guía de viaje en Valencia.", "Lun - Vie: 9:00 - 18:00", "+34 963 000 000", "info@valencia.com", "Valencia, España",
          "https://es.linkedin.com/company/turismo-valencia", "https://www.facebook.com/visitvalenciaSpain", "https://www.instagram.com/visit_valencia", "https://x.com/valenciaturismo", "© 2026 Valencia · Todos los derechos reservados"]

fails = 0


def check(name, ok, detail=""):
    global fails
    fails += 0 if ok else 1
    print(("PASS" if ok else "FAIL"), "-", name, ("· " + detail) if detail else "")


for key, (title, titles, imgs) in LIVE.items():
    c = index["categories"][key]
    check("web=app índice %s (título, %d fichas, orden y fotos)" % (key, len(titles)),
          c["title"] == title and [t["title"] for t in c["tiles"]] == titles and [t["image"].rsplit("/", 1)[1].rsplit(".", 1)[0] for t in c["tiles"]] == imgs,
          "" if c["title"] == title else c["title"])
for key, titles in HOME.items():
    got = [t["title"] for t in index["home"][key]]
    check("web=app portada %s (%d tarjetas)" % (key, len(titles)), got == titles, str(got) if got != titles else "")
for t in HOME_TITLES:
    check("título de portada «%s» en strings.xml" % t, ">%s<" % t in strings)
for t in FOOTER:
    check("pie de la web «%s…» en strings.xml" % t[:28], t.replace("&", "&amp;") in strings)

print("\nRESULTADO: %d fallos" % fails)
sys.exit(1 if fails else 0)
