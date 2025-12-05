// 文件路径: feature/home/components/VideoCard.kt
package com.android.purebilibili.feature.home.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.android.purebilibili.core.util.FormatUtils
import com.android.purebilibili.data.model.response.VideoItem

/**
 * 交错进场动画 (使用 composed 实现 @Composable 效果)
 */
fun Modifier.staggeredEnter(index: Int, isVisible: Boolean): Modifier = composed {
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 400, delayMillis = (index * 50).coerceAtMost(300)),
        label = "alpha"
    )
    val translationY by animateDpAsState(
        targetValue = if (isVisible) 0.dp else 30.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "translate"
    )
    this.graphicsLayer {
        this.alpha = alpha
        this.translationY = translationY.toPx()
    }
}

/**
 * 杂志感视频卡片 (含按压高亮效果)
 */
@Composable
fun ElegantVideoCard(video: VideoItem, index: Int, onClick: (String, Long) -> Unit) {
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isVisible = true }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "scale"
    )
    
    val highlightAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0.12f else 0f,
        animationSpec = tween(durationMillis = 100),
        label = "highlight"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .staggeredEnter(index, isVisible)
            .scale(scale)
            .clickable(interactionSource = interactionSource, indication = null) { onClick(video.bvid, 0) }
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
            
            // 按压时的白色高亮遮罩
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = highlightAlpha))
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
                    text = if (video.stat.view > 0) "▶ ${FormatUtils.formatStat(video.stat.view.toLong())}"
                           else "🕓 ${FormatUtils.formatProgress(video.progress, video.duration)}",
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

/**
 * 简化版视频网格项 (用于搜索结果等)
 * 注意: onClick 只接收 bvid，不接收 cid
 */
@Composable
fun VideoGridItem(video: VideoItem, index: Int, onClick: (String) -> Unit) {
    ElegantVideoCard(video, index) { bvid, _ -> onClick(bvid) }
}
