# ChannelManager 模块设计文档

## 目标

将 iptv-api 的核心功能（M3U 解析、频道管理、多线路管理）封装为 Kotlin 模块，集成到 Orca-tv 项目中，实现完全本地化的频道管理系统，不再依赖外部 HTTP API。

---

## 核心设计理念

### 从外部 API 调用 → 本地模块调用

**之前的架构**:
```
App → HTTP Request → iptv-api (Python 后端) → M3U 文件 → 解析 → Channel 列表
```

**新的架构**:
```
App → ChannelManager (Kotlin 模块) → M3U 解析器 → Channel 列表
```

---

## 模块架构

### 1. 核心组件

```
com.orca.tv.channel/
├── ChannelManager.kt           # 频道管理器（单例）
├── parser/
│   ├── M3UParser.kt            # M3U 文件解析器
│   ├── M3UWriter.kt            # M3U 文件生成器
│   └── ChannelDataParser.kt    # 频道数据解析
├── model/
│   ├── Channel.kt              # 频道数据模型
│   ├── ChannelGroup.kt         # 频道分组
│   ├── ChannelSource.kt        # 频道源（线路）
│   └── ChannelCategory.kt      # 频道分类
├── source/
│   ├── ChannelSourceManager.kt # 源管理器
│   ├── RemoteSourceLoader.kt   # 远程源加载
│   ├── LocalSourceLoader.kt    # 本地源加载
│   └── SourceCache.kt          # 源缓存
├── filter/
│   ├── ChannelFilter.kt        # 频道过滤器
│   ├── BlacklistFilter.kt      # 黑名单过滤
│   ├── WhitelistFilter.kt      # 白名单过滤
│   └── DuplicateFilter.kt      # 去重过滤
└── api/
    ├── ChannelApi.kt           # 本地 API 接口
    └── ChannelApiImpl.kt       # API 实现
```

### 2. 数据模型

#### Channel (频道)
```kotlin
data class Channel(
    val id: String,                    // 频道唯一 ID
    val name: String,                  // 频道名称
    val displayName: String,           // 显示名称
    val category: String,              // 分类（央视、卫视等）
    val logo: String?,                 // 台标 URL
    val sources: List<ChannelSource>,  // 多个线路
    val epgId: String?,                // EPG ID
    val metadata: Map<String, String>  // 扩展元数据
)
```

#### ChannelSource (频道源/线路)
```kotlin
data class ChannelSource(
    val id: String,                    // 源 ID
    val url: String,                   // 播放 URL
    val origin: SourceOrigin,          // 来源类型
    val quality: Quality?,             // 清晰度
    val protocol: Protocol,            // 协议类型（HTTP/RTMP/RTSP）
    val ipvType: IpvType,              // IPv4/IPv6
    val priority: Int,                 // 优先级
    val testResult: TestResult?        // 测速结果
)

enum class SourceOrigin {
    LOCAL,      // 本地源
    REMOTE,     // 远程源
    WHITELIST,  // 白名单
    SUBSCRIBE,  // 订阅源
    HISTORY     // 历史缓存
}

enum class Quality {
    SD,         // 标清
    HD,         // 高清
    FHD,        // 全高清
    UHD         // 超高清
}

enum class Protocol {
    HTTP, HTTPS, RTMP, RTSP, UDP
}

enum class IpvType {
    IPV4, IPV6, UNKNOWN
}
```

#### TestResult (测速结果)
```kotlin
data class TestResult(
    val delay: Long,                   // 延迟（ms）
    val speed: Double,                 // 速率（MB/s）
    val resolution: String?,           // 分辨率
    val testTime: Long                 // 测试时间戳
)
```

---

## 3. ChannelManager 核心功能

### 3.1 单例模式

```kotlin
object ChannelManager {
    private var channels: Map<String, List<Channel>> = emptyMap()
    private val sourceManager = ChannelSourceManager()
    private val parser = M3UParser()
    private val cache = SourceCache()
    
    // 初始化
    fun initialize(context: Context)
    
    // 加载频道列表
    suspend fun loadChannels(sources: List<String>): Result<Unit>
    
    // 获取所有频道
    fun getAllChannels(): List<Channel>
    
    // 按分类获取频道
    fun getChannelsByCategory(category: String): List<Channel>
    
    // 搜索频道
    fun searchChannels(query: String): List<Channel>
    
    // 获取频道详情
    fun getChannelById(id: String): Channel?
    
    // 更新频道源
    suspend fun updateSources(): Result<Unit>
    
    // 添加自定义源
    fun addCustomSource(url: String)
    
    // 移除源
    fun removeSource(url: String)
    
    // 清除缓存
    fun clearCache()
}
```

### 3.2 M3U 解析器

