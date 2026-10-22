# Idiomas

La app está en **cuatro idiomas**: español (el original y el de reserva), **valenciano**, **inglés** (británico) y **francés** (trato de «vous»).
Se elige en el chip de idioma de Inicio o en Perfil → Idioma, y también en los ajustes del móvil (Aplicaciones → Turismo Valencia → Idioma
de la app, desde Android 13). Cambia al instante, se guarda y también sirve para ValBot, las notificaciones y los formatos de fechas y
números.

## Qué está traducido y qué no

| Qué | Dónde está | Estado |
|---|---|---|
| Textos de las pantallas (≈ 380 cadenas, plurales y listas), también la política de privacidad de la app | `res/values/strings.xml` (español) y `values-ca`, `values-en`, `values-fr` | Traducido |
| Fichas de Qué ver, Gastronomía, Eventos, Alojamientos y Sostenible (título, resumen, descripción, cifras, información práctica, actos, palabras clave) y las 16 ofertas de Tickets & Tours | `assets/i18n/<va\|en\|fr>/content.json` | Traducido |
| Títulos y etiquetas de las 65 páginas de la web, tarjetas de la portada y títulos de las secciones | el mismo JSON (`pages`, `tiles`, `sections`) | Traducido |
| Respuestas de ValBot sin conexión (y sus sugerencias) | `strings.xml` + reglas en `ValBotEngine` | Traducido; entiende la pregunta en cualquiera de los cuatro idiomas |
| Notificación de la mascletà, su canal y las notificaciones de demostración | `strings.xml` | Traducido |
| Fechas, días de la semana, precios («18 €», «€18», «18 €» con espacio fino en francés), distancias («1,1 km», «1.1 km») | `util/Format.kt` (tablas propias, no dependen del ICU del móvil) | Traducido |
| **Textos de las 65 páginas de la web** (las guías: «Guía completa», Lo esencial, Planes, guías de entradas, tours y experiencias, textos legales de la web…) | `assets/web/pages/*.json` (español, importado de la web) y `assets/i18n/<va\|en\|fr>/pages/<Pagina>.json` | Traducido; si una página no tuviera su traducción (o no tuviera la forma del español) saldría en español con el aviso «Este contenido de la web solo está disponible en español» (`InfoNotice`) |
| Ofertas que el servidor añada | Servidor (campo `translations` de cada oferta) | Traducido por el servidor: se ven en el idioma de la app sin publicar una versión nueva; lo que el servidor no traiga se completa con `content.json` y, si tampoco está, sale en español |
| ValBot en línea (Gemini) | Función `chat` de la web | Traducido: la función usa el `lang` que manda la app y contesta en ese idioma aunque la pregunta sea en otro |
| Nombres propios y de platos (Oceanogràfic, paella, fideuà…), la marca «Turismo Valencia», direcciones, teléfonos y enlaces | — | No se traducen |

## Cómo funciona

```
Elegir idioma (hoja de Inicio/Perfil)          Ajustes del móvil (Android 13+) / primer arranque
        │                                                    │
        ▼                                                    ▼
SettingsRepository.setLanguage(código)           el sistema ya tiene el idioma de la app
  ├─ Room: settings.language                                 │
  └─ LanguageApplier → AppCompatDelegate.setApplicationLocales("ca-ES-valencia" · "en" · "fr" · "es")
        │                                                    │
        └──────────────────► Android recrea la actividad con los recursos del idioma ◄─────┘
                                           │
                MainActivity.onCreate: language = getString(R.string.locale_code)
                  ├─ si cambió respecto a la actividad anterior: viewModelStore.clear()
                  └─ AppContainer.applyLanguage(language)
                        ├─ Locale.setDefault (formatos), AppStrings.language
                        ├─ ContentRepository.setLanguage → fichas, ofertas y títulos de la web traducidos
                        ├─ AppLanguagePrefs (SharedPreferences: lo lee la alarma de un aviso sin abrir Room)
                        └─ SettingsRepository.syncLanguage → Room anota el idioma efectivo
```

- **El idioma que manda es el de los recursos.** Android elige `values-xx` con la lista entera de idiomas del usuario, y la única forma
  segura de saber cuál ha ganado es la cadena `locale_code` (`ES`, `VA`, `EN`, `FR`) que cada archivo de recursos traduce a su propio código
  (`LanguageCode.fromCode`, `Context.appLanguage()`). Room (`settings.language`) solo recuerda el último idioma efectivo.
