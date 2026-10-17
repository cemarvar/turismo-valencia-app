# Componentes compartidos

Catálogo de lo que hay en `ui/components` y de los tokens del theme. **Antes de crear un componente, mira aquí; si falta una variante, amplía el existente en vez de crear uno paralelo, y actualiza este documento.**

## Componentes (`ui/components`)

| Componente | Archivo | Para qué | Parámetros clave |
|---|---|---|---|
| `AppButton` | `AppButton.kt` | Botón en píldora de 48dp (40dp con `compact`) | `text`, `onClick`, `style` (`Primary`, `Tonal`, `Plain`, `Outlined`, `Danger`), `leadingIcon` / `trailingIcon` (`ImageVector`), `compact`, `enabled` |
| `AppTopBar` | `Bars.kt` | Barra superior blanca con atrás, título, subtítulo y acciones | `title`, `subtitle`, `onBack`, `actions` |
| `BottomNavBar` / `NavBarItem` | `Bars.kt` | Barra inferior de 5 pestañas (la activa va en píldora naranja suave) | `items`, `selectedKey`, `onSelect` |
| `BottomActionBar` | `Bars.kt` | Barra inferior con dos botones («Guardar» + «Cómo llegar»…) | contenido en `Row` |
| `OfflineBanner` | `Bars.kt` | Banda azul marino «Sin conexión · mostrando contenido guardado»; con `text` dice otra cosa (Reservar: «Reservar necesita conexión a Internet») | `text` |
| `OfflineSnackbar` | `Bars.kt` | Snackbar oscuro «Sin conexión a Internet · Reintentar» | `onRetry` |
| `ErrorNotice` | `Notice.kt` | Aviso de fallo en su sitio de la pantalla (icono y texto sobre `errorContainer`), región «en vivo» que el lector de pantalla lee al aparecer; la pantalla de Reservar lo pone sobre la barra de acciones («Ya no quedan plazas…») | `text` |
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
| `OsmMap` / `MapMarker` / `MapCamera` | `OsmMap.kt`, `MapPins.kt` | Mapa de OpenStreetMap (osmdroid) con zoom y arrastre, marcadores por categoría, punto del usuario y ruta punteada. La cámara se mueve desde fuera (`centerOn`, `zoomBy`, `fit`) | `markers`, `selectedId`, `onMarkerClick`, `user`, `route`, `camera`, `interactive`, `showSelectedLabel` |
| `MapAttribution` | `OsmMap.kt` | Atribución obligatoria de OpenStreetMap (abre su licencia); va en toda pantalla con teselas | — |
| `rememberLocationPermissionRequest` / `OnScreenVisible` | `LocationEffects.kt` | Pide el permiso de ubicación del sistema (si el sistema ya no pregunta, además llama a `onBlocked`); avisa cuando una pantalla se ve y deja de verse (para seguir la ubicación solo entonces) | `onResult`, `onBlocked`; `onStart`, `onStop` |
| `rememberNotificationAccessRequest` / `NotificationAccessResult` | `NotificationEffects.kt` | Se asegura de que la app puede notificar: lo dice enseguida si ya puede, abre el diálogo de Android 13+ si falta el permiso y, si el sistema ya no pregunta o las notificaciones están apagadas, devuelve `NeedsSettings`. Se pide al encender un aviso, no al abrir la app | `onResult` (`Granted`, `Denied`, `NeedsSettings`) |
| `OpenSettingsDialog` | `SettingsDialog.kt` | Diálogo que explica que un permiso solo se activa en los ajustes del móvil y ofrece «Abrir ajustes» (`Context.openAppSettings()` / `openNotificationSettings()` en `util/Intents.kt`) | `title`, `text`, `onOpenSettings`, `onDismiss` |
| `MapPlaceholder` | `MapPlaceholder.kt` | Fondo de plano dibujado, sin red ni interacción (pantalla de permiso y Contacto) | — |
| `EmptyState` | `States.kt` | Círculo suave + título + texto + acción (favoritos vacío, sin resultados) | `icon`, `title`, `text`, `actionText`, `onAction` |
| `SkeletonBox` | `States.kt` | Bloque gris que late (inicio cargando) | `shape` |
| `AppSwitch` | `States.kt` | Interruptor naranja con check | `checked`, `onCheckedChange` |
| `SectionLabel` | `States.kt` | Etiqueta en mayúsculas de lista («ESTA SEMANA», «PREFERENCIAS») | `text` |
| `PlaceImage` (asset) | `AssetImage.kt`, `Cards.kt` | `rememberAssetImage(path)` decodifica una foto de `assets/img/` (caché de 32 MB); `PlaceImage(assetPath = …)` la usa cuando no hay recurso propio | `assetPath` |
| `AppPrivacy` (`appPrivacyBlocks()`) | `ui/screens/web/AppPrivacy.kt` | Lo que la app añade a la política de privacidad de la web (reservas, identificador, ValBot, mapa y rutas, actualización automática, derechos), como bloques de texto legal con textos de `strings.xml`; `WebPageScreen` lo pinta debajo del texto importado de la página `PoliticaPrivacidad` | — |
| `webBlocks` / `WebItemCard` | `WebBlocks.kt` | Pinta en una `LazyColumn` los bloques de una página de la web: tarjetas numeradas con foto, datos, puntos, notas, etiquetas y enlace; filas «etiqueta · valor»; consejos; texto legal | `blocks`, `filters` (estado de los filtros), `onOpenUrl`, `legal` |
| `WebPageCard` | `WebPageCard.kt` | Tarjeta horizontal de una página de la web (guías de Tickets & Tours) | `page`, `onClick` |
| `ContactSummaryCard` | `ContactCard.kt` | Resumen de contacto con acceso a la pantalla completa (portada y pie de cada guía) | `onOpenContact` |
| `QrCode` | `QrCode.kt` | Código QR real (ZXing) dibujado en un `Canvas` | `content` |
| `PageIndicator` | `PageIndicator.kt` | Puntos de página; la activa se estira a píldora | `pageCount`, `currentPage`, colores |
| `Eyebrow` | `Eyebrow.kt` | Etiqueta superior en mayúsculas con tracking | `text`, `color` |
| `Scrim` / `scrimColor()` | `Scrim.kt` | Velo negro al 60 % bajo diálogos y sheets | — |
| `SystemBarIcons` | `SystemBarIcons.kt` | Iconos claros u oscuros en las barras del sistema mientras la pantalla está visible | `lightStatusBarIcons`, `lightNavigationBarIcons` |
| `Category.icon()` / `Category.dotColor()` | `CategoryIcons.kt`, `OsmMap.kt` | Icono y color de cada sección | — |

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
- `AppContainer` (creado por `TurismoApp`) guarda los repositorios: `content`, `offers` (catálogo de ofertas del servidor), `favorites`, `downloads`, `reservations`, `settings`, `notifications` (los avisos de la lista G2), `reminders` (avisos de eventos), `notificationAccess`, `searchHistory`, `network`, `location`, `routes` y `valbot`. La base de datos y los servicios de ofertas y reservas (`OffersService`, `ReservationsService`) se pueden sustituir en el constructor, que es lo que hacen las pruebas.
- `data/remote` (API propia del backend, ver `docs/api.md`): `TurismoApi` (interfaz de Retrofit con sus DTO `@Serializable`), `OffersService` y `ReservationsService` (lo que ve el resto de la app; `Retrofit…`, `No…` para pruebas), `ApiException` (fallo con estado HTTP, código del servidor y `Retry-After`; `guardedApiCall` convierte cualquier fallo en ella) y `BuildConfig.API_BASE_URL` como dirección. Las reservas y ofertas las aterriza `data/repo` (`OffersRepository`, `ReservationsRepository`, `ReservationFailure` y los mapeos `OfferMapping` / `ReservationMapping`).
- **Reservas**: `ReservationsRepository.create` es `suspend`: genera (o reutiliza) la clave de idempotencia del intento (tabla `reservation_attempts`), pide `POST /reservations` con el UUID del móvil (`settings.deviceId`, creado en Room la primera vez), guarda lo que el servidor contesta y, si falla, lanza `ReservationFailure` (`Reason`: `Unreachable`, `Server`, `RateLimited`, `SoldOut`, `OfferGone`, `InvalidDate/Slot/Party/Email`, `Rejected`). Los fallos definitivos (la respuesta del servidor) borran el intento; los de red, un 5xx o un 429 lo conservan para que el reintento con los mismos datos mande la misma clave. `refresh()` baja `GET /reservations` al abrir la app y al volver la conexión (las reservas de demostración locales se conservan). Los importes del modelo van en céntimos (`Offer.priceFromCents`, `Reservation.totalCents`) y `formatPrice(cents)` los muestra.
- `data/work` (refresco nocturno en segundo plano): `RefreshSchedule` (Kotlin puro: la espera hasta las 3:00 de la hora del móvil y los reintentos), `RefreshScheduler` (`RefreshPlan` según «Actualizar automáticamente» y «Descargar solo con Wi-Fi»; `WorkRefreshScheduler` es el de WorkManager y `NoRefreshScheduler` el de las pruebas), `RefreshWorker` (`CoroutineWorker`) y `AppWorkerFactory` (lo construye con `AppContainer.refresher`; `TurismoApp` lo registra como `Configuration.Provider`).
- `data/notify` (avisos de eventos, la mascletà de las Fallas): `EventReminders` (calendario de cada aviso: del 1 al 19 de marzo, 14:00, 30 min antes), `ReminderPlanner` (cuándo toca el próximo, en la hora de Valencia; sin Android, probado en la JVM), `ReminderScheduler` (alarma `AlarmManager.setWindow` de 10 min por evento), `ReminderReceiver` (muestra la notificación y programa la siguiente), `BootReceiver` (reprograma tras reiniciar), `ReminderNotifier` (canal «Avisos de eventos» y notificación) y `NotificationAccess` (¿puede notificar la app?). `RemindersRepository.active` = ajuste general ∧ permiso del sistema; un aviso llega solo si el usuario lo pidió (`ids`) y `active` es verdadero.
- `ContentRepository` sirve `ContentItem` (lugares y eventos, desde `data/fake`) y `Offer` (tickets, tours y experiencias: las del servidor guardadas en Room, o el catálogo incluido en la app, `FakeOfertas`, si nunca llegó nada). Un `ContentItem` con `onMap = false` no se dibuja como marcador en el mapa; con `onMap = true` debe llevar `location` (coordenadas reales). Sus distancias se miden desde `ContentRepository.origin`.
- `WebContentRepository` lee `assets/web/index.json` y `assets/web/pages/<Pagina>.json` (65 páginas: modelo `WebPage` con bloques `Items`, `Facts` y `Bullets`) y enlaza fichas, ofertas y fotos con sus páginas. `ContentItem.pageId/thumbAsset/heroAsset` y `Offer.assetImage` los rellena `ContentRepository`.
- `NetworkMonitor.isOnline` refleja la conexión real; `force(Boolean?)` la simula (lo usa el extra `--ez offline true`).

## Reglas de accesibilidad
- Imágenes informativas (logo, fotos): `contentDescription` con texto en `strings.xml`.
- Iconos decorativos junto a un texto: `contentDescription = null`.
- Opciones excluyentes: `Modifier.selectable(role = Role.RadioButton)` dentro de un `selectableGroup()`.
- Elementos pulsables de al menos 48dp (Compose amplía la zona táctil de cualquier clicable más pequeño).
- Con el teclado abierto, `MainApp` oculta la barra inferior y sube el contenido (`imePadding`).
