// 文件路径: feature/dynamic/DynamicViewModel.kt
package com.android.purebilibili.feature.dynamic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.purebilibili.data.model.response.DynamicItem
import com.android.purebilibili.data.repository.DynamicRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 🔥 动态页面 ViewModel
 */
class DynamicViewModel : ViewModel() {
    
    private val _uiState = MutableStateFlow(DynamicUiState())
    val uiState: StateFlow<DynamicUiState> = _uiState.asStateFlow()
    
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()
    
    init {
        loadDynamicFeed(refresh = true)
    }
    
    /**
     * 加载动态列表
     */
    fun loadDynamicFeed(refresh: Boolean = false) {
        if (_uiState.value.isLoading && !refresh) return
        
        viewModelScope.launch {
            if (refresh) {
                _isRefreshing.value = true
            } else {
                _uiState.value = _uiState.value.copy(isLoading = true)
            }
            
            val result = DynamicRepository.getDynamicFeed(refresh)
            
            result.fold(
                onSuccess = { items ->
                    val currentItems = if (refresh) emptyList() else _uiState.value.items
                    _uiState.value = _uiState.value.copy(
                        items = currentItems + items,
                        isLoading = false,
                        error = null,
                        hasMore = DynamicRepository.hasMoreData()
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "加载失败"
                    )
                }
            )
            
            _isRefreshing.value = false
        }
    }
    
    /**
     * 刷新动态列表
     */
    fun refresh() {
        loadDynamicFeed(refresh = true)
    }
    
    /**
     * 加载更多
     */
    fun loadMore() {
        if (!_uiState.value.hasMore || _uiState.value.isLoading) return
        loadDynamicFeed(refresh = false)
    }
}

/**
 * 动态页面 UI 状态
 */
data class DynamicUiState(
    val items: List<DynamicItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val hasMore: Boolean = true
)