- **Primer arranque:** si el móvil está en uno de los cuatro idiomas, la app sale en él; si no (alemán, por ejemplo), en español.
- **El valenciano** es catalán con la variante `valencia` (`ca-ES-valencia`, `LanguageCode.VA.tag`) y sus textos están en `values-ca`
  escritos en valenciano (este, teua, hui, xiquets, ometre…). Un móvil en catalán también los verá.
- **AppCompat:** `MainActivity` es una `AppCompatActivity` con tema AppCompat porque el idioma por app lo exige en Android 12 o menos
  (`AppLocalesMetadataHolderService` en el manifiesto lo guarda allí; desde Android 13 lo guarda el sistema, `res/xml/locales_config.xml`).
- **Al cambiar de idioma se descartan los ViewModel** (`viewModelStore.clear()` en `MainActivity`): así cada pantalla se construye otra vez
  con los textos nuevos, sin tocar cada ViewModel. Se pierde lo que solo estaba en un ViewModel (la conversación de ValBot, por ejemplo).
- **`AppStrings`** da textos en el idioma de la app a quien no tiene una pantalla detrás (reglas de ValBot, sugerencias del buscador, la
  notificación de un aviso): crea los `Resources` de un contexto con el idioma pedido.

## Contenido traducido (`assets/i18n/<va|en|fr>/content.json`)

```json
{
  "items":    { "<id de la ficha>": { "title": "…", "summary": "…", "description": "…", "eyebrow": "…", "area": "…", "subcategory": "…",
                                      "stats": [["valor", "etiqueta"], …], "info": [["título", "texto"], …], "acts": [["título", "cuándo"], …],
                                      "openUntil": "…", "dateLabel": "…", "keywords": ["…"], … } },
  "offers":   { "<id de la oferta>": { "tag": "…", "title": "…", "duration": "…", "place": "…", "extra": "…", "description": "…" } },
  "pages":    { "<Pagina>": { "title": "…", "eyebrow": "…" } },
  "tiles":    { "<Pagina>": "título de su tarjeta en la portada" },
  "sections": { "quever|eventos|gastronomia|alojamientos": { "title": "…", "subtitle": "…" } }
}
```

- Solo lo que viene sustituye al español. `stats`, `info` y `acts` van **en el mismo orden y número** que los del código (`data/fake`); el tipo
  de cada fila de `info` y las fotos de los actos no cambian.
- `keywords` se **suman** a las españolas: se busca en los dos idiomas.
- Lo que el código usa como clave no se traduce: ids, categorías, coordenadas, fotos, `Offer.kind`, `Offer.tagKey` (el `tag` del servidor, con
  el que se decide «Gastronómica») y las subcategorías `Tema` y `Experiencia`. Una subcategoría se traduce siempre igual en todas las fichas
  (es el filtro de la lista).
- **`ContentTranslationsTest`** (JVM) exige que cada ficha, oferta, página, tarjeta y sección tenga traducción en los tres idiomas y con la
  misma forma que el original: si alguien añade una ficha en `data/fake`, falla hasta que se traduzca.

## Texto de las páginas de la web (`assets/i18n/<va|en|fr>/pages/<Pagina>.json`)

El texto de las páginas es el que se importó de la web (español). Cada idioma lleva, por página, un JSON con **la misma forma** que el
original y solo los campos que se traducen; lo que no es texto (números, fotos, enlaces, qué filtro es cada ficha, qué etiqueta es
«gratuita») sale siempre del original.

```json
{
  "lead":   ["párrafo 1", "párrafo 2"],
  "blocks": [
    { "title": "…", "filters": ["…"], "items": [ { "kicker": "…", "title": "…", "subtitle": "…", "body": "…", "facts": [["etiqueta", "valor"]],
                                                   "bullets": ["…"], "bulletsTitle": "…", "notes": [["etiqueta", "valor"]], "tags": ["…"] } ] },
    { "title": "…", "rows": [["etiqueta", "valor"]] },
    { "title": "…", "items": ["…"] }
  ]
}
```

