# Progreso

Última actualización: 2026-10-09 (frontend completo).

## Hecho
- **Base del proyecto**: theme en `ui/theme` (`Color`, `Type`, `Shape`, `Theme`, `Dimens`, `ThemePreview`), fuentes Source Sans 3 en `res/font/`, `MainActivity` edge-to-edge con barras claras y `CLAUDE.md`.
- **Repositorio git**: commit inicial en `main`, subido a GitHub. `.gitignore` con `.env` y `node_modules/`.
- **Grupo A · Bienvenida** (`ui/screens/bienvenida`), solo frontend con datos fake y sin librería de navegación:
  - **A1 Splash** (`SplashScreen`): logo, «Valencia», lema, tres puntos y versión de la app (leída del `PackageManager`). 1 preview.
  - **A2–A4 Onboarding** (`OnboardingScreen`, una sola pantalla con la página en el `UiState`): foto con degradado superior, «Paso x de 3», título y texto. «Siguiente» avanza, también se desliza y «Atrás» vuelve a la página anterior. «Saltar» sale del onboarding y en la última página el botón dice «Empezar». 3 previews.
  - **A5 Permiso de ubicación** (`PermisoUbicacionScreen`): diálogo con las opciones Precisa/Aproximada (se puede cambiar de una a otra), tres botones y panel explicativo sobre un mapa dibujado. 2 previews (una por opción elegida). No pide el permiso real.
  - **A6 Idioma** (`IdiomaScreen`): `ModalBottomSheet` con ES/VA/EN/FR, selección única y «Aplicar». 2 previews (ES y EN). No cambia el idioma real.
  - **Recorrido** (`BienvenidaFlow`): Splash (1,5 s) → Onboarding → Permiso → Idioma sobre la pantalla de inicio provisional (`ThemeShowcase`). «Atrás» funciona en onboarding, permiso e idioma.
- **Componentes compartidos** en `ui/components` (ver `docs/componentes.md`): `AppButton`, `PageIndicator`, `Eyebrow`, `Scrim`, `MapPlaceholder`, `SystemBarIcons`.
- **Verificación en emulador** (Pixel 8, API 37, pantalla ajustada a 390×844 dp): todas las pantallas comparadas con las capturas de Figma midiendo posiciones y tamaños; casi todo a ≤ 2 dp. Recorrido completo probado con toques, deslizamiento y botón atrás.
- **Frontend completo y funcional (grupos B–H)**, todo con datos en memoria y navegación real (`navigation/`):
  - **B1 Inicio** (carrusel de destacados con Fallas primero, chips de categoría, «Cerca de ti», chip de idioma que abre el sheet, campo de búsqueda) y **H2** (esqueleto de carga de 1,1 s). **B2 Búsqueda** (recientes, tendencias, sugerencias en vivo y **H4** sin resultados con salida a ValBot). **B3 Categoría** (filtros, orden por cercanía/valoración, lista/mapa). **B4/B5 Detalle** de lugar y de evento (hero con cifras, galería, información práctica, actos con recordatorio, guardar sin conexión, compartir y añadir al calendario con intents del sistema). **B6 Tickets & Tours** (Entradas/Tours/Experiencias, búsqueda). **B7 Turismo sostenible**.
  - **C1 Mapa** (buscador, filtros por categoría, solo guardados, zoom, pins y hoja inferior con «Cómo llegar») y **C2 Ruta a pie** (4 modos, pasos, «Ruta guardada», abrir navegación en Google Maps).
  - **D1/D2 ValBot** (respuestas por reglas con tarjetas de lugares, sugerencias, dictado por voz con `RecognizerIntent`, estado sin conexión con acceso a favoritos y mapa guardado).
  - **E1 Favoritos** (resumen sin conexión, Todos/Lugares/Eventos, **H3** vacío) y **E2 Descargas** (cuota de 500 MB, solo Wi-Fi, actualización automática, mapa del centro, borrar y actualizar).
  - **F1 Mis reservas**, **F2 Reservar** (semana, horario, adultos/niños, correo, total) y **F3 Reserva confirmada** con QR real; las reservas nuevas aparecen en F1 y en Notificaciones.
  - **G1 Perfil** (idioma, notificaciones, ubicación, descargas, **tamaño del texto** que escala toda la app, privacidad, cerrar sesión que vuelve a la bienvenida) y **G2 Notificaciones** (leídas al abrirlas).
  - **H1 Sin conexión** en Inicio, Mapa y ValBot, con el estado real de la red (`NetworkMonitor`).
  - Contenido de ejemplo sacado de la web (13 Qué ver, 9 platos, 6 alojamientos, 5 eventos, 7 temas sostenibles, 16 ofertas).
