# Modo oscuro

La app **sigue el modo claro u oscuro del móvil** (Ajustes → Pantalla → Tema oscuro) y cambia al instante, con la app abierta. **Perfil → Tema**
deja fijar **Claro** u **Oscuro** con independencia del móvil, o volver a **«Sigue al del sistema»** (lo que hace por defecto). El diseño de
referencia son las imágenes de `docs/Figma/Modo oscuro/` (16 pantallas y 2 mosaicos); la fila de «Tema» no está en ellas.

## Cómo funciona

- **El tema de Compose** (`ui/theme/Theme.kt`): `TurismoValenciaTheme(darkTheme = isSystemInDarkTheme())` elige entre `LightColorScheme` y
  `DarkColorScheme` (Material 3) y entre `LightAppColors` y `DarkAppColors` (los colores propios). `MaterialTheme.isDark` lo da a quien tenga que
  saberlo (hoy, solo el mapa). Sin color dinámico: la marca se ve igual en todos los móviles.
- **El tema de Android** (`res/values/themes.xml`): `Theme.TurismoValencia` hereda de `Theme.AppCompat.DayNight.NoActionBar` y pone el fondo de la
  ventana (`@color/window_background`, con su versión en `values-night/colors.xml`), que es lo que se ve antes de que Compose pinte: sin él, en
  oscuro saldría un destello blanco al abrir una pantalla. AppCompat recrea la actividad cuando cambia el modo (no se declara `configChanges`).
- **La pantalla de arranque del sistema** (`splash_background`) pasa de `#C44D00` a `#AF3C02` en oscuro, el mismo relleno que el A1 de Compose.
- **Las barras del sistema**: `enableEdgeToEdge(SystemBarStyle.auto(…))` pone los iconos claros u oscuros según el modo. Las pantallas con foto o
  fondo de color siguen pidiendo iconos claros con `SystemBarIcons`.
- **El selector** (Perfil → Tema, `ThemeMode`: `System`, `Light`, `Dark`): `SettingsRepository.setThemeMode` lo guarda en Room (`settings.themeMode`,
  v5) y se lo pide a AppCompat (`ThemeApplier` → `AppCompatDelegate.setDefaultNightMode`), que recrea la actividad con el modo nuevo; como el resto
  del tema sigue el modo de la actividad (`isSystemInDarkTheme()`, `SystemBarStyle.auto`, `values-night`), todo cambia a la vez. AppCompat no guarda
  el modo entre arranques, así que Room (la fuente de verdad que enseña Perfil) tiene una **copia rápida** en SharedPreferences (`AppThemePrefs`,
  que `AppCompatThemeApplier` escribe al cambiar el modo y `AppContainer` repasa al arrancar): `TurismoApp.onCreate` la lee y llama a
  `setDefaultNightMode` **antes de que exista ninguna actividad**, cuando Room aún no está abierto. Sin ella, quien fijó un modo distinto al del móvil
  vería un primer cuadro con el del móvil y una recreación en cada arranque. La pantalla de arranque del sistema es lo único que sigue al móvil.

## Paleta

Los valores medidos de las imágenes de diseño y comprobados contra WCAG AA (texto normal 4,5:1; iconos y gráficos 3:1) en `ThemeContrastTest`.

