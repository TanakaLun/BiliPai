package com.android.purebilibili.data.repository

import com.android.purebilibili.core.network.NetworkModule
import com.android.purebilibili.core.network.WbiUtils
import com.android.purebilibili.data.model.response.HotItem
import com.android.purebilibili.data.model.response.VideoItem
import com.android.purebilibili.data.model.response.SearchUpItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.JsonElement

object SearchRepository {
    private val api = NetworkModule.searchApi
    private val navApi = NetworkModule.api
    
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    // 🔥 视频搜索
    suspend fun search(keyword: String): Result<List<VideoItem>> = withContext(Dispatchers.IO) {
        try {
            val navResp = navApi.getNavInfo()
            val wbiImg = navResp.data?.wbi_img
            val imgKey = wbiImg?.img_url?.substringAfterLast("/")?.substringBefore(".") ?: ""
            val subKey = wbiImg?.sub_url?.substringAfterLast("/")?.substringBefore(".") ?: ""

            val params = mapOf(
                "keyword" to keyword,
                "search_type" to "video"
            )
            val signedParams = if (imgKey.isNotEmpty()) WbiUtils.sign(params, imgKey, subKey) else params

            val response = api.search(signedParams)

            val videoList = response.data?.result
                ?.find { it.result_type == "video" }
                ?.data
                ?.map { it.toVideoItem() }
                ?: emptyList()

            Result.success(videoList)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    // 🔥 UP主 搜索
    suspend fun searchUp(keyword: String): Result<List<SearchUpItem>> = withContext(Dispatchers.IO) {
        try {
            val navResp = navApi.getNavInfo()
            val wbiImg = navResp.data?.wbi_img
            val imgKey = wbiImg?.img_url?.substringAfterLast("/")?.substringBefore(".") ?: ""
            val subKey = wbiImg?.sub_url?.substringAfterLast("/")?.substringBefore(".") ?: ""

            val params = mapOf(
                "keyword" to keyword,
                "search_type" to "bili_user" // UP主 搜索类型
            )
            val signedParams = if (imgKey.isNotEmpty()) WbiUtils.sign(params, imgKey, subKey) else params

            val response = api.search(signedParams)
            
            // 提取 bili_user 分类的数据
            val upList = response.data?.result
                ?.find { it.result_type == "bili_user" }
                ?.let { category ->
                    // 使用 Json 解析 data 字段 (因为 data 是 List<SearchVideoItem>，需要重新解析为 SearchUpItem)
                    // 由于现有模型的限制，这里使用反射/手动解析
                    @Suppress("UNCHECKED_CAST")
                    try {
                        // 简化处理：从原始响应中提取用户数据
                        category.data?.mapNotNull { videoItem ->
                            // SearchVideoItem 的字段不完全匹配 UP主，需要一个更通用的方式
                            // 暂时使用已有字段进行映射
                            SearchUpItem(
                                mid = videoItem.id,
                                uname = videoItem.title.replace(Regex("<.*?>"), ""),
                                upic = if (videoItem.pic.startsWith("//")) "https:${videoItem.pic}" else videoItem.pic,
                                fans = 0, // API 需要调整才能获取
                                videos = 0
                            )
                        } ?: emptyList()
                    } catch (e: Exception) {
                        emptyList()
                    }
                }
                ?: emptyList()

            Result.success(upList)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    // 🔥 热搜
    suspend fun getHotSearch(): Result<List<HotItem>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getHotSearch()
            val list = response.data?.trending?.list ?: emptyList()
            Result.success(list)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}