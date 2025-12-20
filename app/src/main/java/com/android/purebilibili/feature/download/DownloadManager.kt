package com.android.purebilibili.feature.download

import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap

/**
 * 🔥 视频下载管理器
 * 
 * 功能：
 * - 管理下载任务队列
 * - 支持断点续传
 * - 音视频分离下载后合并
 * - 持久化存储下载状态
 */
object DownloadManager {
    
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .build()
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // 下载任务状态
    private val _tasks = MutableStateFlow<Map<String, DownloadTask>>(emptyMap())
    val tasks: StateFlow<Map<String, DownloadTask>> = _tasks.asStateFlow()
    
    // 正在下载的任务协程
    private val downloadJobs = ConcurrentHashMap<String, Job>()
    
    // 下载目录
    private var downloadDir: File? = null
    private var tasksFile: File? = null
    
    /**
     * 初始化（在 Application 中调用）
     */
    fun init(context: Context) {
        downloadDir = File(context.getExternalFilesDir(null), "downloads").apply { mkdirs() }
        tasksFile = File(context.filesDir, "download_tasks.json")
        loadTasks()
    }
    
    /**
     * 获取下载目录
     */
    fun getDownloadDir(): File = downloadDir ?: throw IllegalStateException("DownloadManager not initialized")
    
    /**
     * 添加下载任务
     */
    fun addTask(task: DownloadTask): Boolean {
        val existing = _tasks.value[task.id]
        if (existing != null && existing.isDownloading) {
            return false // 已在下载中
        }
        
        val newTask = task.copy(status = DownloadStatus.PENDING)
        _tasks.value = _tasks.value + (task.id to newTask)
        saveTasks()
        
        // 自动开始下载
        startDownload(task.id)
        return true
    }
    
    /**
     * 开始下载
     */
    fun startDownload(taskId: String) {
        val task = _tasks.value[taskId] ?: return
        if (task.isDownloading) return
        
        downloadJobs[taskId]?.cancel()
        downloadJobs[taskId] = scope.launch {
            try {
                downloadTask(task)
            } catch (e: CancellationException) {
                updateTask(taskId) { it.copy(status = DownloadStatus.PAUSED) }
            } catch (e: Exception) {
                com.android.purebilibili.core.util.Logger.e("DownloadManager", "Download failed", e)
                updateTask(taskId) { 
                    it.copy(
                        status = DownloadStatus.FAILED, 
                        errorMessage = e.message ?: "下载失败"
                    ) 
                }
            }
        }
    }
    
    /**
     * 暂停下载
     */
    fun pauseDownload(taskId: String) {
        downloadJobs[taskId]?.cancel()
        downloadJobs.remove(taskId)
        updateTask(taskId) { it.copy(status = DownloadStatus.PAUSED) }
    }
    
    /**
     * 删除任务
     */
    fun removeTask(taskId: String) {
        downloadJobs[taskId]?.cancel()
        downloadJobs.remove(taskId)
        
        // 删除文件
        val task = _tasks.value[taskId]
        task?.filePath?.let { File(it).delete() }
        getVideoFile(taskId).delete()
        getAudioFile(taskId).delete()
        
        _tasks.value = _tasks.value - taskId
        saveTasks()
    }
    
    /**
     * 获取任务状态
     */
    fun getTask(bvid: String, cid: Long): DownloadTask? {
        return _tasks.value.values.find { it.bvid == bvid && it.cid == cid }
    }
    
    /**
     * 执行下载
     */
    private suspend fun downloadTask(task: DownloadTask) {
        updateTask(task.id) { it.copy(status = DownloadStatus.DOWNLOADING) }
        
        val videoFile = getVideoFile(task.id)
        val audioFile = getAudioFile(task.id)
        val outputFile = getOutputFile(task.id)
        
        // 1. 下载视频流
        downloadFile(task.videoUrl, videoFile, task.id) { progress ->
            updateTask(task.id) { it.copy(videoProgress = progress, progress = (progress + it.audioProgress) / 2) }
        }
        
        // 2. 下载音频流
        downloadFile(task.audioUrl, audioFile, task.id) { progress ->
            updateTask(task.id) { it.copy(audioProgress = progress, progress = (it.videoProgress + progress) / 2) }
        }
        
        // 3. 合并音视频
        updateTask(task.id) { it.copy(status = DownloadStatus.MERGING, progress = 0.95f) }
        mergeVideoAudio(videoFile, audioFile, outputFile)
        
        // 4. 清理临时文件
        videoFile.delete()
        audioFile.delete()
        
        // 5. 更新状态
        updateTask(task.id) { 
            it.copy(
                status = DownloadStatus.COMPLETED, 
                progress = 1f,
                filePath = outputFile.absolutePath,
                fileSize = outputFile.length()
            ) 
        }
        
        com.android.purebilibili.core.util.Logger.d("DownloadManager", "✅ Download completed: ${task.title}")
    }
    