- **Calibración contra el Figma**: las 22 capturas de B–H se compararon con el emulador (780×1688 px, 320 dpi) en hojas «diseño | app» y se ajustaron tamaños de letra, alturas y espaciados. Diferencias que quedan: la barra de estado del emulador es más alta que la del diseño (todo baja ~12 dp), los textos de ejemplo cambian (fechas relativas a hoy, «60 MB» calculado), y los pins del mapa dibujado no coinciden con los de OpenStreetMap del diseño.
- **Pruebas funcionales en el emulador**: recorrido bienvenida → app, favoritos, descargas, idioma, búsqueda, ValBot (pregunta y respuesta), reserva completa (F2 → F3 → lista), tamaño de texto, filtros del mapa, cerrar sesión y teclado (la barra inferior se oculta y el contenido sube).

## Pendiente (por tag)
- `TODO(api)`: sustituir los repositorios en memoria por el backend (contenido, reservas y pago, ValBot con Gemini, ofertas de la tabla `Actividad`); refrescar fichas guardadas.
- `TODO(room)`: guardar en local favoritos, descargas, reservas, ajustes y «bienvenida ya vista» (hoy todo se pierde al cerrar la app).
- `TODO(mapa)`: cambiar el mapa dibujado (`MapCanvas`) por osmdroid con teselas de OpenStreetMap, rutas reales y mapa sin conexión; la API del componente se mantiene.
- `TODO(permisos)`: pedir el permiso de ubicación real (precisión, «mientras se usa» / «solo esta vez» / «no permitir») y el de notificaciones; programar el aviso de la mascletà.
- `TODO(i18n)`: aplicar el idioma elegido a la app y a ValBot (hoy se guarda la elección, pero los textos están solo en español).
- `TODO(splash)`: integrar la SplashScreen API del sistema (en Android 12+ aparece antes el splash del sistema con el icono por defecto).
- `TODO(dark)`: el Figma no define modo oscuro.
- Fotos: solo hay las que venían en el `.fig`. Gastronomía, Alojamientos y algunos lugares usan un fondo con el icono de su categoría hasta tener las imágenes de la web (`public/img/…` del repositorio `TurismoValencia`).
- Icono de la app: sigue el icono por defecto; falta el logotipo.
- `backend/`: crearlo cuando empiece su fase.
- **Verificar el theme contra `docs/guia-estilo.md`** cuando esté en el repo. Faltan también `docs/figma.md` y `docs/propuesta.md`.
- Peso Medium (500) de Source Sans 3 si el diseño lo usa: hoy Compose cae a Regular.

