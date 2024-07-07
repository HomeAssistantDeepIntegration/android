package io.homeassistant.deep

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.utils.colorInt
import com.mikepenz.iconics.utils.sizeDp

@Composable
fun rememberIconicsPainter(iconKey: String, size: Dp = 24.dp, color: androidx.compose.ui.graphics.Color? = null): Painter {
    val context = LocalContext.current
    return IconicsPainter(context, iconKey, size, color)
}

class IconicsPainter(
    context: Context,
    iconKey: String,
    private val size: Dp,
    private val color: androidx.compose.ui.graphics.Color?
) : Painter() {

    private val iconicsDrawable = IconicsDrawable(context, iconKey).apply {
        sizeDp = size.value.toInt()
        color?.let { colorInt = it.toArgb() }
    }

    override val intrinsicSize = androidx.compose.ui.geometry.Size(iconicsDrawable.intrinsicWidth.toFloat(), iconicsDrawable.intrinsicHeight.toFloat())

    override fun DrawScope.onDraw() {
        drawImage(
            image = iconicsDrawable.toBitmap().asImageBitmap(),
            colorFilter = color?.let { ColorFilter.tint(it) }
        )
    }
}