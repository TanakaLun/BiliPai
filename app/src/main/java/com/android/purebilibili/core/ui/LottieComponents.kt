// 文件路径: core/ui/LottieComponents.kt
package com.android.purebilibili.core.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.*

/**
 * 🎬 Lottie 动画加载器
 * 使用在线 Lottie 动画 URL
 */
object LottieUrls {
    // 🔥 精选免费 Lottie 动画资源
    const val LOADING_DOTS = "https://lottie.host/5d9d2c7c-d7f4-4f3e-9e9f-f8a6e1f8c3a1/loading.json"
    const val LOADING_CIRCLE = "https://assets2.lottiefiles.com/packages/lf20_p8bfn5to.json"
    const val LOADING_BILIBILI = "https://assets10.lottiefiles.com/packages/lf20_jcikwtux.json"
    const val LIKE_HEART = "https://assets4.lottiefiles.com/packages/lf20_hc7rwmvb.json"
    const val STAR = "https://assets9.lottiefiles.com/packages/lf20_c50nklxn.json"
    const val CONFETTI = "https://assets10.lottiefiles.com/packages/lf20_u4yrau.json"
    const val SUCCESS = "https://assets4.lottiefiles.com/packages/lf20_jbrw3hcz.json"
    const val ERROR = "https://assets1.lottiefiles.com/packages/lf20_cr9slsdh.json"
    const val EMPTY = "https://assets9.lottiefiles.com/packages/lf20_wnqlfojb.json"
    const val REFRESH = "https://assets3.lottiefiles.com/packages/lf20_ykzaax7v.json"
}

/**
 * 🔥 通用 Lottie 动画组件
 */
@Composable
fun LottieAnimation(
    url: String,
    modifier: Modifier = Modifier,
    size: Dp = 100.dp,
    iterations: Int = LottieConstants.IterateForever,
    autoPlay: Boolean = true
) {
    val composition by rememberLottieComposition(
        spec = LottieCompositionSpec.Url(url)
    )
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = iterations,
        isPlaying = autoPlay
    )
    
    com.airbnb.lottie.compose.LottieAnimation(
        composition = composition,
        progress = { progress },
        modifier = modifier.size(size)
    )
}

/**
 * 🔥 加载动画组件
 */
@Composable
fun LoadingAnimation(
    modifier: Modifier = Modifier,
    size: Dp = 80.dp,
    text: String? = null
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LottieAnimation(
            url = LottieUrls.LOADING_CIRCLE,
            size = size
        )
        if (text != null) {
            Spacer(modifier = Modifier.height(BiliDesign.Spacing.sm))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

/**
 * 🔥 点赞动画按钮
 */
@Composable
fun LikeButton(
    isLiked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 32.dp
) {
    var isPlaying by remember { mutableStateOf(false) }
    
    val composition by rememberLottieComposition(
        spec = LottieCompositionSpec.Url(LottieUrls.LIKE_HEART)
    )
    val progress by animateLottieCompositionAsState(
        composition = composition,
        isPlaying = isPlaying,
        restartOnPlay = true,
        iterations = 1
    )
    
    // 动画播放完毕后重置状态
    LaunchedEffect(progress) {
        if (progress == 1f) {
            isPlaying = false
        }
    }
    
    Box(
        modifier = modifier
            .size(size)
            .clickable {
                if (!isLiked) {
                    isPlaying = true
                }
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        if (isLiked || isPlaying) {
            com.airbnb.lottie.compose.LottieAnimation(
                composition = composition,
                progress = { if (isLiked && !isPlaying) 1f else progress },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // 未点赞状态显示静态图标
            com.airbnb.lottie.compose.LottieAnimation(
                composition = composition,
                progress = { 0f },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/**
 * 🔥 收藏动画按钮
 */
@Composable
fun FavoriteButton(
    isFavorite: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 32.dp
) {
    var isPlaying by remember { mutableStateOf(false) }
    
    val composition by rememberLottieComposition(
        spec = LottieCompositionSpec.Url(LottieUrls.STAR)
    )
    val progress by animateLottieCompositionAsState(
        composition = composition,
        isPlaying = isPlaying,
        restartOnPlay = true,
        iterations = 1
    )
    
    LaunchedEffect(progress) {
        if (progress == 1f) {
            isPlaying = false
        }
    }
    
    Box(
        modifier = modifier
            .size(size)
            .clickable {
                if (!isFavorite) {
                    isPlaying = true
                }
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        com.airbnb.lottie.compose.LottieAnimation(
            composition = composition,
            progress = { if (isFavorite && !isPlaying) 1f else progress },
            modifier = Modifier.fillMaxSize()
        )
    }
}

/**
 * 🔥 空状态组件
 */
@Composable
fun EmptyState(
    message: String = "暂无内容",
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(BiliDesign.Spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LottieAnimation(
            url = LottieUrls.EMPTY,
            size = 150.dp
        )
        Spacer(modifier = Modifier.height(BiliDesign.Spacing.lg))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        if (actionText != null && onAction != null) {
            Spacer(modifier = Modifier.height(BiliDesign.Spacing.md))
            Text(
                text = actionText,
                style = MaterialTheme.typography.labelLarge,
                color = BiliDesign.Colors.BiliPink,
                modifier = Modifier.clickable { onAction() }
            )
        }
    }
}

/**
 * 🔥 错误状态组件
 */
@Composable
fun ErrorState(
    message: String = "加载失败",
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(BiliDesign.Spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LottieAnimation(
            url = LottieUrls.ERROR,
            size = 120.dp,
            iterations = 1
        )
        Spacer(modifier = Modifier.height(BiliDesign.Spacing.lg))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        if (onRetry != null) {
            Spacer(modifier = Modifier.height(BiliDesign.Spacing.md))
            Text(
                text = "点击重试",
                style = MaterialTheme.typography.labelLarge,
                color = BiliDesign.Colors.BiliPink,
                modifier = Modifier.clickable { onRetry() }
            )
        }
    }
}

/**
 * 🔥 成功动画
 */
@Composable
fun SuccessAnimation(
    modifier: Modifier = Modifier,
    size: Dp = 80.dp,
    onFinished: () -> Unit = {}
) {
    var finished by remember { mutableStateOf(false) }
    
    val composition by rememberLottieComposition(
        spec = LottieCompositionSpec.Url(LottieUrls.SUCCESS)
    )
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = 1
    )
    
    LaunchedEffect(progress) {
        if (progress == 1f && !finished) {
            finished = true
            onFinished()
        }
    }
    
    com.airbnb.lottie.compose.LottieAnimation(
        composition = composition,
        progress = { progress },
        modifier = modifier.size(size)
    )
}
