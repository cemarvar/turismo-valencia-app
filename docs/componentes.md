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
| `ErrorNotice` | `Notice.kt` | Aviso de fallo en su sitio de la pantalla (icono y texto sobre `errorContainer`), región «en vivo» que el lector de pantalla lee al aparecer; la pantalla de Reservar lo pone sobre la barra de acciones («Ya no quedan plazas…»), la confirmación bajo el billete cuando falla cancelar y el diálogo de recuperar una reserva | `text` |
| `InfoNotice` | `Notice.kt` | Aviso informativo, neutro (icono de idioma y texto sobre `surfaceContainerHigh`): lo que se va a ver no está en el idioma de la app (las guías de la web, que solo están en español; `WebPageScreen` y la «Guía completa» de la ficha) | `text`, `icon` |
| `LabeledTextField` | `Fields.kt` | Campo de texto del diseño (el del correo de F2): borde de 2dp en el naranja de marca, icono decorativo, etiqueta pequeña y el texto debajo. La etiqueta y el icono van dentro del campo (`BasicTextField` con `decorationBox`): el lector de pantalla las lee como parte del campo y todo el recuadro recoge el toque. Con `capitalization` y `autoCorrect` sirve para códigos (el localizador del diálogo de recuperar una reserva) y `onDone` es la tecla «Hecho» | `label`, `value`, `onValueChange`, `icon`, `keyboardType`, `capitalization`, `autoCorrect`, `onDone`, `enabled` |
| `slotLabel` | `SlotLabel.kt` | El horario de una oferta tal como se enseña: «Cualquier hora» (lo que manda el servidor para las entradas de todo el día, `AnyTimeSlot`) sale en el idioma de la app; «18:30» se queda igual. Hay una versión con `Resources` para el texto de compartir | `time` |
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
| `UserAvatar` | `Cards.kt` | Círculo de pizarra (`appColors.navy`) con iniciales | `initials`, `onClick`, `size` |
| `InfoItemRow` | `Cards.kt` | Fila «Horario / Dirección…» con icono en círculo | `icon`, `title`, `text` |
| `DetailHero` | `Hero.kt` | Foto de cabecera con atrás/compartir/favorito, etiqueta, título y tres cifras | `image`, `eyebrow`, `title`, `stats`, `accent`, `favorite`, `onShare` |
| `OsmMap` / `MapMarker` / `MapCamera` | `OsmMap.kt`, `MapPins.kt` | Mapa de OpenStreetMap (osmdroid) con zoom y arrastre, marcadores por categoría, punto del usuario y ruta punteada. En el tema oscuro oscurece las teselas con un filtro de color (`DarkTilesFilter`). La cámara se mueve desde fuera (`centerOn`, `zoomBy`, `fit`) | `markers`, `selectedId`, `onMarkerClick`, `user`, `route`, `camera`, `interactive`, `showSelectedLabel` |
| `MapAttribution` | `OsmMap.kt` | Atribución obligatoria de OpenStreetMap (abre su licencia); va en toda pantalla con teselas | — |
| `rememberLocationPermissionRequest` / `OnScreenVisible` | `LocationEffects.kt` | Pide el permiso de ubicación del sistema (si el sistema ya no pregunta, además llama a `onBlocked`); avisa cuando una pantalla se ve y deja de verse (para seguir la ubicación solo entonces) | `onResult`, `onBlocked`; `onStart`, `onStop` |
| `rememberNotificationAccessRequest` / `NotificationAccessResult` | `NotificationEffects.kt` | Se asegura de que la app puede notificar: lo dice enseguida si ya puede, abre el diálogo de Android 13+ si falta el permiso y, si el sistema ya no pregunta o las notificaciones están apagadas, devuelve `NeedsSettings`. Se pide al encender un aviso, no al abrir la app | `onResult` (`Granted`, `Denied`, `NeedsSettings`) |
| `OpenSettingsDialog` | `SettingsDialog.kt` | Diálogo que explica que un permiso solo se activa en los ajustes del móvil y ofrece «Abrir ajustes» (`Context.openAppSettings()` / `openNotificationSettings()` en `util/Intents.kt`) | `title`, `text`, `onOpenSettings`, `onDismiss` |
| `MapPlaceholder` | `MapPlaceholder.kt` | Fondo de plano dibujado, sin red ni interacción (pantalla de permiso y Contacto) | — |
| `EmptyState` | `States.kt` | Círculo suave + título + texto + acción (favoritos vacío, sin resultados) | `icon`, `title`, `text`, `actionText`, `onAction` |
| `SkeletonBox` | `States.kt` | Bloque gris que late (inicio cargando) | `shape` |
| `AppSwitch` | `States.kt` | Interruptor naranja con check (pulgar `appColors.switchThumb`, claro en los dos temas) | `checked`, `onCheckedChange` |
| `SectionLabel` | `States.kt` | Etiqueta en mayúsculas de lista («ESTA SEMANA», «PREFERENCIAS») | `text` |
| `PlaceImage` (asset) | `AssetImage.kt`, `Cards.kt` | `rememberAssetImage(path)` decodifica una foto de `assets/img/` (caché de 32 MB); `PlaceImage(assetPath = …)` la usa cuando no hay recurso propio | `assetPath` |
| `AppPrivacy` (`appPrivacyBlocks()`) | `ui/screens/web/AppPrivacy.kt` | Lo que la app añade a la política de privacidad de la web (reservas, cancelarlas y recuperarlas, identificador, ValBot, mapa y rutas, actualización automática, derechos), como bloques de texto legal con textos de `strings.xml`; `WebPageScreen` lo pinta debajo del texto importado de la página `PoliticaPrivacidad` | — |
| `webBlocks` / `WebItemCard` | `WebBlocks.kt` | Pinta en una `LazyColumn` los bloques de una página de la web: tarjetas numeradas con foto, datos, puntos, notas, etiquetas y enlace; filas «etiqueta · valor»; consejos; texto legal | `blocks`, `filters` (estado de los filtros), `onOpenUrl`, `legal` |
| `WebPageCard` | `WebPageCard.kt` | Tarjeta horizontal de una página de la web (guías de Tickets & Tours) | `page`, `onClick` |
| `ContactSummaryCard` | `ContactCard.kt` | Resumen de contacto con acceso a la pantalla completa (portada y pie de cada guía) | `onOpenContact` |
| `QrCode` | `QrCode.kt` | Código QR real (ZXing) dibujado en un `Canvas`, con la tinta `appColors.onPaper`: quien lo use le pone debajo el papel `appColors.paper` (blanco en claro y en oscuro, para que el lector del acceso lo lea) | `content` |
| `PageIndicator` | `PageIndicator.kt` | Puntos de página; la activa se estira a píldora | `pageCount`, `currentPage`, colores |
| `Eyebrow` | `Eyebrow.kt` | Etiqueta superior en mayúsculas con tracking | `text`, `color` |
| `Scrim` / `scrimColor()` | `Scrim.kt` | Velo negro al 60 % bajo diálogos y sheets | — |
| `SystemBarIcons` | `SystemBarIcons.kt` | Iconos claros u oscuros en las barras del sistema mientras la pantalla está visible (por defecto los pone `MainActivity` según el modo claro u oscuro; esto es para pantallas con foto o fondo de color) | `lightStatusBarIcons`, `lightNavigationBarIcons` |
| `Category.icon()` / `Category.dotColor()` | `CategoryIcons.kt`, `OsmMap.kt` | Icono y color de cada sección | — |

