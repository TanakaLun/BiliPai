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
import androidx.compose.ui.draw.shadow
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
 * 🔥 交错入场动画 - 强化版
 * 所有可见卡片都有非线性回弹动画
 */
fun Modifier.staggeredEnter(index: Int, isVisible: Boolean): Modifier = composed {
    // 🔥 所有卡片都应用动画，但延迟封顶避免过长等待
    val delay = (index * 50).coerceAtMost(300)  // 每卡片 50ms，最大 300ms
    
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(
            durationMillis = 400,
            delayMillis = delay,
            easing = FastOutSlowInEasing
        ),
        label = "alpha"
    )
    
    // 🔥 使用更明显的回弹效果
    val translationY by animateFloatAsState(
        targetValue = if (isVisible) 0f else 100f,  // 🔥 更大的位移 (100px)
        animationSpec = spring(
            dampingRatio = 0.55f,  // 🔥 更强的回弹 (低于 1.0 会回弹)
            stiffness = 300f       // 🔥 较低刚度，动画更慢更明显
        ),
        label = "translate"
    )
    
    // 🔥 更明显的缩放动画
    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.85f,  // 🔥 从 0.85 放大到 1.0
        animationSpec = spring(
            dampingRatio = 0.6f,
            stiffness = 350f
        ),
        label = "scale_enter"
    )
    
    this.graphicsLayer {
        this.alpha = alpha
        this.translationY = translationY
        this.scaleX = scale
        this.scaleY = scale
    }
}

/**
 * 杂志感视频卡片 (含按压高亮效果)
 */
@Composable
fun ElegantVideoCard(
    video: VideoItem,
    index: Int,
    refreshKey: Long = 0L,  // 🔥 刷新标识符
    onClick: (String, Long) -> Unit
) {
    // 🔥 使用 refreshKey 确保刷新时重新触发动画
    val animationKey = "${video.bvid}_$refreshKey"
    var isVisible by remember(animationKey) { mutableStateOf(false) }
    LaunchedEffect(animationKey) { isVisible = true }

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
                .shadow(
                    elevation = 4.dp,
                    shape = RoundedCornerShape(12.dp),
                    ambientColor = Color.Black.copy(alpha = 0.1f),
                    spotColor = Color.Black.copy(alpha = 0.15f)
                )
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(FormatUtils.fixImageUrl(if (video.pic.startsWith("//")) "https:${video.pic}" else video.pic))
                    .crossfade(200)
                    .size(480, 300)  // 🔥 限制解码尺寸，降低内存占用
                    .memoryCacheKey("cover_${video.bvid}")  // 🔥 统一缓存键
                    .diskCacheKey("cover_${video.bvid}")
                    .build(),
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
            
            // 🔥 优化渐变遮罩 - 更细腻的过渡
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.3f),
                                Color.Black.copy(alpha = 0.7f)
                            ),
                            startY = 0f,
                            endY = Float.POSITIVE_INFINITY
                        )
                    )
            )
            
            // 🔥 时长标签 - 右下角
            Text(
                text = FormatUtils.formatDuration(video.duration),
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .background(Color.Black.copy(0.5f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            )
            
            // 🔥 双重统计 - 左下角 (播放量 + 弹幕)
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 播放量
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "▶",
                        color = Color.White.copy(0.9f),
                        fontSize = 9.sp
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = if (video.stat.view > 0) FormatUtils.formatStat(video.stat.view.toLong())
                               else FormatUtils.formatProgress(video.progress, video.duration),
                        color = Color.White.copy(0.95f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                // 🔥 弹幕数 (仅当有播放量时显示)
                if (video.stat.view > 0 && video.stat.danmaku > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "弹",
                            color = Color.White.copy(0.7f),
                            fontSize = 9.sp
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = FormatUtils.formatStat(video.stat.danmaku.toLong()),
                            color = Color.White.copy(0.85f),
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(10.dp))
        
        // 标题
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
        
        // 🔥 UP主信息行 - 智能统计高亮（左侧）+ 头像 + 名称
        Row(
            modifier = Modifier.padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 🔥🔥 [增强] 智能选择最突出的统计数据并在左侧红色高亮显示
            val statRed = Color(0xFFFF4444)  // 红色
            val stat = video.stat
            // 计算哪个数据最突出
            val bestStat = listOf(
                "点赞" to stat.like,
                "投币" to stat.coin,
                "收藏" to stat.favorite
            ).filter { it.second > 0 }.maxByOrNull { it.second }
            
            if (bestStat != null && bestStat.second >= 100) {  // 至少100才显示
                Text(
                    text = FormatUtils.formatStat(bestStat.second.toLong()),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = statRed  // 🔥 红色高亮
                )
                Text(
                    text = bestStat.first,
                    fontSize = 11.sp,
                    color = statRed  // 🔥 红色高亮
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            
            // 🔥 UP主头像小图标 - 优化加载
            if (video.owner.face.isNotEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(FormatUtils.fixImageUrl(video.owner.face))
                        .crossfade(150)
                        .size(72, 72)  // 🔥 限制头像解码尺寸
                        .memoryCacheKey("avatar_${video.owner.mid}")
                        .build(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(18.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(5.dp))
            }
            
            Text(
                text = video.owner.name,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
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