| Rol | Claro | Oscuro | Para qué |
|---|---|---|---|
| `background` | `#F9F9F9` | `#0A0E11` | Fondo de pantalla |
| `surface` | `#FFFFFF` | `#11171D` | Tarjetas, barras y hojas |
| `surfaceContainerHigh` | `#F0F0F0` | `#1B232B` | Chips neutros, campos de texto, burbujas |
| `outlineVariant` | `#E5E5E5` | `#27313A` | Divisores y bordes de tarjeta |
| `onSurface` / `onSurfaceVariant` | `#1A1A1A` / `#555555` | `#EDF0F2` / `#A5AFB8` | Texto principal y secundario |
| `outline` | `#6E6E6E` | `#8C97A1` | Placeholders, chevrons, eyebrows |
| `primary` | `#C44D00` | `#EB5A00` | **Texto, iconos y bordes naranjas** (también los de los componentes de Material por defecto) |
| `action` / `onAction` | `#C44D00` / blanco | `#AF3C02` / blanco | **Relleno** de botones, chips seleccionados, día elegido y pantalla de arranque |
| `primaryContainer` / `onPrimaryContainer` | `#FDEBDD` / `#A84000` | `#331B0E` / `#FFB27A` | Elemento seleccionado (píldora de la barra inferior, botón tonal) |
| `secondary` | `#2C3E50` | `#B4C3D0` | Iconos y texto de pizarra (el «navy» como primer plano) |
| `navy` / `onNavy` | `#2C3E50` / blanco | `#26323D` / `#E6ECF1` | **Relleno** de pizarra: avatar, aviso sin conexión, marcador «Qué ver» |
| `tertiary` | `#2E7D32` | `#6FCF7A` | Verde de texto e iconos |
| `eco` | `#2E7D32` | `#2E7D32` | Verde de relleno con icono blanco (marcador «Sostenible») |
| `error` / `errorContainer` | `#B71C1C` / `#FDE7E7` | `#FF8A80` / `#3A1514` | Errores y acciones destructivas |
| `inverseSurface` | `#1A2A4A` | `#1E2832` | Snackbar (en oscuro es una barra oscura, no invertida) |
| `bot` / `botAccent` | `#C0392B` | `#9A2E22` / `#FF8A7A` | ValBot: relleno (cabecera, burbuja, enviar) y primer plano (sugerencias) |
| `paper` / `onPaper` | blanco / `#1A1A1A` | blanco / `#1A1A1A` | Papel y tinta del QR y disco del logotipo: **no cambian** |
| `brand` | `#EB5A00` | `#EB5A00` | Naranja de marca en iconos y gráficos: **no cambia** |

El resto de `appColors` (`warning`, `brandSoft`, `ecoContainer`, `slateContainer`, `favorite`, `location`, `controlInactive` y los de `map*`) tiene su
versión oscura en `ui/theme/Color.kt`.

## Decisiones

- **Dos naranjas, uno para pintar texto y otro para rellenar.** En claro, `#C44D00` vale para las dos cosas porque el contraste es el mismo en los dos
  sentidos (4,76:1: naranja sobre blanco y blanco sobre naranja). En oscuro no hay un solo naranja que cumpla las dos: para que el naranja *como texto*
  sobre un fondo casi negro llegue a 4,5:1 tiene que ser claro (`#EB5A00`: 5,5:1) y para que el *texto blanco* sobre un relleno llegue a 4,5:1 tiene que
  ser oscuro (`#AF3C02`: 6:1). Por eso `primary` es el naranja de texto y `action` el de relleno. Lo mismo pasa con `secondary`/`navy`, `bot`/`botAccent` y
  `tertiary`/`eco`. **Regla**: texto, iconos y bordes sobre un fondo, con `MaterialTheme.colorScheme.*`; el relleno de un botón, chip seleccionado, avatar o
  aviso, con `MaterialTheme.appColors.action` / `.navy`.
- **Por defecto sigue el sistema; el selector es un añadido sin diseño.** El diseño no tiene una fila de «Tema» en Perfil: se añadió con los
  componentes que ya había (`SettingRow` y el diálogo de opciones con botones de radio del tamaño del texto) y una columna más en Room (v5,
  `Migration4To5`, con «System» por defecto, así que quien ya tenía la app no nota nada). Se eligió **AppCompat** (`setDefaultNightMode`) y no pasarle
  un valor a `TurismoValenciaTheme(darkTheme = …)`: así también cambian el tema de Android (el fondo de la ventana de `values-night`), los iconos de
  las barras del sistema y los recursos de cualquier ventana, todo con lo que la app ya hacía al cambiar el modo del móvil. A cambio, hace falta la
  copia rápida de arriba.
