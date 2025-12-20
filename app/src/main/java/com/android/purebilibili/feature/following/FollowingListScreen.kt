package com.android.purebilibili.feature.following

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.android.purebilibili.core.network.NetworkModule
import com.android.purebilibili.core.util.FormatUtils
import com.android.purebilibili.data.model.response.FollowingUser
import io.github.alexzhirkevich.cupertino.CupertinoActivityIndicator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// UI 状态
sealed class FollowingListUiState {
    object Loading : FollowingListUiState()
    data class Success(
        val users: List<FollowingUser>,
        val total: Int,
        val isLoadingMore: Boolean = false,
        val hasMore: Boolean = true
    ) : FollowingListUiState()
    data class Error(val message: String) : FollowingListUiState()
}

class FollowingListViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<FollowingListUiState>(FollowingListUiState.Loading)
    val uiState = _uiState.asStateFlow()
    
    private var currentPage = 1
    private var currentMid: Long = 0
    
    fun loadFollowingList(mid: Long) {
        if (mid <= 0) return
        currentMid = mid
        currentPage = 1
        
        viewModelScope.launch {
            _uiState.value = FollowingListUiState.Loading
            
            try {
                val response = NetworkModule.api.getFollowings(mid, pn = 1, ps = 50)
                if (response.code == 0 && response.data != null) {
                    val users = response.data.list ?: emptyList()
                    _uiState.value = FollowingListUiState.Success(
                        users = users,
                        total = response.data.total,
                        hasMore = users.size >= 50
                    )
                } else {
                    _uiState.value = FollowingListUiState.Error("加载失败: ${response.message}")
                }
            } catch (e: Exception) {
                _uiState.value = FollowingListUiState.Error(e.message ?: "网络错误")
            }
        }
    }
    
    fun loadMore() {
        val current = _uiState.value as? FollowingListUiState.Success ?: return
        if (current.isLoadingMore || !current.hasMore) return
        
        viewModelScope.launch {
            _uiState.value = current.copy(isLoadingMore = true)
            
            try {
                currentPage++
                val response = NetworkModule.api.getFollowings(currentMid, pn = currentPage, ps = 50)
                if (response.code == 0 && response.data != null) {
                    val newUsers = response.data.list ?: emptyList()
                    _uiState.value = current.copy(
                        users = current.users + newUsers,
                        isLoadingMore = false,
                        hasMore = newUsers.size >= 50
                    )
                } else {
                    _uiState.value = current.copy(isLoadingMore = false)
                    currentPage--
                }
            } catch (e: Exception) {
                _uiState.value = current.copy(isLoadingMore = false)
                currentPage--
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowingListScreen(
    mid: Long,
    onBack: () -> Unit,
    onUserClick: (Long) -> Unit,  // 点击跳转到 UP 主空间
    viewModel: FollowingListViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(mid) {
        viewModel.loadFollowingList(mid)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("我的关注") },
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (val state = uiState) {
                is FollowingListUiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CupertinoActivityIndicator()
                    }
                }
                
                is FollowingListUiState.Error -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("😢", fontSize = 48.sp)
                            Spacer(Modifier.height(16.dp))
                            Text(state.message, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = { viewModel.loadFollowingList(mid) }) {
                                Text("重试")
                            }
                        }
                    }
                }
                
                is FollowingListUiState.Success -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        // 统计信息
                        item {
                            Text(
                                text = "共 ${state.total} 个关注",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                            )
                        }
                        
                        items(state.users, key = { it.mid }) { user ->
                            FollowingUserItem(
                                user = user,
                                onClick = { onUserClick(user.mid) }
                            )
                        }
                        
                        // 加载更多
                        if (state.isLoadingMore) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CupertinoActivityIndicator()
                                }
                            }
                        } else if (state.hasMore) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.loadMore() }
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "加载更多",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FollowingUserItem(
    user: FollowingUser,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 头像
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(FormatUtils.fixImageUrl(user.face))
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
        
        Spacer(Modifier.width(12.dp))
        
        // 用户信息
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user.uname,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (user.sign.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = user.sign,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
