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
| **Textos de las páginas de la web** (las guías: «Guía completa», Lo esencial, Planes, textos legales de la web…) | `assets/web/pages/*.json`, importados de la web | **Solo en español**; la pantalla avisa con «Este contenido de la web solo está disponible en español» (`InfoNotice`) |
| Ofertas que el servidor añada y que la app no conozca | Servidor | Se ven con el texto del servidor (en español) |
| ValBot en línea (Gemini) | Función `chat` de la web | La app manda el idioma (`lang`); hoy contesta en el idioma de la pregunta |
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

## Cómo añadir o cambiar un texto

1. **Texto de pantalla:** `stringResource(R.string.xxx)` y la cadena en `values/strings.xml` **y** en `values-ca`, `values-en` y `values-fr`
   (cadenas idénticas en todos los idiomas, como una URL, llevan `translatable="false"`). Lint (`MissingTranslation`) y `StringsTranslationsTest`
   avisan de lo que falta y de marcadores (`%1$s`) distintos.
2. **Texto que sale de un repositorio o de un receptor:** `AppStrings.get(R.string.xxx)`, no `context.getString`.
3. **Ficha, oferta o página nueva:** al código (`data/fake`) y al JSON de cada idioma; `ContentTranslationsTest` lo comprueba.
4. **Fecha, precio o distancia:** `util/Format.kt` (`formatShortDate`, `formatPrice`, `formatDistance`…), nunca `String.format` ni
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
  `ValBotIntentTest` (cada regla entiende preguntas en los cuatro idiomas y las palabras cortas solo valen enteras) y `ValBotRepositoryTest`/
  `ChatApiTest` (el idioma viaja al asistente en línea).
- `./gradlew connectedDebugAndroidTest`: `LanguageTest` (recursos de cada idioma, fichas y ofertas traducidas, buscador, títulos de la web, el
  idioma se pide una sola vez al sistema) y `RemindersTest.elAvisoSaleEnElIdiomaDeLaApp` (título, texto y canal de la notificación en los
  cuatro idiomas).
- `python tools/verify/verify_ui.py idiomas` (emulador): cambiar desde la hoja y desde los ajustes del móvil, inglés, valenciano y francés
  en Inicio, ficha, reservas, notificaciones, Tickets & Tours, buscador y ValBot (`--ez chatDown true`: sin asistente en línea), que persiste al
  reiniciar, volver al español, y el primer arranque con el móvil en inglés (sale en inglés) y en alemán (sale en español). Extras de
  depuración: `--es lang ES|VA|EN|FR` y `--ez chatDown true`.

## Pendiente

- Traducir el texto de las páginas de la web: hoy solo se traducen sus títulos. Haría falta traducirlas en la web (`cemarvar/TurismoValencia`)
  y reimportarlas, o añadir un segundo juego de JSON por idioma en `assets/i18n/<idioma>/pages/` y que `WebContentRepository.page` los lea.
- Que el servidor devuelva las ofertas ya traducidas (`Accept-Language` o un campo por idioma): hoy se traducen en la app por su `id`.
- Que la función `chat` de la web use el campo `lang` que manda la app, para que Gemini conteste siempre en el idioma elegido.
- Una revisión por hablantes nativos (valenciano y francés sobre todo) y de los textos legales antes de publicar.
