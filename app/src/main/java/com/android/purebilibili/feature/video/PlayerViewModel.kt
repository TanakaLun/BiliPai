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
        // 🔥 新增：登录状态
        val isLoggedIn: Boolean = false,

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
                val danmaku = VideoRepository.getDanmakuRawData(info.cid)
                val url = playData.durl?.firstOrNull()?.url ?: ""
                val qualities = playData.accept_quality ?: emptyList()
                val labels = playData.accept_description ?: emptyList()
                val realQuality = playData.quality

                if (url.isNotEmpty()) {
                    playVideo(url)
                    // 🔥 获取登录状态
                    val isLogin = !com.android.purebilibili.core.store.TokenManager.sessDataCache.isNullOrEmpty()
                    
                    _uiState.value = PlayerUiState.Success(
                        info = info,
                        playUrl = url,
                        related = relatedVideos,
                        danmakuData = danmaku,
                        currentQuality = realQuality,
                        qualityIds = qualities,
                        qualityLabels = labels,
                        startPosition = 0L,
                        emoteMap = emoteMap,
                        isLoggedIn = isLogin
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
        // 🔥 确保 VideoRepository.getPlayUrlData 已经接收 qn 参数
        val playUrlData = VideoRepository.getPlayUrlData(bvid, cid, qn)

        val url = playUrlData?.durl?.firstOrNull()?.url ?: ""
        val qualities = playUrlData?.accept_quality ?: emptyList()
        val labels = playUrlData?.accept_description ?: emptyList()
        val realQuality = playUrlData?.quality ?: qn

        if (url.isNotEmpty()) {
            // 🔥 强制 ExoPlayer 重置，确保真正切换流
            playVideo(url, startPos, forceReset = true)

            // 🔥 切换完成，更新状态并清除切换标志
            _uiState.value = currentState.copy(
                playUrl = url,
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