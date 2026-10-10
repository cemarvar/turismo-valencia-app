# Componentes compartidos

Catálogo de lo que hay en `ui/components` y de los tokens del theme. **Antes de crear un componente, mira aquí; si falta una variante, amplía el existente en vez de crear uno paralelo, y actualiza este documento.**

## Componentes (`ui/components`)

| Componente | Archivo | Para qué | Parámetros clave |
|---|---|---|---|
| `AppButton` | `AppButton.kt` | Botón en píldora de 48dp (40dp con `compact`) | `text`, `onClick`, `style` (`Primary`, `Tonal`, `Plain`, `Outlined`, `Danger`), `leadingIcon` / `trailingIcon` (`ImageVector`), `compact`, `enabled` |
| `AppTopBar` | `Bars.kt` | Barra superior blanca con atrás, título, subtítulo y acciones | `title`, `subtitle`, `onBack`, `actions` |
| `BottomNavBar` / `NavBarItem` | `Bars.kt` | Barra inferior de 5 pestañas (la activa va en píldora naranja suave) | `items`, `selectedKey`, `onSelect` |
| `BottomActionBar` | `Bars.kt` | Barra inferior con dos botones («Guardar» + «Cómo llegar»…) | contenido en `Row` |
| `OfflineBanner` | `Bars.kt` | Banda azul marino «Sin conexión · mostrando contenido guardado» | — |
| `OfflineSnackbar` | `Bars.kt` | Snackbar oscuro «Sin conexión a Internet · Reintentar» | `onRetry` |
| `AppFilterChip` | `Chips.kt` | Chip de filtro (`ChipStyle.Navy` en listas, `Brand` en inicio y mapa), con icono o punto de color | `label`, `selected`, `onClick`, `style`, `height`, `textStyle`, `dotColor` |
| `StatusChip` | `Chips.kt` | Etiqueta de estado (`StatusTone`: Success, Neutral, Warning, Brand, Eco, Danger) | `text`, `tone`, `icon`, `onClick`, `height`, `small` |
| `SegmentedTabs` | `Chips.kt` | Pestañas en píldora («Todos / Lugares / Eventos», «Próximas / Historial») | `labels`, `selectedIndex`, `onSelect` |
| `PlaceImage` | `Cards.kt` | Foto de un recurso o, si no hay, fondo suave con el icono de la categoría | `image`, `category`, `contentDescription` |
| `FavoriteButton` | `Cards.kt` | Corazón (`FavoriteStyle`: OnPhoto, OnPhotoDark, Plain) | `favorite`, `onToggle`, `style`, `circleSize` |
| `LocationRow` / `RatingRow` | `Cards.kt` | Línea con pin + zona/distancia y línea con estrella + valoración | `text` / `rating`, `count` |
| `OfflineChip` | `Cards.kt` | «Sin conexión» (descargado) o «Descargar», pulsable | `downloaded`, `onClick` |
| `PlaceGridCard` | `Cards.kt` | Tarjeta vertical del inicio («Cerca de ti») | `item`, `favorite`, `downloaded`, `onClick`, `onToggleFavorite`, `onToggleDownload`, `dimmed` (sin conexión y sin descargar) |
| `PlaceListCard` | `Cards.kt` | Tarjeta horizontal (categoría, favoritos, descargas) con huecos `footer` y `trailing` | `item`, `favorite`, `eyebrow`, `locationText`, `footer`, `trailing` |
| `SectionTitle` | `Cards.kt` | Título de sección con barrita naranja y acción a la derecha | `text`, `trailing` |
| `UserAvatar` | `Cards.kt` | Círculo azul marino con iniciales | `initials`, `onClick`, `size` |
| `InfoItemRow` | `Cards.kt` | Fila «Horario / Dirección…» con icono en círculo | `icon`, `title`, `text` |
| `DetailHero` | `Hero.kt` | Foto de cabecera con atrás/compartir/favorito, etiqueta, título y tres cifras | `image`, `eyebrow`, `title`, `stats`, `accent`, `favorite`, `onShare` |
| `MapCanvas` / `MapMarker` / `MapCamera` | `MapView.kt` | Mapa dibujado de 700×1100 dp con zoom, desplazamiento, pins, punto del usuario y ruta | `markers`, `selectedId`, `showUser`, `route`, `showLabels`, `showSelectedLabel`, `camera`, `onSelect` |
| `MapPlaceholder` | `MapView.kt` | Mapa de fondo sin interacción (permiso de ubicación) | — |
| `EmptyState` | `States.kt` | Círculo suave + título + texto + acción (favoritos vacío, sin resultados) | `icon`, `title`, `text`, `actionText`, `onAction` |
| `SkeletonBox` | `States.kt` | Bloque gris que late (inicio cargando) | `shape` |
| `AppSwitch` | `States.kt` | Interruptor naranja con check | `checked`, `onCheckedChange` |
| `SectionLabel` | `States.kt` | Etiqueta en mayúsculas de lista («ESTA SEMANA», «PREFERENCIAS») | `text` |
| `PlaceImage` (asset) | `AssetImage.kt`, `Cards.kt` | `rememberAssetImage(path)` decodifica una foto de `assets/img/` (caché de 32 MB); `PlaceImage(assetPath = …)` la usa cuando no hay recurso propio | `assetPath` |
| `webBlocks` / `WebItemCard` | `WebBlocks.kt` | Pinta en una `LazyColumn` los bloques de una página de la web: tarjetas numeradas con foto, datos, puntos, notas, etiquetas y enlace; filas «etiqueta · valor»; consejos; texto legal | `blocks`, `filters` (estado de los filtros), `onOpenUrl`, `legal` |
| `WebPageCard` | `WebPageCard.kt` | Tarjeta horizontal de una página de la web (guías de Tickets & Tours) | `page`, `onClick` |
| `ContactSummaryCard` | `ContactCard.kt` | Resumen de contacto con acceso a la pantalla completa (portada y pie de cada guía) | `onOpenContact` |
| `QrCode` | `QrCode.kt` | Código QR real (ZXing) dibujado en un `Canvas` | `content` |
| `PageIndicator` | `PageIndicator.kt` | Puntos de página; la activa se estira a píldora | `pageCount`, `currentPage`, colores |
| `Eyebrow` | `Eyebrow.kt` | Etiqueta superior en mayúsculas con tracking | `text`, `color` |
| `Scrim` / `scrimColor()` | `Scrim.kt` | Velo negro al 60 % bajo diálogos y sheets | — |
| `SystemBarIcons` | `SystemBarIcons.kt` | Iconos claros u oscuros en las barras del sistema mientras la pantalla está visible | `lightStatusBarIcons`, `lightNavigationBarIcons` |
| `Category.icon()` / `Category.dotColor()` | `CategoryIcons.kt`, `MapView.kt` | Icono y color de cada sección | — |