Detalles de `AppButton`: `Primary` = relleno `appColors.action` (con texto `onAction`) con resplandor y texto de 15sp; `Tonal` = `primaryContainer`; `Plain` = solo texto; `Outlined` = borde naranja; `Danger` = borde rojo (borrar). `compact` baja el alto a 40dp (botón «Reservar» de tickets). El ancho lo decide quien lo usa.

### Privados de una pantalla que pueden subir a compartidos
Hoy solo se usan en un sitio. Cuando se repitan, se mueven a `ui/components` y se anotan arriba.

| Composable | Dónde está | Repetición esperada |
|---|---|---|
| `TabHeader` (título grande + subtítulo + avatar) | `FavoritosScreen.kt` | Ya lo usan Favoritos y Reservas (público dentro de `ui.screens.favoritos`) |
| `RecoverDialog` y `RecoverCard` (diálogo con dos campos y su entrada en la lista) | `ReservasScreen.kt` | Cualquier diálogo con campos de texto y un error en su sitio |
| `ReservationMessages.kt` (`cancelMessage()`, `recoverMessage()`, `rateLimitedText()`) | `ui/screens/reservas` | El texto de cada fallo de las reservas según su motivo; lo usan Reservar, la confirmación y Mis reservas |
| `SkipButton` (píldora translúcida sobre foto) | `OnboardingScreen.kt` | Cabeceras sobre foto |
| `LanguageRow` + `RadioMark` | `IdiomaScreen.kt` | Cualquier lista de opciones con radio |
| `SettingRow` | `PerfilScreen.kt`, `DescargasScreen.kt` | Dos versiones casi iguales; unificar si aparece una tercera |
| `HomeTileSection` | `HomeSections.kt` | Rejilla de dos columnas con foto y título; hoy solo la portada |

