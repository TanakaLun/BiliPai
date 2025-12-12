// 文件路径: feature/settings/SettingsScreen.kt
package com.android.purebilibili.feature.settings

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import coil.compose.AsyncImage
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.foundation.border
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.vectorResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.purebilibili.core.theme.BiliPink
import com.android.purebilibili.core.ui.AppIcons

const val GITHUB_URL = "https://github.com/jay3-yy/BiliPai/"

enum class DisplayMode(val title: String, val value: Int) {
    Grid("双列网格 (默认)", 0),
    Card("单列大图 (沉浸)", 1)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel(),
    onBack: () -> Unit,
    // 🔥 新增跳转回调
    onOpenSourceLicensesClick: () -> Unit
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    val state by viewModel.state.collectAsState()
    val prefs = remember { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }

    var displayModeInt by remember { mutableIntStateOf(prefs.getInt("display_mode", 0)) }
    var isStatsEnabled by remember { mutableStateOf(prefs.getBoolean("show_stats", false)) }


    var showModeDialog by remember { mutableStateOf(false) }
    var showCacheDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    // 🔥🔥 [新增] 权限弹窗状态
    var showPipPermissionDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.refreshCacheSize()
    }

    fun saveMode(mode: Int) {
        displayModeInt = mode
        prefs.edit().putInt("display_mode", mode).apply()
        showModeDialog = false
    }

    // 🔥🔥 [新增] 检查画中画权限的辅助函数
    fun checkPipPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_PICTURE_IN_PICTURE,
                    Process.myUid(),
                    context.packageName
                )
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_PICTURE_IN_PICTURE,
                    Process.myUid(),
                    context.packageName
                )
            }
            return mode == AppOpsManager.MODE_ALLOWED
        }
        return false
    }

    // 🔥🔥 [新增] 跳转到系统设置的函数
    fun gotoPipSettings() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // 直接使用字符串 action，解决 "Unresolved reference" 报错
                val intent = Intent(
                    "android.settings.PICTURE_IN_PICTURE_SETTINGS",
                    Uri.parse("package:${context.packageName}")
                )
                context.startActivity(intent)
            }
        } catch (e: Exception) {
            // 如果跳转特定页面失败，跳转到应用详情页作为保底
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = Uri.parse("package:${context.packageName}")
            context.startActivity(intent)
        }
    }

    // 1. 首页模式弹窗
    if (showModeDialog) {
        AlertDialog(
            onDismissRequest = { showModeDialog = false },
            title = { Text("选择首页展示方式", color = MaterialTheme.colorScheme.onSurface) },
            text = {
                Column {
                    DisplayMode.entries.forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { saveMode(mode.value) }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (displayModeInt == mode.value),
                                onClick = { saveMode(mode.value) },
                                colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary, unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = mode.title, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showModeDialog = false }) { Text("取消", color = MaterialTheme.colorScheme.primary) } },
            containerColor = MaterialTheme.colorScheme.surface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    // 2. 主题模式弹窗
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("外观设置", color = MaterialTheme.colorScheme.onSurface) },
            text = {
                Column {
                    AppThemeMode.entries.forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setThemeMode(mode)
                                    showThemeDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (state.themeMode == mode),
                                onClick = {
                                    viewModel.setThemeMode(mode)
                                    showThemeDialog = false
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = MaterialTheme.colorScheme.primary,
                                    unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = mode.label, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showThemeDialog = false }) { Text("取消", color = MaterialTheme.colorScheme.primary) } },
            containerColor = MaterialTheme.colorScheme.surface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    // 3. 缓存清理弹窗
    if (showCacheDialog) {
        AlertDialog(
            onDismissRequest = { showCacheDialog = false },
            title = { Text("清除缓存", color = MaterialTheme.colorScheme.onSurface) },
            text = { Text("确定要清除所有图片和视频缓存吗？", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearCache()
                        Toast.makeText(context, "缓存已清除", Toast.LENGTH_SHORT).show()
                        showCacheDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) { Text("确认清除") }
            },
            dismissButton = { TextButton(onClick = { showCacheDialog = false }) { Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant) } },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    // 🔥🔥 [新增] 权限申请弹窗
    if (showPipPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPipPermissionDialog = false },
            title = { Text("权限申请", color = MaterialTheme.colorScheme.onSurface) },
            text = { Text("检测到未开启“画中画”权限。请在设置中开启该权限，否则无法使用小窗播放。", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            confirmButton = {
                Button(
                    onClick = {
                        gotoPipSettings()
                        showPipPermissionDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) { Text("去设置") }
            },
            dismissButton = {
                TextButton(onClick = { showPipPermissionDialog = false }) {
                    Text("暂不开启", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // 🔥 作者联系方式 (置顶)
            item { SettingsSectionTitle("关注作者") }
            item {
                SettingsGroup {
                    SettingClickableItem(
                        iconPainter = androidx.compose.ui.res.painterResource(com.android.purebilibili.R.drawable.ic_telegram_logo),
                        title = "Telegram 频道",
                        value = "@BiliPai",
                        onClick = { uriHandler.openUri("https://t.me/BiliPai") },
                        iconTint = Color.Unspecified // Use original Telegram colors
                    )
                    Divider()
                    SettingClickableItem(
                        icon = AppIcons.Twitter,
                        title = "Twitter / X",
                        value = "@YangY_0x00",
                        onClick = { uriHandler.openUri("https://x.com/YangY_0x00") },
                        iconTint = Color(0xFF1DA1F2) // Twitter Blue
                    )
                }
            }
            
            item { SettingsSectionTitle("首页与外观") }
            item {
                SettingsGroup {
                    SettingClickableItem(
                        icon = Icons.Outlined.Dashboard,
                        title = "首页展示方式",
                        value = DisplayMode.entries.find { it.value == displayModeInt }?.title ?: "未知",
                        onClick = { showModeDialog = true },
                        iconTint = Color(0xFF5C6BC0) // Indigo
                    )
                    Divider()
                    
                    SettingSwitchItem(
                        icon = Icons.Outlined.ViewStream,
                        title = "悬浮底栏",
                        subtitle = "关闭后底栏将沉浸式贴底显示",
                        checked = state.isBottomBarFloating,
                        onCheckedChange = { viewModel.toggleBottomBarFloating(it) },
                        iconTint = Color(0xFF26C6DA) // Cyan
                    )
                    Divider()



                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        SettingSwitchItem(
                            icon = Icons.Outlined.Palette,
                            title = "动态取色 (Material You)",
                            subtitle = "跟随系统壁纸变换应用主题色",
                            checked = state.dynamicColor,
                            onCheckedChange = { viewModel.toggleDynamicColor(it) },
                            iconTint = Color(0xFFEC407A) // Pink
                        )
                        Divider()
                    }

                    // 🔥🔥 [新增] 应用图标选择器
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // 使用简单的 Apps 图标
                            Icon(
                                Icons.Outlined.Apps,
                                contentDescription = null,
                                tint = Color(0xFF9C27B0),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = "应用图标",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "切换个性化启动图标",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // 图标数据类
                        data class IconOption(val key: String, val name: String, val desc: String)
                        val iconOptions = listOf(
                            IconOption("3D", "3D立体", "默认"),
                            IconOption("Blue", "经典蓝", "原版"),
                            IconOption("Retro", "复古怀旧", "80年代"),
                            IconOption("Flat", "扁平现代", "Material"),
                            IconOption("Neon", "霜虹发光", "赛博朋克")
                        )
                        
                        androidx.compose.foundation.lazy.LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp) // 微调 padding
                        ) {
                            items(iconOptions.size) { index ->
                                val option = iconOptions[index]
                                val isSelected = state.appIcon == option.key
                                
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(RoundedCornerShape(14.dp)) // iOS 风格圆角
                                            .clickable { 
                                                if (!isSelected) {
                                                    Toast.makeText(context, "正在切换图标...", Toast.LENGTH_SHORT).show()
                                                    viewModel.setAppIcon(option.key)
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        // 真实应用图标
                                        val iconRes = when(option.key) {
                                            "3D" -> com.android.purebilibili.R.mipmap.ic_launcher_3d
                                            "Blue" -> com.android.purebilibili.R.mipmap.ic_launcher_blue
                                            "Retro" -> com.android.purebilibili.R.mipmap.ic_launcher_retro
                                            "Flat" -> com.android.purebilibili.R.mipmap.ic_launcher_flat
                                            "Neon" -> com.android.purebilibili.R.mipmap.ic_launcher_neon
                                            else -> com.android.purebilibili.R.mipmap.ic_launcher
                                        }
                                        AsyncImage(
                                            model = iconRes,
                                            contentDescription = option.name,
                                            modifier = Modifier.size(48.dp)
                                        )
                                        
                                        if (isSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .matchParentSize()
                                                    .background(Color.Black.copy(alpha = 0.3f))
                                            )
                                            Icon(
                                                Icons.Filled.CheckCircle,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = option.name,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                    Text(
                                        text = option.desc,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }

                    Divider()

                    // 🔥🔥 [新增] 主题色选择器
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Outlined.ColorLens,
                                contentDescription = null,
                                tint = Color(0xFFE91E63),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = "主题色",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (state.dynamicColor) "已启用动态取色，此设置无效" 
                                           else "选择应用主色调",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            com.android.purebilibili.core.theme.ThemeColors.forEachIndexed { index, color ->
                                val isSelected = state.themeColorIndex == index && !state.dynamicColor
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(androidx.compose.foundation.shape.CircleShape)
                                        .background(color)
                                        .then(
                                            if (isSelected) Modifier.border(
                                                3.dp, 
                                                MaterialTheme.colorScheme.onSurface,
                                                androidx.compose.foundation.shape.CircleShape
                                            ) else Modifier
                                        )
                                        .clickable(enabled = !state.dynamicColor) { 
                                            viewModel.setThemeColorIndex(index) 
                                        }
                                        .graphicsLayer { 
                                            alpha = if (state.dynamicColor) 0.4f else 1f 
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    Divider()

                    SettingClickableItem(
                        icon = Icons.Outlined.DarkMode,
                        title = "深色模式",
                        value = state.themeMode.label,
                        onClick = { showThemeDialog = true },
                        iconTint = Color(0xFF42A5F5) // Blue
                    )
                }
            }

            item { SettingsSectionTitle("播放与解码") }
            item {
                SettingsGroup {
                    SettingSwitchItem(
                        icon = Icons.Outlined.Memory,
                        title = "启用硬件解码",
                        subtitle = "减少发热和耗电 (推荐开启)",
                        checked = state.hwDecode,
                        onCheckedChange = { viewModel.toggleHwDecode(it) },
                        iconTint = Color(0xFF66BB6A) // Green
                    )
                    Divider()

                    // 🔥🔥 [修改] 增加权限检测逻辑
                    SettingSwitchItem(
                        icon = Icons.Outlined.PictureInPicture,
                        title = "后台/画中画播放",
                        subtitle = "应用切到后台时继续播放",
                        checked = state.bgPlay,
                        onCheckedChange = { isChecked ->
                            if (isChecked) {
                                // 尝试开启时，先检查权限
                                if (checkPipPermission()) {
                                    viewModel.toggleBgPlay(true)
                                } else {
                                    // 没权限，弹窗，且暂时不开启开关（或者也可以开启开关但提示）
                                    // 这里策略是：允许开启开关，但同时弹窗提醒去设置
                                    viewModel.toggleBgPlay(true)
                                    showPipPermissionDialog = true
                                }
                            } else {
                                // 关闭时直接关闭
                                viewModel.toggleBgPlay(false)
                            }
                        },
                        iconTint = Color(0xFF26A69A) // Teal
                    )
                    Divider()
                    SettingSwitchItem(
                        icon = Icons.Outlined.Info,
                        title = "详细统计信息",
                        subtitle = "显示 Codec、码率等 Geek 信息",
                        checked = isStatsEnabled,
                        onCheckedChange = {
                            isStatsEnabled = it
                            prefs.edit().putBoolean("show_stats", it).apply()
                        },
                        iconTint = Color(0xFF78909C) // Blue Grey
                    )
                    
                    Divider()
                    
                    // 🔥🔥 [新增] 手势灵敏度滑块
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Outlined.TouchApp,
                                contentDescription = null,
                                tint = Color(0xFFFF9800),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "手势灵敏度",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "调整快进/音量/亮度手势响应速度",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "${(state.gestureSensitivity * 100).toInt()}%",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "较慢",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Slider(
                                value = state.gestureSensitivity,
                                onValueChange = { viewModel.setGestureSensitivity(it) },
                                valueRange = 0.5f..2.0f,
                                steps = 5, // 0.5, 0.75, 1.0, 1.25, 1.5, 1.75, 2.0
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary,
                                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                                ),
                                modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                            )
                            Text(
                                "较快",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }


            item { SettingsSectionTitle("高级选项") }
            item {
                SettingsGroup {
                    SettingClickableItem(
                        icon = Icons.Outlined.DeleteOutline,
                        title = "清除缓存",
                        value = state.cacheSize,
                        onClick = { showCacheDialog = true },
                        iconTint = Color(0xFFEF5350) // Red
                    )
                    Divider()
                    SettingClickableItem(
                        icon = Icons.Outlined.Description,
                        title = "开源许可证",
                        value = "License",
                        onClick = onOpenSourceLicensesClick,
                        iconTint = Color(0xFFFFA726)
                    )
                    Divider()
                    SettingClickableItem(
                        icon = Icons.Outlined.Code,
                        title = "开源主页",
                        value = "GitHub",
                        onClick = { uriHandler.openUri(GITHUB_URL) },
                        iconTint = Color(0xFF7E57C2) // Deep Purple
                    )
                    Divider()
                    SettingClickableItem(
                        icon = Icons.Outlined.Info,
                        title = "版本",
                        value = "v${com.android.purebilibili.BuildConfig.VERSION_NAME}",
                        onClick = null,
                        iconTint = Color(0xFF29B6F6) // Light Blue
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

// ... 底部组件封装保持不变 ...
@Composable
fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 8.dp)
    )
}

@Composable
fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp)),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,  // 🔥 微阴影增加层次感
        tonalElevation = 1.dp    // 🔥 Material3 色调提升
    ) {
        Column(content = content)
    }
}

@Composable
fun SettingSwitchItem(
    icon: ImageVector? = null,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    // 🔥 新增：图标颜色
    iconTint: Color = BiliPink
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            // 🔥 彩色圆形背景图标
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconTint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier.scale(0.9f)
        )
    }
}

@Composable
fun SettingClickableItem(
    icon: ImageVector? = null,
    iconPainter: androidx.compose.ui.graphics.painter.Painter? = null,
    title: String,
    value: String? = null,
    onClick: (() -> Unit)? = null,
    // 🔥 新增：图标颜色
    iconTint: Color = BiliPink
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null || iconPainter != null) {
            if (iconTint != Color.Unspecified) {
                // 🔥 彩色圆形背景图标
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(iconTint.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (icon != null) {
                        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
                    } else if (iconPainter != null) {
                        Icon(painter = iconPainter, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
                    }
                }
            } else {
                // 🔥 使用图标原始颜色（无背景容器）
                Box(
                    modifier = Modifier.size(36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (icon != null) {
                        Icon(icon, contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(36.dp))
                    } else if (iconPainter != null) {
                        Icon(painter = iconPainter, contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(36.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
        }
        Text(text = title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (value != null) {
                Text(text = value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (onClick != null) {
                Spacer(modifier = Modifier.width(6.dp))
                Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f), modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun Divider() {
    Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(MaterialTheme.colorScheme.surfaceVariant))
}

fun Modifier.scale(scale: Float): Modifier = this.then(
    Modifier.graphicsLayer(scaleX = scale, scaleY = scale)
)