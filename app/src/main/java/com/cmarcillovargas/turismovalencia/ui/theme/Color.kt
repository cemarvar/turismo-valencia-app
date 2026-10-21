package com.cmarcillovargas.turismovalencia.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Paleta «Web (referencia)».
// PROVISIONAL: valores medidos de las capturas de Figma (Explorar y G1) porque docs/guia-estilo.md
// aún no está en el repo. Verificar contra la guía (ver «Decisiones» en docs/progreso.md).
// Las pantallas nunca usan Color(0x…): solo MaterialTheme.colorScheme.* o MaterialTheme.appColors.*.

private val Brand = Color(0xFFEB5A00)          // Solo iconos y gráficos: 3,5:1 sobre blanco, no vale para texto.
private val PrimaryStrong = Color(0xFFC44D00)  // Texto y botones: 4,76:1 sobre blanco (AA).
private val BrandTint = Color(0xFFFDEBDD)      // Fondo del elemento seleccionado (píldora de la barra inferior).
private val PrimaryDeep = Color(0xFFA84000)    // Texto sobre BrandTint: 5,3:1 (C44D00 solo da 4,1:1).
private val Location = Color(0xFF3498DB)       // Azul de «mi ubicación».
private val Gold = Color(0xFFF0C040)           // Etiquetas y cifras sobre las fotos de cabecera.
private val Bot = Color(0xFFC0392B)            // ValBot: cabecera, burbuja del usuario y botón de enviar.
private val BotTint = Color(0xFFFDECEA)        // Fondo del icono de ValBot en notificaciones.
private val Lodging = Color(0xFF8E44AD)        // Marcador y punto de «Alojamientos» en el mapa.
private val Warning = Color(0xFF8A5A00)        // Texto de «Pendiente».
private val WarningTint = Color(0xFFFFF8E1)    // Fondo de «Pendiente».
private val BrandSoft = Color(0xFFFFF5EE)      // Filas sin leer y fondos suaves de estados vacíos.
private val EcoTint = Color(0xFFE8F5E9)        // Aviso y avisos verdes de turismo sostenible.
private val EcoLight = Color(0xFFA8D5A2)       // Texto verde sobre foto.
private val SlateTint = Color(0xFFE8EDF2)      // Fondo de iconos neutros.
private val Danger = Color(0xFFB71C1C)         // Errores y acciones destructivas.
private val DangerTint = Color(0xFFFDE7E7)     // Fondo de un error.
private val SnackbarBg = Color(0xFF1A2A4A)     // Snackbar y avisos oscuros.
private val SnackbarAction = Color(0xFFFF8C42) // Acción del snackbar («Reintentar»).
private val ControlInactive = Color(0xFFCCCCCC) // Indicador de página, radio, tirador y borde sin seleccionar.

// Mapa (estilo claro tipo mapa web).
private val MapLand = Color(0xFFF2F0E8)
private val MapBuilding = Color(0xFFE1DCD2)
private val MapRoad = Color(0xFFFFFFFF)
private val MapRoadMajor = Color(0xFFF8FABE)
private val MapRoadWarm = Color(0xFFF9D9B0)
private val MapPark = Color(0xFFC8E6AF)
private val MapWater = Color(0xFFAAD2DE)
private val Navy = Color(0xFF2C3E50)           // Avatar, iconos de ajustes, «Mejor esta vida».
private val Eco = Color(0xFF2E7D32)            // Etiqueta «Eco» (con texto blanco).
private val Success = Color(0xFF1B7A43)        // Texto de «Sin conexión» / «reservas».
private val SuccessTint = Color(0xFFE3F4EA)    // Fondo de esas etiquetas.
private val Rating = Color(0xFFF2A900)         // Estrella de valoración.
private val Favorite = Color(0xFFE0453A)       // Corazón de favorito activo.

