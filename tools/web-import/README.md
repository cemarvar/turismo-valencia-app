# Importar el contenido de la web

El contenido de la app (`app/src/main/assets/web/` y `assets/img/`) sale de la web
[cemarvar/TurismoValencia](https://github.com/cemarvar/TurismoValencia). Estos scripts lo regeneran cuando la web cambia.
Hace falta Node 20+ (para `fetch`) y Python 3 con Pillow (`pip install pillow`).

```bash
cd tools/web-import
node fetch.mjs      # descarga las 65 páginas JSX, su CSS y las rejillas de la portada a ./work (ignorado por git)
node convert.mjs    # genera assets/web/index.json y assets/web/pages/<Pagina>.json; escribe work/web/images_needed.txt
python images.py    # descarga las fotos referenciadas, las reduce a 900 px y las guarda como WebP en assets/img/
```

- `convert.mjs` evalúa los arrays de datos de cada página (`planes`, `elementos`, `datosVisita`…), los normaliza a bloques
  (`items`, `facts`, `bullets`) y resuelve cada `imgClass` a la ruta de su foto leyendo el CSS de la página.
- Las fotos conservan la ruta de la web (`img-espacios/MercadoCentral/cupulamc.webp`), así que el JSON y `assets/img/` encajan sin tablas.
- `images.py` es incremental: salta las fotos que ya existen en `assets/img/`.
- La carpeta `work/` es temporal y no se sube a git.
