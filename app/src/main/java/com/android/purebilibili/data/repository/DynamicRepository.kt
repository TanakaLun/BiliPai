// 文件路径: data/repository/DynamicRepository.kt
package com.android.purebilibili.data.repository

import com.android.purebilibili.core.network.NetworkModule
import com.android.purebilibili.data.model.response.DynamicFeedResponse
import com.android.purebilibili.data.model.response.DynamicItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 🔥 动态数据仓库
 * 
 * 负责从 B站 API 获取动态 Feed 数据
 */
object DynamicRepository {
    
    private var lastOffset: String = ""
    private var hasMore: Boolean = true
    
    /**
     * 获取动态列表
     * @param refresh 是否刷新 (重置分页)
     */
    suspend fun getDynamicFeed(refresh: Boolean = false): Result<List<DynamicItem>> = withContext(Dispatchers.IO) {
        try {
            if (refresh) {
                lastOffset = ""
                hasMore = true
            }
            
            if (!hasMore && !refresh) {
                return@withContext Result.success(emptyList())
            }
            
            val response = NetworkModule.dynamicApi.getDynamicFeed(
                type = "all",
                offset = lastOffset
            )
            
            if (response.code != 0) {
                return@withContext Result.failure(Exception("API error: ${response.message}"))
            }
            
            val data = response.data ?: return@withContext Result.success(emptyList())
            
            // 更新分页状态
            lastOffset = data.offset
            hasMore = data.has_more
            
            // 过滤不可见的动态
            val visibleItems = data.items.filter { it.visible }
            
            Result.success(visibleItems)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    /**
     * 是否还有更多数据
     */
    fun hasMoreData(): Boolean = hasMore
    
    /**
     * 重置分页状态
     */
    fun resetPagination() {
        lastOffset = ""
        hasMore = true
    }
}
