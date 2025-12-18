// 文件路径: feature/video/ui/components/VideoAspectRatio.kt
package com.android.purebilibili.feature.video.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AspectRatio
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.ui.AspectRatioFrameLayout
import com.android.purebilibili.core.theme.BiliPink

/**
 * 视频比例枚举
 * 对应 ExoPlayer 的 AspectRatioFrameLayout.ResizeMode
 */
enum class VideoAspectRatio(val displayName: String, val resizeMode: Int) {
    FIT("适应", AspectRatioFrameLayout.RESIZE_MODE_FIT),
    FILL("填充", AspectRatioFrameLayout.RESIZE_MODE_ZOOM),
    RATIO_16_9("16:9", AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH),
    RATIO_4_3("4:3", AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT);
    
    companion object {
        fun fromResizeMode(mode: Int): VideoAspectRatio {
            return entries.find { it.resizeMode == mode } ?: FIT
        }
    }
}

/**
 * 视频比例选择菜单
 */
@Composable
fun AspectRatioMenu(
    currentRatio: VideoAspectRatio,
    onRatioSelected: (VideoAspectRatio) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Color.Black.copy(alpha = 0.85f),
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 标题
            Text(
                text = "画面比例",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // 比例选项
            VideoAspectRatio.entries.forEach { ratio ->
                val isSelected = ratio == currentRatio
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) BiliPink.copy(alpha = 0.2f) else Color.Transparent,
                    onClick = {
                        onRatioSelected(ratio)
                        onDismiss()
                    }
                ) {
                    Text(
                        text = ratio.displayName,
                        color = if (isSelected) BiliPink else Color.White,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                    )
                }
            }
        }
    }
}

/**
 * 比例按钮（用于底部控制栏）
 */
@Composable
fun AspectRatioButton(
    currentRatio: VideoAspectRatio,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = Color.Black.copy(alpha = 0.5f)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Icon(
                Icons.Outlined.AspectRatio,
                contentDescription = "画面比例",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = currentRatio.displayName,
                color = Color.White,
                fontSize = 12.sp
            )
        }
    }
}
