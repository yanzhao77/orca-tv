package com.orca.tv.channel

import android.content.Context
import android.content.SharedPreferences
import com.orca.tv.channel.cache.SourceCache
import com.orca.tv.channel.model.Channel
import com.orca.tv.channel.model.ChannelGroup
import com.orca.tv.channel.parser.M3UParser
import com.orca.tv.channel.source.ChannelSourceManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 频道管理器（单例）
 * 核心频道管理模块，负责加载、更新和管理所有频道数据
 */
object ChannelManager {
    
    private const val TAG = "ChannelManager"
    private const val PREFS_NAME = "channel_manager_prefs"
    private const val KEY_SOURCE_URLS = "source_urls"
    private const val DEFAULT_SOURCE_URL = "https://raw.githubusercontent.com/Guovin/iptv-api/main/output/result.m3u"
    
    private lateinit var context: Context
    private lateinit var prefs: SharedPreferences
    private lateinit var sourceManager: ChannelSourceManager
    private lateinit var parser: M3UParser
    private lateinit var cache: SourceCache
    
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    // 频道数据
    private val _channels = MutableStateFlow<List<Channel>>(emptyList())
    val channels: StateFlow<List<Channel>> = _channels.asStateFlow()
    
    // 加载状态
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    // 错误信息
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    /**
     * 初始化
     */
    fun initialize(context: Context) {
        this.context = context.applicationContext
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        this.sourceManager = ChannelSourceManager(context)
        this.parser = M3UParser()
        this.cache = SourceCache(context)
    }
    
    /**
     * 加载频道列表
     */
    suspend fun loadChannels(forceRefresh: Boolean = false): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            _isLoading.value = true
            _error.value = null
            
            // 如果不强制刷新且缓存有效，直接使用缓存
            if (!forceRefresh && cache.isValid()) {
                cache.get().onSuccess { content ->
                    val parsedChannels = parser.parse(content)
                    _channels.value = parsedChannels
                    return@withContext Result.success(Unit)
                }
            }
            
            // 加载源
            val sourceUrls = getSourceUrls()
            val result = sourceManager.loadSources(sourceUrls)
            
            return@withContext result.mapCatching { content ->
                // 解析 M3U
                val parsedChannels = parser.parse(content)
                _channels.value = parsedChannels
            }.onFailure { exception ->
                _error.value = exception.message
            }
        } catch (e: Exception) {
            _error.value = e.message
            return@withContext Result.failure(e)
        } finally {
            _isLoading.value = false
        }
    }
    
    /**
     * 更新频道源
     */
    suspend fun updateSources(): Result<Unit> {
        return loadChannels(forceRefresh = true)
    }
    
    /**
     * 获取所有频道
     */
    fun getAllChannels(): List<Channel> {
        return _channels.value
    }
    
    /**
     * 按分类获取频道
     */
    fun getChannelsByCategory(category: String): List<Channel> {
        return _channels.value.filter { it.category == category }
    }
    
    /**
     * 获取所有分类
     */
    fun getCategories(): List<String> {
        return _channels.value.map { it.category }.distinct().sorted()
    }
    
    /**
     * 获取分组频道
     */
    fun getChannelGroups(): List<ChannelGroup> {
        return _channels.value.groupBy { it.category }
            .map { (category, channels) ->
                ChannelGroup(category, channels)
            }
            .sortedBy { it.name }
    }
    
    /**
     * 搜索频道
     */
    fun searchChannels(query: String): List<Channel> {
        if (query.isBlank()) {
            return _channels.value
        }
        
        val lowerQuery = query.lowercase()
        return _channels.value.filter { channel ->
            channel.name.lowercase().contains(lowerQuery) ||
            channel.displayName.lowercase().contains(lowerQuery) ||
            channel.category.lowercase().contains(lowerQuery)
        }
    }
    
    /**
     * 获取频道详情
     */
    fun getChannelById(id: String): Channel? {
        return _channels.value.firstOrNull { it.id == id }
    }
    
    /**
     * 获取频道数量
     */
    fun getChannelCount(): Int {
        return _channels.value.size
    }
    
    /**
     * 获取源数量
     */
    fun getSourceCount(): Int {
        return _channels.value.sumOf { it.sources.size }
    }
    
    /**
     * 添加自定义源 URL
     */
    fun addSourceUrl(url: String) {
        val urls = getSourceUrls().toMutableList()
        if (!urls.contains(url)) {
            urls.add(url)
            saveSourceUrls(urls)
        }
    }
    
    /**
     * 移除源 URL
     */
    fun removeSourceUrl(url: String) {
        val urls = getSourceUrls().toMutableList()
        urls.remove(url)
        saveSourceUrls(urls)
    }
    
    /**
     * 获取源 URL 列表
     */
    fun getSourceUrls(): List<String> {
        val urlsString = prefs.getString(KEY_SOURCE_URLS, null)
        return if (urlsString != null) {
            urlsString.split(",").filter { it.isNotBlank() }
        } else {
            listOf(DEFAULT_SOURCE_URL)
        }
    }
    
    /**
     * 保存源 URL 列表
     */
    private fun saveSourceUrls(urls: List<String>) {
        prefs.edit().putString(KEY_SOURCE_URLS, urls.joinToString(",")).apply()
    }
    
    /**
     * 清除缓存
     */
    fun clearCache() {
        cache.clear()
    }
    
    /**
     * 获取缓存信息
     */
    fun getCacheInfo(): CacheInfo {
        return CacheInfo(
            isValid = cache.isValid(),
            size = cache.getSize(),
            lastModified = cache.getLastModified()
        )
    }
}

/**
 * 缓存信息
 */
data class CacheInfo(
    val isValid: Boolean,
    val size: Long,
    val lastModified: Long
)
