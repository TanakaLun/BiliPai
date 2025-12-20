// 文件路径: feature/dynamic/DynamicScreen.kt
package com.android.purebilibili.feature.dynamic

import android.content.ContentValues
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.android.purebilibili.core.theme.BiliPink
import com.android.purebilibili.core.theme.iOSBlue
import com.android.purebilibili.core.ui.EmptyState
import com.android.purebilibili.core.ui.LoadingAnimation
import com.android.purebilibili.core.ui.BiliGradientButton
import com.android.purebilibili.data.model.response.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 🔥 动态页面 - 官方风格重构版
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DynamicScreen(
    viewModel: DynamicViewModel = viewModel(),
    onVideoClick: (String) -> Unit,
    onUserClick: (Long) -> Unit = {},
    onLiveClick: (roomId: Long, title: String, uname: String) -> Unit = { _, _, _ -> },  // 🔥 直播点击
    onBack: () -> Unit,
    onLoginClick: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val listState = rememberLazyListState()
    
    // 🔥 Tab选择
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("全部", "视频")
    
    val density = LocalDensity.current
    val statusBarHeight = WindowInsets.statusBars.getTop(density).let { with(density) { it.toDp() } }
    val pullRefreshState = rememberPullToRefreshState()
    
    // 🔥 GIF图片加载器
    val context = LocalContext.current
    val gifImageLoader = remember {
        ImageLoader.Builder(context)
            .components {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .crossfade(true)
            .build()
    }
    
    // 过滤视频动态
    val filteredItems = remember(state.items, selectedTab) {
        if (selectedTab == 1) {
            state.items.filter { it.type == "DYNAMIC_TYPE_AV" }
        } else {
            state.items
        }
    }
    
    // 加载更多
    val shouldLoadMore by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItemIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisibleItemIndex >= totalItems - 3 && !state.isLoading && state.hasMore
        }
    }
    LaunchedEffect(shouldLoadMore) { if (shouldLoadMore) viewModel.loadMore() }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refresh() },
            state = pullRefreshState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(
                    top = statusBarHeight + 100.dp,  // 顶栏 + Tab 高度
                    bottom = 80.dp
                ),
                modifier = Modifier.fillMaxSize()
            ) {
                // 空状态
                if (filteredItems.isEmpty() && !state.isLoading && state.error == null) {
                    item {
                        EmptyState(
                            message = "暂无动态",
                            actionText = "登录后查看关注 UP主 的动态",
                            modifier = Modifier.height(300.dp)
                        )
                    }
                }
                
                // 动态卡片列表
                items(filteredItems, key = { it.id_str }) { item ->
                    DynamicCardV2(
                        item = item,
                        onVideoClick = onVideoClick,
                        onUserClick = onUserClick,
                        onLiveClick = onLiveClick,
                        gifImageLoader = gifImageLoader
                    )
                    
                    // 分隔线
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )
                }
                
                // 加载中
                if (state.isLoading && state.items.isNotEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            LoadingAnimation(size = 40.dp)
                        }
                    }
                }
                
                // 没有更多
                if (!state.hasMore && filteredItems.isNotEmpty()) {
                    item {
                        Text(
                            "没有更多了",
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f),
                            fontSize = 13.sp
                        )
                    }
                }
            }
            
            // 🔥 顶栏 + Tab
            DynamicTopBarWithTabs(
                selectedTab = selectedTab,
                tabs = tabs,
                onTabSelected = { selectedTab = it },
                modifier = Modifier.align(Alignment.TopCenter)
            )
            
            // 错误提示
            if (state.error != null && state.items.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(state.error ?: "", color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(16.dp))
                    if (state.error?.contains("未登录") == true) {
                        BiliGradientButton(text = "去登录", onClick = onLoginClick)
                    } else {
                        BiliGradientButton(text = "重试", onClick = { viewModel.refresh() })
                    }
                }
            }
        }
    }
}

/**
 * 🔥 带Tab的顶栏
 */