## Tokens del theme (`ui/theme`)

| Qué | Dónde | Uso |
|---|---|---|
| Tema claro y oscuro | `TurismoValenciaTheme(darkTheme)` (por defecto el modo del sistema), `MaterialTheme.isDark` | Ver `docs/modo-oscuro.md`: paleta de los dos temas, decisiones y cómo probarlo |
| Colores de Material 3 | `MaterialTheme.colorScheme.*` | Texto, iconos, bordes, fondos y contenedores. `primary` (`#C44D00` en claro, `#EB5A00` en oscuro) para el naranja **de texto, iconos y bordes**, no para rellenos |
| Rellenos | `MaterialTheme.appColors.action`/`onAction`, `navy`/`onNavy` | El **relleno** (con su texto) de botones, chips seleccionados, día elegido, pantalla de arranque, avatar y aviso sin conexión: en claro son `primary` y `secondary`, en oscuro son colores distintos para que el texto blanco llegue a 4,5:1 |
| Colores propios | `MaterialTheme.appColors.*` | `brand` (`#EB5A00`, solo iconos y gráficos), `switchThumb`, `paper`/`onPaper` (QR y logotipo: blancos en los dos temas), `botAccent` (ValBot como texto), `eco` (verde de relleno), `rating`, `favorite`, `controlInactive`, `location`, `gold`, `bot`/`botContainer`, `lodging`, `warning`/`warningContainer`, `brandSoft`, `ecoContainer`/`ecoLight`, `slateContainer`, `map*` |
| Tipografía | `MaterialTheme.typography.*`, `.eyebrow`, `.screenTitle`, `.tabTitle` | Source Sans 3 (Regular, Italic, Medium, SemiBold, Bold y ExtraBold); escala en `docs/progreso.md` |
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
- Los textos van en `strings.xml` **y** en sus tres traducciones (`values-ca`, `values-en`, `values-fr`); al cambiar de idioma `MainActivity` descarta los ViewModel y la pantalla se rehace sola con los textos nuevos.
- Si la pantalla depende de un argumento de ruta, el ViewModel se crea con `viewModel(key = "…-$id", …)` para que cada entrada del grafo tenga el suyo.