private val Ink = Color(0xFF1A1A1A)            // Texto principal.
private val InkMuted = Color(0xFF555555)       // Texto secundario e iconos inactivos.
private val InkSubtle = Color(0xFF6E6E6E)      // Placeholders, chevrons, eyebrows.
private val Neutral50 = Color(0xFFF9F9F9)      // Fondo de pantalla.
private val Neutral100 = Color(0xFFF0F0F0)     // Chips neutros.
private val Neutral200 = Color(0xFFE5E5E5)     // Divisores.
private val White = Color(0xFFFFFFFF)          // Tarjetas, barras.

// Paleta oscura (imágenes de diseño en docs/Figma/Modo oscuro): fondos casi negros azulados, tarjetas un punto más claras y el
// naranja de marca como acento. Medida de esas imágenes y comprobada contra WCAG AA en ThemeContrastTest (cada pareja de texto y fondo).
private val DarkBackground = Color(0xFF0A0E11)      // Fondo de pantalla.
private val DarkSurface = Color(0xFF11171D)         // Tarjetas, barras y hojas.
private val DarkSurfaceHigh = Color(0xFF1B232B)     // Chips neutros, campos de texto y burbujas.
private val DarkOutline = Color(0xFF8C97A1)         // Placeholders, chevrons y eyebrows: 5,3:1 sobre el campo.
private val DarkOutlineVariant = Color(0xFF27313A)  // Divisores y bordes de tarjeta (decorativos).
private val DarkInk = Color(0xFFEDF0F2)             // Texto principal.
private val DarkInkMuted = Color(0xFFA5AFB8)        // Texto secundario e iconos inactivos.
private val DarkAction = Color(0xFFAF3C02)          // Relleno de botones y chips seleccionados, con texto blanco (6:1).
private val DarkOnPrimary = Color(0xFF1B0B00)       // Sobre el naranja de marca usado como texto (solo por si un componente lo pinta de fondo).
private val DarkPrimaryContainer = Color(0xFF331B0E)
private val DarkOnPrimaryContainer = Color(0xFFFFB27A)
private val DarkSlate = Color(0xFFB4C3D0)           // El «navy» del tema claro como primer plano: iconos y textos de pizarra.
private val DarkOnSlate = Color(0xFF0F1A24)
private val DarkNavy = Color(0xFF26323D)            // El «navy» como relleno: avatar y aviso sin conexión.
private val DarkOnNavy = Color(0xFFE6ECF1)
private val DarkEco = Color(0xFF6FCF7A)             // Verde de texto, iconos y etiqueta «Eco».
private val DarkOnEco = Color(0xFF06210D)
private val DarkSuccessContainer = Color(0xFF12301C)
private val DarkOnSuccessContainer = Color(0xFF8FE0A8)
private val DarkError = Color(0xFFFF8A80)
private val DarkOnError = Color(0xFF2B0A08)
private val DarkErrorContainer = Color(0xFF3A1514)
private val DarkOnErrorContainer = Color(0xFFFFB4AB)
private val DarkSnackbar = Color(0xFF1E2832)        // Snackbar y aviso sin conexión: una barra oscura, no invertida.
private val DarkSnackbarAction = Color(0xFFFF8C42)
private val DarkControlInactive = Color(0xFF5E6A75)
private val DarkBot = Color(0xFF9A2E22)             // ValBot: cabecera, burbuja del usuario y botón de enviar (texto blanco: 7,5:1).
private val DarkBotAccent = Color(0xFFFF8A7A)       // ValBot como texto, borde e icono sobre fondo oscuro.
private val DarkBotTint = Color(0xFF3A1713)
private val DarkWarning = Color(0xFFFFC857)
private val DarkWarningTint = Color(0xFF3A2B08)
private val DarkBrandSoft = Color(0xFF1B1511)
private val DarkEcoTint = Color(0xFF11271A)
private val DarkSlateTint = Color(0xFF1F2A33)
private val DarkFavorite = Color(0xFFEF5350)
private val DarkLocation = Color(0xFF4DA8E8)
private val DarkSwitchThumb = Color(0xFFF4F6F8)

