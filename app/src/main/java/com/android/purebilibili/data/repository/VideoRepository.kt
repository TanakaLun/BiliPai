// 文件路径: data/repository/VideoRepository.kt
package com.android.purebilibili.data.repository

import com.android.purebilibili.core.network.NetworkModule
import com.android.purebilibili.core.network.WbiUtils
import com.android.purebilibili.core.store.TokenManager
import com.android.purebilibili.data.model.response.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.InputStream
import java.util.TreeMap // 🔥 引入 TreeMap 用于参数排序

object VideoRepository {
    private val api = NetworkModule.api

    private val QUALITY_CHAIN = listOf(120, 116, 112, 80, 64, 32, 16)

    // 1. 首页推荐
    suspend fun getHomeVideos(idx: Int = 0): Result<List<VideoItem>> = withContext(Dispatchers.IO) {
        try {
            val navResp = api.getNavInfo()
            val wbiImg = navResp.data?.wbi_img ?: throw Exception("无法获取 Key")
            val imgKey = wbiImg.img_url.substringAfterLast("/").substringBefore(".")
            val subKey = wbiImg.sub_url.substringAfterLast("/").substringBefore(".")

            val params = mapOf(
                "ps" to "10", "fresh_type" to "3", "fresh_idx" to idx.toString(),
                "feed_version" to System.currentTimeMillis().toString(), "y_num" to idx.toString()
            )
            val signedParams = WbiUtils.sign(params, imgKey, subKey)
            val feedResp = api.getRecommendParams(signedParams)
            val list = feedResp.data?.item?.map { it.toVideoItem() }?.filter { it.bvid.isNotEmpty() } ?: emptyList()
            Result.success(list)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun getNavInfo(): Result<NavData> = withContext(Dispatchers.IO) {
        try {
            val resp = api.getNavInfo()
            if (resp.code == 0 && resp.data != null) {
                Result.success(resp.data)
            } else {
                if (resp.code == -101) {
                    Result.success(NavData(isLogin = false))
                } else {
                    Result.failure(Exception("错误码: ${resp.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getVideoDetails(bvid: String): Result<Pair<ViewInfo, PlayUrlData>> = withContext(Dispatchers.IO) {
        try {
            val viewResp = api.getVideoInfo(bvid)
            val info = viewResp.data ?: throw Exception("视频详情为空: ${viewResp.code}")
            val cid = info.cid
            if (cid == 0L) throw Exception("CID 获取失败")

            val isLogin = !TokenManager.sessDataCache.isNullOrEmpty()
            val startQuality = if (isLogin) 120 else 80

            val playData = fetchPlayUrlRecursive(bvid, cid, startQuality)
                ?: throw Exception("无法获取任何画质的播放地址")

            // 🔥 支持 DASH 和 durl 两种格式
            val hasDash = !playData.dash?.video.isNullOrEmpty()
            val hasDurl = !playData.durl.isNullOrEmpty()
            if (!hasDash && !hasDurl) throw Exception("播放地址解析失败 (无 dash/durl)")

            Result.success(Pair(info, playData))
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun getPlayUrlData(bvid: String, cid: Long, qn: Int): PlayUrlData? = withContext(Dispatchers.IO) {
    // 🔥 简化策略：单次请求，如果 412 则等待 2s 后重试一次
    var result = fetchPlayUrlWithWbi(bvid, cid, qn)
    if (result == null) {
        android.util.Log.d("VideoRepo", "🔥 First attempt failed, waiting 2s before retry...")
        kotlinx.coroutines.delay(2000)
        result = fetchPlayUrlWithWbi(bvid, cid, qn)
    }
    result
}

    // 🔥🔥 [稳定版核心修复] 获取评论列表
    suspend fun getComments(aid: Long, page: Int, ps: Int = 20): Result<ReplyData> = withContext(Dispatchers.IO) {
        try {
            val navResp = api.getNavInfo()
            val wbiImg = navResp.data?.wbi_img

            if (wbiImg == null) {
                return@withContext Result.failure(Exception("无法获取 Wbi 签名密钥"))
            }

            val imgKey = wbiImg.img_url.substringAfterLast("/").substringBefore(".")
            val subKey = wbiImg.sub_url.substringAfterLast("/").substringBefore(".")

            // 🔥 使用 TreeMap 保证签名顺序绝对正确
            val params = TreeMap<String, String>()
            params["oid"] = aid.toString()
            params["type"] = "1"     // 1: 视频评论区
            params["mode"] = "3"     // 3: 按热度排序
            params["next"] = page.toString()
            params["ps"] = ps.toString()

            val signedParams = WbiUtils.sign(params, imgKey, subKey)
            val response = api.getReplyList(signedParams)

            if (response.code == 0) {
                Result.success(response.data ?: ReplyData())
            } else {
                Result.failure(Exception("B站接口错误: ${response.code} - ${response.message}"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    // 🔥🔥 [新增] 获取二级评论 (楼中楼)
    suspend fun getSubComments(aid: Long, rootId: Long, page: Int, ps: Int = 20): Result<ReplyData> = withContext(Dispatchers.IO) {
        try {
            // 注意：需要在 ApiClient.kt 中定义 getReplyReply 接口
            val response = api.getReplyReply(
                oid = aid,
                root = rootId,
                pn = page,
                ps = ps
            )
            if (response.code == 0) {
                Result.success(response.data ?: ReplyData())
            } else {
                Result.failure(Exception("接口错误: ${response.code}"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun getEmoteMap(): Map<String, String> = withContext(Dispatchers.IO) {
        val map = mutableMapOf<String, String>()
        map["[doge]"] = "http://i0.hdslb.com/bfs/emote/6f8743c3c13009f4705307b2750e32f5068225e3.png"
        map["[笑哭]"] = "http://i0.hdslb.com/bfs/emote/500b63b2f293309a909403a746566fdd6104d498.png"
        map["[妙啊]"] = "http://i0.hdslb.com/bfs/emote/03c39c8eb009f63568971032b49c716259c72441.png"
        try {
            val response = api.getEmotes()
            response.data?.packages?.forEach { pkg ->
                pkg.emote?.forEach { emote -> map[emote.text] = emote.url }
            }
        } catch (e: Exception) { e.printStackTrace() }
        map
    }

    private suspend fun fetchPlayUrlRecursive(bvid: String, cid: Long, targetQn: Int): PlayUrlData? {
        try {
            val data = fetchPlayUrlWithWbi(bvid, cid, targetQn)
            // 🔥 修复：只要 API 返回有效数据就使用，不再强制降级
            // API 会自动返回用户权限允许的最高画质
            if (data != null && (!data.durl.isNullOrEmpty() || !data.dash?.video.isNullOrEmpty())) {
                android.util.Log.d("VideoRepo", "🔥 Got valid PlayUrl: requested=$targetQn, actual=${data.quality}")
                return data
            }
        } catch (e: Exception) {
            android.util.Log.w("VideoRepo", "fetchPlayUrlRecursive failed for qn=$targetQn: ${e.message}")
        }
        var nextIndex = -1
        if (targetQn in QUALITY_CHAIN) {
            nextIndex = QUALITY_CHAIN.indexOf(targetQn) + 1
        } else {
            for (i in QUALITY_CHAIN.indices) {
                if (QUALITY_CHAIN[i] < targetQn) { nextIndex = i; break }
            }
        }
        if (nextIndex == -1 || nextIndex >= QUALITY_CHAIN.size) return null
        
        // 🔥 增加延迟到 1500ms 防止 412 限流
        kotlinx.coroutines.delay(1500)
        return fetchPlayUrlRecursive(bvid, cid, QUALITY_CHAIN[nextIndex])
    }

    private suspend fun fetchPlayUrlWithWbi(bvid: String, cid: Long, qn: Int): PlayUrlData? {
        try {
            android.util.Log.d("VideoRepo", "fetchPlayUrlWithWbi: bvid=$bvid, cid=$cid, qn=$qn")
            val navResp = api.getNavInfo()
            val wbiImg = navResp.data?.wbi_img ?: throw Exception("Key Error")
            val imgKey = wbiImg.img_url.substringAfterLast("/").substringBefore(".")
            val subKey = wbiImg.sub_url.substringAfterLast("/").substringBefore(".")
            val params = mapOf(
                "bvid" to bvid, "cid" to cid.toString(), "qn" to qn.toString(),
                "fnval" to "16", "fnver" to "0", "fourk" to "1", "platform" to "html5", "high_quality" to "1"
            )
            val signedParams = WbiUtils.sign(params, imgKey, subKey)
            val response = api.getPlayUrl(signedParams)
            android.util.Log.d("VideoRepo", "🔥 PlayUrl response: code=${response.code}, requestedQn=$qn, returnedQuality=${response.data?.quality}, accept_quality=${response.data?.accept_quality}")
            android.util.Log.d("VideoRepo", "🔥 accept_description=${response.data?.accept_description}")
            android.util.Log.d("VideoRepo", "🔥 durl count=${response.data?.durl?.size}, first url length=${response.data?.durl?.firstOrNull()?.url?.length ?: 0}")
            if (response.code == 0) return response.data
            return null
        } catch (e: HttpException) {
            android.util.Log.e("VideoRepo", "HttpException: ${e.code()}")
            if (e.code() in listOf(402, 403, 404, 412)) return null
            throw e
        } catch (e: Exception) { 
            android.util.Log.e("VideoRepo", "Exception: ${e.message}")
            return null 
        }
    }

    suspend fun getRelatedVideos(bvid: String): List<RelatedVideo> = withContext(Dispatchers.IO) {
        try { api.getRelatedVideos(bvid).data ?: emptyList() } catch (e: Exception) { emptyList() }
    }

    suspend fun getDanmakuRawData(cid: Long): ByteArray? = withContext(Dispatchers.IO) {
        try {
            val responseBody = api.getDanmakuXml(cid)
            val bytes = responseBody.bytes() // 下载所有数据

            if (bytes.isEmpty()) return@withContext null

            // 检查首字节 判断是否压缩
            // XML 以 '<' 开头 (0x3C)
            if (bytes[0] == 0x3C.toByte()) {
                return@withContext bytes
            }

            // 尝试 Deflate 解压
            try {
                val inflater = java.util.zip.Inflater(true) // nowrap=true
                inflater.setInput(bytes)
                val buffer = ByteArray(1024 * 1024 * 4) // max 4MB buffer? 自动扩容较麻烦，先用 simple approach
                val outputStream = java.io.ByteArrayOutputStream(bytes.size * 3)
                val tempBuffer = ByteArray(1024)
                while (!inflater.finished()) {
                    val count = inflater.inflate(tempBuffer)
                    if (count == 0) {
                         if (inflater.needsInput()) break
                         if (inflater.needsDictionary()) break
                    }
                    outputStream.write(tempBuffer, 0, count)
                }
                inflater.end()
                return@withContext outputStream.toByteArray()
            } catch (e: Exception) {
                e.printStackTrace()
                // 如果解压失败，返回原始数据（万一是普通 XML 但只有空格在前？）
                return@withContext bytes
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}