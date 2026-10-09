# Progreso

Última actualización: 2026-10-09.

## Hecho
- **Base del proyecto** (sin pantallas ni componentes), `./gradlew assembleDebug` compila.
  - Theme en `ui/theme`: `Color.kt` (paleta como tokens semánticos), `Type.kt` (Source Sans 3), `Shape.kt`, `Theme.kt` (solo claro) y `ThemePreview.kt` (`@Preview` de paleta, tipografía y formas).
  - `MainActivity` limpia: edge-to-edge con barras claras y superficie del tema, sin navegación.
  - Fuentes Source Sans 3 (Regular, Italic, SemiBold, Bold, ExtraBold) en `res/font/`.
  - `CLAUDE.md` en la raíz.
  - Eliminado `res/values/colors.xml` de la plantilla (sin usos).
- **Repositorio git**: commit inicial en la rama `main` (renombrada desde `master`), solo en local. `.gitignore` ampliado con `.env` y `node_modules/`.
- **`CLAUDE.md` ampliado** con el mapa (osmdroid), el backend previsto y la regla «ninguna clave viaja en el APK».

## Pendiente (por tag)
- Crear el repositorio en GitHub, añadir el remoto y hacer el primer `push` (no hay remoto todavía).
- `backend/`: crearlo cuando empiece su fase.
- `TODO(nav)`: host de navegación y pantalla de inicio (`MainActivity`).
- `TODO(dark)`: tema oscuro (`Color.kt`, `Theme.kt`); el Figma no lo define.
- `TODO(i18n)`: textos solo en español.
- `TODO(api)`, `TODO(room)`, `TODO(mapa)`, `TODO(permisos)`: sin empezar.
- **Verificar el theme contra `docs/guia-estilo.md`** cuando esté en el repo (ver Decisiones). Falta también `docs/figma.md` y `docs/propuesta.md`.
- Añadir peso Medium (500) de Source Sans 3 si el diseño lo usa: hoy Compose cae a Regular.

## Decisiones
- **Paleta provisional medida de las capturas.** `docs/guia-estilo.md` no existe en el repo, así que los colores se midieron por píxel en `docs/Figma/Explorar/Explorar.png` y `docs/Figma/Favoritos Reservas Pefil/G1 · Perfil y ajustes.png`. Coinciden con lo indicado: `#EB5A00` (marca) y `#C44D00` (primary-strong). Resto: texto `#1A1A1A`/`#555555`/`#6E6E6E`, navy `#2C3E50`, verde `#2E7D32`/`#1B7A43`/`#E3F4EA`, fondo `#F9F9F9`, chip neutro `#F0F0F0`, divisor `#E5E5E5`, píldora seleccionada `#FDEBDD`, estrella `#F2A900`, favorito `#E0453A`.
- **Fondo `#F9F9F9` unificado.** Explorar usa `#F9F9F9` y G1 `#F7F7F7`; diferencia imperceptible.
- **Rutas de capturas.** Las capturas están en `docs/Figma/…`, no en `docs/diseno/pantallas/`; son de 780×1688 (2x de 390×844).
- **Mapeo a Material 3.** `primary` = `#C44D00`; el naranja `#EB5A00` va como `appColors.brand` (no es un rol de M3). `secondary` = navy, `tertiary` = verde eco/éxito, `secondaryContainer` = gris neutro de los chips, `primaryContainer` = píldora de la barra inferior. `error`, `scrim` y roles inversos quedan con los valores por defecto de M3 (no aparecen en las capturas).
- **Contrastes (WCAG).** `primary` sobre blanco 4,76:1 y sobre `#F9F9F9` 4,52:1; `onSurface` 16,5:1; `onSurfaceVariant` 7,1:1; `outline` (placeholders) 4,8:1; verde sobre su fondo 4,7:1. `onPrimaryContainer` sobre `primaryContainer` 4,11:1: solo iconos, no texto pequeño.
- **Escala tipográfica provisional.** Tamaños estimados midiendo la altura de mayúscula en las capturas: 22 (secciones), 20 (nombre de perfil), 17 (título de tarjeta y barra superior), 16 (cuerpo), 13 (metadatos y chips), 11 (barra inferior y eyebrow). El tracking de los títulos (−0,02 em) y el peso Bold del eyebrow son valores propios; el +14 % del eyebrow es el indicado.
- **Eyebrow** como extensión `Typography.eyebrow`; las mayúsculas se aplican con `.uppercase()` en el texto, porque un `TextStyle` no puede forzarlas.
- **Formas.** Tarjeta 16dp, bottom sheet 24dp (`extraLarge`), píldora para chips (`small`); los botones ya son píldora en M3.
- **Sin color dinámico ni `darkTheme`** en `TurismoValenciaTheme`: la marca debe verse igual en todos los móviles.
- **Barras del sistema** en estilo claro fijo, para que los iconos no desaparezcan con el móvil en modo oscuro.
- **Estructura de paquetes**: solo existe `ui/theme`; el resto está documentado en `CLAUDE.md`.
- **Repositorio**: el proyecto Android vive en la raíz (módulo `app/`) y `backend/` irá al lado. Es el valor por defecto; si se prefiere mover el proyecto Android a una subcarpeta, hay que hacerlo antes de crecer.
- **`.idea/` sí se versiona** (solo configuración compartible: estilo de código, Gradle, JDK); `workspace.xml` y cachés quedan ignorados.