```kotlin
class M3UParser {
    /**
     * 解析 M3U 文件内容
     */
    fun parse(content: String): List<Channel> {
        val channels = mutableListOf<Channel>()
        val lines = content.lines()
        var currentCategory = ""
        var currentChannelInfo: ChannelInfo? = null
        
        for (line in lines) {
            when {
                line.startsWith("#EXTM3U") -> continue
                line.startsWith("#genre#") -> {
                    currentCategory = line.substringBefore(",")
                }
                line.startsWith("#EXTINF:") -> {
                    currentChannelInfo = parseExtInf(line)
                }
                line.isNotBlank() && !line.startsWith("#") -> {
                    currentChannelInfo?.let { info ->
                        val channel = createChannel(
                            info, 
                            line.trim(), 
                            currentCategory
                        )
                        channels.add(channel)
                    }
                    currentChannelInfo = null
                }
            }
        }
        
        return mergeChannelSources(channels)
    }
    
    /**
     * 解析 #EXTINF 行
     */
    private fun parseExtInf(line: String): ChannelInfo {
        // 解析 tvg-id, tvg-name, tvg-logo, group-title 等属性
        val attributes = parseAttributes(line)
        val name = line.substringAfterLast(",").trim()
        
        return ChannelInfo(
            name = name,
            tvgId = attributes["tvg-id"],
            tvgName = attributes["tvg-name"],
            tvgLogo = attributes["tvg-logo"],
            groupTitle = attributes["group-title"]
        )
    }
    
    /**
     * 合并同名频道的多个源
     */
    private fun mergeChannelSources(channels: List<Channel>): List<Channel> {
        return channels.groupBy { it.name }
            .map { (name, channelList) ->
                channelList.first().copy(
                    sources = channelList.flatMap { it.sources }
                )
            }
    }
}
```

### 3.3 源管理器

```kotlin
class ChannelSourceManager(
    private val context: Context
) {
    private val remoteLoader = RemoteSourceLoader()
    private val localLoader = LocalSourceLoader(context)
    private val cache = SourceCache(context)
    
    /**
     * 加载所有源
     */
    suspend fun loadSources(sourceUrls: List<String>): Result<String> {
        val results = mutableListOf<String>()
        
        // 1. 加载远程源
        sourceUrls.forEach { url ->
            remoteLoader.load(url).onSuccess { content ->
                results.add(content)
            }
        }
        
        // 2. 加载本地源
        localLoader.load().onSuccess { content ->
            results.add(content)
        }
        
        // 3. 如果远程加载失败，使用缓存
        if (results.isEmpty()) {
            cache.get().onSuccess { content ->
                results.add(content)
            }
        } else {
            // 缓存最新结果
            cache.save(results.joinToString("\n"))
        }
        
        return if (results.isNotEmpty()) {
            Result.success(results.joinToString("\n"))
        } else {
            Result.failure(Exception("No sources available"))
        }
    }
}
```

### 3.4 本地 API 接口

```kotlin
interface ChannelApi {
    /**
     * 获取所有频道（JSON 格式）
     */
    fun getChannelsJson(): String
    
    /**
     * 获取分组列表
     */
    fun getGroups(): List<String>
    
    /**
     * 按分组获取频道
     */
    fun getChannelsByGroup(group: String): String
    
    /**
     * 获取 M3U 格式播放列表
     */
    fun getM3UPlaylist(): String
}

class ChannelApiImpl(
    private val channelManager: ChannelManager
) : ChannelApi {
    private val gson = Gson()
    
    override fun getChannelsJson(): String {
        val channels = channelManager.getAllChannels()
        return gson.toJson(channels)
    }
    
    override fun getGroups(): List<String> {
        return channelManager.getAllChannels()
            .map { it.category }
            .distinct()
            .sorted()
    }
    
    override fun getChannelsByGroup(group: String): String {
        val channels = channelManager.getChannelsByCategory(group)
        return gson.toJson(channels)
    }
    
    override fun getM3UPlaylist(): String {
        val channels = channelManager.getAllChannels()
        return M3UWriter().write(channels)
    }
}
```

---

## 4. 数据流程

### 4.1 初始化流程

```
App 启动
    ↓
ChannelManager.initialize(context)
    ↓
加载配置（源 URL 列表）
    ↓
ChannelSourceManager.loadSources()
    ↓
并行加载：远程源 + 本地源
    ↓
如果失败 → 加载缓存
    ↓
M3UParser.parse(content)
    ↓
生成 Channel 列表
    ↓
应用过滤器（黑名单/白名单/去重）
    ↓
保存到内存 + 缓存到本地
    ↓
通知 UI 更新
```

### 4.2 播放流程

```
用户选择频道
    ↓
ChannelManager.getChannelById(id)
    ↓
获取 Channel.sources（多个线路）
    ↓
按优先级排序（历史成功 > 测速结果 > 默认）
    ↓
尝试播放第一条线路
    ↓
播放失败 → 切换下一条线路
    ↓
记录成功线路，下次优先使用
```

### 4.3 更新流程

```
定时任务 / 手动触发
    ↓
ChannelManager.updateSources()
    ↓
重新加载远程源
    ↓
解析 M3U
    ↓
与现有频道列表合并
    ↓
保留历史成功线路
    ↓
更新缓存
    ↓
通知 UI 刷新
```

---

## 5. 配置管理

### 5.1 配置文件

