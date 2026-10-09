package com.cmarcillovargas.turismovalencia.ui.theme

import androidx.compose.material3.MaterialTheme
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

// TODO(dark): el Figma no define modo oscuro; solo existe el esquema claro.
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

/** Tokens que Material 3 no tiene como rol: marca para iconos/gráficos, valoración, favorito, ubicación y mapa. */
@Immutable
data class AppColors(
    val brand: Color,
    val rating: Color,
    val favorite: Color,
    val controlInactive: Color,
    val location: Color,
    val gold: Color,
    val bot: Color,
    val botContainer: Color,
    val lodging: Color,
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
    rating = Rating,
    favorite = Favorite,
    controlInactive = ControlInactive,
    location = Location,
    gold = Gold,
    bot = Bot,
    botContainer = BotTint,
    lodging = Lodging,
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

internal val LocalAppColors = staticCompositionLocalOf { LightAppColors }

val MaterialTheme.appColors: AppColors
    @Composable
    @ReadOnlyComposable
    get() = LocalAppColors.current
