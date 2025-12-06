// 文件路径: core/util/PaletteUtils.kt
package com.android.purebilibili.core.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 🔥 从图片 URL 提取主色调
 * 
 * 使用 Android Palette API 从视频封面提取颜色
 * 用于实现类似 iOS 的动态取色效果
 */
suspend fun extractDominantColor(
    context: Context,
    imageUrl: String,
    defaultColor: Color = Color(0xFF2C2C2E)
): Color = withContext(Dispatchers.IO) {
    try {
        val loader = ImageLoader(context)
        val request = ImageRequest.Builder(context)
            .data(imageUrl)
            .size(100, 100) // 使用小尺寸加快处理速度
            .allowHardware(false) // Palette 需要软件渲染的 Bitmap
            .build()

        val result = loader.execute(request)
        if (result is SuccessResult) {
            val bitmap = (result.drawable as? BitmapDrawable)?.bitmap
                ?: result.drawable.toBitmap(100, 100)
            
            extractColorFromBitmap(bitmap, defaultColor)
        } else {
            defaultColor
        }
    } catch (e: Exception) {
        e.printStackTrace()
        defaultColor
    }
}

/**
 * 🔥 从 Bitmap 提取颜色
 */
fun extractColorFromBitmap(bitmap: Bitmap, defaultColor: Color): Color {
    return try {
        val palette = Palette.from(bitmap).generate()
        
        // 优先级：振动色 > 主色 > 柔和色
        val colorInt = palette.vibrantSwatch?.rgb
            ?: palette.dominantSwatch?.rgb
            ?: palette.mutedSwatch?.rgb
            ?: palette.lightVibrantSwatch?.rgb
            ?: palette.darkVibrantSwatch?.rgb
        
        if (colorInt != null) {
            Color(colorInt)
        } else {
            defaultColor
        }
    } catch (e: Exception) {
        defaultColor
    }
}

/**
 * 🔥 Composable 版本：从 URL 提取颜色
 * 
 * 使用方式:
 * ```kotlin
 * val dominantColor by rememberDominantColor(imageUrl)
 * ```
 */
@Composable
fun rememberDominantColor(
    imageUrl: String?,
    defaultColor: Color = Color(0xFF2C2C2E)
): State<Color> {
    val context = androidx.compose.ui.platform.LocalContext.current
    val colorState = remember { mutableStateOf(defaultColor) }
    
    LaunchedEffect(imageUrl) {
        if (imageUrl.isNullOrEmpty()) {
            colorState.value = defaultColor
            return@LaunchedEffect
        }
        
        colorState.value = extractDominantColor(context, imageUrl, defaultColor)
    }
    
    return colorState
}
