// 文件路径: feature/video/VideoDetailScreen.kt
package com.android.purebilibili.feature.video

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.view.Window
import android.view.WindowManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.purebilibili.core.theme.BiliPink
import com.android.purebilibili.core.util.BiliDanmakuParser
import com.android.purebilibili.core.util.StreamDataSource
import com.android.purebilibili.data.model.response.RelatedVideo
import com.android.purebilibili.data.model.response.ReplyItem
import com.android.purebilibili.data.model.response.ViewInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun VideoDetailScreen(
    bvid: String,
    coverUrl: String,
    onBack: () -> Unit,
    isInPipMode: Boolean = false,
    isVisible: Boolean = true,
    viewModel: PlayerViewModel = viewModel(),
    commentViewModel: VideoCommentViewModel = viewModel() // 🔥
) {
    val context = LocalContext.current
    val view = LocalView.current
    val configuration = LocalConfiguration.current
    val uiState by viewModel.uiState.collectAsState()
    
    // 🔥 监听评论状态
    val commentState by commentViewModel.commentState.collectAsState()
    val subReplyState by commentViewModel.subReplyState.collectAsState()

    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    var isPipMode by remember { mutableStateOf(isInPipMode) }
    LaunchedEffect(isInPipMode) { isPipMode = isInPipMode }

    // 退出重置亮度
    DisposableEffect(Unit) {
        onDispose {
            val window = context.findActivity()?.window
            val layoutParams = window?.attributes
            layoutParams?.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            window?.attributes = layoutParams
        }
    }

    // 初始化播放器状态
    val playerState = rememberVideoPlayerState(
        context = context,
        viewModel = viewModel,
        bvid = bvid
    )

    // 🔥🔥🔥 核心修改：初始化评论 & 媒体中心信息
    LaunchedEffect(uiState) {
        if (uiState is PlayerUiState.Success) {
            val info = (uiState as PlayerUiState.Success).info
            
            // 初始化评论
            commentViewModel.init(info.aid)
            
            playerState.updateMediaMetadata(
                title = info.title,
                artist = info.owner.name,
                coverUrl = info.pic
            )
        } else if (uiState is PlayerUiState.Loading) {
            playerState.updateMediaMetadata(
                title = "加载中...",
                artist = "",
                coverUrl = coverUrl
            )
        }
    }
    
    // 🔥🔥🔥 弹幕加载逻辑已移至 VideoPlayerState 内部处理
    // 避免在此处重复消耗 InputStream

    // 辅助函数：切换屏幕方向
    fun toggleOrientation() {
        val activity = context.findActivity() ?: return
        if (isLandscape) {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
    }

    // 沉浸式状态栏控制
    val backgroundColor = MaterialTheme.colorScheme.background
    val isLightBackground = remember(backgroundColor) { backgroundColor.luminance() > 0.5f }

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context.findActivity())?.window ?: return@SideEffect
            val insetsController = WindowCompat.getInsetsController(window, view)

            if (isLandscape) {
                insetsController.hide(WindowInsetsCompat.Type.systemBars())
                insetsController.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                window.statusBarColor = Color.Black.toArgb()
                window.navigationBarColor = Color.Black.toArgb()
            } else {
                insetsController.show(WindowInsetsCompat.Type.systemBars())
                insetsController.isAppearanceLightStatusBars = isLightBackground
                window.statusBarColor = Color.Transparent.toArgb()
                window.navigationBarColor = Color.Transparent.toArgb()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isLandscape) Color.Black else MaterialTheme.colorScheme.background)
    ) {
        // 🔥 横竖屏过渡动画
        AnimatedContent(
            targetState = isLandscape,
            transitionSpec = {
                (fadeIn(animationSpec = tween(300)) + 
                 scaleIn(initialScale = 0.92f, animationSpec = tween(300)))
                    .togetherWith(
                        fadeOut(animationSpec = tween(200)) + 
                        scaleOut(targetScale = 1.08f, animationSpec = tween(200))
                    )
            },
            label = "orientation_transition"
        ) { targetIsLandscape ->
            if (targetIsLandscape) {
                VideoPlayerSection(
                    playerState = playerState,
                    uiState = uiState,
                    isFullscreen = true,
                    isInPipMode = isPipMode,
                    onToggleFullscreen = { toggleOrientation() },
                    onQualityChange = { qid, pos -> viewModel.changeQuality(qid, pos) },
                    onBack = { toggleOrientation() }
                )
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .background(Color.Black)
                    ) {
                        VideoPlayerSection(
                            playerState = playerState,
                            uiState = uiState,
                            isFullscreen = false,
                            isInPipMode = isPipMode,
                            onToggleFullscreen = { toggleOrientation() },
                            onQualityChange = { qid, pos -> viewModel.changeQuality(qid, pos) },
                            onBack = onBack
                        )
                    }

                    when (uiState) {
                        is PlayerUiState.Loading -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = BiliPink)
                            }
                        }

                        is PlayerUiState.Success -> {
                            val success = uiState as PlayerUiState.Success
                            VideoContentSection(
                                info = success.info,
                                relatedVideos = success.related,
                                replies = commentState.replies, // 🔥
                                replyCount = commentState.replyCount, // 🔥
                                emoteMap = success.emoteMap,
                                isRepliesLoading = commentState.isRepliesLoading, // 🔥
                                onRelatedVideoClick = { vid -> viewModel.loadVideo(vid) },
                                onSubReplyClick = { commentViewModel.openSubReply(it) }, // 🔥
                                onLoadMoreReplies = { commentViewModel.loadComments() } // 🔥
                            )
                        }

                        is PlayerUiState.Error -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text((uiState as PlayerUiState.Error).msg)
                                    Spacer(Modifier.height(16.dp))
                                    Button(
                                        onClick = { viewModel.loadVideo(bvid) },
                                        colors = ButtonDefaults.buttonColors(containerColor = BiliPink)
                                    ) { Text("重试") }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // 🔥 评论二级弹窗
        if (subReplyState.visible) {
            BackHandler {
                commentViewModel.closeSubReply()
            }
            val successState = uiState as? PlayerUiState.Success
            SubReplySheet(
                state = subReplyState,
                emoteMap = successState?.emoteMap ?: emptyMap(),
                onDismiss = { commentViewModel.closeSubReply() },
                onLoadMore = { commentViewModel.loadMoreSubReplies() }
            )
        }
    }
}

private fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

// VideoContentSection 保持原样，无需修改
@Composable
fun VideoContentSection(
    info: ViewInfo,
    relatedVideos: List<RelatedVideo>,
    replies: List<ReplyItem>,
    replyCount: Int,
    emoteMap: Map<String, String>,
    isRepliesLoading: Boolean,
    onRelatedVideoClick: (String) -> Unit,
    onSubReplyClick: (ReplyItem) -> Unit,
    onLoadMoreReplies: () -> Unit
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    // 粗略计算评论区的 Index
    val commentHeaderIndex = 6 + relatedVideos.size + 1

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item { VideoHeaderSection(info = info) }

        item {
            ActionButtonsRow(
                info = info,
                onCommentClick = {
                    coroutineScope.launch {
                        listState.animateScrollToItem(commentHeaderIndex)
                    }
                }
            )
        }

        item {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )
        }

        item { DescriptionSection(desc = info.desc) }

        item {
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(thickness = 8.dp, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        }

        item { RelatedVideosHeader() }

        items(relatedVideos, key = { it.bvid }) { video ->
            RelatedVideoItem(video = video, onClick = { onRelatedVideoClick(video.bvid) })
        }

        item {
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(thickness = 8.dp, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        }

        item { ReplyHeader(count = replyCount) }

        if (replies.isEmpty() && replyCount > 0 && isRepliesLoading) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(64.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = BiliPink)
                }
            }
        } else {
            items(replies, key = { it.rpid }) { reply ->
                ReplyItemView(
                    item = reply,
                    emoteMap = emoteMap,
                    onClick = { },
                    onSubClick = { onSubReplyClick(reply) } // 🔥 Open sub-reply
                )
            }

            // 如果还有更多评论
            if (replies.size < replyCount) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onLoadMoreReplies() }
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isRepliesLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = BiliPink, strokeWidth = 2.dp)
                        } else {
                            Text("加载更多评论", color = BiliPink)
                        }
                    }
                }
            } else if (replies.isNotEmpty()) {
                 item {
                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                         Text("—— end ——", color = Color.Gray, fontSize = 12.sp)
                    }
                 }
            }
        }
    }
}