- `PageTranslation` (`data/repo/PageTranslations.kt`) lo lee y `WebPage.translated(…)` lo aplica al original. Si la forma no coincide (un
  párrafo, un dato, una etiqueta o un bloque de más o de menos) **no se aplica** y la página sale en español con el aviso: mejor eso que un
  texto desparejado. `WebPage.textLanguage` dice en qué idioma está el texto; `WebPageScreen` y la guía de `DetalleScreen` avisan cuando no es
  el de la app.
- `WebContentRepository` carga la traducción al abrir una página (y guarda las ya aplicadas por idioma); al cambiar de idioma se descartan.
- **El título de cada página y su sobretítulo** no están en este JSON: salen de `assets/i18n/<idioma>/content.json` (`pages`).
- **`WebPageTextTest`** (JVM) exige las 65 páginas en los tres idiomas, con la forma del original, sin marcas de código, comillas o apóstrofos
  rectos, espacios de más ni cifras perdidas (cada número del español tiene que estar en la traducción), y el francés con espacios de no
  separación en los miles y las unidades.
- **Cómo se hizo y cómo se rehace:** `tools/i18n/pages.py` (ver `tools/i18n/README.md`). `extract` escribe las cadenas por traducir de cada
  página (`work/<Pagina>.es.txt`, ignorado por git); quien traduce escribe `work/<Pagina>.<idioma>.txt` con `id|texto`; `build` lo valida (cifras,
  comillas, saltos de línea, tipografía, glosario) y escribe el JSON de la app. Si se reimporta la web, `extract` vuelve a sacar las cadenas y
  `status` dice qué páginas hay que revisar.
- **Convenciones propias de estos textos:** las cifras, precios y horarios no se cambian de formato más que por el idioma (`2,5 km` / `2.5 km`,
  `10:00` / `10 h 00` en francés, «From €82» / «À partir de 82 €» / «Des de 82 €»); los nombres propios, direcciones, teléfonos y correos se dejan
  como están; la grafía valenciana de los topónimos se usa en valenciano (Xàtiva, Russafa, Cullera, Borriana, Alboraia, la Malva-rosa).
- **Los textos legales** (política de privacidad y aviso legal de la web) están traducidos como cortesía, pero el texto que obliga es el español de
  la web: conviene que los revise quien responda de ellos antes de publicar la app.

## Ofertas del servidor

La API (`GET /offers`) manda cada oferta con sus textos originales (español) y, en `translations`, un objeto por idioma (`va`, `en`, `fr`) con
solo los campos traducidos (`tag`, `title`, `duration`, `place`, `extra`, `description`). La app los guarda en Room (`offers.translations`, base
v6) y `ContentRepository.textOf` elige, campo a campo, **servidor → `content.json` → español**. Así una oferta nueva se ve en los cuatro idiomas
sin publicar una versión de la app: basta con dar de alta sus filas en `app.offer_translations` (ver `backend/README.md`, «Mantenimiento»). Cambiar
una traducción cambia la versión del catálogo (el `ETag`), y la app lo vuelve a pedir en la siguiente apertura o en el refresco nocturno.

## Cómo añadir o cambiar un texto

1. **Texto de pantalla:** `stringResource(R.string.xxx)` y la cadena en `values/strings.xml` **y** en `values-ca`, `values-en` y `values-fr`
   (cadenas idénticas en todos los idiomas, como una URL, llevan `translatable="false"`). Lint (`MissingTranslation`) y `StringsTranslationsTest`
   avisan de lo que falta y de marcadores (`%1$s`) distintos.
2. **Texto que sale de un repositorio o de un receptor:** `AppStrings.get(R.string.xxx)`, no `context.getString`.
3. **Ficha, oferta incluida en la app o título de página nuevos:** al código (`data/fake`) y a `content.json` de cada idioma;
   `ContentTranslationsTest` lo comprueba.
4. **Página de la web nueva o cambiada** (tras reimportar): `python tools/i18n/pages.py extract <Pagina>`, traducir `work/<Pagina>.<idioma>.txt`,
   `python tools/i18n/pages.py build all <Pagina>` y `check`; `WebPageTextTest` lo comprueba.
5. **Oferta nueva del servidor:** sus filas en `app.offer_translations` (el servidor las manda en `translations`); no hace falta tocar la app.
6. **Fecha, precio o distancia:** `util/Format.kt` (`formatShortDate`, `formatPrice`, `formatDistance`…), nunca `String.format` ni
   `DateTimeFormatter` con un idioma fijo.

