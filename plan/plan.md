# 视频播放优化技术规划

## 问题根因分析

### 问题 1: 视频加载缓慢

| 位置 | 问题 | 影响 |
|------|------|------|
| `VideoRepository.kt:67-72` | PlayUrlCache 缓存被禁用 | 每次加载都需网络请求 |
| `VideoRepository.kt:225` | 重试延迟 `(0, 200, 500ms)` × 多画质 | 最坏情况等待 2100ms+ |
| `VideoRepository.kt:108-134` | WBI Keys 获取串行执行 | 阻塞视频加载 |

### 问题 2: 找不到播放地址

| 位置 | 问题 | 影响 |
|------|------|------|
| `VideoRepository.kt:133` | WBI 密钥失败时抛出异常 | 没有优雅降级 |
| `VideoRepository.kt:285-296` | 错误返回 null 而非错误信息 | 无法区分错误类型 |

### 问题 3: 清晰度切换失败

| 位置 | 问题 | 影响 |
|------|------|------|
| `PlayerViewModel.kt:481-517` | 使用缓存的 DASH URL | URL 可能已过期 |
| `VideoRepository.kt:219` | 画质链不完整 | 不包含高画质选项 |

---

## 修复方案

### 修复 1: 恢复 PlayUrlCache [HIGH PRIORITY]

**文件**: `VideoRepository.kt`

**改动**:

- 取消注释 L67-72 的缓存逻辑
- 确保缓存命中时直接返回数据

**预期效果**: 重复播放同一视频时秒开

---

### 修复 2: 优化重试策略 [MEDIUM PRIORITY]

**文件**: `VideoRepository.kt`

**改动**:

- 将 `retryDelays` 从 `(0, 200, 500)` 改为 `(0, 100)`
- 减少不必要的等待时间

**预期效果**: 减少 ~400ms 的最大等待时间

---

### 修复 3: 完善画质降级链 [MEDIUM PRIORITY]

**文件**: `VideoRepository.kt`

**改动**:

- 更新 `QUALITY_CHAIN` 包含所有画质: `(120, 116, 112, 80, 74, 64, 32, 16)`
- 在 `fetchPlayUrlRecursive` 中使用完整的降级链

**预期效果**: 支持更多画质选项的降级

---

### 修复 4: 改进 DASH 缓存 URL 验证 [MEDIUM PRIORITY]

**文件**: `PlayerViewModel.kt`

**改动**:

- 切换清晰度时，优先使用缓存但验证 URL 有效性
- 如果缓存 URL 请求失败，fallback 到 API 请求

**预期效果**: 减少清晰度切换失败率

---

## 验证计划

### 自动化测试

- 运行现有单元测试确保无回归

### 手动验证

1. 测试首次加载视频速度
2. 测试重复加载同一视频（验证缓存）
3. 测试清晰度切换功能
4. 测试网络波动情况下的重试机制

### 构建验证

```bash
./gradlew assembleDebug
```
