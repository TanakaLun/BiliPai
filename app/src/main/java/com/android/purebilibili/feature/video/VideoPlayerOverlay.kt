// 文件路径: feature/video/VideoPlayerOverlay.kt
package com.android.purebilibili.feature.video

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import com.android.purebilibili.core.theme.BiliPink
import com.android.purebilibili.core.util.FormatUtils
import kotlinx.coroutines.delay

@Stable
data class PlayerProgress(
    val current: Long = 0L,
    val duration: Long = 0L,
    val buffered: Long = 0L
)

@Composable
fun VideoPlayerOverlay(
    player: Player,
    title: String,
    isVisible: Boolean,
    onToggleVisible: () -> Unit,
    isFullscreen: Boolean,
    isDanmakuOn: Boolean,
    currentQualityLabel: String,
    qualityLabels: List<String>,
    qualityIds: List<Int> = emptyList(), // 🔥 新增: 清晰度ID列表
    isLoggedIn: Boolean = false, // 🔥 新增: 登录状态
    onQualitySelected: (Int) -> Unit,
    onToggleDanmaku: () -> Unit,
    onBack: () -> Unit,
    onToggleFullscreen: () -> Unit,
    // 🔥🔥 [新增参数] 是否显示统计信息，以及分辨率字符串
    showStats: Boolean = false,
    realResolution: String = "",
    // 🔥🔥 [新增参数] 清晰度切换中状态
    isQualitySwitching: Boolean = false
) {
    var showQualityMenu by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(player.isPlaying) }

    val progressState by produceState(initialValue = PlayerProgress(), key1 = player) {
        while (true) {
            if (player.isPlaying) {
                value = PlayerProgress(
                    current = player.currentPosition,
                    duration = if (player.duration < 0) 0L else player.duration,
                    buffered = player.bufferedPosition
                )
                isPlaying = true
            } else {
                isPlaying = false
            }
            delay(200)
        }
    }

    LaunchedEffect(isVisible, isPlaying) {
        if (isVisible && isPlaying) {
            delay(4000)
            if (isVisible) {
                onToggleVisible()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // --- 1. 顶部渐变遮罩 ---
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.75f),
                                Color.Black.copy(alpha = 0.1f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }

        // --- 2. 底部渐变遮罩 ---
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.1f),
                                Color.Black.copy(alpha = 0.9f)
                            )
                        )
                    )
            )
        }

        // --- 3. 控制栏内容 ---
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(tween(300)),
            exit = fadeOut(tween(300))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopControlBar(
                    title = title,
                    isFullscreen = isFullscreen,
                    isDanmakuOn = isDanmakuOn,
                    currentQualityLabel = currentQualityLabel,
                    onBack = onBack,
                    onToggleDanmaku = onToggleDanmaku,
                    onQualityClick = { showQualityMenu = true }
                )

                Spacer(modifier = Modifier.weight(1f))

                BottomControlBar(
                    isPlaying = isPlaying,
                    progress = progressState,
                    isFullscreen = isFullscreen,
                    onPlayPauseClick = {
                        if (isPlaying) player.pause() else player.play()
                        isPlaying = !isPlaying
                    },
                    onSeek = { position -> player.seekTo(position) },
                    onToggleFullscreen = onToggleFullscreen
                )
            }
        }

        // --- 4. 🔥🔥 [新增] 真实分辨率统计信息 (仅在设置开启时显示) ---
        if (showStats && realResolution.isNotEmpty() && isVisible) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 80.dp, end = 24.dp) // 放在右上角，避开 TopBar
                    .background(Color.Black.copy(0.6f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Resolution: $realResolution",
                    color = Color.Green,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 12.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }
        }

        // --- 5. 中央播放/暂停大图标 ---
        AnimatedVisibility(
            visible = isVisible && !isPlaying && !isQualitySwitching,
            modifier = Modifier.align(Alignment.Center),
            enter = scaleIn(tween(250)) + fadeIn(tween(200)),
            exit = scaleOut(tween(200)) + fadeOut(tween(200))
        ) {
            Surface(
                onClick = { player.play(); isPlaying = true },
                color = Color.Black.copy(alpha = 0.5f),
                shape = CircleShape,
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "播放",
                        tint = Color.White.copy(alpha = 0.95f),
                        modifier = Modifier.size(42.dp)
                    )
                }
            }
        }

        // --- 5.5 🔥🔥 清晰度切换中 Loading 指示器 ---
        AnimatedVisibility(
            visible = isQualitySwitching,
            modifier = Modifier.align(Alignment.Center),
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(200))
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.7f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.padding(24.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    CircularProgressIndicator(
                        color = BiliPink,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "正在切换清晰度...",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // --- 6. 清晰度菜单 ---
        if (showQualityMenu) {
            QualitySelectionMenu(
                qualities = qualityLabels,
                qualityIds = qualityIds,
                currentQuality = currentQualityLabel,
                isLoggedIn = isLoggedIn,
                onQualitySelected = { index ->
                    onQualitySelected(index)
                    showQualityMenu = false
                },
                onDismiss = { showQualityMenu = false }
            )
        }
    }
}

