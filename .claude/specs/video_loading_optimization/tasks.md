# 视频加载优化实施计划

本文档将设计转化为一系列可执行的编码任务，采用测试驱动的渐进式开发方式。每个任务都引用需求文档中的具体验收标准。

---

## 1. 创建基础组件

### 1.1 创建 VideoLoadError 错误类型定义

- **创建文件**: `data/model/VideoLoadError.kt`
- **实现内容**:
  - 定义 sealed class 包含所有错误类型（NetworkError, WbiSignatureError, VideoNotFound 等）
  - 实现 `toUserMessage()` 方法返回用户友好的错误描述
  - 实现 `isRetryable()` 方法判断错误是否可重试
- **引用需求**: 3.1（根据错误类型显示不同提示）

### 1.2 创建 RetryStrategy 重试策略工具

- **创建文件**: `core/util/RetryStrategy.kt`
- **实现内容**:
  - 定义 `RetryConfig` 数据类（maxAttempts=4, initialDelayMs=500, multiplier=2.0）
  - 实现 `executeWithRetry()` 挂起函数，支持指数退避
  - 添加 `onAttempt` 回调用于 UI 进度显示
- **引用需求**: 1.3（指数退避重试），3.3（显示重试进度），3.4（最多自动重试 4 次）

---

## 2. 实现缓存层

### 2.1 创建 PlayUrlCache 播放地址缓存

- **创建文件**: `core/cache/PlayUrlCache.kt`
- **实现内容**:
  - 使用 `LruCache<String, CachedPlayUrl>(50)` 存储缓存
  - 实现 `get(bvid, cid)` 方法检查缓存有效性（10 分钟过期）
  - 实现 `put(bvid, cid, data)` 方法添加缓存
  - 实现 `invalidate()` 和 `clear()` 方法
- **引用需求**: 1.1（检查本地缓存），2.1（缓存播放 URL 10 分钟）

### 2.2 创建 WbiKeyManager 密钥管理器

- **创建文件**: `core/network/WbiKeyManager.kt`
- **实现内容**:
  - 使用 `Mutex` 保护并发刷新
  - 实现内存缓存（24 小时有效期）
  - 实现 `persistToStorage()` 和 `restoreFromStorage()` 方法持久化到 DataStore
  - 实现 `getWbiKeys()` 方法：缓存有效返回缓存，否则刷新
  - 实现 `invalidateCache()` 方法用于强制刷新
- **引用需求**: 4.1（WBI 密钥缓存 24 小时），4.2（过期时刷新），4.3（使用互斥锁）

---

## 3. 修改 PlayerUiState

### 3.1 增强 PlayerUiState 状态类型

- **修改文件**: `feature/video/PlayerViewModel.kt`（PlayerUiState 所在文件）
- **修改内容**:
  - 修改 `Loading` 状态：添加 `retryAttempt`、`maxAttempts`、`message` 字段
  - 修改 `Error` 状态：使用 `VideoLoadError` 类型，添加 `canRetry` 字段
- **引用需求**: 3.3（显示重试进度），3.2（包含重试按钮）

---

## 4. 重构 VideoRepository

### 4.1 集成 PlayUrlCache 到 VideoRepository

- **修改文件**: `data/repository/VideoRepository.kt`
- **修改内容**:
  - 在 `getVideoDetails()` 开始时检查 `PlayUrlCache`
  - 获取成功后调用 `PlayUrlCache.put()` 缓存结果
  - 添加日志记录缓存命中/未命中
- **引用需求**: 1.1（先检查缓存），2.1（缓存到内存）

### 4.2 重构 fetchPlayUrlRecursive 使用 RetryStrategy

- **修改文件**: `data/repository/VideoRepository.kt`
- **修改内容**:
  - 替换现有重试逻辑为 `RetryStrategy.executeWithRetry()`
  - 将错误映射为 `VideoLoadError` 类型
  - 保留画质降级逻辑
- **引用需求**: 1.3（指数退避），1.4（画质降级链），1.5（不静默失败）

### 4.3 集成 WbiKeyManager 替换内联 WBI 逻辑

- **修改文件**: `data/repository/VideoRepository.kt`
- **修改内容**:
  - 将 `wbiKeysCache` 和 `getWbiKeys()` 逻辑迁移到 `WbiKeyManager`
  - 更新所有使用 WBI 签名的方法调用 `WbiKeyManager.getWbiKeys()`
  - 在 412 错误时调用 `WbiKeyManager.invalidateCache()`
- **引用需求**: 1.2（WBI 失败时清除缓存重试），4.2（签名失败时刷新密钥）

### 4.4 增强错误分类和处理

- **修改文件**: `data/repository/VideoRepository.kt`
- **修改内容**:
  - 根据 API 返回码映射到 `VideoLoadError`：
    - 412 → WbiSignatureError
    - -404 → VideoNotFound
    - -10403 → RegionRestricted 或 VipRequired
    - 网络异常 → NetworkError
  - 修改返回类型使用 `Result<T>` 携带具体错误
