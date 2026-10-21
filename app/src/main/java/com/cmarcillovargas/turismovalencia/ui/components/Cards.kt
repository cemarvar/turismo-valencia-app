package com.cmarcillovargas.turismovalencia.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cmarcillovargas.turismovalencia.R
import com.cmarcillovargas.turismovalencia.data.model.Category
import com.cmarcillovargas.turismovalencia.data.model.ContentItem
import com.cmarcillovargas.turismovalencia.ui.theme.CardShape
import com.cmarcillovargas.turismovalencia.ui.theme.Spacing
import com.cmarcillovargas.turismovalencia.ui.theme.appColors
import com.cmarcillovargas.turismovalencia.util.formatDistance
import com.cmarcillovargas.turismovalencia.util.formatRating

private val GridImageHeight = 124.dp
private val ListThumbSize = 84.dp
private val ThumbShape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
private const val DimmedAlpha = 0.45f
private const val PhotoButtonAlpha = 0.4f

/** Línea de ubicación de una ficha: «Ciutat Vella · 450 m», «15–19 marzo · Centro» o solo el área. */
fun ContentItem.locationLine(): String = when {
    isEvent -> listOfNotNull(dateLabel, area).joinToString(" · ")
    distanceMeters > 0 -> "$area · ${formatDistance(distanceMeters)}"
    else -> area
}

/** Foto de una ficha o, si aún no hay, un bloque suave con el icono de su sección. */
@Composable
fun PlaceImage(
    @androidx.annotation.DrawableRes image: Int?,
    category: Category,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.Crop,
    /** Foto de la web (ruta dentro de `assets/img/`) que se usa si no hay recurso propio. */
    assetPath: String? = null,
) {
    val asset = if (image == null) rememberAssetImage(assetPath) else null
    if (image != null) {
        Image(painterResource(image), contentDescription, modifier, contentScale = contentScale)
    } else if (asset != null) {
        Image(asset, contentDescription, modifier, contentScale = contentScale)
    } else {
        Box(
            modifier = modifier.background(MaterialTheme.appColors.brandSoft).semantics { if (contentDescription != null) this.contentDescription = contentDescription },
            contentAlignment = Alignment.Center,
        ) {
            Icon(category.icon(), contentDescription = null, tint = MaterialTheme.appColors.brand.copy(alpha = 0.5f), modifier = Modifier.size(Spacing.xl + Spacing.xs))
        }
    }
}

enum class FavoriteStyle {
    /** Círculo blanco sobre una foto. */
    OnPhotoLight,

    /** Círculo oscuro translúcido sobre una foto. */
    OnPhotoDark,

    /** Solo el corazón, en las listas. */
    Plain,

    /** Círculo gris con el corazón rojo (hoja del mapa). */
    Tinted,
}

@Composable
fun FavoriteButton(
    favorite: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    style: FavoriteStyle = FavoriteStyle.Plain,
    circleSize: Dp = 36.dp,
) {
    val colors = MaterialTheme.colorScheme
    val red = MaterialTheme.appColors.favorite
    val background = when (style) {
        FavoriteStyle.OnPhotoLight -> colors.surface
        FavoriteStyle.OnPhotoDark -> colors.scrim.copy(alpha = PhotoButtonAlpha)
        FavoriteStyle.Tinted -> colors.surfaceContainerHighest
        FavoriteStyle.Plain -> Color.Transparent
    }
    val tint = when (style) {
        FavoriteStyle.OnPhotoDark -> Color.White
        else -> if (favorite) red else colors.secondary
    }
    val description = stringResource(if (favorite) R.string.common_favorite_remove else R.string.common_favorite_add)
    Box(
        modifier = modifier
            .size(circleSize)
            .clip(CircleShape)
            .background(background)
            .toggleable(value = favorite, role = Role.Checkbox, onValueChange = { onToggle() })
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (favorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(if (style == FavoriteStyle.Plain) Spacing.xl - Spacing.xxs else Spacing.l - Spacing.xxs),
        )
    }
}

