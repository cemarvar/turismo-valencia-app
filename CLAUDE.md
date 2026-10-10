# Turismo Valencia (app Android)

App nativa para turistas, derivada de la web Turismo Valencia. Proyecto del TFG de DAM.

## Stack
- Kotlin 2.2 + Jetpack Compose (BOM 2026.02.01) + Material 3. AGP 9.4, compileSdk/targetSdk 37, minSdk 24.
- Arquitectura MVVM. Una pantalla = `Screen` + `UiState` + `ViewModel` (más un `Route` que los conecta; ver `docs/componentes.md`).
- Tipografía Source Sans 3 empaquetada en `res/font` (la app funciona sin conexión).
- Mapa previsto: osmdroid como primera opción, Google Maps SDK de reserva si la prueba de humo falla (`TODO(mapa)`).
- Backend previsto en `backend/` (Node.js + MySQL; hosting Railway por confirmar; ValBot usa Gemini). Aún no existe. La app lo llama por HTTPS (Retrofit + corrutinas).
- Dependencias en uso: Compose/Material 3, `material-icons-extended`, `navigation-compose`, `lifecycle-viewmodel-compose` + `lifecycle-runtime-compose`, `zxing-core` (código QR real de la reserva) y `desugar_jdk_libs` (`java.time` en minSdk 24). Las demás (Room, Retrofit, osmdroid, Coil…) se añaden en su fase, no antes.
- **Estado actual: frontend completo y funcional con datos en memoria.** Las 27 pantallas del Figma existen y navegan entre sí, y además todas las secciones de la web (Lo esencial, Planes, guías de entradas/tours/experiencias, textos legales y Contacto). Los datos salen de repositorios en memoria (`data/`) y de JSON en `assets/`; se pierde lo que el usuario cambia al cerrar la app. No hay backend, base de datos local ni mapa real todavía (ver «Tags de pendientes»).
- **Contenido de la web**: `assets/web/index.json` + `assets/web/pages/<Pagina>.json` (65 páginas) y `assets/img/…` (443 fotos reducidas), generados desde el repositorio `cemarvar/TurismoValencia` con `tools/web-import` (ver su README). Se leen con `WebContentRepository`; las fotos se pintan con `PlaceImage(assetPath = …)`.

## Comandos
```bash
./gradlew assembleDebug        # compilar el APK de debug (en Windows: ./gradlew.bat; JAVA_HOME = jbr de Android Studio)
./gradlew test                 # tests unitarios
./gradlew lint                 # lint
```

## Estructura de paquetes
Raíz: `com.cmarcillovargas.turismovalencia`. Un paquete existe solo cuando tiene archivos.

| Paquete | Estado | Contenido |
|---|---|---|
| (raíz) | creado | `MainActivity` (con el gancho de depuración, ver abajo), `TurismoApp` (crea el `AppContainer`) |
| `data` | creado | `AppContainer` (repositorios únicos de la app), `model/` (`ContentItem`, `Offer`, `Reservation`…), `fake/` (contenido basado en la web: Qué ver, Gastronomía, Eventos, Alojamientos, Sostenible y 16 ofertas), `repo/` (contenido, favoritos, descargas, reservas, ajustes, notificaciones, búsqueda reciente, `NetworkMonitor`, `ValBotEngine`) |
| `util` | creado | `Format.kt` (distancias, euros, fechas) e `Intents.kt` (abrir web, llamar, correo, mapa y compartir) |
| `ui/theme` | creado | `Color`, `Type`, `Shape`, `Theme`, `Dimens` (espaciado), `ThemePreview` |
| `ui/components` | creado | componentes reutilizables sin lógica de negocio; catálogo en `docs/componentes.md` |
| `ui/screens/bienvenida` | creado | A1–A6: splash, onboarding (3 páginas), permiso de ubicación, idioma, y `BienvenidaFlow` que los encadena |
| `ui/screens/explorar` | creado | B1–B7: inicio, búsqueda, categorías, detalle (lugar y evento), tickets & tours, sostenible |
| `ui/screens/mapa` | creado | C1–C2: mapa con filtros y ruta a pie (mapa dibujado, `TODO(mapa)`) |
| `ui/screens/valbot` | creado | D1–D2: asistente (respuestas por reglas; con y sin conexión) |
| `ui/screens/favoritos` | creado | E1–E2: favoritos y descargas sin conexión (también H3, favoritos vacío) |
| `ui/screens/reservas` | creado | F1–F3: mis reservas, reservar (2 pasos), reserva confirmada con QR |
| `ui/screens/perfil` | creado | G1–G2: perfil y ajustes (idioma, tamaño de texto, ubicación…), notificaciones |
| `ui/screens/web` | creado | Páginas de la web: `WebPageScreen` (`pagina/{id}`: Lo esencial, Planes, guías de Tickets & Tours y textos legales) y `ContactoScreen` (`contacto`) |
| `ui/screens/estados` | creado | H2 (inicio cargando). H1 (sin conexión), H3 y H4 viven dentro de sus pantallas |
| `navigation` | creado | `Routes`, `AppNavHost` (grafo), `MainApp` (Scaffold + barra inferior solo en las rutas que la llevan) |
| `ui` | creado | `AppRoot`: bienvenida ↔ app principal y escala de texto elegida en Perfil |

