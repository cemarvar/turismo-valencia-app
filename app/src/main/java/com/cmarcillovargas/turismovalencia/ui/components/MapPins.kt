package com.cmarcillovargas.turismovalencia.ui.components

import android.graphics.Bitmap
import android.graphics.RectF
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.VectorGroup
import androidx.compose.ui.graphics.vector.VectorNode
import androidx.compose.ui.graphics.vector.VectorPath
import androidx.compose.ui.graphics.vector.toPath
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.ceil
import kotlin.math.max

// Medidas de los marcadores, en dp (las del diseño: círculo de 36 con icono de 20; el seleccionado, de 64 con icono de 34).
private const val PinCircle = 36f
private const val PinIcon = 20f
private const val PinRing = 2f
private const val PinWidth = 48f
private const val PinHeight = 48f
private const val PinTipWidth = 16f
private const val PinTipHeight = 8f
private const val SelectedCircle = 64f
private const val SelectedIcon = 34f
private const val SelectedRing = 3f
private const val LabelHeight = 28f
private const val LabelPadding = 12f
private const val LabelMaxWidth = 190f
private const val LabelGap = 4f
private const val UserSize = 88f
private val ShadowColor = Color(0x33000000)

/** Dónde queda el punto del lugar dentro del bitmap del marcador (fracción del ancho y del alto). */
internal data class PinAnchor(val x: Float, val y: Float)

/**
 * Zona que recoge el toque de un marcador, en píxeles respecto al punto del lugar (x a la derecha, y hacia abajo): un círculo y,
 * si lo hay, un rectángulo (la etiqueta). El bitmap de un marcador es mayor que lo que se ve y, sin esto, el seleccionado se
 * quedaría los toques de los marcadores de al lado.
 */
internal class PinHit(
    private val cx: Float,
    private val cy: Float,
    private val radius: Float,
    private val label: RectF? = null,
) {
    fun contains(dx: Float, dy: Float): Boolean {
        val ox = dx - cx
        val oy = dy - cy
        return (radius >= 0f && ox * ox + oy * oy <= radius * radius) || label?.contains(dx, dy) == true
    }

    companion object {
        /** El marcador del usuario no recoge toques. */
        val None = PinHit(0f, 0f, -1f)
    }
}

/** Marcador seleccionado ya dibujado: su imagen, dónde queda el lugar en ella y su zona de toque. */
internal class SelectedPin(val bitmap: Bitmap, val anchor: PinAnchor, val hit: PinHit)

/** Colores y estilos que necesita [PinRenderer], ya resueltos desde el tema. */
internal data class PinStyle(
    val labelStyle: TextStyle,
    val labelColor: Color,
    val labelBackground: Color,
    val userColor: Color,
)

/**
 * Dibuja los marcadores del mapa en bitmaps con los gráficos de Compose (círculo de color con el icono de la categoría, el
 * seleccionado con su nombre debajo, y el punto azul del usuario). osmdroid pinta bitmaps como marcadores, así que esto da el
 * mismo aspecto que el del diseño sin duplicar los iconos en XML. Las imágenes se guardan en caché.
 */
