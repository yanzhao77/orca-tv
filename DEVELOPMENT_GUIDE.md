# Orca-tv 开发指南

本文档为开发者提供详细的开发指导，帮助您快速理解项目架构并进行二次开发。

## 目录

1. [架构概览](#架构概览)
2. [核心模块详解](#核心模块详解)
3. [关键代码流程](#关键代码流程)
4. [扩展开发指南](#扩展开发指南)
5. [调试技巧](#调试技巧)
6. [常见问题](#常见问题)

---

## 架构概览

Orca-tv 采用 **MVVM (Model-View-ViewModel)** 架构模式，结合 Android Jetpack 组件实现清晰的分层。

```
┌─────────────────────────────────────────┐
│            UI Layer (View)              │
│  BrowseFragment / PlaybackActivity      │
└──────────────┬──────────────────────────┘
               │ LiveData
               ▼
┌─────────────────────────────────────────┐
│        ViewModel Layer                  │
│       ChannelViewModel                  │
└──────────────┬──────────────────────────┘
               │ Repository
               ▼
┌─────────────────────────────────────────┐
│         Data Layer                      │
│  ChannelRepository (三级回退逻辑)        │
└──────────────┬──────────────────────────┘
               │
      ┌────────┴────────┬────────────┐
      ▼                 ▼            ▼
┌──────────┐    ┌──────────┐  ┌──────────┐
│ Remote   │    │  Cache   │  │ Default  │
│ API      │    │  (Room)  │  │ (Assets) │
└──────────┘    └──────────┘  └──────────┘
```

---

## 核心模块详解

### 1. 数据层 (`data` 包)

#### Channel.kt - 数据模型
```kotlin
data class Channel(
    val id: Long,
    val name: String,
    val category: String,
    val logo: String?,
    val lines: List<ChannelLine>,  // 多线路支持
    val isFavorite: Boolean,
    val lastPlayedLineIndex: Int,  // 记录上次成功的线路
    val failCount: Int              // 失败计数，用于自动剔除
)
```

**设计要点**:
- 一个频道可以有多个播放线路（`lines`）
- 记录上次成功的线路索引，下次优先使用
- 失败计数用于标记不稳定的频道

#### M3UParser.kt - 播放列表解析器

**解析流程**:
1. 逐行读取 M3U 文件
2. 识别 `#EXTINF:` 行，提取频道元信息
3. 下一行为播放地址，与元信息组合成 `Channel` 对象
4. 同名频道的多个 URL 会被合并为多条线路

**支持的属性**:
- `tvg-id`: EPG 节目单 ID
- `tvg-name`: 频道名称
- `tvg-logo`: 频道图标 URL
- `group-title`: 频道分类

#### ChannelRepository.kt - 数据仓库

**三级回退逻辑**:
```kotlin
fun loadChannels() {
    // 1. 尝试从远程 API 加载
    val remoteResult = loadFromRemote()
    if (remoteResult.isSuccess) return remoteResult
    
    // 2. 尝试从本地缓存加载
    val cacheResult = loadFromCache()
    if (cacheResult.isSuccess) return cacheResult
    
    // 3. 加载内置默认列表
    return loadFromDefault()
}
```

### 2. UI 层 (`ui` 包)

#### BrowseFragment.kt - 主界面

**核心组件**:
- `ArrayObjectAdapter`: 管理行（Row）列表
- `ListRowPresenter`: 渲染每一行
- `ChannelCardPresenter`: 渲染频道卡片

**数据绑定流程**:
```kotlin
viewModel.channels.observe { channels ->
    val categories = channels.map { it.category }.distinct()
    
    for (category in categories) {
        val categoryChannels = channels.filter { it.category == category }
        val listRowAdapter = ArrayObjectAdapter(ChannelCardPresenter())
        categoryChannels.forEach { listRowAdapter.add(it) }
        
        rowsAdapter.add(ListRow(HeaderItem(category), listRowAdapter))
    }
}
```

#### PlaybackActivity.kt - 播放界面

**播放流程**:
1. 从 Intent 获取 `channel_id`
2. 从数据库加载完整的 `Channel` 对象
3. 初始化 ExoPlayer
4. 从 `lastPlayedLineIndex` 开始播放
5. 监听播放错误，自动切换到下一条线路

**错误处理**:
```kotlin
private val playerListener = object : Player.Listener {
    override fun onPlayerError(error: PlaybackException) {
        trySwitchToNextLine()  // 自动切换线路
    }
    
    override fun onPlaybackStateChanged(playbackState: Int) {
        if (playbackState == Player.STATE_READY) {
            // 播放成功，记录成功的线路索引
            updateLastPlayedLine(currentLineIndex)
        }
    }
}
```

### 3. ViewModel 层

#### ChannelViewModel.kt

**职责**:
- 管理 UI 状态（加载中、错误、数据）
- 调用 Repository 加载数据
- 提供数据过滤和分类方法

**LiveData 使用**:
```kotlin
private val _channels = MutableLiveData<List<Channel>>()
val channels: LiveData<List<Channel>> = _channels  // 只读，暴露给 UI

private val _errorState = MutableLiveData<String>()
val errorState: LiveData<String> = _errorState
```

---

## 关键代码流程

### 流程 1: 应用启动到显示频道列表

```
1. BrowseActivity.onCreate()
   ├─> 初始化 ViewModel
   ├─> 调用 viewModel.loadChannels()
   └─> 添加 BrowseFragment

2. ChannelViewModel.loadChannels()
   ├─> repository.loadFromRemote()
   │   ├─ 成功 → 保存到缓存 → 返回数据
   │   └─ 失败 ↓
   ├─> repository.loadFromCache()
   │   ├─ 成功 → 返回缓存数据
   │   └─ 失败 ↓
   └─> repository.loadFromDefault()
       └─ 加载 assets/default_channels.m3u

3. BrowseFragment 观察 channels LiveData
   ├─> buildRows(channels)
   └─> 渲染到 Leanback UI
```

### 流程 2: 播放频道与线路切换

```
1. 用户点击频道卡片
   └─> BrowseFragment.onItemClicked()
       └─> startActivity(PlaybackActivity, channel_id)

2. PlaybackActivity.onCreate()
   ├─> loadChannelAndPlay(channel_id)
   │   ├─> 从数据库加载 Channel
   │   └─> currentLineIndex = channel.lastPlayedLineIndex
   ├─> initializePlayer()
   └─> startPlayback()

3. startPlayback()
   ├─> 获取 channel.lines[currentLineIndex]
   ├─> player.setMediaItem(MediaItem.fromUri(line.url))
   └─> player.prepare() → player.play()

4. 播放错误 → onPlayerError()
   └─> trySwitchToNextLine()
       ├─> currentLineIndex++
       ├─ 如果还有下一条线路 → startPlayback()
       └─ 否则 → 提示"所有线路均无法播放" → finish()

5. 播放成功 → onPlaybackStateChanged(STATE_READY)
   └─> updateChannel(lastPlayedLineIndex = currentLineIndex)
```

---

## 扩展开发指南

### 添加新的频道来源

1. **在 ChannelRepository 中添加新的数据源方法**:
```kotlin
suspend fun loadFromCustomSource(): Result<List<Channel>> {
    // 实现自定义加载逻辑
}
```

2. **在 loadChannels() 中集成**:
```kotlin
fun loadChannels() {
    val customResult = loadFromCustomSource()
    if (customResult.isSuccess) return customResult
    
    // 继续原有的三级回退逻辑
    ...
}
```

### 实现 EPG 节目指南

1. **创建 EPG 数据模型**:
```kotlin
data class EpgProgram(
    val channelId: String,
    val title: String,
    val startTime: Long,
    val endTime: Long,
    val description: String?
)
```

2. **解析 EPG XML**:
```kotlin
class EpgParser {
    fun parse(xmlContent: String): List<EpgProgram> {
        // 使用 XmlPullParser 解析 XMLTV 格式
    }
}
```

3. **在播放界面显示当前节目**:
```kotlin
// 在 PlaybackActivity 中
private fun loadCurrentProgram(epgId: String) {
    val now = System.currentTimeMillis()
    val program = epgRepository.getProgramAt(epgId, now)
    // 更新 UI 显示节目信息
}
```

### 添加频道搜索功能

1. **在 BrowseFragment 中启用搜索**:
```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setOnSearchClickedListener {
        // 启动搜索界面
    }
}
```

2. **实现搜索 Fragment**:
```kotlin
class SearchFragment : SearchSupportFragment() {
    override fun onQueryTextChange(newQuery: String): Boolean {
        viewModel.searchChannels(newQuery)
        return true
    }
}
```

---

## 调试技巧

### 1. 查看数据库内容

使用 Android Studio 的 Database Inspector:
1. 运行应用
2. 打开 `View > Tool Windows > App Inspection`
3. 选择 `Database Inspector` 标签
4. 查看 `channels` 表

### 2. 网络请求调试

在 `ChannelRepository` 中添加日志:
```kotlin
val response = httpClient.newCall(request).execute()
Log.d(TAG, "Response code: ${response.code}")
Log.d(TAG, "Response body: ${response.body?.string()}")
```

### 3. 播放器调试

监听 ExoPlayer 的详细事件:
```kotlin
player.addAnalyticsListener(object : AnalyticsListener {
    override fun onPlayerError(eventTime: AnalyticsListener.EventTime, error: PlaybackException) {
        Log.e(TAG, "Playback error: ${error.errorCodeName}")
        Log.e(TAG, "Error message: ${error.message}")
    }
})
```

### 4. 使用 ADB 测试

```bash
# 安装 APK
adb install app-debug.apk

# 查看日志
adb logcat | grep "Orca"

# 清除应用数据（重置到首次启动状态）
adb shell pm clear com.orca.tv
```

---

## 常见问题

### Q1: 为什么首次启动加载很慢？

**A**: 首次启动需要从远程 API 下载播放列表并解析，可能需要几秒钟。建议：
- 在 UI 上显示加载进度
- 优化 M3U 解析性能（使用协程并发处理）

### Q2: 如何处理大量频道（1000+）的性能问题？

**A**: 
1. 使用 RecyclerView 的 DiffUtil 优化列表更新
2. 实现分页加载（Paging 3 库）
3. 对频道图标使用 Glide 的缓存策略

### Q3: 播放器频繁切换线路导致卡顿？

**A**: 
1. 增加播放错误的超时时间，避免过早判断失败
2. 实现线路预测速功能，优先尝试速度快的线路
3. 缓存最近成功的线路，下次直接使用

### Q4: 如何支持自定义播放器？

**A**: 
1. 创建 `PlayerEngine` 接口
2. 实现 `ExoPlayerEngine` 和 `IjkPlayerEngine`
3. 在设置中让用户选择播放器类型

---

## 代码规范

### Kotlin 代码风格

遵循 [Kotlin 官方代码风格](https://kotlinlang.org/docs/coding-conventions.html):
- 使用 4 个空格缩进
- 类名使用 PascalCase
- 函数和变量使用 camelCase
- 常量使用 UPPER_SNAKE_CASE

### 命名约定

- Activity: `XxxActivity`
- Fragment: `XxxFragment`
- ViewModel: `XxxViewModel`
- Repository: `XxxRepository`
- Adapter: `XxxAdapter`

### 注释规范

```kotlin
/**
 * 频道数据仓库
 * 
 * 实现三级回退机制：
 * 1. 远程 API
 * 2. 本地缓存
 * 3. 内置默认源
 * 
 * @param context 应用上下文
 */
class ChannelRepository(private val context: Context) {
    // ...
}
```

---

**祝您开发顺利！如有问题，欢迎提交 Issue。**
