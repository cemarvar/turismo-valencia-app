# Traducir el texto de las páginas de la web

El texto de las 65 páginas de la web (`app/src/main/assets/web/pages/<Pagina>.json`, en español y generado con `tools/web-import`) está
traducido a valenciano, inglés y francés en `app/src/main/assets/i18n/<va|en|fr>/pages/<Pagina>.json`. Cada archivo tiene **la misma forma que la
página** y solo los campos que se traducen (ver «Texto de las páginas de la web» en `docs/idiomas.md`). `pages.py` hace el trabajo mecánico para que
quien traduce solo escriba frases. Hace falta Python 3 (sin librerías).

```bash
python tools/i18n/pages.py status                  # qué páginas están traducidas en cada idioma (devuelve un código distinto de 0 si falta alguna)
python tools/i18n/pages.py extract [Pagina …]      # escribe work/<Pagina>.es.txt con las cadenas por traducir (todas sin argumentos)
python tools/i18n/pages.py build <va|en|fr|all> [Pagina …]   # lee work/<Pagina>.<idioma>.txt, valida y escribe el JSON de la app
python tools/i18n/pages.py check [<va|en|fr>]      # revisa los JSON que ya están en la app (forma, cifras, comillas, tipografía)
python tools/i18n/pages.py glossary-candidates     # las cadenas cortas que se repiten (candidatas al glosario)
```

## Flujo para traducir una página

1. `extract Pagina` escribe `work/Pagina.es.txt`: una cadena por línea, `NNN|código|texto` (`lead`, `t` título, `s` subtítulo, `b` cuerpo, `fl`/`fv`
   etiqueta y valor de un dato, `bl` viñeta, `btitle` título de bloque, `tg` etiqueta, `k` sobretítulo…). Los saltos de línea del texto van como
   `\n` literal. La primera línea (`#Pagina|huella|N cadenas`) guarda una huella del español: si la página cambia, `build` da error hasta que se vuelva a hacer `extract`.
2. Se escribe `work/Pagina.<idioma>.txt` con **los mismos números** y solo `NNN|texto traducido`. Lo que está en el glosario
   (`glossary/<idioma>.json`: cadena completa en español → traducción) y las cifras sueltas no salen en el archivo: se rellenan solos.
3. `build all Pagina` valida y escribe el JSON de la app. **Errores** (no escribe): cadena que falta, cifra del español que no está en la traducción
   (los «00» de los minutos no cuentan: «10:00» es «10 h» en francés), comillas o apóstrofos rectos, marcas de código, distinto número de saltos
   de línea, espacio normal antes de `: ; ? ! »` en francés. **Avisos** (se escribe igual): igual que el original, longitud rara, palabras en
   otro idioma. `polish` pone lo tipográfico (apóstrofo ’, espacios de no separación del francés, miles y unidades).
4. `check` repasa todos los JSON de la app; `./gradlew test` (`WebPageTextTest`, `PageTranslationsTest`) lo comprueba con el código de la app.

`work/` está ignorado por git: la fuente de verdad son los JSON de `assets/i18n/*/pages/`. Para retocar una traducción basta con editar el JSON
(o el `.txt` y volver a hacer `build`).

## Convenciones

- Valenciano con las formas valencianas (-ix, hui, xiquets, mentres, ací, dona/donar), y los topónimos en valenciano (Xàtiva, Russafa, Cullera,
  Borriana, Alboraia, la Malva-rosa, la Llotja de la Seda, el Mercat Central, el Micalet). Inglés británico. Francés con «vous».
- Nombres propios, direcciones, teléfonos, correos y enlaces no se traducen. Un plato o un lugar con nombre propio se deja (paella, fideuà,
  Oceanogràfic, Mestalla) y se explica la primera vez si ayuda.
- Cifras, precios y horarios se conservan: cambia solo el formato del idioma (`2,5 km` / `2.5 km`, `~70–100 €/nit`, `~€70–100/night`).
- Los textos legales (`PoliticaPrivacidad`, `AvisoLegal`) se traducen como cortesía; el texto que obliga es el español de la web.
- Si el texto en español tiene una errata o una duda, se traduce lo que quiere decir y se anota para el dueño de la web.

Revisión por hablantes nativos pendiente (sobre todo valenciano y francés, y los textos legales).
