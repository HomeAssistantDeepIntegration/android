package io.homeassistant.deep

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.TypedValue
import androidx.compose.ui.unit.Dp
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Size
import coil.transform.CircleCropTransformation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun getDrawableFromUrl(context: Context, imageUrl: String, placeholder: Drawable, size: Size = Size.ORIGINAL, circleCrop: Boolean = false): Drawable {
    return withContext(Dispatchers.IO) {
        try {
            val request = ImageRequest.Builder(context).apply {
                data(imageUrl)
                placeholder(placeholder)
                size(size)
                if (circleCrop) {
                    transformations(CircleCropTransformation())
                }
                memoryCachePolicy(CachePolicy.ENABLED) // Enable memory caching
                diskCachePolicy(CachePolicy.ENABLED)   // Enable disk caching
            }.build()

            request.context.imageLoader.execute(request).drawable as? BitmapDrawable ?: placeholder
        } catch (e: Exception) {
            e.printStackTrace()
            placeholder
        }
    }
}

fun Dp.Int(context: Context): Int {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        this.value,
        context.resources.displayMetrics
    ).toInt()
}

fun Dp.Float(context: Context): Float {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        this.value,
        context.resources.displayMetrics
    )
}
