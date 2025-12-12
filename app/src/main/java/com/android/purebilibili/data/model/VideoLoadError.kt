// 文件路径: data/model/VideoLoadError.kt
package com.android.purebilibili.data.model

/**
 * 视频加载错误类型定义
 * 
 * 用于统一分类视频加载过程中可能出现的各种错误，
 * 并提供用户友好的错误信息和重试判断。
 */
sealed class VideoLoadError {
    
    /** 网络连接错误（超时、无网络等） */
    object NetworkError : VideoLoadError()
    
    /** WBI 签名验证失败（412 风控等） */
    object WbiSignatureError : VideoLoadError()
    
    /** 视频不存在或已删除 */
    object VideoNotFound : VideoLoadError()
    
    /** 地区限制，无法播放 */
    object RegionRestricted : VideoLoadError()
    
    /** 需要大会员才能观看 */
    object VipRequired : VideoLoadError()
    
    /** CID 获取失败 */
    object CidNotFound : VideoLoadError()
    
    /** API 返回错误 */
    data class ApiError(val code: Int, val message: String) : VideoLoadError()
    
    /** 未知错误 */
    data class UnknownError(val throwable: Throwable) : VideoLoadError()
    
    /**
     * 获取用户友好的错误信息
     */
    fun toUserMessage(): String = when (this) {
        is NetworkError -> "网络连接失败，请检查网络后重试"
        is WbiSignatureError -> "验证失败，正在重试..."
        is VideoNotFound -> "视频不存在或已被删除"
        is RegionRestricted -> "该视频在当前地区不可用"
        is VipRequired -> "该视频需要大会员才能观看"
        is CidNotFound -> "视频信息加载失败，请重试"
        is ApiError -> "加载失败: $message (错误码: $code)"
        is UnknownError -> "加载失败: ${throwable.message ?: "未知错误"}"
    }
    
    /**
     * 判断该错误是否可以通过重试解决
     */
    fun isRetryable(): Boolean = when (this) {
        is NetworkError -> true
        is WbiSignatureError -> true
        is CidNotFound -> true
        is ApiError -> code in listOf(-412, -504, -502, -500) // 服务端临时错误
        is UnknownError -> true
        // 以下错误重试无意义
        is VideoNotFound -> false
        is RegionRestricted -> false
        is VipRequired -> false
    }
    
    companion object {
        /**
         * 从 API 错误码创建对应的错误类型
         */
        fun fromApiCode(code: Int, message: String = ""): VideoLoadError = when (code) {
            -404 -> VideoNotFound
            -403, -10403 -> {
                // 需要判断是地区限制还是大会员限制
                if (message.contains("大会员") || message.contains("VIP")) {
                    VipRequired
                } else {
                    RegionRestricted
                }
            }
            -412 -> WbiSignatureError
            else -> ApiError(code, message)
        }
        
        /**
         * 从异常创建对应的错误类型
         */
        fun fromException(e: Throwable): VideoLoadError = when {
            e is java.net.UnknownHostException -> NetworkError
            e is java.net.SocketTimeoutException -> NetworkError
            e is java.io.IOException -> NetworkError
            e.message?.contains("412") == true -> WbiSignatureError
            e.message?.contains("Wbi") == true -> WbiSignatureError
            else -> UnknownError(e)
        }
    }
}
