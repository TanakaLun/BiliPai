// 文件路径: feature/home/HomeScreen.kt
package com.android.purebilibili.feature.home

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.purebilibili.core.theme.BiliPink
import com.android.purebilibili.feature.settings.GITHUB_URL
// 🔥 从 components 包导入拆分后的组件
import com.android.purebilibili.feature.home.components.BottomNavItem
import com.android.purebilibili.feature.home.components.ElegantVideoCard
import com.android.purebilibili.feature.home.components.FluidHomeTopBar
import com.android.purebilibili.feature.home.components.FrostedBottomBar
import com.android.purebilibili.core.ui.LoadingAnimation
import com.android.purebilibili.core.ui.VideoCardSkeleton
import com.android.purebilibili.core.ui.ErrorState as ModernErrorState

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(),
    onVideoClick: (String, Long, String) -> Unit,
    onAvatarClick: () -> Unit,
    onProfileClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onSearchClick: () -> Unit,
    // 🔥 新增：动态页面回调
    onDynamicClick: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val pullRefreshState = rememberPullToRefreshState()
    val context = LocalContext.current
    val gridState = rememberLazyGridState()

    val scrollOffset by remember {
        derivedStateOf {
            if (gridState.firstVisibleItemIndex > 0) 500f
            else gridState.firstVisibleItemScrollOffset.toFloat()
        }
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
        }
    }

    val density = LocalDensity.current
    val statusBarHeight = WindowInsets.statusBars.getTop(density).let { with(density) { it.toDp() } }
    val navBarHeight = WindowInsets.navigationBars.getBottom(density).let { with(density) { it.toDp() } }

    // 内容的 Padding：状态栏 + TopBar(64) + 间距
    val topBarHeight = 64.dp
    val contentTopPadding = statusBarHeight + topBarHeight + 16.dp
    
    // 🔥 底部导航栏高度
    val bottomBarHeight = 56.dp + navBarHeight

    val prefs = remember { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }
    
    // 🔥 当前选中的导航项
    var currentNavItem by remember { mutableStateOf(BottomNavItem.HOME) }

    val shouldLoadMore by remember {
        derivedStateOf {
            val layoutInfo = gridState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItemIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisibleItemIndex >= totalItems - 4 && !state.isLoading && !isRefreshing
        }
    }
    LaunchedEffect(shouldLoadMore) { if (shouldLoadMore) viewModel.loadMore() }

    if (pullRefreshState.isRefreshing) {
        LaunchedEffect(true) { viewModel.refresh() }
    }
    LaunchedEffect(isRefreshing) {
        if (isRefreshing) pullRefreshState.startRefresh() else pullRefreshState.endRefresh()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { _ ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(pullRefreshState.nestedScrollConnection)
        ) {
            // 1. 底层：视频列表
            if (state.isLoading && state.videos.isEmpty()) {
                // 🔥 骨架屏加载动画
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = contentTopPadding,
                        bottom = bottomBarHeight + 20.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(6) { VideoCardSkeleton() }
                }
            } else if (state.error != null && state.videos.isEmpty()) {
                // 🔥 使用现代化错误组件
                ModernErrorState(
                    message = state.error ?: "加载失败",
                    onRetry = { viewModel.refresh() }
                )
            } else {
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = contentTopPadding,
                        bottom = bottomBarHeight + 20.dp  // 🔥 底部为导航栏高度
                    ),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(
                        items = state.videos,
                        key = { _, video -> video.bvid }
                    ) { index, video ->
                        ElegantVideoCard(video, index) { bvid, cid ->
                            onVideoClick(bvid, cid, video.pic)
                        }
                    }
                    if (state.videos.isNotEmpty() && state.isLoading) {
                        item(span = { GridItemSpan(2) }) {
                            Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.primary, strokeWidth = 2.dp)
                            }
                        }
                    }
                }
            }

            // 2. 中层：顶栏
            FluidHomeTopBar(
                user = state.user,
                scrollOffset = scrollOffset,
                onAvatarClick = { if (state.user.isLogin) onProfileClick() else onAvatarClick() },
                onSettingsClick = onSettingsClick,
                onSearchClick = onSearchClick
            )

            // 3. 顶层：刷新指示器
            PullToRefreshContainer(
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter),
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = BiliPink
            )
            
            // 4. 🔥 底部导航栏 (视频封面动态取色)
            // 获取当前可见的第一个视频封面
            val firstVisibleIndex by remember { derivedStateOf { gridState.firstVisibleItemIndex } }
            val videos = state.videos
            
            // 🔥 根据 firstVisibleIndex 和 videos 计算封面 URL
            val visibleCoverUrl = remember(firstVisibleIndex, videos.size) {
                val url = videos.getOrNull(firstVisibleIndex)?.pic
                android.util.Log.d("BottomBarColor", "📸 封面URL更新: index=$firstVisibleIndex, url=${url?.take(50)}...")
                url
            }
            
            FrostedBottomBar(
                currentItem = currentNavItem,
                onItemClick = { item ->
                    currentNavItem = item
                    when (item) {
                        BottomNavItem.HOME -> { /* 已在首页 */ }
                        BottomNavItem.DYNAMIC -> onDynamicClick()
                        BottomNavItem.DISCOVER -> { /* TODO: 跳转发现页 */ }
                        BottomNavItem.PROFILE -> onProfileClick()
                    }
                },
                modifier = Modifier.align(Alignment.BottomCenter),
                visibleCoverUrl = visibleCoverUrl
            )
        }
    }
}