// 文件路径: feature/home/components/BottomBar.kt
package com.android.purebilibili.feature.home.components

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
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
 * iOS 风格底部导航栏 (磨砂效果)
 */
@Composable
fun FrostedBottomBar(
    currentItem: BottomNavItem = BottomNavItem.HOME,
    onItemClick: (BottomNavItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = !MaterialTheme.colorScheme.background.luminance().let { it > 0.5f }
    
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = if (isDark) 
            Color(0xFF1C1C1E).copy(alpha = 0.85f)
        else 
            Color(0xFFF2F2F7).copy(alpha = 0.88f),
        tonalElevation = 0.dp
    ) {
        Column {
            // 顶部细边框
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(
                        if (isDark) Color.White.copy(alpha = 0.15f)
                        else Color.Black.copy(alpha = 0.1f)
                    )
            )
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomNavItem.entries.forEach { item ->
                    val isSelected = item == currentItem
                    
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
                            modifier = Modifier.size(22.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CompositionLocalProvider(
                                LocalContentColor provides if (isSelected) 
                                    BiliPink 
                                else if (isDark)
                                    Color.White.copy(alpha = 0.55f)
                                else
                                    Color.Black.copy(alpha = 0.45f)
                            ) {
                                if (isSelected) item.selectedIcon() else item.unselectedIcon()
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = item.label,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                            color = if (isSelected) 
                                BiliPink 
                            else if (isDark)
                                Color.White.copy(alpha = 0.55f)
                            else
                                Color.Black.copy(alpha = 0.45f)
                        )
                    }
                }
            }
            
            // 底部安全区域
            Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }
}
