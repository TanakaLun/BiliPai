// 文件路径: feature/home/components/BottomBar.kt
package com.android.purebilibili.feature.home.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState // 🔥 Add import
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Subscriptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale // 🔥 Import scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeChild
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials

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
    HISTORY(
        "历史",
        { Icon(Icons.Outlined.History, null) },
        { Icon(Icons.Outlined.History, null) }
    ),
    PROFILE(
        "我的",
        { Icon(Icons.Outlined.AccountCircle, null) },
        { Icon(Icons.Outlined.AccountCircle, null) }
    )
}

/**
 * 🔥 iOS 风格磨砂玻璃底部导航栏
 * 
 * 特性：
 * - 实时磨砂玻璃效果 (使用 Haze 库)
 * - 悬浮圆角设计
 * - 自动适配深色/浅色模式
 */
@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun FrostedBottomBar(
    currentItem: BottomNavItem = BottomNavItem.HOME,
    onItemClick: (BottomNavItem) -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    isFloating: Boolean = true // 🔥 新增参数
) {
    val isDarkTheme = MaterialTheme.colorScheme.background.red < 0.5f

    // 🔥 样式参数计算
    // Floating 使用固定高度，Docked 使用自适应高度 (Content 64dp + SystemBars)
    val barHorizontalPadding = if (isFloating) 24.dp else 0.dp
    val barBottomPadding = if (isFloating) 16.dp else 0.dp
    val barShape = if (isFloating) RoundedCornerShape(36.dp) else RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = barHorizontalPadding)
            .padding(bottom = barBottomPadding)
            .then(if (isFloating) Modifier.navigationBarsPadding() else Modifier) // Docked needs manual handling or internal padding
    ) {
        // 🔥 主内容层
        Surface(
            modifier = Modifier
                .then(
                    if (isFloating) {
                         Modifier
                            .shadow(
                                elevation = 8.dp,
                                shape = barShape,
                                ambientColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                                spotColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                            )
                            .height(72.dp) // Floating 固定高度
                    } else {
                        Modifier // Docked 高度由内容撑开
                    }
                )
                .fillMaxWidth()
                .clip(barShape)
                .then(
                    if (hazeState != null) {
                        Modifier.hazeChild(
                            state = hazeState,
                            style = HazeMaterials.thin(), // 🔥 恢复 thin，更通透
                            shape = barShape
                        )
                    } else {
                        Modifier
                    }
                ),
            // 🔥 关键修复：背景色完全透明，让 Haze 全权负责模糊和着色
            color = Color.Transparent, 
            shape = barShape,
            shadowElevation = 0.dp,
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.onSurface.copy(alpha = if (isFloating) 0.2f else 0.15f), 
                        MaterialTheme.colorScheme.onSurface.copy(alpha = if (isFloating) 0.05f else 0.02f)
                    )
                )
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (!isFloating) {
                            Modifier
                                .windowInsetsPadding(WindowInsets.navigationBars) // 🔥 Docked: 增加底部避让
                                .height(64.dp) // 🔥 Docked: 内容高度固定 64dp
                        } else {
                            Modifier.fillMaxHeight() // 🔥 Floating: 充满父容器 (72dp)
                        }
                    )
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomNavItem.entries.forEach { item ->
                    val isSelected = item == currentItem
                    
                    val iconColor by animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        animationSpec = spring(),
                        label = "iconColor"
                    )
                    
                    // 🔥 缩放动画 (选中时放大回弹)
                    val scale by animateFloatAsState(
                        targetValue = if (isSelected) 1.2f else 1.0f,
                        animationSpec = spring(
                            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                            stiffness = androidx.compose.animation.core.Spring.StiffnessLow
                        ),
                        label = "scale"
                    )
                    
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onItemClick(item) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .scale(scale), // 🔥 Apply scale
                            contentAlignment = Alignment.Center
                        ) {
                            CompositionLocalProvider(
                                LocalContentColor provides iconColor
                            ) {
                                if (isSelected) item.selectedIcon() else item.unselectedIcon()
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = item.label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = iconColor
                        )
                    }
                }
            }
        }
    }
}
