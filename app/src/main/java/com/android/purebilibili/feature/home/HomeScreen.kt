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

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(),
    onVideoClick: (String, Long, String) -> Unit,
    onAvatarClick: () -> Unit,
    onProfileClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onSearchClick: () -> Unit
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

    val prefs = remember { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }
    var showWelcomeDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (prefs.getBoolean("is_first_run", true)) showWelcomeDialog = true
    }

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
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = BiliPink)
                }
            } else if (state.error != null && state.videos.isEmpty()) {
                ErrorState(state.error!!) { viewModel.refresh() }
            } else {
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = contentTopPadding,
                        bottom = navBarHeight + 20.dp
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

            // 3. 顶层：刷新指示器 (🔥 修复：不加 padding，让它从屏幕最顶部滑下来，覆盖在顶栏之上)
            PullToRefreshContainer(
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter),
                containerColor = MaterialTheme.colorScheme.surface, // 白色背景
                contentColor = BiliPink
            )
        }
    }

    if (showWelcomeDialog) {
        WelcomeDialog(GITHUB_URL) {
            prefs.edit().putBoolean("is_first_run", false).apply()
            showWelcomeDialog = false
        }
    }
}