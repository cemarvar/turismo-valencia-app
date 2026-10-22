# -*- coding: utf-8 -*-
"""Traducciones del texto de las páginas de la web (assets/web/pages) a valenciano, inglés y francés.

El texto de las páginas es lo que hay en `app/src/main/assets/web/pages/<Pagina>.json` (español, importado de la web). Cada idioma
guarda su traducción en `app/src/main/assets/i18n/<va|en|fr>/pages/<Pagina>.json`: la misma forma que la página pero solo con los campos
que se traducen (ver `docs/idiomas.md`). Este script hace el trabajo mecánico, para que quien traduce solo escriba frases:

  python tools/i18n/pages.py extract [Pagina ...]      escribe work/<Pagina>.es.txt con las cadenas por traducir (todas las páginas sin argumentos)
  python tools/i18n/pages.py build <va|en|fr> [Pagina ...]   lee work/<Pagina>.<idioma>.txt y escribe el JSON de la app (con todas las comprobaciones)
  python tools/i18n/pages.py check [<va|en|fr>]       revisa los JSON que ya están en la app (forma, cifras, comillas, tipografía)
  python tools/i18n/pages.py status                   qué páginas están traducidas en cada idioma
  python tools/i18n/pages.py glossary-candidates      las cadenas cortas que se repiten (candidatas al glosario)

Formato de work/<Pagina>.es.txt (y de las traducciones `<Pagina>.<idioma>.txt`, que llevan solo `id|texto`):
  NNN|código|texto en español        (los saltos de línea del texto van como \\n)
Lo que está en el glosario (`glossary/<idioma>.json`: cadena completa en español -> traducción) se rellena solo y no sale en el archivo.
"""
import hashlib
import json
import pathlib
import re
import sys
from collections import Counter

sys.stdout.reconfigure(encoding="utf-8")

ROOT = pathlib.Path(__file__).resolve().parents[2]
PAGES = ROOT / "app" / "src" / "main" / "assets" / "web" / "pages"
I18N = ROOT / "app" / "src" / "main" / "assets" / "i18n"
HERE = pathlib.Path(__file__).resolve().parent
WORK = HERE / "work"
GLOSSARY = HERE / "glossary"
LANGS = ("va", "en", "fr")
NBSP = " "


# ---------------------------------------------------------------------------------------------------------------------------
# Las cadenas de una página y su sitio en la traducción
# ---------------------------------------------------------------------------------------------------------------------------
class Skeleton:
    """La forma de la traducción de una página: la misma estructura, con el número de cadena donde irá cada texto."""

    def __init__(self, page):
        self.slots = []  # (código, texto en español)
        self.shape = self._build(page)

    def _slot(self, code, text):
        self.slots.append((code, text))
        return len(self.slots) - 1

    def _pairs(self, rows, code_l, code_v):
        return [[self._slot(code_l, label), self._slot(code_v, value)] for label, value in rows]

    def _build(self, page):
        shape = {}
        if page.get("lead"):
            shape["lead"] = [self._slot("lead", t) for t in page["lead"]]
        blocks = []
        for block in page["blocks"]:
            out = {}
            if block.get("title"):
                out["title"] = self._slot("btitle", block["title"])
            if block.get("filters"):
                out["filters"] = [self._slot("filter", f["label"]) for f in block["filters"]]
            kind = block["type"]
            if kind == "facts":
                out["rows"] = self._pairs(block["rows"], "fl", "fv")
            elif kind == "bullets":
                out["items"] = [self._slot("bl", t) for t in block["items"]]
            else:
                items = []
                for item in block["items"]:
                    o = {}
                    for key, code in (("kicker", "k"), ("title", "t"), ("subtitle", "s"), ("body", "b")):
                        if item.get(key):
                            o[key] = self._slot(code, item[key])
                    if item.get("facts"):
                        o["facts"] = self._pairs(item["facts"], "fl", "fv")
                    if item.get("bullets"):
                        o["bullets"] = [self._slot("bl", t) for t in item["bullets"]]
                    if item.get("bulletsTitle"):
                        o["bulletsTitle"] = self._slot("bt", item["bulletsTitle"])
                    if item.get("notes"):
                        o["notes"] = self._pairs(item["notes"], "nl", "nv")
                    if item.get("tags"):
                        o["tags"] = [self._slot("tg", t["l"]) for t in item["tags"]]
                    items.append(o)
                out["items"] = items
            blocks.append(out)
        shape["blocks"] = blocks
        return shape

    def fill(self, texts):
        """La estructura con los textos traducidos (`texts[i]` es la traducción de la cadena i)."""

        def go(node):
            if isinstance(node, int):
                return texts[node]
            if isinstance(node, list):
                return [go(x) for x in node]
            return {k: go(v) for k, v in node.items()}

        return go(self.shape)

    def digest(self):
        return hashlib.sha1("\n".join(t for _, t in self.slots).encode("utf-8")).hexdigest()[:12]