```kotlin
data class ChannelConfig(
    val sources: List<String>,         // 源 URL 列表
    val updateInterval: Long,          // 更新间隔（小时）
    val enableAutoUpdate: Boolean,     // 自动更新
    val enableSpeedTest: Boolean,      // 启用测速
    val enableCache: Boolean,          // 启用缓存
    val cacheExpiration: Long,         // 缓存过期时间
    val blacklist: List<String>,       // 黑名单关键词
    val whitelist: List<String>,       // 白名单关键词
    val maxSourcesPerChannel: Int,     // 单频道最大线路数
    val preferredProtocol: Protocol?,  // 首选协议
    val preferredIpvType: IpvType?     // 首选 IP 类型
)
```

### 5.2 默认配置

```kotlin
object DefaultConfig {
    val DEFAULT_SOURCES = listOf(
        "https://raw.githubusercontent.com/Guovin/iptv-api/main/output/result.m3u",
        "https://iptv-org.github.io/iptv/index.m3u"
    )
    
    const val DEFAULT_UPDATE_INTERVAL = 24L // 24 小时
    const val DEFAULT_CACHE_EXPIRATION = 7 * 24 * 60 * 60 * 1000L // 7 天
    const val DEFAULT_MAX_SOURCES_PER_CHANNEL = 10
}
```

---

## 6. 优势分析

### 6.1 性能优势

| 对比项 | 外部 API 方式 | ChannelManager 方式 |
|--------|--------------|-------------------|
| **网络依赖** | 每次调用需要网络 | 仅更新时需要网络 |
| **响应速度** | 100-500ms | < 10ms（内存读取） |
| **离线可用** | ❌ 不可用 | ✅ 使用缓存 |
| **并发性能** | 受后端限制 | 无限制 |
| **数据一致性** | 依赖网络稳定性 | 高度一致 |

### 6.2 功能优势

- ✅ **完全离线**: 首次加载后可完全离线使用
- ✅ **即时响应**: 频道列表、搜索、过滤等操作毫秒级响应
- ✅ **灵活扩展**: 易于添加新功能（测速、EPG、收藏等）
- ✅ **统一管理**: 所有频道数据在一个模块中管理
- ✅ **易于测试**: 纯 Kotlin 代码，易于单元测试

### 6.3 维护优势

- ✅ **无需后端**: 不依赖 iptv-api 后端服务
- ✅ **代码统一**: 全部使用 Kotlin，无需维护 Python 代码
- ✅ **版本控制**: 与 App 代码一起版本管理
- ✅ **调试方便**: 可直接在 Android Studio 中调试

---

## 7. 迁移计划

### 阶段 1: 创建 ChannelManager 模块 ✅
- 创建包结构
- 实现数据模型
- 实现 M3U 解析器

### 阶段 2: 实现源管理 ✅
- 远程源加载
- 本地源加载
- 缓存机制

### 阶段 3: 重构现有代码
- 替换 ChannelRepository 中的 HTTP 调用
- 使用 ChannelManager 替代
- 更新 ViewModel

### 阶段 4: 测试与优化
- 单元测试
- 集成测试
- 性能优化

### 阶段 5: 合并到主干
- 代码审查
- 文档更新
- 合并 PR

---

## 8. 兼容性保证

为了平滑迁移，ChannelManager 将保持与现有 `Channel` 数据模型的兼容性：

```kotlin
// 现有模型
@Entity(tableName = "channels")
data class Channel(
    @PrimaryKey val id: String,
    val name: String,
    val category: String,
    val logo: String?,
    val urls: List<String>,
    val lastPlayedUrl: String?,
    val isFavorite: Boolean = false
)

// 新模型可以轻松转换
fun com.orca.tv.channel.model.Channel.toEntity(): com.orca.tv.data.Channel {
    return com.orca.tv.data.Channel(
        id = this.id,
        name = this.displayName,
        category = this.category,
        logo = this.logo,
        urls = this.sources.map { it.url },
        lastPlayedUrl = this.sources.firstOrNull()?.url,
        isFavorite = false
    )
}
```

---

## 9. 风险与挑战

### 风险

1. **M3U 格式多样性**: 不同源的 M3U 格式可能不统一
2. **性能问题**: 大量频道（1000+）的解析和管理
3. **内存占用**: 所有频道数据常驻内存

### 应对措施

1. **格式兼容**: 实现灵活的解析器，支持多种格式
2. **懒加载**: 按需加载频道详情
3. **内存优化**: 使用弱引用、LRU 缓存等策略

---

## 10. 总结

ChannelManager 模块将 iptv-api 的核心功能完全集成到 Orca-tv 中，实现了：

- ✅ **本地化**: 不再依赖外部 HTTP API
- ✅ **高性能**: 毫秒级响应速度
- ✅ **离线可用**: 支持完全离线使用
- ✅ **易维护**: 纯 Kotlin 代码，统一技术栈
- ✅ **可扩展**: 易于添加新功能

这是一个架构升级，将显著提升 Orca-tv 的用户体验和开发效率。