## Decisiones
- **Adaptación del encargo.** Las capturas están en `docs/Figma/…` (780×1688 = 2x de 390×844), no en `docs/diseno/pantallas/`. **A5 no tiene archivo propio**: se recortó de `docs/Figma/Pantallas/A · Bienvenida y permisos.png`. `docs/componentes.md` no existía y se creó. «Frontend funcional» se interpretó como interacción real y recorrido encadenado en memoria, sin persistir nada ni usar la librería de navegación.
- **Fotos y logo** tomados del archivo `.fig` (780×940 px y 192×192 px, calidad original) y guardados en `res/drawable-nodpi/`. Las capturas no sirven porque llevan encima la barra de estado y «Saltar».
- **Paleta provisional medida de las capturas.** `docs/guia-estilo.md` no existe en el repo, así que los colores se midieron por píxel. Base: `#EB5A00` (marca) y `#C44D00` (primary-strong), texto `#1A1A1A`/`#555555`/`#6E6E6E`, navy `#2C3E50`, verde `#2E7D32`/`#1B7A43`/`#E3F4EA`, fondo `#F9F9F9`, chip neutro `#F0F0F0`, divisor `#E5E5E5`, `#FDEBDD` para seleccionado. Añadidos con el grupo A: texto sobre seleccionado `#A84000`, controles inactivos `#CCCCCC`, azul de ubicación `#3498DB` y colores del mapa.
- **Mapeo a Material 3.** `primary` = `#C44D00`; el naranja `#EB5A00` va como `appColors.brand` (no es un rol de M3). `secondary` = navy, `tertiary` = verde, `secondaryContainer` = gris de los chips, `primaryContainer` = fondo de lo seleccionado. `error` e inversos quedan por defecto de M3.
- **Contrastes (WCAG).** `primary` sobre blanco 4,76:1 y sobre `#F9F9F9` 4,52:1; `onSurface` 16,5:1; `onSurfaceVariant` 7,1:1; `outline` 4,8:1; verde sobre su fondo 4,7:1; `onPrimaryContainer` (`#A84000`) sobre `primaryContainer` 5,3:1.
- **Escala tipográfica** ajustada ahora con anchos de texto medidos en las capturas, con el mismo método sobre diseño y emulador: `headlineLarge` 33 (título del splash), `headlineMedium` 26 (título del onboarding), `headlineSmall` 21 (títulos de sección), `titleLarge` 20, `titleMedium` 17, `titleSmall` 15 (botones rellenos), `bodyLarge` 16, `bodyMedium` 15 (subtítulo del sheet), `bodySmall` 13, `labelMedium` 13, `labelSmall` 11. Los botones de diálogo usan 16sp SemiBold. El tracking de los títulos (−0,02 em) es un valor propio; el +14 % del eyebrow es el indicado.
- **Tokens nuevos** en `ui/theme/Dimens.kt`: `Spacing` (múltiplos de 4dp), `MinTouchTarget` (48dp) y `ScrimAlpha` (0,6, medido).
- **Dependencias añadidas** porque ya se usan: `lifecycle-viewmodel-compose` y `lifecycle-runtime-compose` (2.10.0). La versión `lifecycleRuntimeKtx` subió de 2.6.1 a 2.10.0 para que las tres vayan alineadas. No se añadió `material-icons`: la flecha y el icono de ubicación son vector drawables (`ic_arrow_forward`, `ic_my_location`).
- **Estado del recorrido** con `rememberSaveable`; los `ViewModel` cuelgan de la Activity, así que sobreviven al cambiar de paso (volver atrás conserva la página del onboarding y la selección de precisión). Nada se guarda al cerrar la app.
- **Barras del sistema** con `SystemBarIcons`: iconos claros sobre splash, fotos y velos; oscuros en el resto. Al salir de la pantalla se restauran.
- **Idioma como `ModalBottomSheet`**, no como pantalla: así será al conectarlo a Explorar. En los `@Preview` no se ve (abre su propia ventana), por eso allí se reproduce el sheet sobre el velo.
- **Desviaciones conocidas respecto al diseño** (todas ≤ 3 dp): el diálogo de A5 sale 2 dp más alto; el mapa de fondo es un dibujo estilizado; en móviles con la barra de estado más alta que 24dp «Saltar» y el contenido bajan lo mismo (la app es edge-to-edge); el resplandor del botón principal es algo más difuso.
- **Fondo `#F9F9F9` unificado.** Explorar usa `#F9F9F9` y G1 `#F7F7F7`; diferencia imperceptible.
- **Eyebrow** como extensión `Typography.eyebrow`; las mayúsculas se aplican con `.uppercase()` porque un `TextStyle` no puede forzarlas.
- **Formas.** Tarjeta 16dp, bottom sheet 24dp (`extraLarge`), píldora para chips (`small`); los botones son píldora.
- **Sin color dinámico ni `darkTheme`** en `TurismoValenciaTheme`: la marca debe verse igual en todos los móviles.
- **Repositorio**: el proyecto Android vive en la raíz (módulo `app/`) y `backend/` irá al lado. `.idea/` se versiona (solo configuración compartible).
- **«Frontend funcional» = todas las pantallas del Figma navegables con datos en memoria.** No hay persistencia, red ni mapa real; cada repositorio tiene el `TODO` de su fase.
- **Arquitectura de datos.** `TurismoApp` crea un `AppContainer` con repositorios únicos; los `ViewModel` los reciben con `containerFactory`. Así, cambiar a Room/Retrofit solo toca `data/repo`.
- **Navegación**: `navigation-compose` con rutas de texto (`Routes`), barra inferior solo en las rutas que la llevan en el diseño (`Routes.tabOf`) y un `ViewModel` por entrada del grafo cuando la ruta lleva argumento. La bienvenida va fuera del grafo (`AppRoot`) y «Cerrar sesión» vuelve a ella.
- **Dependencias añadidas**: `material-icons-extended` (los iconos de las pantallas nuevas; sustituye a la decisión de usar solo vectores propios; `ic_my_location` e `ic_database` siguen siendo vectores), `navigation-compose` 2.10.2, `zxing-core` 3.5.4 (QR real, sin la librería de cámara) y `desugar_jdk_libs` 2.1.5 (`java.time` desde minSdk 24).
- **Mapa dibujado** (`MapCanvas`): un lienzo de 700×1100 dp con zoom y desplazamiento propios y pins como composables. Los lugares con `onMap = false` (platos menores, temas sostenibles…) no salen como pin para no saturar el mapa.
- **ValBot por reglas** (`ValBotEngine`): detecta palabras clave (paella, gratis, playa, museos, ruta…) y responde con texto y tarjetas de lugares reales; sin conexión muestra el aviso del diseño. El sustituto real es Gemini detrás del backend, nunca con la clave en la app.
- **Teclado**: `MainApp` oculta la barra inferior y aplica `imePadding()` al contenido cuando el teclado está abierto.
- **Depuración**: `MainActivity` acepta los extras `route`, `offline` y `emptyFavorites` solo en builds de depuración (ver `CLAUDE.md`); sirven para abrir cualquier pantalla con adb y compararla con el Figma.
- **Pesos de fuente**: los pesos Medium del Figma se aproximan con SemiBold/Regular porque la fuente empaquetada no tiene Medium.
