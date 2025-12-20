package com.android.purebilibili.feature.download

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

/**
 * 🔥 离线缓存列表页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadListScreen(
    onBack: () -> Unit,
    onVideoClick: (String) -> Unit  // bvid
) {
    val tasks by DownloadManager.tasks.collectAsState()
    val taskList = tasks.values.toList().sortedByDescending { it.createdAt }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("离线缓存") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        if (taskList.isEmpty()) {
            // 空状态
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "📥",
                        fontSize = 48.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "暂无缓存视频",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "在视频详情页点击「缓存」按钮下载",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(taskList, key = { it.id }) { task ->
                    DownloadTaskItem(
                        task = task,
                        onClick = { 
                            if (task.isComplete) {
                                onVideoClick(task.bvid)
                            }
                        },
                        onPauseResume = {
                            if (task.isDownloading) {
                                DownloadManager.pauseDownload(task.id)
                            } else if (task.canResume) {
                                DownloadManager.startDownload(task.id)
                            }
                        },
                        onDelete = {
                            DownloadManager.removeTask(task.id)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun DownloadTaskItem(
    task: DownloadTask,
    onClick: () -> Unit,
    onPauseResume: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 封面
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                // 🔥 确保使用 HTTPS 并添加 Referer
                val coverUrl = task.cover.let { url ->
                    if (url.startsWith("http://")) url.replace("http://", "https://")
                    else url
                }
                AsyncImage(
                    model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                        .data(coverUrl)
                        .addHeader("Referer", "https://www.bilibili.com")
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                
                // 进度/状态覆盖层
                if (!task.isComplete) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        when (task.status) {
                            DownloadStatus.DOWNLOADING, DownloadStatus.MERGING -> {
                                CircularProgressIndicator(
                                    progress = { task.progress },
                                    modifier = Modifier.size(32.dp),
                                    color = Color.White,
                                    strokeWidth = 3.dp
                                )
                            }
                            DownloadStatus.PAUSED -> {
                                Text("已暂停", color = Color.White, fontSize = 12.sp)
                            }
                            DownloadStatus.FAILED -> {
                                Text("失败", color = Color.Red, fontSize = 12.sp)
                            }
                            else -> {}
                        }
                    }
                }
                
                // 画质标签
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .background(
                            Color.Black.copy(alpha = 0.7f),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = task.qualityDesc,
                        color = Color.White,
                        fontSize = 10.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // 信息
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = task.title,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = task.ownerName,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // 状态文字
                val statusText = when (task.status) {
                    DownloadStatus.PENDING -> "等待中..."
                    DownloadStatus.DOWNLOADING -> "下载中 ${(task.progress * 100).toInt()}%"
                    DownloadStatus.MERGING -> "处理中..."
                    DownloadStatus.COMPLETED -> "已完成"
                    DownloadStatus.PAUSED -> "已暂停"
                    DownloadStatus.FAILED -> task.errorMessage ?: "下载失败"
                }
                Text(
                    text = statusText,
                    fontSize = 11.sp,
                    color = when (task.status) {
                        DownloadStatus.COMPLETED -> Color(0xFF4CAF50)
                        DownloadStatus.FAILED -> Color.Red
                        else -> MaterialTheme.colorScheme.primary
                    }
                )
            }
            
            // 操作按钮
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 暂停/继续
                if (task.isDownloading || task.canResume) {
                    IconButton(onClick = onPauseResume) {
                        Icon(
                            imageVector = if (task.isDownloading) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (task.isDownloading) "暂停" else "继续",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                // 删除
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "删除",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