def load_pages(names=None):
    files = sorted(PAGES.glob("*.json"))
    if names:
        wanted = set(names)
        missing = wanted - {f.stem for f in files}
        if missing:
            sys.exit("No existe la página: " + ", ".join(sorted(missing)))
        files = [f for f in files if f.stem in wanted]
    return {f.stem: json.loads(f.read_text(encoding="utf-8")) for f in files}


def load_glossary(lang):
    path = GLOSSARY / (lang + ".json")
    return json.loads(path.read_text(encoding="utf-8")) if path.exists() else {}


def is_automatic(text):
    """Cadenas que no se traducen: vacías o solo cifras y guiones (años, rangos)."""
    return text == "" or re.fullmatch(r"[\d\s–—\-/]+", text) is not None


def enc(text):
    return text.replace("\\", "\\\\").replace("\n", "\\n")


def dec(text):
    return re.sub(r"\\(n|\\)", lambda m: "\n" if m.group(1) == "n" else "\\", text)


# ---------------------------------------------------------------------------------------------------------------------------
# Comprobaciones y tipografía
# ---------------------------------------------------------------------------------------------------------------------------
def numbers(text):
    """Las cifras de un texto, sin ceros a la izquierda y sin los «00» de los minutos («10:00» es «10 h» en francés; «08:00», «8 h»)."""
    return Counter((n.lstrip("0") or "0") for n in re.findall(r"\d+", text) if n != "00")


def polish(text, lang):
    """Lo mecánico: apóstrofo tipográfico, espacios y, en francés, los espacios de no separación."""
    t = text.replace("'", "’")
    t = re.sub(r"[ \t]+", " ", t).strip()
    if lang == "fr":
        # Idempotente: lo que ya lleva espacio de no separación no se duplica (lo usa también `check`).
        sp = "[ " + NBSP + "]"
        t = re.sub(sp + r"+([:;?!»])", NBSP + r"\1", t)
        t = re.sub("«" + sp + "*", "«" + NBSP, t)
        t = re.sub(r"(\d)" + sp + r"(?=\d{3}(?!\d))", r"\1" + NBSP, t)  # separador de miles
        t = re.sub(r"(\d)" + sp + r"(€|km|m²|m2|%|°C|h|min|ha|kg)(?![A-Za-zÀ-ÿ])", r"\1" + NBSP + r"\2", t)
        t = re.sub(r"(\d)%", r"\1" + NBSP + "%", t)
    return t


SPANISH_WORDS = re.compile(r"\b(los|las|del|una|para|con|por|desde|hasta|donde|también|más|muy|sus|entre|sobre|este|esta)\b", re.I)
ENGLISH_ONLY_WORDS = re.compile(r"\b(the|and|with|from|that|which|their|this|are|is)\b", re.I)


def problems(lang, spanish, text):
    """(errores, avisos) de una traducción."""
    errors, warnings = [], []
    if not text.strip():
        errors.append("vacía")
        return errors, warnings
    if "[object" in text or re.search(r"</?[a-zA-Z][^>]*>", text) or re.search(r"&[a-z#0-9]+;", text):
        errors.append("lleva marcas de código ([object Object], etiquetas o entidades)")
    if '"' in text:
        errors.append("comillas rectas: usa «…» (valenciano), “…” (inglés) o « … » (francés)")
    if "'" in text:
        errors.append("apóstrofo recto: usa ’")
    lost = numbers(spanish) - numbers(text)
    if lost:
        errors.append("faltan cifras del original: " + ", ".join(sorted(lost.elements())))
    if "\n" in spanish and text.count("\n") != spanish.count("\n"):
        errors.append("distinto número de saltos de línea")
    if len(spanish) >= 40:
        ratio = len(text) / len(spanish)
        if ratio < 0.4 or ratio > 2.6:
            warnings.append("longitud rara (%.1f× el original)" % ratio)
        if text == spanish:
            warnings.append("igual que el original (¿sin traducir?)")
    if lang in ("en", "fr") and len(SPANISH_WORDS.findall(text)) >= 3 and len(text) > 60:
        warnings.append("parece tener palabras en español")
    if lang in ("va", "fr") and len(ENGLISH_ONLY_WORDS.findall(text)) >= 3:
        warnings.append("parece tener palabras en inglés")
    if lang == "fr":
        if re.search(r" [:;?!»]|« ", text):
            errors.append("francés: espacio normal antes de : ; ? ! » (debe ser de no separación)")
    return errors, warnings


