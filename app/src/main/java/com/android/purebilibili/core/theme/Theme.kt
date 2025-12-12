// 文件路径: core/theme/Theme.kt
package com.android.purebilibili.core.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// --- 扩展颜色定义 ---
private val LightSurfaceVariant = Color(0xFFF1F2F3)

// 🔥🔥 [优化] 根据主题色索引生成配色方案
private fun createDarkColorScheme(primaryColor: Color) = darkColorScheme(
    primary = primaryColor,
    onPrimary = White,
    secondary = primaryColor.copy(alpha = 0.85f),
    background = DarkBackground,
    surface = DarkSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondaryDark,
    surfaceContainer = DarkSurfaceElevated,
    outline = Color(0xFF3D3D3D),
    outlineVariant = Color(0xFF2A2A2A)
)

private fun createLightColorScheme(primaryColor: Color) = lightColorScheme(
    primary = primaryColor,
    onPrimary = White,
    secondary = primaryColor.copy(alpha = 0.8f),
    background = Color(0xFFF5F5F5),  // 🔥 柔和浅灰背景
    surface = Color(0xFFFAFAFA),     // 🔥 微灰表面，不再纯白
    onSurface = TextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = TextSecondary
)

// 保留默认配色作为后备
private val DarkColorScheme = createDarkColorScheme(BiliPink)
private val LightColorScheme = createLightColorScheme(BiliPink)

@Composable
fun PureBiliBiliTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    themeColorIndex: Int = 0, // 🔥🔥 [新增] 主题色索引
    content: @Composable () -> Unit
) {
    // 🔥 获取自定义主题色
    val customPrimaryColor = ThemeColors.getOrElse(themeColorIndex) { BiliPink }
    
    val colorScheme = when {
        // 如果开启了动态取色 且 系统版本 >= Android 12 (S)
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        // 🔥🔥 [新增] 使用自定义主题色
        darkTheme -> createDarkColorScheme(customPrimaryColor)
        else -> createLightColorScheme(customPrimaryColor)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = BiliTypography,
        content = content
    )
}