## Datos (`data/`)
- `AppContainer` (creado por `TurismoApp`) guarda los repositorios: `content`, `offers` (catálogo de ofertas del servidor), `favorites`, `downloads`, `reservations`, `settings`, `notifications` (los avisos de la lista G2), `reminders` (avisos de eventos), `notificationAccess`, `searchHistory`, `network`, `location`, `routes`, `valbot` y `strings` (`AppStrings`: textos en el idioma de la app para quien no tiene pantalla). La base de datos y los servicios de ofertas y reservas (`OffersService`, `ReservationsService`) se pueden sustituir en el constructor, que es lo que hacen las pruebas.
- `data/remote` (API propia del backend, ver `docs/api.md`): `TurismoApi` (interfaz de Retrofit con sus DTO `@Serializable`), `OffersService` y `ReservationsService` (lo que ve el resto de la app; `Retrofit…`, `No…` para pruebas), `ApiException` (fallo con estado HTTP, código del servidor y `Retry-After`; `guardedApiCall` convierte cualquier fallo en ella) y `BuildConfig.API_BASE_URL` como dirección. Las reservas y ofertas las aterriza `data/repo` (`OffersRepository`, `ReservationsRepository`, `ReservationFailure`, `ReservationActionFailure`, las reglas de `ReservationRules.kt` —`canCancel` e `isPlausibleEmail`, Kotlin puro— y los mapeos `OfferMapping` / `ReservationMapping`).
- **Reservas**: `ReservationsRepository.create` es `suspend`: genera (o reutiliza) la clave de idempotencia del intento (tabla `reservation_attempts`), pide `POST /reservations` con el UUID del móvil (`settings.deviceId`, creado en Room la primera vez), guarda lo que el servidor contesta y, si falla, lanza `ReservationFailure` (`Reason`: `Unreachable`, `Server`, `RateLimited`, `SoldOut`, `OfferGone`, `InvalidDate/Slot/Party/Email`, `Rejected`). Los fallos definitivos (la respuesta del servidor) borran el intento; los de red, un 5xx o un 429 lo conservan para que el reintento con los mismos datos mande la misma clave. `refresh()` baja `GET /reservations` al abrir la app y al volver la conexión (las reservas de demostración locales se conservan). `cancel(id)` cancela en el servidor (`POST /reservations/{localizador}/cancel` con el UUID del móvil y el correo de la reserva) y guarda lo que contesta; las de demostración (`Reservation.isDemo`, id `seed-*`) se cancelan solo en el móvil. `recover(localizador, correo)` baja `GET /reservations/{localizador}?email=` y la guarda. Las dos corren en el ámbito de la app (no se pierden si se cierra la pantalla) y lanzan `ReservationActionFailure` (`Reason`: `Unreachable`, `Server`, `RateLimited`, `NotFound`, `TooLate`, `Rejected`). Los importes del modelo van en céntimos (`Offer.priceFromCents`, `Reservation.totalCents`) y `formatPrice(cents)` los muestra.
- **Tema** (`data/AppTheme.kt`, ver `docs/modo-oscuro.md`): `ThemeApplier` le pide el modo claro u oscuro al sistema (`AppCompatThemeApplier`; las pruebas pasan `NoThemeApplier`), `ThemeMode.nightMode()` es su valor de AppCompat y `AppThemePrefs` guarda la copia rápida que `TurismoApp.onCreate` lee antes de crear ninguna actividad. La fuente de verdad es Room (`SettingsRepository.themeMode`, que enseña Perfil → Tema; `setThemeMode` guarda y aplica).
- `data/work` (refresco nocturno en segundo plano): `RefreshSchedule` (Kotlin puro: la espera hasta las 3:00 de la hora del móvil y los reintentos), `RefreshScheduler` (`RefreshPlan` según «Actualizar automáticamente» y «Descargar solo con Wi-Fi»; `WorkRefreshScheduler` es el de WorkManager y `NoRefreshScheduler` el de las pruebas), `RefreshWorker` (`CoroutineWorker`) y `AppWorkerFactory` (lo construye con `AppContainer.refresher`; `TurismoApp` lo registra como `Configuration.Provider`).
- `data/notify` (avisos de eventos, la mascletà de las Fallas): `EventReminders` (calendario de cada aviso: del 1 al 19 de marzo, 14:00, 30 min antes), `ReminderPlanner` (cuándo toca el próximo, en la hora de Valencia; sin Android, probado en la JVM), `ReminderScheduler` (alarma `AlarmManager.setWindow` de 10 min por evento), `ReminderReceiver` (muestra la notificación y programa la siguiente), `BootReceiver` (reprograma tras reiniciar), `ReminderNotifier` (canal «Avisos de eventos» y notificación) y `NotificationAccess` (¿puede notificar la app?). `RemindersRepository.active` = ajuste general ∧ permiso del sistema; un aviso llega solo si el usuario lo pidió (`ids`) y `active` es verdadero.
- **Idioma** (`data/AppLanguage.kt`, ver `docs/idiomas.md`): `LanguageApplier` pide el idioma al sistema (`AppCompatLanguageApplier`; las pruebas pasan `NoLanguageApplier`), `AppStrings` crea los `Resources` del idioma de la app (ValBot, sugerencias del buscador, notificaciones: `strings.get(R.string.xxx)`, nunca `context.getString`), `AppLanguagePrefs` guarda el último idioma aparte de Room (lo lee la alarma de un aviso) y `Context.appLanguage()` lee el idioma que de verdad dan los recursos (`locale_code`). `AppContainer.applyLanguage(idioma)` lo reparte: `Locale` por defecto (formatos de `util/Format.kt`), `AppStrings`, `ContentRepository.setLanguage` y Room.
- `ContentRepository` sirve `ContentItem` (lugares y eventos, desde `data/fake`) y `Offer` (tickets, tours y experiencias: las del servidor guardadas en Room, o el catálogo incluido en la app, `FakeOfertas`, si nunca llegó nada). Un `ContentItem` con `onMap = false` no se dibuja como marcador en el mapa; con `onMap = true` debe llevar `location` (coordenadas reales). Sus distancias se miden desde `ContentRepository.origin`. Los textos salen en el idioma de la app: `ContentTranslations` (leídas de `assets/i18n/<va|en|fr>/content.json`) sustituyen a las españolas de las fichas y de las ofertas por su `id` (`ContentItem.translated`, `Offer.translated`); lo que usa el código como clave (`Offer.tagKey`, las subcategorías `Tema` y `Experiencia`) no cambia. `Reservation.shownAs(offer)` pone a una reserva el título y el lugar de su oferta en ese idioma.
- `WebContentRepository` lee `assets/web/index.json` y `assets/web/pages/<Pagina>.json` (65 páginas: modelo `WebPage` con bloques `Items`, `Facts` y `Bullets`) y enlaza fichas, ofertas y fotos con sus páginas. `ContentItem.pageId/thumbAsset/heroAsset` y `Offer.assetImage` los rellena `ContentRepository`. Del contenido de las páginas solo se traducen el título y la etiqueta (`index` y `page(id)` los devuelven en el idioma de la app, y también las tarjetas de la portada y los títulos de las secciones); su texto sigue en español y las pantallas lo avisan con `InfoNotice`.
- `NetworkMonitor.isOnline` refleja la conexión real; `force(Boolean?)` la simula (lo usa el extra `--ez offline true`).

## Reglas de accesibilidad
- Imágenes informativas (logo, fotos): `contentDescription` con texto en `strings.xml`.
- Iconos decorativos junto a un texto: `contentDescription = null`.
- Opciones excluyentes: `Modifier.selectable(role = Role.RadioButton)` dentro de un `selectableGroup()`.
- Elementos pulsables de al menos 48dp (Compose amplía la zona táctil de cualquier clicable más pequeño).
- Con el teclado abierto, `MainApp` oculta la barra inferior y sube el contenido (`imePadding`).