@Composable
fun LocationRow(text: String, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = MaterialTheme.appColors.brand, modifier = Modifier.size(Spacing.m + Spacing.xxs))
        Spacer(Modifier.width(Spacing.xxs))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun RatingRow(rating: Double, count: String?, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.Star, contentDescription = null, tint = MaterialTheme.appColors.rating, modifier = Modifier.size(Spacing.m + Spacing.xxs))
        Spacer(Modifier.width(Spacing.xxs))
        Text(
            text = formatRating(rating),
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (count != null) {
            Spacer(Modifier.width(Spacing.xxs))
            Text(text = "($count)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Chip «Sin conexión» (ya guardado) o «Descargar». */
@Composable
fun OfflineChip(downloaded: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    StatusChip(
        text = stringResource(if (downloaded) R.string.chip_offline_ready else R.string.chip_download),
        tone = if (downloaded) StatusTone.Success else StatusTone.Neutral,
        icon = Icons.Outlined.Download,
        onClick = onClick,
        modifier = modifier,
        height = Spacing.l - Spacing.xxs,
        small = true,
    )
}

/** Tarjeta de la cuadrícula del inicio. */
@Composable
fun PlaceGridCard(
    item: ContentItem,
    favorite: Boolean,
    downloaded: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleDownload: () -> Unit,
    modifier: Modifier = Modifier,
    dimmed: Boolean = false,
) {
    Surface(
        modifier = modifier.alpha(if (dimmed) DimmedAlpha else 1f).clip(CardShape).clickable(onClick = onClick),
        shape = CardShape,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = Spacing.xxs / 2,
    ) {
        Column {
            Box(Modifier.fillMaxWidth().height(GridImageHeight)) {
                PlaceImage(item.thumbForCard(), item.category, Modifier.fillMaxSize(), item.title, assetPath = item.thumbAsset ?: item.heroAsset)
                FavoriteButton(
                    favorite = favorite,
                    onToggle = onToggleFavorite,
                    modifier = Modifier.align(Alignment.TopEnd).padding(Spacing.xs),
                    style = FavoriteStyle.OnPhotoLight,
                )
                if (item.isEco) {
                    StatusChip(
                        text = stringResource(R.string.eco_tag),
                        tone = StatusTone.Eco,
                        icon = Icons.Outlined.Eco,
                        modifier = Modifier.align(Alignment.TopStart).padding(Spacing.xs),
                    )
                }
            }
            Column(Modifier.padding(Spacing.s), verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                LocationRow(item.locationLine())
                if (item.rating != null) RatingRow(item.rating, item.ratingCount)
                Spacer(Modifier.height(Spacing.xxs))
                OfflineChip(downloaded = downloaded, onClick = onToggleDownload, modifier = Modifier.heightIn(min = Spacing.l))
            }
        }
    }
}

/** Tarjeta de lista: foto cuadrada, etiqueta, título, ubicación y (opcional) valoración. */
@Composable
fun PlaceListCard(
    item: ContentItem,
    favorite: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
    eyebrow: String = item.eyebrow,
    showRating: Boolean = true,
    locationText: String = item.locationLine(),
    footer: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Surface(
        modifier = modifier.fillMaxWidth().clip(CardShape).clickable(onClick = onClick),
        shape = CardShape,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = Spacing.xxs / 2,
    ) {
        Row(Modifier.padding(Spacing.xs), verticalAlignment = Alignment.CenterVertically) {
            PlaceImage(
                image = item.thumb,
                category = item.category,
                modifier = Modifier.size(ListThumbSize).clip(ThumbShape),
                contentDescription = item.title,
                assetPath = item.thumbAsset ?: item.heroAsset,
            )
            Spacer(Modifier.width(Spacing.s))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xxs / 2)) {
                Eyebrow(eyebrow)
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                LocationRow(locationText)
                if (showRating && item.rating != null) RatingRow(item.rating, item.ratingCount)
                footer?.invoke()
            }
            if (trailing != null) {
                trailing()
            } else {
                FavoriteButton(favorite = favorite, onToggle = onToggleFavorite, style = FavoriteStyle.Plain, circleSize = Spacing.xl)
            }
        }
    }
}

/** Título de sección con la barrita de acento naranja debajo. */
@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, trailing: (@Composable () -> Unit)? = null) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
        Column(Modifier.weight(1f)) {
            Text(text = text, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(Spacing.xxs))
            Box(
                Modifier
                    .size(width = Spacing.xl, height = Spacing.xxs - 1.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.appColors.brand),
            )
        }
        trailing?.invoke()
    }
}

@Composable
fun UserAvatar(initials: String, onClick: (() -> Unit)?, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.appColors.navy)
            .let { if (onClick != null) it.clickable(role = Role.Button, onClick = onClick) else it },
        contentAlignment = Alignment.Center,
    ) {
        Text(text = initials, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.appColors.onNavy)
    }
}

/** Fila de información con icono en círculo naranja suave («Horario», «Dirección»…). */
@Composable
fun InfoItemRow(icon: ImageVector, title: String, text: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(Spacing.xl + Spacing.xs).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.appColors.brand, modifier = Modifier.size(Spacing.l - Spacing.xxs))
        }
        Spacer(Modifier.width(Spacing.m))
        Column {
            Text(text = title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
            Text(text = text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Imagen que se usa en las tarjetas pequeñas: la miniatura y, si falta, la foto de cabecera. */
fun ContentItem.thumbForCard(): Int? = thumb ?: hero