// Mapa dibujado (pantalla de permiso y Contacto) en oscuro; el mapa real oscurece sus teselas con un filtro (ver OsmMap).
private val DarkMapLand = Color(0xFF0F1820)
private val DarkMapBuilding = Color(0xFF16212B)
private val DarkMapRoad = Color(0xFF27323D)
private val DarkMapRoadMajor = Color(0xFF344353)
private val DarkMapRoadWarm = Color(0xFF4A3B2C)
private val DarkMapPark = Color(0xFF14301F)
private val DarkMapWater = Color(0xFF102B38)

internal val DarkColorScheme = darkColorScheme(
    primary = Brand,                                // Como texto y gráfico sobre fondo oscuro el naranja de marca da 5,5:1 (en claro, C44D00).
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSlate,
    onSecondary = DarkOnSlate,
    secondaryContainer = DarkSurfaceHigh,
    onSecondaryContainer = DarkInkMuted,
    tertiary = DarkEco,
    onTertiary = DarkOnEco,
    tertiaryContainer = DarkSuccessContainer,
    onTertiaryContainer = DarkOnSuccessContainer,
    background = DarkBackground,
    onBackground = DarkInk,
    surface = DarkSurface,
    onSurface = DarkInk,
    surfaceVariant = DarkSurfaceHigh,
    onSurfaceVariant = DarkInkMuted,
    surfaceTint = DarkSurface,                      // Sin tinte naranja al elevar una tarjeta o una hoja.
    surfaceContainerLowest = DarkSurface,
    surfaceContainerLow = DarkSurface,
    surfaceContainer = DarkSurface,
    surfaceContainerHigh = DarkSurfaceHigh,
    surfaceContainerHighest = DarkSurfaceHigh,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    error = DarkError,
    onError = DarkOnError,
    errorContainer = DarkErrorContainer,
    onErrorContainer = DarkOnErrorContainer,
    inverseSurface = DarkSnackbar,
    inverseOnSurface = DarkInk,
    inversePrimary = DarkSnackbarAction,
)

internal val LightColorScheme = lightColorScheme(
    primary = PrimaryStrong,
    onPrimary = White,
    primaryContainer = BrandTint,
    onPrimaryContainer = PrimaryDeep,
    secondary = Navy,
    onSecondary = White,
    secondaryContainer = Neutral100,
    onSecondaryContainer = InkMuted,
    tertiary = Eco,
    onTertiary = White,
    tertiaryContainer = SuccessTint,
    onTertiaryContainer = Success,
    background = Neutral50,
    onBackground = Ink,
    surface = White,
    onSurface = Ink,
    surfaceVariant = Neutral100,
    onSurfaceVariant = InkMuted,
    surfaceContainerLowest = White,
    surfaceContainerLow = White,
    surfaceContainer = White,
    surfaceContainerHigh = Neutral100,
    surfaceContainerHighest = Neutral100,
    outline = InkSubtle,
    outlineVariant = Neutral200,
    error = Danger,
    onError = White,
    errorContainer = DangerTint,
    onErrorContainer = Danger,
    inverseSurface = SnackbarBg,
    inverseOnSurface = White,
    inversePrimary = SnackbarAction,
    // scrim: valor por defecto de Material 3.
)

/**
 * Tokens que Material 3 no tiene como rol: marca para iconos/gráficos, valoración, favorito, ubicación y mapa.
 *
 * `action`/`onAction` y `navy`/`onNavy` son los **rellenos** que en el tema claro coinciden con `primary` y `secondary`. En oscuro
 * esos dos roles pasan a ser colores claros (para pintar texto e iconos sobre fondo oscuro) y un relleno con texto blanco ya no puede
 * ser el mismo color, así que los botones, los chips seleccionados, el avatar y el aviso sin conexión usan estos.
 */
