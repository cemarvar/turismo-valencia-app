# Pruebas de contenido e interfaz

Comprueban que la app muestra lo mismo que la web (`cemarvar/TurismoValencia` y https://turismovalenciaa.netlify.app) y que las fotos cargan.
Hace falta Python 3 con Pillow (`pip install pillow`).

| Script | Qué comprueba | Necesita |
|---|---|---|
| `verify_data.py` | Las 65 páginas de la web están en `assets/web`, son alcanzables desde la app, cada ficha de los índices tiene su ficha en la app, todas las fotos referenciadas existen y cada página tiene título, texto y bloques | `node fetch.mjs` de `tools/web-import` (para leer `App.jsx`) |
| `verify_live.py` | Títulos, fotos, orden y textos de la portada y de los índices de Qué ver, Eventos, Gastronomía y Alojamientos de la web publicada frente a los datos de la app | Nada (los datos de la web van incluidos en el script; vuelve a capturarlos si la web cambia) |
| `verify_ui.py [grupo…]` | En el emulador: chips de la portada, secciones, las 21 tarjetas de la portada, contacto y textos legales, guías de Tickets & Tours, fichas con su guía completa, 16 páginas con su foto y el buscador | Emulador a 780×1688 px / 320 dpi (`adb shell wm size 780x1688`, `wm density 320`) y la app de depuración instalada |
| `verify_img.py` | Tras cambiar de chip, cada tarjeta muestra **su** foto (compara la captura con el archivo de `assets/img`) | Igual que `verify_ui.py` |

Grupos de `verify_ui.py`: `chips cats tiles contacto guias detalles paginas busqueda`. Al terminar: `adb shell wm size reset` y `adb shell wm density reset`.
