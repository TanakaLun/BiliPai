package com.android.purebilibili.data.repository

import com.android.purebilibili.core.network.NetworkModule
import com.android.purebilibili.data.model.response.HistoryData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object HistoryRepository {
    private val api = NetworkModule.api

    suspend fun getHistoryList(ps: Int = 20): Result<List<HistoryData>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.getHistoryList(ps)
                if (response.code == 0) {
                    // ListData 中 list 字段存储历史记录
                    Result.success(response.data?.list ?: emptyList())
                } else {
                    Result.failure(Exception(response.message))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
