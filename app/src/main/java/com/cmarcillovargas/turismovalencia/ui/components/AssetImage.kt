package com.cmarcillovargas.turismovalencia.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val ImageFolder = "img/"
private const val CacheBytes = 32 * 1024 * 1024
private const val MaxWidthPx = 1080

/** Fotos de la web guardadas en `assets/img/` (misma ruta que en la web). Se decodifican a ≤ 1080 px y se guardan en una caché de 32 MB. */
private object AssetBitmaps {
    private val cache = object : LruCache<String, Bitmap>(CacheBytes) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }

    fun cached(path: String): ImageBitmap? = cache.get(path)?.asImageBitmap()

    fun load(context: Context, path: String): Bitmap? {
        cache.get(path)?.let { return it }
        return runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.assets.open(ImageFolder + path).use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (bounds.outWidth / (sample * 2) >= MaxWidthPx) sample *= 2
            val options = BitmapFactory.Options().apply { inSampleSize = sample }
            context.assets.open(ImageFolder + path).use { BitmapFactory.decodeStream(it, null, options) }
        }.getOrNull()?.also { cache.put(path, it) }
    }
}

/** Foto de `assets/img/` o `null` mientras carga o si el archivo no existe. */
@Composable
fun rememberAssetImage(path: String?): ImageBitmap? {
    if (path == null) return null
    val context = LocalContext.current
    // El estado depende de la ruta: si la tarjeta se reutiliza con otra foto (cambio de categoría), no se queda la anterior.
    var image by remember(path) { mutableStateOf(AssetBitmaps.cached(path)) }
    LaunchedEffect(path) {
        if (image == null) image = withContext(Dispatchers.IO) { AssetBitmaps.load(context, path) }?.asImageBitmap()
    }
    return image
}
