package com.android.purebilibili.navigation

sealed class ScreenRoutes(val route: String) {
    object Home : ScreenRoutes("home")
    object Search : ScreenRoutes("search")
    object Settings : ScreenRoutes("settings")
    object Login : ScreenRoutes("login")
    object Profile : ScreenRoutes("profile")

    // 🔥 新增路由：历史记录和收藏
    object History : ScreenRoutes("history")
    object Favorite : ScreenRoutes("favorite")
    
    // 🔥 动态页面
    object Dynamic : ScreenRoutes("dynamic")

    // 🔥 开源许可证页面
    object OpenSourceLicenses : ScreenRoutes("open_source_licenses")

    object VideoPlayer : ScreenRoutes("video_player/{bvid}?cid={cid}") {
        fun createRoute(bvid: String, cid: Long = 0): String {
            return "video_player/$bvid?cid=$cid"
        }
    }
    
    // 🔥🔥 [新增] UP主空间页面
    object Space : ScreenRoutes("space/{mid}") {
        fun createRoute(mid: Long): String {
            return "space/$mid"
        }
    }
    
    // 🔥🔥 [新增] 直播播放页面
    object Live : ScreenRoutes("live/{roomId}?title={title}&uname={uname}") {
        fun createRoute(roomId: Long, title: String, uname: String): String {
            val encodedTitle = android.net.Uri.encode(title)
            val encodedUname = android.net.Uri.encode(uname)
            return "live/$roomId?title=$encodedTitle&uname=$encodedUname"
        }
    }
}