internal class PinRenderer(
    private val density: Density,
    private val measurer: TextMeasurer,
    private val style: PinStyle,
) {
    private data class Key(val kind: String, val color: Color, val icon: String, val title: String?)

    private val cache = HashMap<Key, Bitmap>()

    private fun dp(value: Float) = value * density.density

    /** Marcador normal: la punta de abajo señala el lugar. */
    val pinAnchor = PinAnchor(0.5f, 1f)

    /** El círculo del marcador normal, con al menos 48 dp de zona de toque, y sobre la punta. */
    val pinHit: PinHit
        get() {
            val circleCenterAboveTip = dp(PinHeight) - dp(PinRing) - dp(PinCircle) / 2f
            return PinHit(0f, -circleCenterAboveTip, max(dp(PinCircle) / 2f + dp(PinRing), dp(24f)))
        }

    fun pin(color: Color, icon: ImageVector): Bitmap = cache.getOrPut(Key("pin", color, icon.name, null)) {
        val width = dp(PinWidth).toInt()
        val height = dp(PinHeight).toInt()
        render(width, height) {
            val radius = dp(PinCircle) / 2f
            val ring = dp(PinRing)
            val cx = size.width / 2f
            val cy = ring + radius
            val tipTop = cy + radius
            tip(cx, tipTop - dp(1f), dp(PinTipWidth) + ring, size.height, Color.White)
            drawCircle(ShadowColor, radius + ring, Offset(cx, cy + dp(1.5f)))
            drawCircle(Color.White, radius + ring, Offset(cx, cy))
            tip(cx, tipTop - dp(1f), dp(PinTipWidth) - ring, size.height - ring * 1.5f, color)
            drawCircle(color, radius, Offset(cx, cy))
            drawVector(icon, dp(PinIcon), Offset(cx - dp(PinIcon) / 2f, cy - dp(PinIcon) / 2f), Color.White)
        }
    }

    /** Marcador seleccionado: círculo grande con el icono y, si hay título, su nombre en una etiqueta debajo. */
    fun selected(color: Color, icon: ImageVector, title: String?): SelectedPin {
        val ring = dp(SelectedRing)
        val radius = dp(SelectedCircle) / 2f
        val circleBox = (radius + ring) * 2f
        val label = title?.let {
            measurer.measure(
                text = AnnotatedString(it),
                style = style.labelStyle.copy(color = style.labelColor),
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                constraints = Constraints(maxWidth = (dp(LabelMaxWidth) - dp(LabelPadding) * 2).toInt()),
            )
        }
        val pillWidth = label?.let { ceil(it.size.width + dp(LabelPadding) * 2).toInt() } ?: 0
        val width = ceil(max(circleBox, pillWidth.toFloat())).toInt() + dp(4f).toInt()
        val height = ceil(circleBox + if (label != null) dp(LabelGap) + dp(LabelHeight) + dp(3f) else dp(3f)).toInt()
        val bitmap = cache.getOrPut(Key("selected", color, icon.name, title)) {
            render(width, height) {
                val cx = size.width / 2f
                val cy = ring + radius
                drawCircle(ShadowColor, radius + ring, Offset(cx, cy + dp(2f)))
                drawCircle(Color.White, radius + ring, Offset(cx, cy))
                drawCircle(color, radius, Offset(cx, cy))
                drawVector(icon, dp(SelectedIcon), Offset(cx - dp(SelectedIcon) / 2f, cy - dp(SelectedIcon) / 2f), Color.White)
                if (label != null) {
                    val top = circleBox + dp(LabelGap)
                    val left = (size.width - pillWidth) / 2f
                    val corner = CornerRadius(dp(10f))
                    drawRoundRect(ShadowColor, Offset(left, top + dp(1.5f)), Size(pillWidth.toFloat(), dp(LabelHeight)), corner)
                    drawRoundRect(style.labelBackground, Offset(left, top), Size(pillWidth.toFloat(), dp(LabelHeight)), corner)
                    drawText(label, topLeft = Offset(left + dp(LabelPadding), top + (dp(LabelHeight) - label.size.height) / 2f))
                }
            }
        }
        // El círculo está centrado en el punto del lugar; la etiqueta, debajo.
        val labelRect = label?.let {
            val top = radius + ring + dp(LabelGap)
            RectF(-pillWidth / 2f, top, pillWidth / 2f, top + dp(LabelHeight))
        }
        return SelectedPin(bitmap, PinAnchor(0.5f, (ring + radius) / height), PinHit(0f, 0f, radius + ring, labelRect))
    }

    /** Punto azul del usuario con su halo. */
    fun user(): Bitmap = cache.getOrPut(Key("user", style.userColor, "", null)) {
        val side = dp(UserSize).toInt()
        render(side, side) {
            val center = Offset(size.width / 2f, size.height / 2f)
            drawCircle(style.userColor.copy(alpha = 0.15f), dp(40f), center)
            drawCircle(style.userColor.copy(alpha = 0.45f), dp(40f), center, style = Stroke(width = dp(1f)))
            drawCircle(ShadowColor, dp(10f), center + Offset(0f, dp(1f)))
            drawCircle(Color.White, dp(9f), center)
            drawCircle(style.userColor, dp(6.5f), center)
        }
    }

    private fun DrawScope.tip(cx: Float, top: Float, width: Float, bottom: Float, color: Color) {
        drawPath(
            Path().apply {
                moveTo(cx - width / 2f, top)
                lineTo(cx + width / 2f, top)
                lineTo(cx, bottom)
                close()
            },
            color,
        )
    }

    private fun render(width: Int, height: Int, block: DrawScope.() -> Unit): Bitmap {
        val image = ImageBitmap(width, height)
        CanvasDrawScope().draw(density, LayoutDirection.Ltr, Canvas(image), Size(width.toFloat(), height.toFloat()), block)
        return image.asAndroidBitmap()
    }
}

/** Dibuja un icono de Material (`ImageVector`) de un solo color en un `DrawScope` cualquiera, sin pasar por la composición. */
private fun DrawScope.drawVector(image: ImageVector, side: Float, topLeft: Offset, tint: Color) {
    val scale = side / image.viewportWidth
    withTransform({
        translate(topLeft.x, topLeft.y)
        scale(scale, scale, pivot = Offset.Zero)
    }) {
        drawNode(image.root, tint)
    }
}

private fun DrawScope.drawNode(node: VectorNode, tint: Color) {
    when (node) {
        is VectorGroup -> withTransform({
            translate(node.translationX + node.pivotX, node.translationY + node.pivotY)
            rotate(node.rotation, Offset.Zero)
            scale(node.scaleX, node.scaleY, Offset.Zero)
            translate(-node.pivotX, -node.pivotY)
        }) {
            node.forEach { drawNode(it, tint) }
        }

        is VectorPath -> drawPath(node.pathData.toPath().apply { fillType = node.pathFillType }, tint)
    }
}