Fuera del módulo `app/`: `tools/web-import/` (scripts que regeneran el contenido de la web), `tools/verify/` (pruebas de contenido e interfaz frente a la web; ver su README) y `backend/` (pendiente), en la raíz del repositorio, junto a `app/`.

Diseño de referencia: capturas de 390×844 (exportadas a 2x) en `docs/Figma/`.

## Depurar una pantalla desde adb
En builds de depuración `MainActivity` acepta extras para saltarse la bienvenida:
```bash
adb shell am start -n com.cmarcillovargas.turismovalencia/.MainActivity --es route detalle/mercado-central
# otros extras: --ez offline true (simula sin conexión) · --ez emptyFavorites true (vacía los favoritos)
```
Rutas: `inicio`, `mapa`, `valbot`, `favoritos`, `reservas`, `busqueda`, `categoria/que-ver`, `detalle/<id>`, `entradas?tab=experiencias`, `sostenible`, `ruta/<id>`, `reservar/<offerId>`, `reserva/seed-1`, `descargas`, `perfil`, `notificaciones`, `contacto`, `pagina/<Pagina>` (p. ej. `pagina/Navidad`, `pagina/AvisoLegal`). Para comparar con el Figma: emulador a 780×1688 px / 320 dpi (`adb shell wm size 780x1688`, `wm density 320`) = 390×844 dp; al terminar, `wm size reset` y `wm density reset`.

## Pruebas
`tools/verify/` comprueba contra la web que están todas las páginas, índices, fotos y enlaces, y en el emulador que cada chip, sección, tarjeta y guía abre lo que debe y que cada tarjeta muestra su foto. Ejecútalas tras tocar contenido, navegación o imágenes (`python tools/verify/verify_data.py`, `verify_live.py`, `verify_ui.py`, `verify_img.py`).

## Convenciones
- **MVVM + state hoisting**: el `ViewModel` expone un `UiState` inmutable; los composables reciben estado y callbacks, no el `ViewModel`.
- **Nada hardcodeado en las pantallas**: ni colores (`MaterialTheme.colorScheme.*` o `MaterialTheme.appColors.*`), ni tipografías (`MaterialTheme.typography.*`), ni formas (`MaterialTheme.shapes` o `CardShape`/`BottomSheetShape`/`PillShape`), ni espaciado (`Spacing.*`), ni textos. Las medidas propias de una pantalla van como constantes privadas con nombre («Medidas del diseño»).
- **Reutilizar antes de crear**: mira `docs/componentes.md`; si falta una variante, amplía el componente existente y actualiza ese documento.
- **Color**: el naranja de marca `#EB5A00` solo para iconos y gráficos (`appColors.brand`, 3,5:1). Texto y botones usan `primary` = `#C44D00` (4,76:1). No usar la paleta del modo «MD3 · Prompt».
- **Textos** en `res/values/strings.xml`, solo español por ahora (`TODO(i18n)`). Excepción: textos de muestra de `@Preview`.
- **Eyebrows**: `MaterialTheme.typography.eyebrow` y el texto con `.uppercase()`.
- **Ninguna clave viaja en el APK**: ni API keys (Gemini incluida), ni contraseñas, ni tokens, ni en el código, ni en `strings.xml`, ni en `BuildConfig`. Viven solo como variables de entorno del backend. `local.properties` y `.env` no se suben a git.
- **Fotos de la web**: nunca como `R.drawable`; van en `assets/img/<ruta de la web>` y se piden con `PlaceImage(image = null, …, assetPath = ruta)` (si el archivo no existe se pinta el fondo con el icono de la sección). Las fotos del diseño (`.fig`) siguen siendo `R.drawable` y tienen prioridad.
- **Datos**: las pantallas nunca leen los `Fake*` directamente; piden al repositorio de `AppContainer` desde su `ViewModel` (`containerFactory { MiViewModel(it) }`). Cuando llegue el backend solo cambian los repositorios.
- **Accesibilidad**: áreas táctiles ≥ 48dp (`MinTouchTarget`); `contentDescription` en imágenes informativas y `null` en iconos decorativos junto a un texto.
- Solo tema claro (`TODO(dark)`).

## Tags de pendientes
`TODO(nav)` navegación · `TODO(api)` datos remotos · `TODO(room)` base de datos local · `TODO(mapa)` mapa y rutas · `TODO(permisos)` permisos en tiempo de ejecución · `TODO(i18n)` idiomas · `TODO(dark)` tema oscuro · `TODO(splash)` integrar la SplashScreen API del sistema. El grafo de navegación ya existe, así que `TODO(nav)` casi no queda.

## Documentación
- `docs/progreso.md`: **al terminar cada tarea, actualizarlo** (Hecho, Pendiente por tag y Decisiones).
- `docs/componentes.md`: catálogo de componentes compartidos y tokens del theme; mantenerlo al día.
- `docs/Figma/`: capturas del diseño (el A5 solo existe dentro de `Pantallas/A · Bienvenida y permisos.png`). `docs/guia-estilo.md`, `docs/figma.md` y `docs/propuesta.md` aún no están en el repo.
