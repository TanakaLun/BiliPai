// 文件路径: core/ui/DesignSystem.kt
package com.android.purebilibili.core.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 🎨 BiliPai 设计系统
 * 统一的颜色、间距、圆角、动画时长定义
 */
object BiliDesign {
    
    // ==================== 品牌色 ====================
    object Colors {
        // 主品牌色 (B站粉)
        val BiliPink = Color(0xFFFB7299)
        val BiliPinkLight = Color(0xFFFFE4E1)
        val BiliPinkDark = Color(0xFFE45A7B)
        
        // 辅助色
        val BiliBlue = Color(0xFF23ADE5)
        val BiliGreen = Color(0xFF00C853)
        val BiliYellow = Color(0xFFFFB300)
        val BiliOrange = Color(0xFFFF7043)
        
        // 中性色
        val TextPrimary = Color(0xFF212121)
        val TextSecondary = Color(0xFF757575)
        val TextHint = Color(0xFFBDBDBD)
        val Divider = Color(0xFFEEEEEE)
        
        // 背景色
        val Background = Color(0xFFFAFAFA)
        val Surface = Color(0xFFFFFFFF)
        val SurfaceVariant = Color(0xFFF5F5F5)
        
        // 骨架屏色
        val ShimmerBase = Color(0xFFE0E0E0)
        val ShimmerHighlight = Color(0xFFF5F5F5)
        
        // 暗色模式
        object Dark {
            val TextPrimary = Color(0xFFFFFFFF)
            val TextSecondary = Color(0xFFB0B0B0)
            val Background = Color(0xFF121212)
            val Surface = Color(0xFF1E1E1E)
            val SurfaceVariant = Color(0xFF2C2C2C)
            val ShimmerBase = Color(0xFF2A2A2A)
            val ShimmerHighlight = Color(0xFF3A3A3A)
        }
    }
    
    // ==================== 间距 ====================
    object Spacing {
        val xs = 4.dp
        val sm = 8.dp
        val md = 12.dp
        val lg = 16.dp
        val xl = 24.dp
        val xxl = 32.dp
        val xxxl = 48.dp
    }
    
    // ==================== 圆角 ====================
    object Radius {
        val xs = 4.dp
        val sm = 8.dp
        val md = 12.dp
        val lg = 16.dp
        val xl = 24.dp
        val full = 999.dp
    }
    
    // ==================== 动画时长 ====================
    object Duration {
        const val fast = 150
        const val normal = 300
        const val slow = 500
        const val shimmer = 1200
    }
    
    // ==================== 阴影 ====================
    object Elevation {
        val none = 0.dp
        val xs = 1.dp
        val sm = 2.dp
        val md = 4.dp
        val lg = 8.dp
        val xl = 16.dp
    }
}

/**
 * 🔥 Shimmer 骨架屏效果 Modifier
 * 用法: Modifier.shimmer()
 */
fun Modifier.shimmer(
    durationMillis: Int = BiliDesign.Duration.shimmer
): Modifier = composed {
    val shimmerColors = listOf(
        BiliDesign.Colors.ShimmerBase,
        BiliDesign.Colors.ShimmerHighlight,
        BiliDesign.Colors.ShimmerBase
    )
    
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )
    
    background(
        brush = Brush.linearGradient(
            colors = shimmerColors,
            start = Offset(translateAnim - 500f, 0f),
            end = Offset(translateAnim, 0f)
        )
    )
}

/**
 * 🔥 骨架屏占位符组件
 */
@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    width: Dp = 100.dp,
    height: Dp = 16.dp,
    radius: Dp = BiliDesign.Radius.sm
) {
    Box(
        modifier = modifier
            .size(width, height)
            .clip(RoundedCornerShape(radius))
            .shimmer()
    )
}

/**
 * 🔥 视频卡片骨架屏
 */
@Composable
fun VideoCardSkeleton(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(BiliDesign.Spacing.sm)
    ) {
        // 封面
        ShimmerBox(
            modifier = Modifier.fillMaxWidth(),
            height = 180.dp,
            radius = BiliDesign.Radius.md
        )
        Spacer(modifier = Modifier.height(BiliDesign.Spacing.sm))
        
        // 标题
        ShimmerBox(
            modifier = Modifier.fillMaxWidth(0.9f),
            height = 18.dp
        )
        Spacer(modifier = Modifier.height(BiliDesign.Spacing.xs))
        ShimmerBox(
            modifier = Modifier.fillMaxWidth(0.6f),
            height = 18.dp
        )
        
        Spacer(modifier = Modifier.height(BiliDesign.Spacing.sm))
        
        // UP主和播放量
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ShimmerBox(width = 80.dp, height = 14.dp)
            ShimmerBox(width = 60.dp, height = 14.dp)
        }
    }
}

/**
 * 🔥 评论骨架屏
 */
@Composable
fun CommentSkeleton(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(BiliDesign.Spacing.md)
    ) {
        // 头像
        ShimmerBox(
            width = 40.dp,
            height = 40.dp,
            radius = BiliDesign.Radius.full
        )
        
        Spacer(modifier = Modifier.width(BiliDesign.Spacing.md))
        
        Column(modifier = Modifier.weight(1f)) {
            // 用户名
            ShimmerBox(width = 100.dp, height = 14.dp)
            Spacer(modifier = Modifier.height(BiliDesign.Spacing.sm))
            
            // 评论内容
            ShimmerBox(modifier = Modifier.fillMaxWidth(0.95f), height = 14.dp)
            Spacer(modifier = Modifier.height(BiliDesign.Spacing.xs))
            ShimmerBox(modifier = Modifier.fillMaxWidth(0.7f), height = 14.dp)
        }
    }
}

/**
 * 🔥 加载列表骨架屏
 */
@Composable
fun ListLoadingSkeleton(
    itemCount: Int = 5,
    itemContent: @Composable () -> Unit = { VideoCardSkeleton() }
) {
    Column {
        repeat(itemCount) {
            itemContent()
            Spacer(modifier = Modifier.height(BiliDesign.Spacing.sm))
        }
    }
}
