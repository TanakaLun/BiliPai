// 文件路径: feature/video/VideoPlayerState.kt
package com.android.purebilibili.feature.video

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.view.WindowManager
import java.io.InputStream
import androidx.activity.ComponentActivity
import androidx.compose.runtime.*
import androidx.core.app.NotificationCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaMetadata
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.size.Scale
import coil.transform.RoundedCornersTransformation
import com.android.purebilibili.R
import com.android.purebilibili.core.network.NetworkModule
import com.android.purebilibili.core.util.FormatUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import master.flame.danmaku.danmaku.model.android.DanmakuContext
import master.flame.danmaku.ui.widget.DanmakuView
import kotlin.math.abs

private const val NOTIFICATION_ID = 1001
private const val CHANNEL_ID = "media_playback_channel"
private const val THEME_COLOR = 0xFFFB7299.toInt()

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class VideoPlayerState(
    val context: Context,
    val player: ExoPlayer,
    val danmakuView: DanmakuView,
    val mediaSession: MediaSession,
    // 🔥 性能优化：传入受管理的 CoroutineScope，避免内存泄漏
    private val scope: CoroutineScope
) {
    var isDanmakuOn by mutableStateOf(true)

    fun updateMediaMetadata(title: String, artist: String, coverUrl: String) {
        val currentItem = player.currentMediaItem ?: return

        // 1. 更新 Player 内部元数据
        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setArtworkUri(Uri.parse(FormatUtils.fixImageUrl(coverUrl)))
            .setDisplayTitle(title)
            .setIsPlayable(true)
            .build()

        val newItem = currentItem.buildUpon()
            .setMediaMetadata(metadata)
            .build()

        player.replaceMediaItem(player.currentMediaItemIndex, newItem)

        // 2. 🔥 性能优化：使用传入的 scope 而非裸创建的 CoroutineScope
        scope.launch(Dispatchers.IO) {
            val bitmap = loadBitmap(context, coverUrl)

            // 切回主线程操作 Player 和发送通知
            launch(Dispatchers.Main) {
                pushMediaNotification(title, artist, bitmap)
            }
        }
    }

    private suspend fun loadBitmap(context: Context, url: String): Bitmap? {
        return try {
            // 🔥 性能优化：使用 Coil 单例，避免重复创建 ImageLoader
            val loader = context.imageLoader
            val request = ImageRequest.Builder(context)
                .data(FormatUtils.fixImageUrl(url))
                .allowHardware(false)
                .scale(Scale.FILL)
                .transformations(RoundedCornersTransformation(16f))
                .size(512, 512)
                .build()
            val result = loader.execute(request)
            (result as? SuccessResult)?.drawable?.let { (it as BitmapDrawable).bitmap }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun pushMediaNotification(title: String, artist: String, bitmap: Bitmap?) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // 确保渠道存在
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(CHANNEL_ID, "媒体播放", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "显示播放控制"
                    setShowBadge(false)
                    setSound(null, null)
                }
                notificationManager.createNotificationChannel(channel)
            }
        }

        val style = androidx.media.app.NotificationCompat.MediaStyle()
            .setMediaSession(mediaSession.sessionCompatToken)
            .setShowActionsInCompactView(0)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(artist)
            .setLargeIcon(bitmap)
            .setStyle(style)
            .setColor(THEME_COLOR)
            .setColorized(true)
            .setOngoing(player.isPlaying)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            // 🔥🔥🔥 修复点：直接使用 sessionActivity
            .setContentIntent(mediaSession.sessionActivity)

        try {
            notificationManager.notify(NOTIFICATION_ID, builder.build())
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    fun loadDanmaku(data: ByteArray) {
        if (data.isEmpty()) {
            android.util.Log.w("Danmaku", "Empty danmaku data, skip loading")
            return
        }
        scope.launch(Dispatchers.IO) {
            try {
                android.util.Log.d("Danmaku", "Loading danmaku, data size: ${data.size} bytes")
                val stream = java.io.ByteArrayInputStream(data)
                
                // 🔥 创建解析器
                val parser = com.android.purebilibili.core.util.BiliDanmakuParser().apply {
                    load(com.android.purebilibili.core.util.StreamDataSource(stream))
                }

                // 🔥 在主线程绑定到 View
                launch(Dispatchers.Main) {
                    // 🔥🔥 关键修复：设置 Callback 监听 prepared 事件
                    danmakuView.setCallback(object : master.flame.danmaku.controller.DrawHandler.Callback {
                        override fun prepared() {
                            // 🔥🔥 关键：Callback 可能在后台线程调用，必须切回主线程访问 Player
                            android.os.Handler(android.os.Looper.getMainLooper()).post {
                                val viewWidth = danmakuView.width
                                val viewHeight = danmakuView.height
                                val isAttached = danmakuView.isAttachedToWindow
                                android.util.Log.d("Danmaku", "DanmakuView prepared! isDanmakuOn=$isDanmakuOn, currentPos=${player.currentPosition}ms")
                                android.util.Log.d("Danmaku", "DanmakuView dimensions: ${viewWidth}x${viewHeight}, attached=$isAttached")
                                
                                if (viewWidth == 0 || viewHeight == 0) {
                                    android.util.Log.e("Danmaku", "⚠️ DanmakuView has ZERO dimensions! Cannot render danmaku")
                                }
                                
                                if (isDanmakuOn) {
                                    android.util.Log.d("Danmaku", "Calling danmakuView.show() and start()")
                                    try {
                                        danmakuView.show()
                                        danmakuView.start(player.currentPosition)
                                        android.util.Log.d("Danmaku", "✅ show() and start() called successfully")
                                    } catch (e: Exception) {
                                        android.util.Log.e("Danmaku", "❌ Error calling show/start", e)
                                    }
                                } else {
                                    android.util.Log.w("Danmaku", "isDanmakuOn is false, skipping start()")
                                }
                            }
                        }
                        override fun updateTimer(timer: master.flame.danmaku.danmaku.model.DanmakuTimer) {}
                        override fun danmakuShown(danmaku: master.flame.danmaku.danmaku.model.BaseDanmaku?) {
                            // 🔥 添加日志：确认弹幕被显示
                            android.util.Log.d("Danmaku", "danmakuShown: ${danmaku?.text?.take(20)}")
                        }
                        override fun drawingFinished() {}
                    })
                    
                    val danmakuContext = DanmakuContext.create().apply {
                        setDanmakuStyle(master.flame.danmaku.danmaku.model.IDisplayer.DANMAKU_STYLE_STROKEN, 3f)
                        isDuplicateMergingEnabled = true
                        setScrollSpeedFactor(1.2f)
                        setScaleTextSize(1.2f)
                        setMaximumLines(mapOf(
                            master.flame.danmaku.danmaku.model.BaseDanmaku.TYPE_SCROLL_RL to 5,
                            master.flame.danmaku.danmaku.model.BaseDanmaku.TYPE_FIX_TOP to 3,
                            master.flame.danmaku.danmaku.model.BaseDanmaku.TYPE_FIX_BOTTOM to 3
                        ))
                    }
                    
                    android.util.Log.d("Danmaku", "Calling danmakuView.prepare()")
                    danmakuView.prepare(parser, danmakuContext)
                    danmakuView.showFPS(false)
                    danmakuView.enableDanmakuDrawingCache(true)
                    
                    // 🔥 先调用 show() 确保可见
                    android.util.Log.d("Danmaku", "Initial show() call, isDanmakuOn=$isDanmakuOn")
                    if (isDanmakuOn) {
                        danmakuView.show()
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("Danmaku", "Failed to load danmaku", e)
            }
        }
    }
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun rememberVideoPlayerState(
    context: Context,
    viewModel: PlayerViewModel,
    bvid: String
): VideoPlayerState {

    val player = remember(context) {
        val headers = mapOf(
            "Referer" to "https://www.bilibili.com",
            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"
        )
        val dataSourceFactory = OkHttpDataSource.Factory(NetworkModule.okHttpClient)
            .setDefaultRequestProperties(headers)

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()

        ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build()
            .apply {
                prepare()
                playWhenReady = true
            }
    }

    val sessionActivityPendingIntent = remember(context, bvid) {
        val intent = Intent(context, VideoActivity::class.java).apply {
            putExtra("bvid", bvid)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    val mediaSession = remember(player, sessionActivityPendingIntent) {
        MediaSession.Builder(context, player)
            .setSessionActivity(sessionActivityPendingIntent)
            .build()
    }

    val danmakuContext = remember {
        DanmakuContext.create().apply {
            setDanmakuStyle(0, 3f)
            isDuplicateMergingEnabled = true
            setScrollSpeedFactor(1.2f)
            setScaleTextSize(1.0f)
        }
    }
    val danmakuView = remember(context) { DanmakuView(context) }
    
    // 🔥 性能优化：使用 rememberCoroutineScope 创建受管理的协程作用域
    val scope = rememberCoroutineScope()

    val holder = remember(player, danmakuView, mediaSession, scope) {
        VideoPlayerState(context, player, danmakuView, mediaSession, scope)
    }

    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(uiState) {
        if (uiState is PlayerUiState.Success) {
            val info = (uiState as PlayerUiState.Success).info
            holder.updateMediaMetadata(info.title, info.owner.name, info.pic)
        }
    }

    DisposableEffect(player, danmakuView, mediaSession) {
        onDispose {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(NOTIFICATION_ID)

            mediaSession.release()
            player.release()
            danmakuView.release()
            (context as? ComponentActivity)?.window?.attributes?.screenBrightness =
                WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        }
    }

    LaunchedEffect(bvid) { viewModel.loadVideo(bvid) }
    LaunchedEffect(player) { viewModel.attachPlayer(player) }
    
    // 🔥 监听弹幕数据并在加载后初始化弹幕（仅执行一次）
    val danmakuData = (uiState as? PlayerUiState.Success)?.danmakuData
    LaunchedEffect(danmakuData) {
        android.util.Log.d("VideoPlayerState", "LaunchedEffect(danmakuData): data size = ${danmakuData?.size ?: 0}")
        if (danmakuData != null && danmakuData.isNotEmpty()) {
            android.util.Log.d("VideoPlayerState", "Calling holder.loadDanmaku()")
            holder.loadDanmaku(danmakuData)
        }
    }

    LaunchedEffect(player.isPlaying) {
        while (true) {
            if (danmakuView.isPrepared && holder.isDanmakuOn) {
                if (player.isPlaying) {
                    if (danmakuView.isPaused) danmakuView.resume()
                    if (abs(player.currentPosition - danmakuView.currentTime) > 1000) {
                        danmakuView.seekTo(player.currentPosition)
                    }
                } else if (!danmakuView.isPaused) danmakuView.pause()
            }
            kotlinx.coroutines.delay(500)
        }
    }
    LaunchedEffect(holder.isDanmakuOn) {
        if (holder.isDanmakuOn) danmakuView.show() else danmakuView.hide()
    }

    return holder
}