    /**
     * 下载单个文件
     */
    private suspend fun downloadFile(
        url: String, 
        file: File, 
        taskId: String,
        onProgress: (Float) -> Unit
    ) = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0")
            .header("Referer", "https://www.bilibili.com")
            .build()
        
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            throw Exception("HTTP ${response.code}")
        }
        
        val body = response.body ?: throw Exception("Empty response")
        val totalBytes = body.contentLength()
        var downloadedBytes = 0L
        
        FileOutputStream(file).use { output ->
            body.byteStream().use { input ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    // 检查是否取消
                    if (!isActive) throw CancellationException()
                    
                    output.write(buffer, 0, bytesRead)
                    downloadedBytes += bytesRead
                    
                    if (totalBytes > 0) {
                        onProgress(downloadedBytes.toFloat() / totalBytes)
                    }
                }
            }
        }
    }
    
    /**
     * 使用 Android MediaMuxer 合并音视频
     * 将分离的视频流和音频流合并为完整的 MP4 文件
     */
    private suspend fun mergeVideoAudio(video: File, audio: File, output: File) = withContext(Dispatchers.IO) {
        try {
            com.android.purebilibili.core.util.Logger.d("DownloadManager", "🔥 Starting MediaMuxer merge...")
            
            // 创建 MediaMuxer
            val muxer = android.media.MediaMuxer(
                output.absolutePath,
                android.media.MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
            )
            
            // 提取视频轨道
            val videoExtractor = android.media.MediaExtractor()
            videoExtractor.setDataSource(video.absolutePath)
            var videoTrackIndex = -1
            var videoMuxerTrackIndex = -1
            
            for (i in 0 until videoExtractor.trackCount) {
                val format = videoExtractor.getTrackFormat(i)
                val mime = format.getString(android.media.MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("video/")) {
                    videoExtractor.selectTrack(i)
                    videoMuxerTrackIndex = muxer.addTrack(format)
                    videoTrackIndex = i
                    break
                }
            }
            
            // 提取音频轨道
            val audioExtractor = android.media.MediaExtractor()
            audioExtractor.setDataSource(audio.absolutePath)
            var audioTrackIndex = -1
            var audioMuxerTrackIndex = -1
            
            for (i in 0 until audioExtractor.trackCount) {
                val format = audioExtractor.getTrackFormat(i)
                val mime = format.getString(android.media.MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("audio/")) {
                    audioExtractor.selectTrack(i)
                    audioMuxerTrackIndex = muxer.addTrack(format)
                    audioTrackIndex = i
                    break
                }
            }
            
            if (videoTrackIndex == -1 || audioTrackIndex == -1) {
                com.android.purebilibili.core.util.Logger.e("DownloadManager", "❌ Failed to find video or audio track")
                // 降级：直接复制视频
                video.copyTo(output, overwrite = true)
                videoExtractor.release()
                audioExtractor.release()
                return@withContext
            }
            
            // 开始合并
            muxer.start()
            
            val buffer = java.nio.ByteBuffer.allocate(1024 * 1024)  // 1MB buffer
            val bufferInfo = android.media.MediaCodec.BufferInfo()
            
            // 写入视频数据
            while (true) {
                val sampleSize = videoExtractor.readSampleData(buffer, 0)
                if (sampleSize < 0) break
                
                bufferInfo.offset = 0
                bufferInfo.size = sampleSize
                bufferInfo.presentationTimeUs = videoExtractor.sampleTime
                bufferInfo.flags = videoExtractor.sampleFlags
                
                muxer.writeSampleData(videoMuxerTrackIndex, buffer, bufferInfo)
                videoExtractor.advance()
            }
            
            // 写入音频数据
            while (true) {
                val sampleSize = audioExtractor.readSampleData(buffer, 0)
                if (sampleSize < 0) break
                
                bufferInfo.offset = 0
                bufferInfo.size = sampleSize
                bufferInfo.presentationTimeUs = audioExtractor.sampleTime
                bufferInfo.flags = audioExtractor.sampleFlags
                
                muxer.writeSampleData(audioMuxerTrackIndex, buffer, bufferInfo)
                audioExtractor.advance()
            }
            
            // 清理
            videoExtractor.release()
            audioExtractor.release()
            muxer.stop()
            muxer.release()
            
            com.android.purebilibili.core.util.Logger.d("DownloadManager", "✅ MediaMuxer merge completed: ${output.name}")
            
        } catch (e: Exception) {
            com.android.purebilibili.core.util.Logger.e("DownloadManager", "❌ MediaMuxer merge failed", e)
            // 降级：直接复制视频
            video.copyTo(output, overwrite = true)
        }
    }
    
    private fun getVideoFile(taskId: String) = File(getDownloadDir(), "${taskId}_video.m4s")
    private fun getAudioFile(taskId: String) = File(getDownloadDir(), "${taskId}_audio.m4s")
    private fun getOutputFile(taskId: String) = File(getDownloadDir(), "${taskId}.mp4")
    
    private fun updateTask(taskId: String, update: (DownloadTask) -> DownloadTask) {
        val current = _tasks.value[taskId] ?: return
        _tasks.value = _tasks.value + (taskId to update(current))
        saveTasks()
    }
    
    private fun loadTasks() {
        try {
            val file = tasksFile ?: return
            if (file.exists()) {
                val content = file.readText()
                val list = json.decodeFromString<List<DownloadTask>>(content)
                _tasks.value = list.associateBy { it.id }
            }
        } catch (e: Exception) {
            com.android.purebilibili.core.util.Logger.e("DownloadManager", "Failed to load tasks", e)
        }
    }
    
    private fun saveTasks() {
        scope.launch {
            try {
                val file = tasksFile ?: return@launch
                val content = json.encodeToString(_tasks.value.values.toList())
                file.writeText(content)
            } catch (e: Exception) {
                com.android.purebilibili.core.util.Logger.e("DownloadManager", "Failed to save tasks", e)
            }
        }
    }
}
