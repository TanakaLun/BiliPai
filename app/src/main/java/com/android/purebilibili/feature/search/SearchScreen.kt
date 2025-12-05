// 文件路径: feature/search/SearchScreen.kt
package com.android.purebilibili.feature.search

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.purebilibili.core.database.entity.SearchHistory
import com.android.purebilibili.core.theme.BiliPink
import com.android.purebilibili.feature.home.components.VideoGridItem

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel = viewModel(),
    userFace: String = "",
    onBack: () -> Unit,
    onVideoClick: (String, Long) -> Unit,
    onAvatarClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current

    // 1. 滚动状态监听 (用于列表)
    val historyListState = rememberLazyListState()
    val resultGridState = rememberLazyGridState()

    // 2. 顶部避让高度计算
    val density = LocalDensity.current
    val statusBarHeight = WindowInsets.statusBars.getTop(density).let { with(density) { it.toDp() } }
    val topBarHeight = 64.dp // 搜索栏高度
    val contentTopPadding = statusBarHeight + topBarHeight

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background,
        // 🔥 移除 bottomBar，搜索栏现在位于顶部 Box 中
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // --- 列表内容层 ---
            if (state.showResults) {
                if (state.isSearching) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = BiliPink)
                } else if (state.error != null) {
                    Text(
                        text = state.error ?: "未知错误",
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        state = resultGridState,
                        // 🔥 contentPadding 顶部避让搜索栏
                        contentPadding = PaddingValues(top = contentTopPadding + 8.dp, bottom = 16.dp, start = 8.dp, end = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        itemsIndexed(state.searchResults) { index, video ->
                            VideoGridItem(video, index) { bvid -> onVideoClick(bvid, 0) }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = historyListState,
                    // 🔥 contentPadding 顶部避让搜索栏
                    contentPadding = PaddingValues(top = contentTopPadding + 16.dp, bottom = 16.dp, start = 16.dp, end = 16.dp)
                ) {
                    if (state.hotList.isNotEmpty()) {
                        item {
                            Text("大家都在搜", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 12.dp), color = MaterialTheme.colorScheme.onSurface)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                state.hotList.forEach { hotItem ->
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(4.dp),
                                        modifier = Modifier.clickable { viewModel.search(hotItem.keyword); keyboardController?.hide() }
                                    ) {
                                        Text(hotItem.show_name, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                    if (state.historyList.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "历史记录",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                TextButton(onClick = { viewModel.clearHistory() }) {
                                    Text("清空", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        
                        // 🔥 气泡化历史记录
                        item {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                state.historyList.forEach { history ->
                                    HistoryChip(
                                        keyword = history.keyword,
                                        onClick = { viewModel.search(history.keyword); keyboardController?.hide() },
                                        onDelete = { viewModel.deleteHistory(history) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // --- 🔥 顶部搜索栏 (常驻顶部) ---
            SearchTopBar(
                query = state.query,
                onBack = onBack,
                onQueryChange = { viewModel.onQueryChange(it) },
                onSearch = {
                    viewModel.search(it)
                    keyboardController?.hide()
                },
                onClearQuery = { viewModel.onQueryChange("") },
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}

// 🔥 新设计的顶部搜索栏 (含 Focus 高亮动画)
@Composable
fun SearchTopBar(
    query: String,
    onBack: () -> Unit,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    onClearQuery: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 🔥 Focus 状态追踪
    var isFocused by remember { mutableStateOf(false) }
    
    // 🔥 边框宽度动画
    val borderWidth by animateDpAsState(
        targetValue = if (isFocused) 2.dp else 0.dp,
        animationSpec = tween(durationMillis = 200),
        label = "borderWidth"
    )
    
    // 🔥 搜索图标颜色动画
    val searchIconColor by animateColorAsState(
        targetValue = if (isFocused) BiliPink else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        animationSpec = tween(durationMillis = 200),
        label = "iconColor"
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 3.dp
    ) {
        Column {
            Spacer(modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // 🔥 搜索输入框 (带 Focus 边框动画)
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .clip(RoundedCornerShape(50))
                        .border(
                            width = borderWidth,
                            color = BiliPink,
                            shape = RoundedCornerShape(50)
                        )
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Search,
                        null,
                        tint = searchIconColor,
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    BasicTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        modifier = Modifier
                            .weight(1f)
                            .onFocusChanged { isFocused = it.isFocused },
                        textStyle = TextStyle(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 15.sp
                        ),
                        singleLine = true,
                        cursorBrush = SolidColor(BiliPink),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { onSearch(query) }),
                        decorationBox = { inner ->
                            Box(contentAlignment = Alignment.CenterStart) {
                                if (query.isEmpty()) {
                                    Text(
                                        "搜索视频、UP主...",
                                        style = TextStyle(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f),
                                            fontSize = 15.sp
                                        )
                                    )
                                }
                                inner()
                            }
                        }
                    )

                    if (query.isNotEmpty()) {
                        IconButton(
                            onClick = onClearQuery,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.Clear,
                                null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                TextButton(
                    onClick = { onSearch(query) },
                    enabled = query.isNotEmpty()
                ) {
                    Text(
                        "搜索",
                        color = if (query.isNotEmpty()) BiliPink else MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f),
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

// 🔥 气泡化历史记录组件
@Composable
fun HistoryChip(
    keyword: String,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .height(36.dp)
                .padding(start = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.History,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = keyword,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 13.sp,
                maxLines = 1
            )
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

// 保留旧版 HistoryItem 用于兼容 (可选保留)
@Composable
fun HistoryItem(
    history: SearchHistory,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f), modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = history.keyword, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, modifier = Modifier.weight(1f))
        IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f), modifier = Modifier.size(16.dp))
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 0.5.dp)
}