# ---------------------------------------------------------------------------------------------------------------------------
# extract
# ---------------------------------------------------------------------------------------------------------------------------
def cmd_extract(names):
    WORK.mkdir(exist_ok=True)
    glossaries = {lang: load_glossary(lang) for lang in LANGS}
    total = lines = 0
    for name, page in load_pages(names).items():
        sk = Skeleton(page)
        out = ["#%s|%s|%d cadenas" % (name, sk.digest(), len(sk.slots))]
        for i, (code, text) in enumerate(sk.slots):
            total += 1
            if is_automatic(text) or all(text in g for g in glossaries.values()):
                continue
            lines += 1
            out.append("%03d|%s|%s" % (i, code, enc(text)))
        (WORK / (name + ".es.txt")).write_text("\n".join(out) + "\n", encoding="utf-8")
    print("extraído: %d páginas, %d cadenas, %d por traducir (el resto es automático o del glosario)" % (len(load_pages(names)), total, lines))


# ---------------------------------------------------------------------------------------------------------------------------
# build
# ---------------------------------------------------------------------------------------------------------------------------
def read_work(path):
    rows = {}
    for n, raw in enumerate(path.read_text(encoding="utf-8").split("\n"), 1):
        if not raw.strip() or raw.startswith("#"):
            continue
        head, sep, text = raw.partition("|")
        if not sep or not head.strip().isdigit():
            sys.exit("%s:%d: línea sin «NNN|texto»: %s" % (path.name, n, raw[:60]))
        i = int(head)
        if i in rows:
            sys.exit("%s:%d: la cadena %03d está repetida" % (path.name, n, i))
        rows[i] = dec(text.strip())
    return rows


def translate_page(name, page, lang, rows, glossary):
    """(textos, errores, avisos): la traducción de todas las cadenas de la página, o lo que falla."""
    sk = Skeleton(page)
    errors, warnings = [], []
    snapshot = WORK / (name + ".es.txt")
    if snapshot.exists():
        header = snapshot.read_text(encoding="utf-8").split("\n", 1)[0]
        if header.split("|")[1] != sk.digest():
            errors.append("el español de la página cambió desde que se extrajo: vuelve a ejecutar extract")
    texts = []
    for i, (code, spanish) in enumerate(sk.slots):
        if is_automatic(spanish):
            texts.append(spanish)
        elif i in rows:
            texts.append(polish(rows[i], lang))
        elif spanish in glossary:
            texts.append(polish(glossary[spanish], lang))
        else:
            texts.append(None)
            errors.append("%03d (%s) sin traducir: %s" % (i, code, spanish[:60]))
    extra = set(rows) - {i for i in range(len(sk.slots))}
    if extra:
        errors.append("números de cadena que no existen: " + ", ".join("%03d" % i for i in sorted(extra)))
    for i, (code, spanish) in enumerate(sk.slots):
        if texts[i] is None or is_automatic(spanish):
            continue
        e, w = problems(lang, spanish, texts[i])
        errors += ["%03d (%s): %s" % (i, code, m) for m in e]
        warnings += ["%03d (%s): %s" % (i, code, m) for m in w]
    return sk, texts, errors, warnings


def write_overlay(lang, name, sk, texts):
    out = I18N / lang / "pages"
    out.mkdir(parents=True, exist_ok=True)
    (out / (name + ".json")).write_text(json.dumps(sk.fill(texts), ensure_ascii=False, separators=(",", ":")), encoding="utf-8")


