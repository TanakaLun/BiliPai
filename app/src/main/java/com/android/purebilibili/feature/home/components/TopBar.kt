// 文件路径: feature/home/components/TopBar.kt
package com.android.purebilibili.feature.home.components

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.android.purebilibili.core.util.FormatUtils
import com.android.purebilibili.feature.home.UserState

/**
 * Q弹点击效果
 */
fun Modifier.premiumClickable(onClick: () -> Unit): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        label = "scale"
    )
    this
        .scale(scale)
        .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
}

/**
 * 液态顶栏 - 滚动时显示半透明背景
 */
@Composable
fun FluidHomeTopBar(
    user: UserState,
    scrollOffset: Float,
    onAvatarClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onSearchClick: () -> Unit
) {
    val collapseProgress by animateFloatAsState(
        targetValue = (scrollOffset / 200f).coerceIn(0f, 1f),
        label = "collapse"
    )
    val isCollapsed = collapseProgress > 0.8f

    val targetAlpha = if (collapseProgress > 0.1f) 0.95f else 0f
    val backgroundColor = MaterialTheme.colorScheme.surface.copy(alpha = targetAlpha)
    val dividerAlpha by animateFloatAsState(if (collapseProgress > 0.1f) 1f else 0f, label = "divider")

    Surface(
        color = backgroundColor,
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 0.dp
    ) {
        Column {
            Spacer(modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 头像
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .premiumClickable { onAvatarClick() }
                        .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
                ) {
                    if (user.isLogin && user.face.isNotEmpty()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(FormatUtils.fixImageUrl(user.face))
                                .crossfade(true).build(),
                            contentDescription = "Avatar",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("未", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // 搜索框
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .clip(CircleShape)
                        .background(
                            if (isCollapsed) MaterialTheme.colorScheme.surfaceVariant.copy(0.6f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(0.3f)
                        )
                        .clickable { onSearchClick() }
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Search,
                            null,
                            tint = if(isCollapsed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))

                        Crossfade(targetState = isCollapsed, label = "text") { collapsed ->
                            Text(
                                text = if (collapsed) "搜索..." else "搜索视频、UP主...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                maxLines = 1
                            )
                        }
                    }
                }

                // 设置按钮
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + expandHorizontally(),
                    exit = fadeOut() + shrinkHorizontally()
                ) {
                    Row {
                        Spacer(modifier = Modifier.width(12.dp))
                        IconButton(onClick = onSettingsClick) {
                            Icon(
                                Icons.Outlined.Settings,
                                contentDescription = "Settings",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }
            }

            // 底部微分割线
            if (dividerAlpha > 0f) {
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f * dividerAlpha)
                )
            }
        }
    }
}
