// 文件路径: app/src/main/java/com/android/purebilibili/MainActivity.kt
package com.android.purebilibili

import android.app.PictureInPictureParams
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.purebilibili.core.store.SettingsManager
import com.android.purebilibili.core.theme.BiliPink
import com.android.purebilibili.core.theme.PureBiliBiliTheme
import com.android.purebilibili.feature.settings.AppThemeMode
import com.android.purebilibili.navigation.AppNavigation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

private const val TAG = "MainActivity"
private const val PREFS_NAME = "app_welcome"
private const val KEY_FIRST_LAUNCH = "first_launch_shown"

class MainActivity : ComponentActivity() {
    
    // 🔥 PiP 状态
    var isInPipMode by mutableStateOf(false)
        private set
    
    // 🔥 是否在视频页面 (用于决定是否进入 PiP)
    var isInVideoDetail by mutableStateOf(false)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val context = LocalContext.current
            
            // 🔥 首次启动检测
            val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
            var showWelcome by remember { mutableStateOf(!prefs.getBoolean(KEY_FIRST_LAUNCH, false)) }

            // 1. 获取存储的模式 (默认为跟随系统)
            val themeMode by SettingsManager.getThemeMode(context).collectAsState(initial = AppThemeMode.FOLLOW_SYSTEM)

            // 🔥🔥 2. [新增] 获取动态取色设置 (默认为 true)
            val dynamicColor by SettingsManager.getDynamicColor(context).collectAsState(initial = true)

            // 3. 获取系统当前的深色状态
            val systemInDark = isSystemInDarkTheme()

            // 4. 根据枚举值决定是否开启 DarkTheme
            val useDarkTheme = when (themeMode) {
                AppThemeMode.FOLLOW_SYSTEM -> systemInDark // 跟随系统：系统黑则黑，系统白则白
                AppThemeMode.LIGHT -> false                // 强制浅色
                AppThemeMode.DARK -> true                  // 强制深色
            }

            // 5. 传入参数
            PureBiliBiliTheme(
                darkTheme = useDarkTheme,
                dynamicColor = dynamicColor // 🔥🔥 传入动态取色开关
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(
                        isInPipMode = isInPipMode,
                        onVideoDetailEnter = { 
                            isInVideoDetail = true
                            Log.d(TAG, "🎬 进入视频详情页")
                        },
                        onVideoDetailExit = { 
                            isInVideoDetail = false
                            Log.d(TAG, "🔙 退出视频详情页")
                        }
                    )
                    
                    // 🔥 首次启动欢迎弹窗
                    if (showWelcome) {
                        WelcomeDialog(
                            onDismiss = {
                                prefs.edit().putBoolean(KEY_FIRST_LAUNCH, true).apply()
                                showWelcome = false
                            }
                        )
                    }
                }
            }
        }
    }
    
    // 🔥 用户按 Home 键或切换应用时触发
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        
        Log.d(TAG, "👋 onUserLeaveHint 触发, isInVideoDetail=$isInVideoDetail")
        
        // 🔥 使用 runBlocking 从 DataStore 读取设置 (仅在 onUserLeaveHint 中短暂使用)
        val bgPlayEnabled = runBlocking {
            SettingsManager.getBgPlay(this@MainActivity).first()
        }
        
        Log.d(TAG, "📺 bgPlayEnabled=$bgPlayEnabled, API=${Build.VERSION.SDK_INT}")
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && isInVideoDetail && bgPlayEnabled) {
            try {
                Log.d(TAG, "🎬 尝试进入 PiP 模式...")
                
                val pipParams = PictureInPictureParams.Builder()
                    .setAspectRatio(Rational(16, 9))
                
                // Android 12+: 启用自动进入和无缝调整
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    pipParams.setAutoEnterEnabled(true)
                    pipParams.setSeamlessResizeEnabled(true)
                }
                
                enterPictureInPictureMode(pipParams.build())
                Log.d(TAG, "✅ 成功进入 PiP 模式")
            } catch (e: Exception) {
                Log.e(TAG, "❌ 进入 PiP 失败", e)
            }
        } else {
            Log.d(TAG, "⏳ 未满足 PiP 条件: API>=${Build.VERSION_CODES.O}=${Build.VERSION.SDK_INT >= Build.VERSION_CODES.O}, inVideoDetail=$isInVideoDetail, bgPlay=$bgPlayEnabled")
        }
    }
    
    // 🔥 PiP 模式变化回调
    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPipMode = isInPictureInPictureMode
        Log.d(TAG, "📱 PiP 模式变化: $isInPictureInPictureMode")
    }
}

/**
 * 🔥 首次启动欢迎弹窗
 */
@Composable
fun WelcomeDialog(onDismiss: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("🎉", fontSize = 48.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "欢迎使用 BiliPai",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = BiliPink
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                // 祝福语
                Text(
                    "✨ 愿这款应用能带给你美好的观影体验！\n\n" +
                    "BiliPai 是一款开源的第三方 Bilibili 客户端，致力于提供简洁、流畅的使用体验。",
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 开源信息
                Surface(
                    color = BiliPink.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "📦 开源地址",
                            fontWeight = FontWeight.Medium,
                            color = BiliPink
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "github.com/jay3-yy/BiliPai",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 免责声明
                Surface(
                    color = Color(0xFFFFF3CD),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "⚠️ 免责声明",
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF856404)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "本应用仅供学习交流使用，所有内容版权归 Bilibili 及原作者所有。请勿用于商业用途。",
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            color = Color(0xFF856404)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = BiliPink),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("开始体验 🚀", fontWeight = FontWeight.Medium)
            }
        }
    )
}