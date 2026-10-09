# Turismo Valencia (app Android)

App nativa para turistas, derivada de la web Turismo Valencia. Proyecto del TFG de DAM.

## Stack
- Kotlin 2.2 + Jetpack Compose (BOM 2026.02.01) + Material 3. AGP 9.4, compileSdk/targetSdk 37, minSdk 24.
- Arquitectura MVVM. Una pantalla = `Screen` + `UiState` + `ViewModel`.
- Tipografía Source Sans 3 empaquetada en `res/font` (la app funciona sin conexión).
- Mapa previsto: osmdroid como primera opción, Google Maps SDK de reserva si la prueba de humo falla (`TODO(mapa)`).
- Backend previsto en `backend/` (Node.js + MySQL; hosting Railway por confirmar; ValBot usa Gemini). Aún no existe. La app lo llama por HTTPS (Retrofit + corrutinas).
- Dependencias solo cuando se usan: Room, Retrofit, osmdroid, Coil, Navigation… se añaden en su fase, no antes.

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
| (raíz) | creado | `MainActivity` |
| `ui/theme` | creado | `Color`, `Type`, `Shape`, `Theme`, `ThemePreview` |
| `ui/components` | pendiente | componentes reutilizables sin lógica de negocio |
| `ui/screens/bienvenida` | pendiente | A1–A6: splash, onboarding, idioma, permisos |
| `ui/screens/explorar` | pendiente | B1–B7: inicio, búsqueda, categorías, detalle, tickets, sostenible |
| `ui/screens/mapa` | pendiente | C1–C2: mapa y ruta a pie |
| `ui/screens/valbot` | pendiente | D1–D2: asistente IA (con y sin conexión) |
| `ui/screens/favoritos` | pendiente | E1–E2: favoritos y descargas sin conexión |
| `ui/screens/reservas` | pendiente | F1–F3: reservas, reservar, entrada QR |
| `ui/screens/perfil` | pendiente | G1–G2: perfil, ajustes, notificaciones |
| `ui/screens/estados` | pendiente | H1–H4: sin conexión, cargando, vacíos |
| `navigation` | pendiente | grafo de navegación y rutas |

Fuera del módulo `app/`: `backend/` (pendiente) irá en la raíz del repositorio, junto a `app/`.

Diseño de referencia: capturas de 390×844 (exportadas a 2x) en `docs/Figma/`.

## Convenciones
- **MVVM + state hoisting**: el `ViewModel` expone un `UiState` inmutable; los composables reciben estado y callbacks, no el `ViewModel`.
- **Nada hardcodeado en las pantallas**: ni colores (`MaterialTheme.colorScheme.*` o `MaterialTheme.appColors.*`), ni tipografías (`MaterialTheme.typography.*`), ni formas (`MaterialTheme.shapes` o `CardShape`/`BottomSheetShape`/`PillShape`), ni textos.
- **Color**: el naranja de marca `#EB5A00` solo para iconos y gráficos (`appColors.brand`, 3,5:1). Texto y botones usan `primary` = `#C44D00` (4,76:1). No usar la paleta del modo «MD3 · Prompt».
- **Textos** en `res/values/strings.xml`, solo español por ahora (`TODO(i18n)`). Excepción: textos de muestra de `@Preview`.
- **Eyebrows**: `MaterialTheme.typography.eyebrow` y el texto con `.uppercase()`.
- **Ninguna clave viaja en el APK**: ni API keys (Gemini incluida), ni contraseñas, ni tokens, ni en el código, ni en `strings.xml`, ni en `BuildConfig`. Viven solo como variables de entorno del backend. `local.properties` y `.env` no se suben a git.
- **Áreas táctiles ≥ 48dp**.
- Solo tema claro (`TODO(dark)`).

## Tags de pendientes
`TODO(nav)` navegación · `TODO(api)` datos remotos · `TODO(room)` base de datos local · `TODO(mapa)` mapa y rutas · `TODO(permisos)` permisos en tiempo de ejecución · `TODO(i18n)` idiomas · `TODO(dark)` tema oscuro.

## Documentación
- `docs/progreso.md`: **al terminar cada tarea, actualizarlo** (Hecho, Pendiente por tag y Decisiones).
- `docs/Figma/`: capturas del diseño. `docs/guia-estilo.md`, `docs/figma.md` y `docs/propuesta.md` aún no están en el repo.