Detalles de `AppButton`: `Primary` = relleno `primary` con resplandor y texto de 15sp; `Tonal` = `primaryContainer`; `Plain` = solo texto; `Outlined` = borde naranja; `Danger` = borde rojo (borrar). `compact` baja el alto a 40dp (botón «Reservar» de tickets). El ancho lo decide quien lo usa.

### Privados de una pantalla que pueden subir a compartidos
Hoy solo se usan en un sitio. Cuando se repitan, se mueven a `ui/components` y se anotan arriba.

| Composable | Dónde está | Repetición esperada |
|---|---|---|
| `TabHeader` (título grande + subtítulo + avatar) | `FavoritosScreen.kt` | Ya lo usan Favoritos y Reservas (público dentro de `ui.screens.favoritos`) |
| `SkipButton` (píldora translúcida sobre foto) | `OnboardingScreen.kt` | Cabeceras sobre foto |
| `LanguageRow` + `RadioMark` | `IdiomaScreen.kt` | Cualquier lista de opciones con radio |
| `SettingRow` | `PerfilScreen.kt`, `DescargasScreen.kt` | Dos versiones casi iguales; unificar si aparece una tercera |
| `HomeTileSection` | `HomeSections.kt` | Rejilla de dos columnas con foto y título; hoy solo la portada |