// ... TopControlBar, BottomControlBar, QualitySelectionMenu 等子组件保持不变 ...
// (你可以直接保留原来文件中的这些子组件代码)
@Composable
fun TopControlBar(
    title: String,
    isFullscreen: Boolean,
    isDanmakuOn: Boolean,
    currentQualityLabel: String,
    onBack: () -> Unit,
    onToggleDanmaku: () -> Unit,
    onQualityClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .statusBarsPadding(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = Color.White)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onToggleDanmaku) {
            Icon(
                if (isDanmakuOn) Icons.Default.Subtitles else Icons.Default.SubtitlesOff,
                contentDescription = null,
                tint = if (isDanmakuOn) BiliPink else Color.White.copy(alpha = 0.8f)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Surface(
            onClick = onQualityClick,
            color = Color.White.copy(alpha = 0.2f),
            shape = RoundedCornerShape(4.dp)
        ) {
            Text(
                text = currentQualityLabel,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
            )
        }
    }
}

@Composable
fun BottomControlBar(
    isPlaying: Boolean,
    progress: PlayerProgress,
    isFullscreen: Boolean,
    onPlayPauseClick: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleFullscreen: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 4.dp)
            .navigationBarsPadding()
    ) {
        VideoProgressBar(
            currentPosition = progress.current,
            duration = progress.duration,
            bufferedPosition = progress.buffered,
            onSeek = onSeek
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 0.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onPlayPauseClick,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    null,
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "${FormatUtils.formatDuration((progress.current / 1000).toInt())} / ${FormatUtils.formatDuration((progress.duration / 1000).toInt())}",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.weight(1f))

            IconButton(
                onClick = onToggleFullscreen,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                    null,
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
    }
}

@Composable
fun VideoProgressBar(
    currentPosition: Long,
    duration: Long,
    bufferedPosition: Long,
    onSeek: (Long) -> Unit
) {
    val progress = if (duration > 0) currentPosition.toFloat() / duration else 0f
    var tempProgress by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    LaunchedEffect(progress) {
        if (!isDragging) {
            tempProgress = progress
        }
    }

    Slider(
        value = if (isDragging) tempProgress else progress,
        onValueChange = {
            isDragging = true
            tempProgress = it
        },
        onValueChangeFinished = {
            isDragging = false
            onSeek((tempProgress * duration).toLong())
        },
        colors = SliderDefaults.colors(
            thumbColor = BiliPink,
            activeTrackColor = BiliPink,
            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp)
    )
}

@Composable
fun QualitySelectionMenu(
    qualities: List<String>,
    qualityIds: List<Int> = emptyList(), // 🔥 新增: 清晰度ID列表用于判断VIP要求
    currentQuality: String,
    isLoggedIn: Boolean = false, // 🔥 新增: 是否已登录
    onQualitySelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    // 🔥 判断清晰度是否需要特殊权限
    fun getQualityTag(qualityId: Int): String? {
        return when (qualityId) {
            127, 126, 125, 120 -> "大会员" // 8K, 杜比, HDR, 4K
            116, 112 -> "大会员"          // 1080P60, 1080P+
            80 -> if (!isLoggedIn) "登录" else null // 1080P 需要登录
            else -> null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .widthIn(min = 200.dp, max = 280.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable(enabled = false) {},
            color = Color(0xFF2B2B2B),
            shape = RoundedCornerShape(12.dp),
            tonalElevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Text(
                    text = "画质选择",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
                HorizontalDivider(color = Color.White.copy(0.1f))
                qualities.forEachIndexed { index, quality ->
                    val isSelected = quality == currentQuality
                    val qualityId = qualityIds.getOrNull(index) ?: 0
                    val tag = getQualityTag(qualityId)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onQualitySelected(index) }
                            .background(if (isSelected) BiliPink.copy(alpha = 0.15f) else Color.Transparent)
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = quality,
                            color = if (isSelected) BiliPink else Color.White.copy(0.9f),
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                        
                        // 🔥 显示权限标签
                        if (tag != null) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = if (tag == "大会员") Color(0xFFFB7299) else Color(0xFF666666),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = tag,
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.weight(1f))
                        
                        if (isSelected) {
                            Icon(Icons.Default.Check, null, tint = BiliPink, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}