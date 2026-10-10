"""Pruebas estáticas: contenido de la app frente al repositorio de la web.
Comprueba que cada página, foto y tarjeta de la web está en la app y es alcanzable desde alguna pantalla."""
import json
import os
import re
import sys

REPO = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
ROOT = os.path.join(REPO, "app", "src", "main")
ASSETS = os.path.join(ROOT, "assets")
J = os.path.join(ROOT, "java", "com", "cmarcillovargas", "turismovalencia")
WORK = os.environ.get("WEB_IMPORT_WORK", os.path.join(REPO, "tools", "web-import", "work"))

index = json.load(open(os.path.join(ASSETS, "web", "index.json"), encoding="utf-8"))
pages = {f[:-5]: json.load(open(os.path.join(ASSETS, "web", "pages", f), encoding="utf-8")) for f in os.listdir(os.path.join(ASSETS, "web", "pages"))}

results = []


def check(name, ok, detail=""):
    results.append((name, ok, detail))
    print(("PASS" if ok else "FAIL"), "-", name, ("· " + detail) if detail else "")


# 1) Las 65 páginas de la web (rutas de App.jsx) están como JSON
app_jsx_path = os.path.join(WORK, "web", "src", "Pagina_Inicio", "components", "App.jsx")
app_jsx = open(app_jsx_path, encoding="utf-8").read() if os.path.exists(app_jsx_path) else ""
routes = set(re.findall(r'<Route path=[\'"]/([A-Za-z_]+)[\'"]', app_jsx))
section_pages = {"Inicio", "Que_Ver", "Eventos", "Gastronomia", "Alojamientos", "Entradas"}
content_routes = routes - section_pages
check("rutas de la web en App.jsx", len(content_routes) == 65, "%d rutas de contenido" % len(content_routes))
check("todas las rutas de la web tienen su JSON", content_routes <= set(pages), ",".join(sorted(content_routes - set(pages))))

# 2) Alcanzabilidad desde la app
src_repo = open(os.path.join(J, "data", "repo", "WebContentRepository.kt"), encoding="utf-8").read()
content_to_page = dict(re.findall(r'"([a-z-]+)" to "([A-Za-z]+)"', src_repo.split("ContentToPage = mapOf(")[1].split("val PageToContent")[0]))
entradas_vm = open(os.path.join(J, "ui", "screens", "explorar", "EntradasViewModel.kt"), encoding="utf-8").read()
guides = set(re.findall(r'"([A-Za-z]+)"', entradas_vm.split("val GuideOrder")[1]))
home_tiles = {t["route"] for k in ("esencial", "planes", "sostenible") for t in index["home"][k]} - {"Entradas"}
cat_tiles = {t["route"] for c in index["categories"].values() for t in c["tiles"]}
reachable = set(content_to_page.values()) | guides | home_tiles | cat_tiles | {"AvisoLegal", "PoliticaPrivacidad"}
missing = set(pages) - reachable
check("las 65 páginas son alcanzables desde la app", not missing, ",".join(sorted(missing)))

# 3) Índices de sección: cada tarjeta de la web tiene ficha en la app
fakes = ""
for f in os.listdir(os.path.join(J, "data", "fake")):
    fakes += open(os.path.join(J, "data", "fake", f), encoding="utf-8").read()
item_ids = set(re.findall(r'id = "([a-z-]+)"', fakes))
page_to_content = {v: k for k, v in content_to_page.items()}
for key, cat in index["categories"].items():
    miss = [t["title"] for t in cat["tiles"] if page_to_content.get(t["route"]) not in item_ids]
    check("sección %s: %d tarjetas de la web con ficha en la app" % (key, len(cat["tiles"])), not miss, ", ".join(miss))

# 4) Rejillas de la portada
for key in ("esencial", "planes", "sostenible"):
    miss = []
    for t in index["home"][key]:
        r = t["route"]
        ok = r == "Entradas" or r in pages
        if not ok:
            miss.append(r)
    check("portada %s: %d tarjetas abren una página" % (key, len(index["home"][key])), not miss, ",".join(miss))

# 5) Fotos: todas las referenciadas existen en assets/img
refs = set()
for m in index["pages"].values():
    refs |= {m.get("hero"), m.get("thumb")}
for p in pages.values():
    refs.add(p.get("hero"))
    for b in p["blocks"]:
        for it in b.get("items", []):
            if isinstance(it, dict):
                refs.add(it.get("image"))
for g in index["home"].values():
    refs |= {t["image"] for t in g}
for c in index["categories"].values():
    refs |= {t["image"] for t in c["tiles"]}
refs.discard(None)
absent = [r for r in sorted(refs) if not os.path.exists(os.path.join(ASSETS, "img", *r.split("/")))]
check("%d fotos referenciadas existen en assets/img" % len(refs), not absent, ", ".join(absent[:5]))

# 6) Cada página tiene título, texto y al menos un bloque con contenido (los textos legales solo bloques)
empty = [k for k, p in pages.items() if not p["title"] or not p["blocks"] or (p["family"] != "legal" and not p["lead"])]
check("las 65 páginas tienen título, texto y bloques", not empty, ",".join(empty))

fails = [r for r in results if not r[1]]
print("\nRESULTADO: %d/%d correctas" % (len(results) - len(fails), len(results)))
sys.exit(1 if fails else 0)