## Tokens del theme (`ui/theme`)

| Qué | Dónde | Uso |
|---|---|---|
| Colores de Material 3 | `MaterialTheme.colorScheme.*` | Texto, fondos, contenedores. `primary` (`#C44D00`) para texto y botones |
| Colores propios | `MaterialTheme.appColors.*` | `brand` (`#EB5A00`, solo iconos y gráficos), `rating`, `favorite`, `controlInactive`, `location`, `gold`, `bot`/`botContainer`, `lodging`, `warning`/`warningContainer`, `brandSoft`, `ecoContainer`/`ecoLight`, `slateContainer`, `map*` |
| Tipografía | `MaterialTheme.typography.*`, `.eyebrow`, `.screenTitle`, `.tabTitle` | Source Sans 3; escala en `docs/progreso.md` |
| Formas | `CardShape` (16dp), `BottomSheetShape` (24dp arriba), `PillShape`; `MaterialTheme.shapes` | Tarjetas, sheets, botones y chips |
| Espaciado | `Spacing.xxs`…`xxl` (4, 8, 12, 16, 24, 32, 48dp) | Márgenes y huecos |
| Área táctil | `MinTouchTarget` (48dp) | Alto de botones y filas pulsables |
| Velo | `ScrimAlpha` (0,6) | Siempre vía `scrimColor()` |
| Escala de texto | `TurismoValenciaTheme(fontScaleFactor)` | Ajuste «Tamaño del texto» de Perfil |

Las medidas que son propias de una pantalla se dejan como constantes privadas con nombre en el archivo de esa pantalla.

## Patrón de pantalla

Cada pantalla tiene, en `ui/screens/<grupo>/`:
- `XxxViewModel.kt`: `XxxUiState` (inmutable) y `XxxViewModel` (recibe el `AppContainer`) con `StateFlow` derivado de los repositorios.
- `XxxScreen.kt`: `XxxRoute` (obtiene el ViewModel con `containerFactory { … }` y conecta callbacks), `XxxScreen` (sin estado: solo `uiState` y lambdas) y un `@Preview` por estado visible.
- Si la pantalla depende de un argumento de ruta, el ViewModel se crea con `viewModel(key = "…-$id", …)` para que cada entrada del grafo tenga el suyo.

## Datos (`data/`)
- `AppContainer` (creado por `TurismoApp`) guarda los repositorios: `content`, `favorites`, `downloads`, `reservations`, `settings`, `notifications`, `searchHistory`, `network` y `valbot`.
- `ContentRepository` sirve `ContentItem` (lugares y eventos) y `Offer` (tickets, tours y experiencias) desde `data/fake`. Un `ContentItem` con `onMap = false` no se dibuja como marcador en el mapa.
- `WebContentRepository` lee `assets/web/index.json` y `assets/web/pages/<Pagina>.json` (65 páginas: modelo `WebPage` con bloques `Items`, `Facts` y `Bullets`) y enlaza fichas, ofertas y fotos con sus páginas. `ContentItem.pageId/thumbAsset/heroAsset` y `Offer.assetImage` los rellena `ContentRepository`.
- `NetworkMonitor.isOnline` refleja la conexión real; `force(Boolean?)` la simula (lo usa el extra `--ez offline true`).

## Reglas de accesibilidad
- Imágenes informativas (logo, fotos): `contentDescription` con texto en `strings.xml`.
- Iconos decorativos junto a un texto: `contentDescription = null`.
- Opciones excluyentes: `Modifier.selectable(role = Role.RadioButton)` dentro de un `selectableGroup()`.
- Elementos pulsables de al menos 48dp (Compose amplía la zona táctil de cualquier clicable más pequeño).
- Con el teclado abierto, `MainApp` oculta la barra inferior y sube el contenido (`imePadding`).
