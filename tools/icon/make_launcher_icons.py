"""Genera los iconos de lanzador «de siempre» (mipmap-*dpi/ic_launcher.webp y ic_launcher_round.webp), los que usan los móviles
con Android 7.x (API 24 y 25). Del Android 8 en adelante se usa el icono adaptativo de `res/mipmap-anydpi-v26`, que no necesita
generarse (disco blanco + logotipo sobre el color de la pantalla de arranque, ver `res/drawable/ic_launcher_foreground.xml`).

Mismo dibujo que el adaptativo: fondo del color de la pantalla de arranque, disco blanco y encima el logotipo
(`res/drawable-nodpi/logo_turismo_valencia.png`, de interior transparente).

Uso: python make_launcher_icons.py          (reescribe los .webp de app/src/main/res)
     python make_launcher_icons.py --preview hoja.png   (además, una hoja con los tamaños para revisarlos)
"""
import os
import sys

from PIL import Image, ImageDraw

REPO = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
RES = os.path.join(REPO, "app", "src", "main", "res")
LOGO = os.path.join(RES, "drawable-nodpi", "logo_turismo_valencia.png")
BACKGROUND = (0xC4, 0x4D, 0x00)  # `splash_background` (PrimaryStrong del tema)
SIZES = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}
SUPERSAMPLE = 4  # se dibuja 4 veces mayor y se reduce: bordes sin escalones


def icon(size, round_shape):
    big = size * SUPERSAMPLE
    img = Image.new("RGBA", (big, big), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    if round_shape:
        draw.ellipse((0, 0, big - 1, big - 1), fill=BACKGROUND + (255,))
    else:
        draw.rounded_rectangle((0, 0, big - 1, big - 1), radius=int(big * 0.2), fill=BACKGROUND + (255,))
    disc = big * (0.70 if round_shape else 0.74)
    low, high = (big - disc) / 2, (big + disc) / 2
    draw.ellipse((low, low, high, high), fill=(255, 255, 255, 255))
    logo_size = int(big * (0.62 if round_shape else 0.66))
    logo = Image.open(LOGO).convert("RGBA").resize((logo_size, logo_size), Image.LANCZOS)
    img.alpha_composite(logo, ((big - logo_size) // 2, (big - logo_size) // 2))
    return img.resize((size, size), Image.LANCZOS)


def main():
    for density, size in SIZES.items():
        folder = os.path.join(RES, "mipmap-" + density)
        icon(size, round_shape=False).save(os.path.join(folder, "ic_launcher.webp"), lossless=True)
        icon(size, round_shape=True).save(os.path.join(folder, "ic_launcher_round.webp"), lossless=True)
        print("mipmap-%s: %d px" % (density, size))
    if "--preview" in sys.argv:
        out = sys.argv[sys.argv.index("--preview") + 1]
        sheet = Image.new("RGB", (192 * 2 + 60, 192 * len(SIZES) // 2 + 100), (235, 235, 235))
        y = 10
        for size in SIZES.values():
            sheet.paste(icon(size, False), (10, y), icon(size, False))
            sheet.paste(icon(size, True), (size + 30, y), icon(size, True))
            y += size + 10
        sheet.save(out)
        print("hoja:", out)


if __name__ == "__main__":
    main()