@Composable
fun DynamicTopBarWithTabs(
    selectedTab: Int,
    tabs: List<String>,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val statusBarHeight = WindowInsets.statusBars.getTop(density).let { with(density) { it.toDp() } }
    
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        Column {
            Spacer(modifier = Modifier.height(statusBarHeight))
            
            // 标题
            Text(
                "动态",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
            
            // Tab栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.Start
            ) {
                tabs.forEachIndexed { index, tab ->
                    val isSelected = selectedTab == index
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable { onTabSelected(index) }
                            .padding(end = 24.dp)
                    ) {
                        Text(
                            tab,
                            fontSize = 15.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) BiliPink else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .width(20.dp)
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(if (isSelected) BiliPink else Color.Transparent)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 🔥 动态卡片V2 - 官方风格
 */
@Composable
fun DynamicCardV2(
    item: DynamicItem,
    onVideoClick: (String) -> Unit,
    onUserClick: (Long) -> Unit,
    onLiveClick: (roomId: Long, title: String, uname: String) -> Unit = { _, _, _ -> },
    gifImageLoader: ImageLoader
) {
    val author = item.modules.module_author
    val content = item.modules.module_dynamic
    val stat = item.modules.module_stat
    val type = DynamicType.fromApiValue(item.type)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        // 🔥 用户头部（头像 + 名称 + 时间 + 更多）
        if (author != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 头像
                AsyncImage(
                    model = coil.request.ImageRequest.Builder(LocalContext.current)
                        .data(author.face.let { if (it.startsWith("http://")) it.replace("http://", "https://") else it })
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable { onUserClick(author.mid) },
                    contentScale = ContentScale.Crop
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        author.name,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = if (author.vip?.status == 1) BiliPink else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        author.pub_time,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f)
                    )
                }
                
                // 更多按钮
                IconButton(onClick = { /* TODO: 更多菜单 */ }) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "更多",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
        
        // 🔥 动态内容文字（支持@高亮）
        content?.desc?.let { desc ->
            if (desc.text.isNotEmpty()) {
                RichTextContent(
                    desc = desc,
                    onUserClick = onUserClick
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
        
        // 🔥 视频类型动态 - 大图预览
        content?.major?.archive?.let { archive ->
            VideoCardLarge(
                archive = archive,
                onClick = { onVideoClick(archive.bvid) }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
        
        // 🔥 图片类型动态（支持GIF + 点击预览）
        content?.major?.draw?.let { draw ->
            var selectedImageIndex by remember { mutableIntStateOf(-1) }
            
            DrawGridV2(
                items = draw.items,
                gifImageLoader = gifImageLoader,
                onImageClick = { index -> selectedImageIndex = index }
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            // 全屏图片预览
            if (selectedImageIndex >= 0) {
                ImagePreviewDialog(
                    images = draw.items.map { it.src },
                    initialIndex = selectedImageIndex,
                    onDismiss = { selectedImageIndex = -1 }
                )
            }
        }
        
        // 🔥 直播推荐动态
        content?.major?.live_rcmd?.let { liveRcmd ->
            LiveCard(
                liveRcmd = liveRcmd,
                onLiveClick = onLiveClick
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
        
        // 🔥 转发动态 - 嵌套显示原始内容
        if (type == DynamicType.FORWARD && item.orig != null) {
            ForwardedContent(
                orig = item.orig,
                onVideoClick = onVideoClick,
                onUserClick = onUserClick,
                gifImageLoader = gifImageLoader
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
        
        // 🔥 交互按钮（转发 评论 点赞）
        if (stat != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ActionButton(
                    icon = Icons.Default.Repeat,
                    count = stat.forward.count,
                    label = "转发"
                )
                ActionButton(
                    icon = Icons.Default.ChatBubbleOutline,
                    count = stat.comment.count,
                    label = "评论"
                )
                ActionButton(
                    icon = Icons.Default.FavoriteBorder,
                    count = stat.like.count,
                    label = "点赞",
                    activeColor = BiliPink
                )
            }
        }
    }
}

/**
 * 🔥 富文本内容（支持@提及高亮）
 */
@Composable
fun RichTextContent(
    desc: DynamicDesc,
    onUserClick: (Long) -> Unit
) {
    // 简化版：直接渲染文本，@提及用蓝色
    val text = buildAnnotatedString {
        val rawText = desc.text
        var lastEnd = 0
        
        // 查找 @xxx 模式
        val atPattern = Regex("@[^@\\s]+")
        atPattern.findAll(rawText).forEach { match ->
            // 普通文本
            if (match.range.first > lastEnd) {
                append(rawText.substring(lastEnd, match.range.first))
            }
            // @提及
            withStyle(SpanStyle(color = iOSBlue, fontWeight = FontWeight.Medium)) {
                append(match.value)
            }
            lastEnd = match.range.last + 1
        }
        // 剩余文本
        if (lastEnd < rawText.length) {
            append(rawText.substring(lastEnd))
        }
    }
    
    Text(
        text = text,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        color = MaterialTheme.colorScheme.onSurface
    )
}

/**
 * 🔥 大尺寸视频卡片
 */
@Composable
fun VideoCardLarge(
    archive: ArchiveMajor,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val coverUrl = remember(archive.cover) {
        val raw = archive.cover.trim()
        when {
            raw.startsWith("https://") -> raw
            raw.startsWith("http://") -> raw.replace("http://", "https://")
            raw.startsWith("//") -> "https:$raw"
            raw.isNotEmpty() -> "https://$raw"
            else -> ""
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
    ) {
        // 视频封面 - 16:9
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            if (coverUrl.isNotEmpty()) {
                AsyncImage(
                    model = coil.request.ImageRequest.Builder(context)
                        .data(coverUrl)
                        .addHeader("Referer", "https://www.bilibili.com/")
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            
            // 时长标签
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .background(Color.Black.copy(0.7f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(archive.duration_text, fontSize = 12.sp, color = Color.White)
            }
            
            // 播放量和弹幕
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
                    .background(Color.Black.copy(0.5f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(14.dp), tint = Color.White)
                Spacer(modifier = Modifier.width(2.dp))
                Text(archive.stat.play, fontSize = 11.sp, color = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("弹幕 ${archive.stat.danmaku}", fontSize = 11.sp, color = Color.White)
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 视频标题
        Text(
            archive.title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * 🔥 图片九宫格V2（支持GIF + 点击预览）
 */
@Composable
fun DrawGridV2(
    items: List<DrawItem>,
    gifImageLoader: ImageLoader,
    onImageClick: (Int) -> Unit = {}  // 🔥 图片点击回调
) {
    if (items.isEmpty()) return
    
    val context = LocalContext.current
    val displayItems = items.take(9)
    val columns = when {
        displayItems.size == 1 -> 1
        displayItems.size <= 4 -> 2
        else -> 3
    }
    
    val singleImageRatio = if (displayItems.size == 1 && displayItems[0].width > 0 && displayItems[0].height > 0) {
        displayItems[0].width.toFloat() / displayItems[0].height.toFloat()
    } else {
        1f
    }
    
    var globalIndex = 0
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        displayItems.chunked(columns).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                row.forEach { item ->
                    val currentIndex = globalIndex++
                    val imageUrl = remember(item.src) {
                        val rawSrc = item.src.trim()
                        when {
                            rawSrc.startsWith("https://") -> rawSrc
                            rawSrc.startsWith("http://") -> rawSrc.replace("http://", "https://")
                            rawSrc.startsWith("//") -> "https:$rawSrc"
                            rawSrc.isNotEmpty() -> "https://$rawSrc"
                            else -> ""
                        }
                    }
                    
                    val aspectRatio = if (displayItems.size == 1) singleImageRatio else 1f
                    val isGif = imageUrl.endsWith(".gif", ignoreCase = true)
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(aspectRatio.coerceIn(0.5f, 2f))
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { onImageClick(currentIndex) },  // 🔥 点击预览
                        contentAlignment = Alignment.Center
                    ) {
                        if (imageUrl.isNotEmpty()) {
                            AsyncImage(
                                model = coil.request.ImageRequest.Builder(context)
                                    .data(imageUrl)
                                    .addHeader("Referer", "https://www.bilibili.com/")
                                    .crossfade(!isGif)
                                    .build(),
                                imageLoader = if (isGif) gifImageLoader else ImageLoader(context),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                Icons.Default.BrokenImage,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = Color.Gray.copy(0.5f)
                            )
                        }
                    }
                }
                repeat(columns - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

/**
 * 🔥 转发的原始内容
 */
@Composable
fun ForwardedContent(
    orig: DynamicItem,
    onVideoClick: (String) -> Unit,
    onUserClick: (Long) -> Unit,
    gifImageLoader: ImageLoader
) {
    val author = orig.modules.module_author
    val content = orig.modules.module_dynamic
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(12.dp)
    ) {
        // 原作者
        if (author != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "@${author.name}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = iOSBlue,
                    modifier = Modifier.clickable { onUserClick(author.mid) }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    author.pub_time,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        // 原文字内容
        content?.desc?.text?.takeIf { it.isNotEmpty() }?.let { text ->
            Text(
                text,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(0.8f),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        // 原视频
        content?.major?.archive?.let { archive ->
            VideoCardSmall(
                archive = archive,
                onClick = { onVideoClick(archive.bvid) }
            )
        }
        
        // 原图片
        content?.major?.draw?.let { draw ->
            DrawGridV2(items = draw.items.take(4), gifImageLoader = gifImageLoader)
        }
    }
}

/**
 * 🔥 小尺寸视频卡片（用于转发）
 */
@Composable
fun VideoCardSmall(
    archive: ArchiveMajor,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val coverUrl = remember(archive.cover) {
        val raw = archive.cover.trim()
        when {
            raw.startsWith("https://") -> raw
            raw.startsWith("http://") -> raw.replace("http://", "https://")
            raw.startsWith("//") -> "https:$raw"
            raw.isNotEmpty() -> "https://$raw"
            else -> ""
        }
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp)
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 封面
        Box(
            modifier = Modifier
                .width(110.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(6.dp))
        ) {
            if (coverUrl.isNotEmpty()) {
                AsyncImage(
                    model = coil.request.ImageRequest.Builder(context)
                        .data(coverUrl)
                        .addHeader("Referer", "https://www.bilibili.com/")
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .background(Color.Black.copy(0.7f), RoundedCornerShape(3.dp))
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            ) {
                Text(archive.duration_text, fontSize = 10.sp, color = Color.White)
            }
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // 标题
        Text(
            archive.title,
            fontSize = 13.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * 🔥 操作按钮
 */
@Composable
fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    count: Int,
    label: String,
    activeColor: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f)
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable { /* TODO */ }
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Icon(
            icon,
            contentDescription = label,
            modifier = Modifier.size(18.dp),
            tint = activeColor
        )
        if (count > 0) {
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                if (count >= 10000) "${count / 10000}万" else count.toString(),
                fontSize = 12.sp,
                color = activeColor
            )
        }
    }
}

/**
 * 🔥 图片预览对话框 - 支持左右切换和下载保存
 */
@Composable
fun ImagePreviewDialog(
    images: List<String>,
    initialIndex: Int,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var currentIndex by remember { mutableIntStateOf(initialIndex) }
    val scope = rememberCoroutineScope()
    var isSaving by remember { mutableStateOf(false) }
    
    // 🔐 存储权限状态（Android 9 及以下需要）
    var pendingSaveUrl by remember { mutableStateOf<String?>(null) }
    val storagePermission = com.android.purebilibili.core.util.rememberStoragePermissionState { granted ->
        if (granted && pendingSaveUrl != null) {
            // 权限授予后执行保存
            isSaving = true
            scope.launch {
                val success = saveImageToGallery(context, pendingSaveUrl!!)
                isSaving = false
                pendingSaveUrl = null
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        if (success) "图片已保存到相册" else "保存失败，请重试",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
    
    // 规范化图片 URL
    val imageUrl = remember(images.getOrNull(currentIndex)) {
        val rawSrc = (images.getOrNull(currentIndex) ?: "").trim()
        when {
            rawSrc.startsWith("https://") -> rawSrc
            rawSrc.startsWith("http://") -> rawSrc.replace("http://", "https://")
            rawSrc.startsWith("//") -> "https:$rawSrc"
            rawSrc.isNotEmpty() -> "https://$rawSrc"
            else -> ""
        }
    }
    
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable { onDismiss() }
        ) {
            // 当前图片
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .addHeader("Referer", "https://www.bilibili.com/")
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(onClick = {}),  // 阻止点击穿透
                contentScale = ContentScale.Fit
            )
            
            // 左右切换
            if (images.size > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center)
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // 上一张
                    if (currentIndex > 0) {
                        FilledIconButton(
                            onClick = { currentIndex-- },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = Color.White.copy(0.3f)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = "上一张",
                                tint = Color.White
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.size(48.dp))
                    }
                    
                    // 下一张
                    if (currentIndex < images.size - 1) {
                        FilledIconButton(
                            onClick = { currentIndex++ },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = Color.White.copy(0.3f)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "下一张",
                                tint = Color.White
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.size(48.dp))
                    }
                }
                
                // 页码指示器
                Text(
                    "${currentIndex + 1} / ${images.size}",
                    color = Color.White,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 32.dp)
                        .background(Color.Black.copy(0.5f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
            
            // 顶部按钮栏（关闭 + 下载）
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 关闭按钮
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "关闭",
                        tint = Color.White
                    )
                }
                
                // 🔥 下载按钮
                IconButton(
                    onClick = {
                        if (!isSaving && imageUrl.isNotEmpty()) {
                            // 🔐 检查权限（Android 10+ 自动授权）
                            if (storagePermission.isGranted) {
                                isSaving = true
                                scope.launch {
                                    val success = saveImageToGallery(context, imageUrl)
                                    isSaving = false
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(
                                            context,
                                            if (success) "图片已保存到相册" else "保存失败，请重试",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            } else {
                                // 保存待执行的 URL，请求权限
                                pendingSaveUrl = imageUrl
                                storagePermission.request()
                            }
                        }
                    },
                    enabled = !isSaving
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "保存图片",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

/**
 * 🔥 保存图片到相册
 */
private suspend fun saveImageToGallery(context: android.content.Context, imageUrl: String): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            // 使用 Coil 下载图片
            val imageLoader = ImageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(imageUrl)
                .addHeader("Referer", "https://www.bilibili.com/")
                .build()
            
            val result = imageLoader.execute(request)
            if (result !is SuccessResult) {
                Log.e("DynamicScreen", "Failed to download image: $imageUrl")
                return@withContext false
            }
            
            val bitmap = (result.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
            if (bitmap == null) {
                Log.e("DynamicScreen", "Failed to convert drawable to bitmap")
                return@withContext false
            }
            
            // 生成文件名
            val fileName = "BiliPai_${System.currentTimeMillis()}.jpg"
            
            // 使用 MediaStore 保存图片
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/BiliPai")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }
            
            val uri = context.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            ) ?: return@withContext false
            
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 95, outputStream)
            }
            
            // 标记保存完成
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                context.contentResolver.update(uri, contentValues, null, null)
            }
            
            Log.d("DynamicScreen", "Image saved successfully: $fileName")
            true
        } catch (e: Exception) {
            Log.e("DynamicScreen", "Error saving image", e)
            false
        }
    }
}

/**
 * 🔥 直播卡片
 */
@Composable
fun LiveCard(
    liveRcmd: LiveRcmdMajor,
    onLiveClick: (roomId: Long, title: String, uname: String) -> Unit = { _, _, _ -> }
) {
    // 解析直播内容 JSON
    val liveInfo = remember(liveRcmd.content) {
        try {
            val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
            json.decodeFromString<LiveContentInfo>(liveRcmd.content)
        } catch (e: Exception) {
            // 🔥 添加日志帮助调试
            Log.e("DynamicScreen", "Failed to parse live_rcmd content: ${e.message}")
            Log.d("DynamicScreen", "Raw content: ${liveRcmd.content.take(500)}")
            null
        }
    }
    
    val context = LocalContext.current
    
    if (liveInfo != null) {
        val roomId = liveInfo.live_play_info?.room_id ?: 0L
        val title = liveInfo.live_play_info?.title ?: ""
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onLiveClick(roomId, title, "") },  // 🔥 点击跳转直播
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 直播封面
                Box(
                    modifier = Modifier
                        .width(120.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(6.dp))
                ) {
                    liveInfo.live_play_info?.cover?.let { coverUrl ->
                        val url = if (coverUrl.startsWith("http://")) coverUrl.replace("http://", "https://") else coverUrl
                        AsyncImage(
                            model = coil.request.ImageRequest.Builder(context)
                                .data(url)
                                .addHeader("Referer", "https://www.bilibili.com/")
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    
                    // 直播标识
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(4.dp)
                            .background(BiliPink, RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text("直播中", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
                
                Spacer(modifier = Modifier.width(10.dp))
                
                // 直播信息
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        liveInfo.live_play_info?.title ?: "直播中",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.PlayArrow,
                            null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f)
                        )
                        Text(
                            "${liveInfo.live_play_info?.online ?: 0} 人观看",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f)
                        )
                    }
                }
            }
        }
    } else {
        // 无法解析时显示占位
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(BiliPink, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🔴", fontSize = 18.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "直播中",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

/**
 * 🔥 直播内容信息（用于解析 JSON）
 * 注意：B站动态API的live_rcmd.content是嵌套的JSON字符串
 */
@kotlinx.serialization.Serializable
data class LiveContentInfo(
    val live_play_info: LivePlayInfo? = null,
    val type: Int = 0  // 直播类型
)

@kotlinx.serialization.Serializable
data class LivePlayInfo(
    val title: String = "",
    val cover: String = "",
    val online: Int = 0,
    val room_id: Long = 0,
    // 🔥 添加更多可选字段提高兼容性
    val area_name: String = "",  // 分区名称
    val parent_area_name: String = "",  // 父分区名称
    val uid: Long = 0,  // UP主ID
    val link: String = "",  // 直播间链接
    val watched_show: WatchedShow? = null  // 观看人数展示信息
)

@kotlinx.serialization.Serializable
data class WatchedShow(
    val num: Int = 0,
    val text_small: String = "",
    val text_large: String = ""
)