@Immutable
data class AppColors(
    val brand: Color,
    val action: Color,
    val onAction: Color,
    val navy: Color,
    val onNavy: Color,
    val switchThumb: Color,
    /** Blanco que no cambia con el tema: el papel del código QR (lo lee un lector de acceso) y el disco del logotipo. */
    val paper: Color,
    val onPaper: Color,
    val rating: Color,
    val favorite: Color,
    val controlInactive: Color,
    val location: Color,
    val gold: Color,
    val bot: Color,
    /** El color de ValBot como primer plano (texto, borde e icono sobre fondo): en claro es `bot`; en oscuro, uno más claro (2,3:1 no vale). */
    val botAccent: Color,
    val botContainer: Color,
    val lodging: Color,
    /** Verde de relleno con texto o icono blanco (marcador de «Sostenible»); `tertiary` en oscuro es un verde claro de texto. */
    val eco: Color,
    val warning: Color,
    val warningContainer: Color,
    val brandSoft: Color,
    val ecoContainer: Color,
    val ecoLight: Color,
    val slateContainer: Color,
    val mapLand: Color,
    val mapBuilding: Color,
    val mapRoad: Color,
    val mapRoadMajor: Color,
    val mapRoadWarm: Color,
    val mapPark: Color,
    val mapWater: Color,
)

internal val LightAppColors = AppColors(
    brand = Brand,
    action = PrimaryStrong,
    onAction = White,
    navy = Navy,
    onNavy = White,
    switchThumb = White,
    paper = White,
    onPaper = Ink,
    rating = Rating,
    favorite = Favorite,
    controlInactive = ControlInactive,
    location = Location,
    gold = Gold,
    bot = Bot,
    botAccent = Bot,
    botContainer = BotTint,
    lodging = Lodging,
    eco = Eco,
    warning = Warning,
    warningContainer = WarningTint,
    brandSoft = BrandSoft,
    ecoContainer = EcoTint,
    ecoLight = EcoLight,
    slateContainer = SlateTint,
    mapLand = MapLand,
    mapBuilding = MapBuilding,
    mapRoad = MapRoad,
    mapRoadMajor = MapRoadMajor,
    mapRoadWarm = MapRoadWarm,
    mapPark = MapPark,
    mapWater = MapWater,
)

internal val DarkAppColors = AppColors(
    brand = Brand,
    action = DarkAction,
    onAction = White,
    navy = DarkNavy,
    onNavy = DarkOnNavy,
    switchThumb = DarkSwitchThumb,
    paper = White,                                  // Papel blanco y tinta oscura, como en claro: el QR tiene que leerse.
    onPaper = Ink,
    rating = Rating,
    favorite = DarkFavorite,
    controlInactive = DarkControlInactive,
    location = DarkLocation,
    gold = Gold,
    bot = DarkBot,
    botAccent = DarkBotAccent,
    botContainer = DarkBotTint,
    lodging = Lodging,
    eco = Eco,
    warning = DarkWarning,
    warningContainer = DarkWarningTint,
    brandSoft = DarkBrandSoft,
    ecoContainer = DarkEcoTint,
    ecoLight = EcoLight,
    slateContainer = DarkSlateTint,
    mapLand = DarkMapLand,
    mapBuilding = DarkMapBuilding,
    mapRoad = DarkMapRoad,
    mapRoadMajor = DarkMapRoadMajor,
    mapRoadWarm = DarkMapRoadWarm,
    mapPark = DarkMapPark,
    mapWater = DarkMapWater,
)

internal val LocalAppColors = staticCompositionLocalOf { LightAppColors }

/** `true` cuando la app está pintada con el tema oscuro (lo decide `TurismoValenciaTheme`; por defecto, el modo del sistema). */
internal val LocalDarkTheme = staticCompositionLocalOf { false }

val MaterialTheme.appColors: AppColors
    @Composable
    @ReadOnlyComposable
    get() = LocalAppColors.current

val MaterialTheme.isDark: Boolean
    @Composable
    @ReadOnlyComposable
    get() = LocalDarkTheme.current
