// 文件路径: feature/home/HomeComponents.kt
package com.android.purebilibili.feature.home

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert // ✅ 修复：补全 MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow // ✅ 修复：补全 shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.android.purebilibili.core.theme.BiliPink
import com.android.purebilibili.core.util.FormatUtils
import com.android.purebilibili.data.model.response.VideoItem

// --- 动效工具 ---
fun Modifier.premiumClickable(onClick: () -> Unit): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "scale"
    )
    this.scale(scale).clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
}

@Composable
fun Modifier.staggeredEnter(index: Int, visible: Boolean): Modifier {
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 300, delayMillis = index * 50),
        label = "alpha"
    )
    val translationY by animateDpAsState(
        targetValue = if (visible) 0.dp else 50.dp,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow),
        label = "translate"
    )
    return this.graphicsLayer {
        this.alpha = alpha
        this.translationY = translationY.toPx()
    }
}

// --- ✨ 核心组件：杂志感卡片 ---
@Composable
fun ElegantVideoCard(video: VideoItem, index: Int, onClick: (String, Long) -> Unit) {
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isVisible = true }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .staggeredEnter(index, isVisible)
            .premiumClickable { onClick(video.bvid, 0) }
            .padding(bottom = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.6f)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(FormatUtils.fixImageUrl(if (video.pic.startsWith("//")) "https:${video.pic}" else video.pic))
                    .crossfade(true).build(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .align(Alignment.BottomCenter)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.7f))))
            )
            Text(
                text = FormatUtils.formatDuration(video.duration),
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .background(Color.Black.copy(0.3f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            )
            Row(modifier = Modifier.align(Alignment.BottomStart).padding(8.dp)) {
                Text(
                    text = "▶ ${FormatUtils.formatStat(video.stat.view.toLong())}",
                    color = Color.White.copy(0.9f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = video.title,
            maxLines = 2,
            minLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                letterSpacing = 0.1.sp
            ),
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(modifier = Modifier.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = video.owner.name,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
            Icon(
                Icons.Default.MoreVert,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

// --- ✨ 核心组件：液态顶栏 (无模糊版) ---
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

    // 🔥 移除 RenderEffect，改为纯透明度变化
    // 滚动时背景变为 95% 不透明的 Surface 色，保留一点点通透感，但不模糊
    val targetAlpha = if (collapseProgress > 0.1f) 0.95f else 0f
    val backgroundColor = MaterialTheme.colorScheme.surface.copy(alpha = targetAlpha)

    val dividerAlpha by animateFloatAsState(if (collapseProgress > 0.1f) 1f else 0f, label = "divider")

    Surface(
        color = backgroundColor,
        modifier = Modifier.fillMaxWidth(),
        // 移除 shadow elevation，用分割线代替
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

            // 底部微分割线：仅在滚动时显示
            if (dividerAlpha > 0f) {
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f * dividerAlpha)
                )
            }
        }
    }
}

// --- 🩹 兼容性修复 (全量补全) ---

// ✅ 必需：解决 SearchScreen 和 CommonListScreen 的报错
@Composable
fun VideoGridItem(video: VideoItem, index: Int, onClick: (String, Long) -> Unit) {
    ElegantVideoCard(video = video, index = index, onClick = onClick)
}

// ✅ 必需：解决 HomeScreen 的报错
@Composable
fun ErrorState(msg: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("加载失败", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.height(8.dp))
        Text(msg, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = BiliPink)) {
            Text("重试")
        }
    }
}

// ✅ 必需：解决 HomeScreen 的报错
@Composable
fun WelcomeDialog(githubUrl: String, onConfirm: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    AlertDialog(
        onDismissRequest = {},
        title = { Text("欢迎") },
        text = {
            Column {
                Text("本应用仅供学习使用。")
                TextButton(onClick = { uriHandler.openUri(githubUrl) }) {
                    Text("开源地址: $githubUrl", fontSize = 12.sp, color = BiliPink)
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = BiliPink)) {
                Text("进入")
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}