- **El QR es siempre tinta oscura sobre papel blanco** (`paper`/`onPaper`), también en oscuro: el lector del acceso lo tiene que leer, y un QR invertido no lo
  leen todos los lectores (el margen blanco también cuenta). El disco blanco del logotipo del arranque es lo mismo.
- **El mapa oscuro es un filtro de color sobre las teselas de OpenStreetMap** (`DarkTilesFilter` en `OsmMap.kt`): invierte la luminosidad, gira el tono 180° y
  enfría el resultado hacia el azul del tema, que es el tono del mapa de las imágenes. No se cambia de proveedor de teselas: el estándar de OpenStreetMap es el
  que permite su política de uso y la atribución se mantiene. Las calles quedan más oscuras que el suelo, con su borde claro, en vez de más claras como en las
  imágenes: es lo que se puede hacer con una matriz de color. El mapa dibujado (permiso y Contacto) tiene sus variantes en `appColors.map*`.
- **`surfaceTint` es la propia superficie en oscuro**: así elevar una tarjeta o una hoja no la tiñe de naranja.
- **Fotos y texto sobre fotos no cambian.** Los `Color.White` que quedan en componentes y pantallas son texto o iconos sobre una foto, un velo o un relleno de
  color (cabeceras, ValBot, marcadores), que valen igual en los dos modos.
- **Los contrastes se comprueban en el código**: `ThemeContrastTest` (JVM) mide 29 parejas de texto y fondo en cada tema; si alguien cambia un
  color de `Color.kt`, dice qué pareja deja de cumplir.

## Diferencias con las imágenes de diseño

- Los chips seleccionados de las listas (por ejemplo «Todos» en «Qué ver») siguen en pizarra, como en el diseño claro; en las imágenes salen naranjas.
- La pantalla de arranque es `#AF3C02` (el relleno de los botones) y no el `#973201` de la imagen, para que el arranque del sistema, el A1 y los botones sean del mismo naranja.
- Los textos de las imágenes (están hechas con un generador de imágenes) tienen erratas y no se han tomado de ellas.

## Cómo probarlo

- En el emulador: `adb shell cmd uimode night yes` (o `no`), o los ajustes del móvil. La app cambia sin cerrarse.
- `./gradlew test`: `ThemeContrastTest` (todas las parejas de color, en claro y en oscuro).
- `python tools/verify/verify_ui.py tema`: el selector de Perfil en el emulador (con el móvil en claro y en oscuro): «Oscuro» y «Claro» cada uno por su
  cuenta, los iconos de la barra de estado, «Sigue al del sistema» y que sigue al móvil en caliente, que Room y la copia rápida guardan lo mismo, que
  sobrevive a cerrar y abrir la app y que el gancho `resetData` lo restablece. `verify_release.py` lo repite bajo R8 (AppCompat, la copia rápida y Room).
  Además, `AppThemeTest` (JVM: el valor de AppCompat de cada modo y la lectura del modo guardado), `PersistenceTest` y `MigrationTest` (instrumentados:
  se guarda, se le pide al sistema una vez y la v4 llega a la v5 sin perder nada).
- `python tools/verify/verify_ui.py oscuro`: 21 comprobaciones por píxeles en el emulador (fondo, barra superior, iconos de la barra de estado, chip, QR en
  blanco, mapa oscuro con detalle y su atribución, pantalla de arranque, y cambiar el modo con la app abierta). El resto de grupos de `verify_ui.py` fijan el modo
  claro al empezar.
- `@Preview` «Theme · oscuro» en `ui/theme/ThemePreview.kt`.

## Pendiente

- Probarlo en un móvil real (OLED) y con TalkBack: hasta ahora solo en el emulador.
- Un mapa con otro proveedor de teselas oscuras (CARTO u otro) si el filtro no bastara; hoy se lee bien.
