// 文件路径: feature/live/LivePlayerScreen.kt
package com.android.purebilibili.feature.live

import android.app.Activity
import android.content.pm.ActivityInfo
import android.net.Uri
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.launch

private const val TAG = "LivePlayerScreen"

@OptIn(UnstableApi::class)
@Composable
fun LivePlayerScreen(
    roomId: Long,
    title: String,
    uname: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    
    var playUrl by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    
    // 🔥 创建带 Referer 的数据源
    val dataSourceFactory = remember {
        DefaultHttpDataSource.Factory()
            .setDefaultRequestProperties(mapOf(
                "Referer" to "https://live.bilibili.com",
                "User-Agent" to "Mozilla/5.0 (Linux; Android 11) AppleWebKit/537.36"
            ))
    }
    
    // 🔥 ExoPlayer 实例 - 使用自定义数据源
    val exoPlayer = remember {
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .build().apply {
                playWhenReady = true
            }
    }
    
    // 🔥 播放直播流
    fun playLiveStream(url: String) {
        Log.d(TAG, "Playing live stream: $url")
        playUrl = url
        
        // 🔥 根据 URL 后缀判断格式并创建合适的 MediaSource
        val mediaSource = if (url.contains(".m3u8") || url.contains("hls")) {
            // HLS 格式
            HlsMediaSource.Factory(dataSourceFactory)
                .createMediaSource(MediaItem.fromUri(url))
        } else {
            // FLV 或其他格式 - 让 ExoPlayer 自动识别
            DefaultMediaSourceFactory(dataSourceFactory)
                .createMediaSource(MediaItem.Builder()
                    .setUri(url)
                    .setMimeType("video/x-flv")  // 🔥 明确指定 FLV MIME 类型
                    .build())
        }
        
        exoPlayer.setMediaSource(mediaSource)
        exoPlayer.prepare()
        isLoading = false
    }
    
    // 🔥 加载直播流
    LaunchedEffect(roomId) {
        isLoading = true
        error = null
        val result = com.android.purebilibili.data.repository.VideoRepository.getLivePlayUrl(roomId)
        result.onSuccess { url ->
            playLiveStream(url)
        }.onFailure { e ->
            Log.e(TAG, "Failed to get live URL", e)
            error = e.message ?: "加载失败"
            isLoading = false
        }
    }
    
    // 🔥🔥 [性能优化] 生命周期感知：进入后台时暂停播放，返回前台时继续
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_PAUSE -> {
                    Log.d(TAG, "🔴 App entering background, pausing player")
                    exoPlayer.pause()
                }
                androidx.lifecycle.Lifecycle.Event.ON_RESUME -> {
                    Log.d(TAG, "🟢 App returning to foreground, resuming player")
                    exoPlayer.play()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }
    
    // 🔥 返回处理
    BackHandler { onBack() }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 播放器 - 🔥 禁用默认控制器，使用自定义覆盖层
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false  // 🔥 隐藏默认控制器（包含进度条）
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        
        // 🔥 中心播放/暂停按钮 - 点击切换
        var isPlaying by remember { mutableStateOf(true) }
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable {
                    if (exoPlayer.isPlaying) {
                        exoPlayer.pause()
                        isPlaying = false
                    } else {
                        exoPlayer.play()
                        isPlaying = true
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            // 🔥 只有暂停时显示播放按钮
            if (!isPlaying) {
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.6f)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "播放",
                        tint = Color.White,
                        modifier = Modifier
                            .padding(20.dp)
                            .size(48.dp)
                    )
                }
            }
        }
        
        // 🔥 顶部信息
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp)
                .align(Alignment.TopStart),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 返回按钮
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.5f),
                modifier = Modifier.clickable { onBack() }
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "返回",
                    tint = Color.White,
                    modifier = Modifier.padding(8.dp)
                )
            }
            
            Spacer(Modifier.width(16.dp))
            
            Column {
                Text(
                    text = title.ifEmpty { "直播间 $roomId" },
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                if (uname.isNotEmpty()) {
                    Text(
                        text = uname,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }
            }
            
            Spacer(Modifier.weight(1f))
            
            // 刷新按钮
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.5f),
                modifier = Modifier.clickable {
                    scope.launch {
                        isLoading = true
                        error = null
                        val result = com.android.purebilibili.data.repository.VideoRepository.getLivePlayUrl(roomId)
                        result.onSuccess { url ->
                            playLiveStream(url)
                        }.onFailure { e ->
                            error = e.message ?: "加载失败"
                            isLoading = false
                        }
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "刷新",
                    tint = Color.White,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
        
        // 🔥 加载中/错误状态
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.White)
            }
        }
        
        if (error != null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = error ?: "加载失败",
                        color = Color.White,
                        fontSize = 16.sp
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onBack) {
                        Text("返回")
                    }
                }
            }
        }
    }
}
