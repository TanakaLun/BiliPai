package com.android.purebilibili.feature.download

import kotlinx.serialization.Serializable

/**
 * 下载任务状态
 */
enum class DownloadStatus {
    PENDING,        // 等待下载
    DOWNLOADING,    // 下载中
    MERGING,        // 合并音视频中
    COMPLETED,      // 已完成
    FAILED,         // 失败
    PAUSED          // 已暂停
}

/**
 * 下载任务数据模型
 */
@Serializable
data class DownloadTask(
    val bvid: String,
    val cid: Long,
    val title: String,
    val cover: String,
    val ownerName: String,
    val ownerFace: String,
    val duration: Int,           // 时长（秒）
    val quality: Int,            // 画质 ID
    val qualityDesc: String,     // 画质描述（如 "1080P"）
    val videoUrl: String,        // 视频流 URL
    val audioUrl: String,        // 音频流 URL
    val status: DownloadStatus = DownloadStatus.PENDING,
    val progress: Float = 0f,    // 0.0 ~ 1.0
    val videoProgress: Float = 0f,
    val audioProgress: Float = 0f,
    val filePath: String? = null,// 下载完成后的文件路径
    val fileSize: Long = 0,      // 文件大小（字节）
    val downloadedSize: Long = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val errorMessage: String? = null
) {
    val id: String get() = "${bvid}_${cid}_$quality"
    
    val isComplete: Boolean get() = status == DownloadStatus.COMPLETED
    val isDownloading: Boolean get() = status == DownloadStatus.DOWNLOADING || status == DownloadStatus.MERGING
    val canResume: Boolean get() = status == DownloadStatus.PAUSED || status == DownloadStatus.FAILED
}

/**
 * 下载画质选项
 */
data class QualityOption(
    val id: Int,
    val desc: String,
    val videoUrl: String,
    val audioUrl: String
)