- **引用需求**: 1.5（显示明确错误信息），1.6（CID 为空时重试）

---

## 5. 更新 PlayerViewModel

### 5.1 更新 loadVideo() 方法处理新状态

- **修改文件**: `feature/video/PlayerViewModel.kt`
- **修改内容**:
  - 使用 `Loading(retryAttempt, maxAttempts, message)` 更新加载状态
  - 捕获 `VideoLoadError` 并转换为 `Error` 状态
  - 添加 `onRetryProgress` 回调更新 UI
- **引用需求**: 3.3（显示重试进度）

### 5.2 实现重试功能

- **修改文件**: `feature/video/PlayerViewModel.kt`
- **修改内容**:
  - 添加 `retry()` 公共方法，清除缓存后重新调用 `loadVideo()`
  - 在重试前调用 `PlayUrlCache.invalidate()` 清除可能过期的缓存
- **引用需求**: 3.2（重试按钮功能）

---

## 6. 更新 UI 组件

### 6.1 更新 VideoDetailScreen 错误界面

- **修改文件**: `feature/video/VideoDetailScreen.kt`
- **修改内容**:
  - 根据 `VideoLoadError.toUserMessage()` 显示错误文案
  - 添加"重试"按钮调用 `viewModel.retry()`
  - 根据 `canRetry` 决定是否显示重试按钮
- **引用需求**: 3.1（不同错误类型不同提示），3.2（重试按钮）

### 6.2 更新加载界面显示重试进度

- **修改文件**: `feature/video/VideoDetailScreen.kt`
- **修改内容**:
  - 读取 `Loading.retryAttempt` 和 `Loading.maxAttempts`
  - 当 `retryAttempt > 0` 时显示"正在重试 X/Y..."
- **引用需求**: 3.3（显示重试进度提示）

---

## 7. 初始化和生命周期

### 7.1 应用启动时恢复 WBI 缓存

- **修改文件**: `BiliPaiApplication.kt`（或 MainActivity）
- **修改内容**:
  - 在 `onCreate()` 中调用 `WbiKeyManager.restoreFromStorage(context)`
- **引用需求**: 4.1（缓存到本地存储）

### 7.2 应用前台时检查 WBI 密钥

- **修改文件**: 创建 `core/lifecycle/AppLifecycleObserver.kt` 或修改现有 LifecycleObserver
- **修改内容**:
  - 监听 `ON_START` 事件
  - 检查 WBI 密钥剩余有效期，如果 < 1 小时则后台刷新
- **引用需求**: 4.4（前台恢复时预刷新密钥）

---

## 验证计划

### 手动验证步骤

由于项目当前没有自动化测试基础设施，请按以下步骤手动验证：

**测试 1: 正常视频加载**

1. 启动应用并登录
2. 在首页点击任意视频
3. 预期：视频在 3 秒内开始播放
4. 检查 Logcat 过滤 `PlayUrlCache`，应看到 "Cache miss"

**测试 2: 缓存命中**

1. 在测试 1 后返回首页
2. 再次点击同一视频
3. 预期：视频在 1 秒内开始播放
4. 检查 Logcat，应看到 "Cache hit"

**测试 3: 网络错误恢复**

1. 开启飞行模式
2. 点击任意视频
3. 预期：显示"网络连接失败"错误界面和重试按钮
4. 关闭飞行模式
5. 点击重试
6. 预期：视频成功加载

**测试 4: 重试进度显示**

1. 使用 Charles/Fiddler 等工具模拟网络延迟或 412 响应
2. 点击视频
3. 预期：看到"正在重试 1/4..."、"正在重试 2/4..."等进度提示

**测试 5: 画质降级提示**

1. 使用非大会员账号
2. 找一个标注 4K/大会员 的视频并点击
3. 预期：视频正常播放，Toast 显示"已自动切换到 XXX"

**测试 6: WBI 密钥持久化**

1. 打开应用，播放一个视频成功
2. 完全关闭应用（从最近任务中划掉）
3. 重新打开应用
4. 立即播放视频
5. 预期：首次加载不需要获取 WBI 密钥（Logcat 显示 "WBI keys restored from storage"）

---

## 任务依赖关系

```
1.1 (VideoLoadError) ─┬─► 4.4 (错误分类)
                      │
1.2 (RetryStrategy) ──┴─► 4.2 (重构重试逻辑)
                              │
2.1 (PlayUrlCache) ──────────► 4.1 (集成缓存)
                              │
2.2 (WbiKeyManager) ─────────► 4.3 (集成 WBI)
                              │
3.1 (PlayerUiState) ─────────► 5.1 (更新 loadVideo)
                              │
                              ▼
                         5.2 (重试功能)
                              │
                              ▼
                    6.1, 6.2 (UI 更新)
                              │
                              ▼
                    7.1, 7.2 (生命周期)
```
