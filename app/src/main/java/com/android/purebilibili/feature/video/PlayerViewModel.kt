// 文件路径: feature/video/PlayerViewModel.kt
package com.android.purebilibili.feature.video

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.android.purebilibili.data.model.response.RelatedVideo
import com.android.purebilibili.data.model.response.ReplyItem
import com.android.purebilibili.data.model.response.ViewInfo
import com.android.purebilibili.data.repository.VideoRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.io.InputStream

// 移除 SubReplyUiState 定义，移入 VideoCommentViewModel.kt

sealed class PlayerUiState {
    object Loading : PlayerUiState()
    data class Success(
        val info: ViewInfo,
        val playUrl: String,
        val related: List<RelatedVideo> = emptyList(),
        val danmakuData: ByteArray? = null,
        val currentQuality: Int = 64,
        val qualityLabels: List<String> = emptyList(),
        val qualityIds: List<Int> = emptyList(),
        val startPosition: Long = 0L,
        // 🔥 新增：清晰度切换状态
        val isQualitySwitching: Boolean = false,
        val requestedQuality: Int? = null, // 用户请求的清晰度，用于显示降级提示
        // 🔥 登录与大会员状态
        val isLoggedIn: Boolean = false,
        val isVip: Boolean = false,  // 🔥 新增：大会员状态

        // 移除评论相关状态: replies, isRepliesLoading, replyCount, repliesError, isRepliesEnd, nextPage

        val emoteMap: Map<String, String> = emptyMap()
    ) : PlayerUiState()
    data class Error(val msg: String) : PlayerUiState()
}

class PlayerViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<PlayerUiState>(PlayerUiState.Loading)
    val uiState = _uiState.asStateFlow()

    // 移除 subReplyState

    private val _toastEvent = Channel<String>()
    val toastEvent = _toastEvent.receiveAsFlow()

    private var currentBvid: String = ""
    private var currentCid: Long = 0
    private var exoPlayer: ExoPlayer? = null

    fun attachPlayer(player: ExoPlayer) {
        this.exoPlayer = player
        val currentState = _uiState.value
        if (currentState is PlayerUiState.Success) {
            playVideo(currentState.playUrl, currentState.startPosition)
        }
    }

    fun getPlayerCurrentPosition(): Long = exoPlayer?.currentPosition ?: 0L
    fun getPlayerDuration(): Long = if ((exoPlayer?.duration ?: 0L) < 0) 0L else exoPlayer?.duration ?: 0L
    fun seekTo(pos: Long) { exoPlayer?.seekTo(pos) }

    override fun onCleared() {
        super.onCleared()
        exoPlayer = null
    }

    // 🔥🔥🔥 [修改 1] 增加 forceReset 参数，默认 false
    private fun playVideo(url: String, seekTo: Long = 0L, forceReset: Boolean = false) {
        val player = exoPlayer ?: return

        val currentUri = player.currentMediaItem?.localConfiguration?.uri.toString()

        // 如果不是强制重置，且 URL 相同，且正在播放，则跳过（避免重复加载）
        // 但如果是切换画质，即使 URL 看起来一样（有时 B 站返回相同 URL），我们也要强制重置
        if (!forceReset && currentUri == url && player.playbackState != Player.STATE_IDLE) {
            return
        }

        val mediaItem = MediaItem.fromUri(url)
        player.setMediaItem(mediaItem)
        if (seekTo > 0) {
            player.seekTo(seekTo)
        }
        player.prepare()
        player.playWhenReady = true
    }

    // 🔥🔥 [新增] DASH 格式播放：合并视频和音频流
    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    private fun playDashVideo(videoUrl: String, audioUrl: String?, seekTo: Long = 0L) {
        val player = exoPlayer ?: return
        android.util.Log.d("PlayerVM", "🔥 playDashVideo: video=${videoUrl.take(50)}..., audio=${audioUrl?.take(50) ?: "null"}")
        
        val headers = mapOf(
            "Referer" to "https://www.bilibili.com",
            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
        )
        val dataSourceFactory = androidx.media3.datasource.okhttp.OkHttpDataSource.Factory(
            com.android.purebilibili.core.network.NetworkModule.okHttpClient
        ).setDefaultRequestProperties(headers)
        
        val mediaSourceFactory = androidx.media3.exoplayer.source.ProgressiveMediaSource.Factory(dataSourceFactory)
        
        val videoSource = mediaSourceFactory.createMediaSource(MediaItem.fromUri(videoUrl))
        
        val finalSource = if (audioUrl != null) {
            val audioSource = mediaSourceFactory.createMediaSource(MediaItem.fromUri(audioUrl))
            // 🔥 使用 MergingMediaSource 合并视频和音频
            androidx.media3.exoplayer.source.MergingMediaSource(videoSource, audioSource)
        } else {
            videoSource
        }
        
        player.setMediaSource(finalSource)
        if (seekTo > 0) {
            player.seekTo(seekTo)
        }
        player.prepare()
        player.playWhenReady = true
    }

    fun loadVideo(bvid: String) {
        if (bvid.isBlank()) return
        currentBvid = bvid
        viewModelScope.launch {
            _uiState.value = PlayerUiState.Loading

            val detailDeferred = async { VideoRepository.getVideoDetails(bvid) }
            val relatedDeferred = async { VideoRepository.getRelatedVideos(bvid) }
            val emoteDeferred = async { VideoRepository.getEmoteMap() }

            val detailResult = detailDeferred.await()
            val relatedVideos = relatedDeferred.await()
            val emoteMap = emoteDeferred.await()

            detailResult.onSuccess { (info, playData) ->
                currentCid = info.cid
                android.util.Log.d("PlayerVM", "Fetching danmaku for cid: $currentCid")
                val danmaku = VideoRepository.getDanmakuRawData(info.cid)
                android.util.Log.d("PlayerVM", "Danmaku data result: ${danmaku?.size ?: 0} bytes")
                // 🔥 DASH 格式处理：分别获取视频和音频 URL
                val dashVideo = playData.dash?.video?.firstOrNull()
                val dashAudio = playData.dash?.audio?.firstOrNull()
                val videoUrl = dashVideo?.baseUrl ?: playData.durl?.firstOrNull()?.url ?: ""
                val audioUrl = dashAudio?.baseUrl  // 可能为 null
                android.util.Log.d("PlayerVM", "🔥 DASH: video=${dashVideo?.id ?: "none"}, audio=${dashAudio?.id ?: "none"}")
                
                val qualities = playData.accept_quality ?: emptyList()
                val labels = playData.accept_description ?: emptyList()
                // 🔥 使用正在播放的 DASH 视频画质，而不是 durl 画质
                val realQuality = dashVideo?.id ?: playData.quality

                if (videoUrl.isNotEmpty()) {
                    // 🔥 根据是否有音频流选择播放方式
                    if (dashVideo != null) {
                        playDashVideo(videoUrl, audioUrl, 0L)
                    } else {
                        playVideo(videoUrl)
                    }
                    // 🔥 获取登录状态和大会员状态
                    val isLogin = !com.android.purebilibili.core.store.TokenManager.sessDataCache.isNullOrEmpty()
                    val isVip = com.android.purebilibili.core.store.TokenManager.isVipCache
                    
                    _uiState.value = PlayerUiState.Success(
                        info = info,
                        playUrl = videoUrl,
                        related = relatedVideos,
                        danmakuData = danmaku,
                        currentQuality = realQuality,
                        qualityIds = qualities,
                        qualityLabels = labels,
                        startPosition = 0L,
                        emoteMap = emoteMap,
                        isLoggedIn = isLogin,
                        isVip = isVip
                    )
                    // 移除 loadComments 调用
                } else {
                    _uiState.value = PlayerUiState.Error("无法获取播放地址")
                }
            }.onFailure {
                _uiState.value = PlayerUiState.Error(it.message ?: "加载失败")
            }
        }
    }
    
    // 移除 loadComments, openSubReply, closeSubReply, loadMoreSubReplies, loadSubReplies

    // --- 核心优化: 清晰度切换 ---
    fun changeQuality(qualityId: Int, currentPos: Long) {
        val currentState = _uiState.value
        if (currentState is PlayerUiState.Success) {
            // 🔥 防止重复切换：如果正在切换中或已是目标画质，则跳过
            if (currentState.isQualitySwitching) {
                viewModelScope.launch { _toastEvent.send("正在切换中，请稍候...") }
                return
            }
            if (currentState.currentQuality == qualityId) {
                viewModelScope.launch { _toastEvent.send("已是当前清晰度") }
                return
            }

            viewModelScope.launch {
                // 🔥 进入切换状态
                _uiState.value = currentState.copy(
                    isQualitySwitching = true,
                    requestedQuality = qualityId
                )

                try {
                    fetchAndPlay(
                        currentBvid, currentCid, qualityId,
                        currentState, currentPos
                    )
                } catch (e: Exception) {
                    // 🔥 切换失败，恢复状态
                    _uiState.value = currentState.copy(
                        isQualitySwitching = false,
                        requestedQuality = null
                    )
                    _toastEvent.send("清晰度切换失败: ${e.message}")
                }
            }
        }
    }

    private suspend fun fetchAndPlay(
        bvid: String, cid: Long, qn: Int,
        currentState: PlayerUiState.Success,
        startPos: Long
    ) {
        // 调用 Repository 获取新画质链接
        val playUrlData = VideoRepository.getPlayUrlData(bvid, cid, qn)

        // 🔥 DASH 格式处理：找到对应画质的视频，并获取最佳音频
        val dashVideo = playUrlData?.dash?.video?.find { it.id == qn }
            ?: playUrlData?.dash?.video?.firstOrNull()
        val dashAudio = playUrlData?.dash?.audio?.firstOrNull()  // 选择最高质量音频
        val videoUrl = dashVideo?.baseUrl ?: playUrlData?.durl?.firstOrNull()?.url ?: ""
        val audioUrl = dashAudio?.baseUrl
        android.util.Log.d("PlayerVM", "🔥 fetchAndPlay DASH: video=${dashVideo?.id ?: "none"}, audio=${dashAudio?.id ?: "none"}")
        
        val qualities = playUrlData?.accept_quality ?: emptyList()
        val labels = playUrlData?.accept_description ?: emptyList()
        // 🔥 使用正在播放的 DASH 视频画质
        val realQuality = dashVideo?.id ?: playUrlData?.quality ?: qn

        if (videoUrl.isNotEmpty()) {
            // 🔥 使用 DASH 播放（如果有音频流）或普通播放
            if (dashVideo != null) {
                playDashVideo(videoUrl, audioUrl, startPos)
            } else {
                playVideo(videoUrl, startPos, forceReset = true)
            }

            // 🔥 切换完成，更新状态并清除切换标志
            _uiState.value = currentState.copy(
                playUrl = videoUrl,
                currentQuality = realQuality,
                qualityIds = qualities,
                qualityLabels = labels,
                startPosition = startPos,
                isQualitySwitching = false,
                requestedQuality = null
            )

            // 🔥 提示用户实际切换结果
            val targetLabel = labels.getOrNull(qualities.indexOf(qn)) ?: "$qn"
            val realLabel = labels.getOrNull(qualities.indexOf(realQuality)) ?: "$realQuality"

            if (realQuality != qn) {
                _toastEvent.send("⚠️ $targetLabel 需要登录大会员，已自动切换至 $realLabel")
            } else {
                _toastEvent.send("✓ 已切换至 $realLabel")
            }
        } else {
            // 🔥 切换失败，恢复状态
            _uiState.value = currentState.copy(
                isQualitySwitching = false,
                requestedQuality = null
            )
            _toastEvent.send("该清晰度无法播放")
        }
    }
}