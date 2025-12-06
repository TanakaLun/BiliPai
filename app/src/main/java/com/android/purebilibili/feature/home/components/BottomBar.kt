// 文件路径: feature/home/components/BottomBar.kt
package com.android.purebilibili.feature.home.components

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Subscriptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.purebilibili.core.theme.BiliPink

/**
 * 底部导航项枚举
 */
enum class BottomNavItem(
    val label: String,
    val selectedIcon: @Composable () -> Unit,
    val unselectedIcon: @Composable () -> Unit
) {
    HOME(
        "首页",
        { Icon(Icons.Filled.Home, null) },
        { Icon(Icons.Outlined.Home, null) }
    ),
    DYNAMIC(
        "动态",
        { Icon(Icons.Outlined.Subscriptions, null) },
        { Icon(Icons.Outlined.Subscriptions, null) }
    ),
    DISCOVER(
        "发现",
        { Icon(Icons.Outlined.Explore, null) },
        { Icon(Icons.Outlined.Explore, null) }
    ),
    PROFILE(
        "我的",
        { Icon(Icons.Outlined.AccountCircle, null) },
        { Icon(Icons.Outlined.AccountCircle, null) }
    )
}

/**
 * 🔥 增强版 iOS 风格底部导航栏 (更强磨砂 + 视频内容动态取色)
 * 
 * - 从当前可见视频封面提取主色调
 * - 更强的磨砂/毛玻璃效果
 * - 细腻的渐变边框
 */
@Composable
fun FrostedBottomBar(
    currentItem: BottomNavItem = BottomNavItem.HOME,
    onItemClick: (BottomNavItem) -> Unit,
    modifier: Modifier = Modifier,
    // 🔥 新增：当前可见视频封面 URL
    visibleCoverUrl: String? = null
) {
    val isDark = !MaterialTheme.colorScheme.background.luminance().let { it > 0.5f }
    
    // 🔥 从视频封面提取主色调 (使用 Palette API)
    val extractedColor by com.android.purebilibili.core.util.rememberDominantColor(
        imageUrl = visibleCoverUrl,
        defaultColor = if (isDark) Color(0xFF2C2C2E) else Color(0xFFE5E5EA)
    )
    
    // 🔥 调试日志
    LaunchedEffect(visibleCoverUrl, extractedColor) {
        android.util.Log.d("BottomBarColor", "🎨 提取颜色: url=${visibleCoverUrl?.take(40)}..., color=$extractedColor")
    }
    
    // 🔥 动画过渡提取的颜色
    val animatedExtractedColor by animateColorAsState(
        targetValue = extractedColor,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 800),
        label = "extractedColor"
    )
    
    // 🔥 背景色：使用提取的颜色混合
    val backgroundColor = if (isDark) {
        // 深色模式：提取色 40% + 基础深色 60%
        animatedExtractedColor.copy(alpha = 0.40f).compositeOver(Color(0xFF1C1C1E).copy(alpha = 0.85f))
    } else {
        // 浅色模式：提取色 30% + 基础浅色 70%
        animatedExtractedColor.copy(alpha = 0.30f).compositeOver(Color(0xFFF8F8F8).copy(alpha = 0.90f))
    }
    
    Box(modifier = modifier.fillMaxWidth()) {
        // 🔥 底层模糊背景层 (Android 12+ 才支持 Modifier.blur)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .blur(radius = 30.dp)
                    .background(backgroundColor)
            )
        }
        
        // 🔥 主内容层
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) 
                Color.Transparent 
            else 
                backgroundColor,
            tonalElevation = 0.dp
        ) {
            Column {
                // 🔥 顶部渐变边框 - 更细腻
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f),
                                    if (isDark) Color.White.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.12f),
                                    if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f),
                                    Color.Transparent
                                )
                            )
                        )
                )
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BottomNavItem.entries.forEach { item ->
                        val isSelected = item == currentItem
                        
                        // 🔥 动画过渡颜色
                        val iconColor by animateColorAsState(
                            targetValue = when {
                                isSelected -> BiliPink
                                isDark -> Color.White.copy(alpha = 0.6f)
                                else -> Color.Black.copy(alpha = 0.55f)
                            },
                            animationSpec = spring(),
                            label = "iconColor"
                        )
                        
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { onItemClick(item) },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier.size(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CompositionLocalProvider(
                                    LocalContentColor provides iconColor
                                ) {
                                    if (isSelected) item.selectedIcon() else item.unselectedIcon()
                                }
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = item.label,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = iconColor
                            )
                        }
                    }
                }
                
                // 底部安全区域
                Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
            }
        }
    }
}

// 🔥 辅助函数：颜色合成
private fun Color.compositeOver(background: Color): Color {
    val fg = this
    val alpha = fg.alpha
    return Color(
        red = fg.red * alpha + background.red * (1 - alpha),
        green = fg.green * alpha + background.green * (1 - alpha),
        blue = fg.blue * alpha + background.blue * (1 - alpha),
        alpha = 1f
    )
}
