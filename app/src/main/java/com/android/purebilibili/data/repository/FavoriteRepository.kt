package com.android.purebilibili.data.repository

import com.android.purebilibili.core.network.NetworkModule
import com.android.purebilibili.data.model.response.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object FavoriteRepository {
    private val api = NetworkModule.api

    suspend fun getFavFolders(mid: Long): Result<List<FavFolder>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.getFavFolders(mid)
                if (response.code == 0) {
                    Result.success(response.data?.list ?: emptyList())
                } else {
                    Result.failure(Exception("获取收藏夹失败: ${response.code}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun getFavoriteList(mediaId: Long, pn: Int): Result<List<FavoriteData>> {
        return withContext(Dispatchers.IO) {
            try {
                // pn defaults to 1 if not passed, but here we pass it
                val response = api.getFavoriteList(mediaId = mediaId, pn = pn)
                if (response.code == 0) {
                    // 收藏夹内容通常在 medias 字段
                    Result.success(response.data?.medias ?: emptyList())
                } else {
                    Result.failure(Exception(response.message))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