def cmd_build(lang, names):
    if lang not in LANGS:
        sys.exit("idioma: " + "|".join(LANGS))
    glossary = load_glossary(lang)
    pages = load_pages(names)
    if not names:
        pages = {n: p for n, p in pages.items() if (WORK / ("%s.%s.txt" % (n, lang))).exists()}
    done = failed = 0
    for name, page in pages.items():
        path = WORK / ("%s.%s.txt" % (name, lang))
        if not path.exists():
            print("%s: falta %s" % (name, path.name))
            failed += 1
            continue
        sk, texts, errors, warnings = translate_page(name, page, lang, read_work(path), glossary)
        for w in warnings:
            print("  aviso %s %s: %s" % (lang, name, w))
        if errors:
            failed += 1
            print("ERROR %s %s:" % (lang, name))
            for e in errors[:25]:
                print("   ", e)
            continue
        write_overlay(lang, name, sk, texts)
        done += 1
    print("%s: %d páginas escritas, %d con errores" % (lang, done, failed))
    if failed:
        sys.exit(1)


# ---------------------------------------------------------------------------------------------------------------------------
# check / status
# ---------------------------------------------------------------------------------------------------------------------------
def flatten(shape_json, sk):
    """Los textos de un JSON traducido en el orden de las cadenas, o None si no tiene la misma forma que el original."""
    texts = [None] * len(sk.slots)
    ok = True

    def go(shape, node):
        nonlocal ok
        if isinstance(shape, int):
            if not isinstance(node, str):
                ok = False
            else:
                texts[shape] = node
        elif isinstance(shape, list):
            if not isinstance(node, list) or len(node) != len(shape):
                ok = False
                return
            for s, n in zip(shape, node):
                go(s, n)
        else:
            if not isinstance(node, dict) or set(node) != set(shape):
                ok = False
                return
            for k in shape:
                go(shape[k], node[k])

    go(sk.shape, shape_json)
    return texts if ok else None


def cmd_check(langs):
    pages = load_pages()
    bad = 0
    for lang in langs:
        done = 0
        for name, page in pages.items():
            path = I18N / lang / "pages" / (name + ".json")
            if not path.exists():
                continue
            done += 1
            sk = Skeleton(page)
            texts = flatten(json.loads(path.read_text(encoding="utf-8")), sk)
            if texts is None:
                print("ERROR %s %s: no tiene la forma de la página en español" % (lang, name))
                bad += 1
                continue
            for i, (code, spanish) in enumerate(sk.slots):
                if is_automatic(spanish):
                    continue
                e, w = problems(lang, spanish, texts[i])
                if texts[i] != polish(texts[i], lang):
                    e.append("sin la tipografía del idioma (ejecuta build)")
                for m in e:
                    print("ERROR %s %s %03d (%s): %s" % (lang, name, i, code, m))
                    bad += 1
                for m in w:
                    print("aviso %s %s %03d (%s): %s" % (lang, name, i, code, m))
        print("%s: %d/%d páginas" % (lang, done, len(pages)))
    if bad:
        sys.exit(1)


def cmd_status():
    pages = load_pages()
    for lang in LANGS:
        have = [n for n in pages if (I18N / lang / "pages" / (n + ".json")).exists()]
        print("%s: %d/%d" % (lang, len(have), len(pages)))
        missing = [n for n in pages if n not in have]
        if missing and len(missing) <= 70:
            print("   faltan:", ", ".join(missing))


def cmd_candidates():
    counts = Counter()
    for page in load_pages().values():
        for _, text in Skeleton(page).slots:
            if not is_automatic(text) and len(text.split()) <= 8:
                counts[text] += 1
    known = set()
    for lang in LANGS:
        known |= set(load_glossary(lang))
    for text, n in counts.most_common():
        if n < 3:
            break
        print("%4d  %s%s" % (n, text, "   (ya está)" if text in known else ""))


if __name__ == "__main__":
    args = sys.argv[1:]
    if not args:
        print(__doc__)
        sys.exit(0)
    cmd, rest = args[0], args[1:]
    if cmd == "extract":
        cmd_extract(rest)
    elif cmd == "build":
        if not rest:
            sys.exit("build <va|en|fr|all> [Pagina ...]")
        failed = False
        for lang in (LANGS if rest[0] == "all" else (rest[0],)):
            try:
                cmd_build(lang, rest[1:])
            except SystemExit as e:
                failed = failed or bool(e.code)
        if failed:
            sys.exit(1)
    elif cmd == "check":
        cmd_check(rest or list(LANGS))
    elif cmd == "status":
        cmd_status()
    elif cmd == "glossary-candidates":
        cmd_candidates()
    else:
        sys.exit(__doc__)