## Cómo añadir un idioma

1. `LanguageCode` (con su etiqueta BCP 47) y `res/xml/locales_config.xml`.
2. `res/values-xx/strings.xml` completo (con `locale_code`) y `assets/i18n/xx/content.json`.
3. Las tablas de `util/Format.kt` y `languageOf`.
4. Palabras de `ValBotEngine.Intent` en ese idioma, y los textos de `idioma_xx_nombre` / `idioma_xx_detalle` en todos los idiomas.
5. La hoja de idioma (`IdiomaOptions`) y los tests (`FormatTest`, `ValBotIntentTest`, el grupo `idiomas` de `verify_ui.py`).

## Convenciones de traducción

- Inglés británico (favourites, centre, programme). Francés con «vous». Valenciano con las formas valencianas (-ix, hui, xiquets, arrere).
- Apóstrofo tipográfico (’) en todos los idiomas (el recto exige escaparlo en `strings.xml`). Comillas «…» en español y valenciano,
  “…” en inglés y « … » con espacio de no separación en francés (`&#160;`); en francés también antes de `:`, `;`, `?` y `!`.
- Nombres propios sin traducir (Oceanogràfic, Mestalla, Bioparc, Les Falles de València, mascletà…), con el equivalente cuando ayuda a
  entender (Silk Exchange, Bourse de la soie, Llotja de la Seda). La ciudad: València (valenciano), Valencia (español e inglés), Valence
  (francés, en los textos; la marca «Valencia» no cambia).
- Mismos términos en toda la app: reservas = bookings / réservations / reserves; favoritos = favourites / favoris / preferits; descargas =
  downloads / téléchargements / descàrregues.

## Pruebas

- `./gradlew test`: `FormatTest` (precios, fechas y distancias en los cuatro idiomas), `StringsTranslationsTest` (todas las claves, marcadores,
  plurales, listas, `locale_code` y `locales_config`), `ContentTranslationsTest` (todo el contenido traducido y con la misma forma),
  `WebPageTextTest` y `PageTranslationsTest` (las 65 páginas en los tres idiomas y cómo se aplica una traducción), `OfferMappingTest` (las
  traducciones del ejemplo del contrato y el orden servidor → `content.json` → español), `ValBotIntentTest` (cada regla entiende preguntas en
  los cuatro idiomas y las palabras cortas solo valen enteras) y `ValBotRepositoryTest`/`ChatApiTest` (el idioma viaja al asistente en línea).
- `./gradlew connectedDebugAndroidTest`: `LanguageTest` (recursos de cada idioma, fichas y ofertas traducidas, también las del servidor, buscador,
  páginas de la web traducidas, el idioma se pide una sola vez al sistema), `OffersTest`/`MigrationTest` (las traducciones de las ofertas se
  guardan en Room y la migración 5→6 las añade) y `RemindersTest.elAvisoSaleEnElIdiomaDeLaApp` (título, texto y canal de la notificación en los
  cuatro idiomas).
- `python tools/verify/verify_ui.py idiomas` (emulador): cambiar desde la hoja y desde los ajustes del móvil, inglés, valenciano y francés
  en Inicio, ficha (con su guía de la web ya traducida, sin aviso), reservas, notificaciones, Tickets & Tours, buscador y ValBot (`--ez chatDown true`:
  sin asistente en línea), una guía y una página legal en francés sin el aviso de «solo en español», que persiste al reiniciar, volver al español,
  y el primer arranque con el móvil en inglés (sale en inglés) y en alemán (sale en español). Extras de depuración: `--es lang ES|VA|EN|FR` y
  `--ez chatDown true`.
- `python tools/i18n/pages.py check`: revisa los JSON de `assets/i18n/*/pages/` (forma, cifras, comillas, tipografía) sin necesitar Android.

## Pendiente

- Una revisión por hablantes nativos (valenciano y francés sobre todo) antes de publicar, y de los textos legales por quien responda de ellos.
- Si la web cambia y se reimporta, las páginas cuyo texto cambie hay que volver a extraerlas y traducirlas (`pages.py status` dice cuáles).
- Las ofertas incluidas en la app (`assets/i18n/*/content.json`, `offers`) y las del servidor se traducen en dos sitios: cuando el catálogo del
  servidor sea la única fuente, se podrán quitar de `content.json` (hoy sirven de respaldo sin conexión).
