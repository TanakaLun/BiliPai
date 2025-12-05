// 文件路径: feature/video/DanmakuSettingsPanel.kt
package com.android.purebilibili.feature.video

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.purebilibili.core.theme.BiliPink

/**
 * 弹幕设置状态
 */
data class DanmakuSettings(
    val enabled: Boolean = true,
    val opacity: Float = 0.8f,         // 0.0 - 1.0
    val speed: Float = 1.0f,           // 0.5 - 2.0
    val fontSize: Float = 1.0f,        // 0.5 - 1.5
    val showScrolling: Boolean = true,  // 滚动弹幕
    val showTop: Boolean = true,        // 顶部弹幕
    val showBottom: Boolean = true      // 底部弹幕
)

/**
 * 🔥 弹幕设置面板
 */
@Composable
fun DanmakuSettingsPanel(
    settings: DanmakuSettings,
    onSettingsChange: (DanmakuSettings) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // 标题栏
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "弹幕设置",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "关闭",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 弹幕开关
            SettingRow(
                title = "弹幕开关",
                trailing = {
                    Switch(
                        checked = settings.enabled,
                        onCheckedChange = { onSettingsChange(settings.copy(enabled = it)) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = BiliPink
                        )
                    )
                }
            )
            
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )
            
            // 透明度
            SettingSliderRow(
                title = "透明度",
                value = settings.opacity,
                valueRange = 0.2f..1f,
                valueLabel = "${(settings.opacity * 100).toInt()}%",
                onValueChange = { onSettingsChange(settings.copy(opacity = it)) }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 速度
            SettingSliderRow(
                title = "弹幕速度",
                value = settings.speed,
                valueRange = 0.5f..2f,
                valueLabel = "${String.format("%.1f", settings.speed)}x",
                onValueChange = { onSettingsChange(settings.copy(speed = it)) }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 字体大小
            SettingSliderRow(
                title = "字体大小",
                value = settings.fontSize,
                valueRange = 0.5f..1.5f,
                valueLabel = when {
                    settings.fontSize < 0.8f -> "小"
                    settings.fontSize < 1.2f -> "中"
                    else -> "大"
                },
                onValueChange = { onSettingsChange(settings.copy(fontSize = it)) }
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // 弹幕类型过滤
            Text(
                text = "显示类型",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DanmakuTypeChip(
                    text = "滚动",
                    selected = settings.showScrolling,
                    onClick = { onSettingsChange(settings.copy(showScrolling = !settings.showScrolling)) },
                    modifier = Modifier.weight(1f)
                )
                DanmakuTypeChip(
                    text = "顶部",
                    selected = settings.showTop,
                    onClick = { onSettingsChange(settings.copy(showTop = !settings.showTop)) },
                    modifier = Modifier.weight(1f)
                )
                DanmakuTypeChip(
                    text = "底部",
                    selected = settings.showBottom,
                    onClick = { onSettingsChange(settings.copy(showBottom = !settings.showBottom)) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SettingRow(
    title: String,
    trailing: @Composable () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        trailing()
    }
}

@Composable
private fun SettingSliderRow(
    title: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    valueLabel: String,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = valueLabel,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = BiliPink
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = BiliPink,
                activeTrackColor = BiliPink,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun DanmakuTypeChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) BiliPink else MaterialTheme.colorScheme.surfaceVariant,
        label = "bg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "content"
    )
    
    Surface(
        onClick = onClick,
        modifier = modifier.height(40.dp),
        shape = RoundedCornerShape(20.dp),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = text,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                color = contentColor
            )
        }
    }
}
