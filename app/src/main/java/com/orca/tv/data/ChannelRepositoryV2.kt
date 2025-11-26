package com.orca.tv.data

import android.content.Context
import android.util.Log
import com.orca.tv.channel.ChannelManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 频道数据仓库 V2
 * 使用 ChannelManager 替代原有的 HTTP API 调用
 * 实现完全本地化的频道管理
 */
class ChannelRepositoryV2(private val context: Context) {
    
    private val channelDao = ChannelDatabase.getDatabase(context).channelDao()
    
    companion object {
        private const val TAG = "ChannelRepositoryV2"
    }
    
    /**
     * 初始化 ChannelManager
     */
    fun initialize() {
        ChannelManager.initialize(context)
    }
    
    /**
     * 加载频道列表
     * 使用 ChannelManager 的三级回退机制：远程源 -> 本地源 -> 缓存
     */
    suspend fun loadChannels(forceRefresh: Boolean = false): Result<List<Channel>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "开始加载频道列表（forceRefresh=$forceRefresh）")
            
            // 使用 ChannelManager 加载
            val result = ChannelManager.loadChannels(forceRefresh)
            
            result.onSuccess {
                // 转换为旧版 Channel 实体并保存到数据库
                val channels = ChannelManager.getAllChannels().map { it.toEntity() }
                saveToDatabase(channels)
                
                Log.d(TAG, "加载成功，共 ${channels.size} 个频道")
                return@withContext Result.success(channels)
            }
            
            result.onFailure { exception ->
                Log.e(TAG, "加载失败: ${exception.message}", exception)
                
                // 尝试从数据库加载
                val cachedChannels = loadFromDatabase()
                if (cachedChannels.isNotEmpty()) {
                    Log.d(TAG, "从数据库加载，共 ${cachedChannels.size} 个频道")
                    return@withContext Result.success(cachedChannels)
                }
                
                return@withContext Result.failure(exception)
            }
            
            Result.failure(Exception("Unknown error"))
            
        } catch (e: Exception) {
            Log.e(TAG, "加载频道列表异常: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * 从数据库加载频道列表
     */
    private suspend fun loadFromDatabase(): List<Channel> = withContext(Dispatchers.IO) {
        try {
            channelDao.getAllChannels()
        } catch (e: Exception) {
            Log.e(TAG, "从数据库加载失败: ${e.message}", e)
            emptyList()
        }
    }
    
    /**
     * 保存频道列表到数据库
     */
    private suspend fun saveToDatabase(channels: List<Channel>) {
        try {
            channelDao.deleteAll()
            channelDao.insertAll(channels)
            Log.d(TAG, "保存到数据库成功")
        } catch (e: Exception) {
            Log.e(TAG, "保存到数据库失败: ${e.message}", e)
        }
    }
    
    /**
     * 更新频道信息（如收藏状态、播放记录）
     */
    suspend fun updateChannel(channel: Channel) = withContext(Dispatchers.IO) {
        try {
            channelDao.update(channel)
        } catch (e: Exception) {
            Log.e(TAG, "更新频道失败: ${e.message}", e)
        }
    }
    
    /**
     * 获取收藏的频道
     */
    suspend fun getFavoriteChannels(): List<Channel> = withContext(Dispatchers.IO) {
        channelDao.getFavoriteChannels()
    }
    
    /**
     * 搜索频道
     */
    fun searchChannels(query: String): List<Channel> {
        return ChannelManager.searchChannels(query).map { it.toEntity() }
    }
    
    /**
     * 按分类获取频道
     */
    fun getChannelsByCategory(category: String): List<Channel> {
        return ChannelManager.getChannelsByCategory(category).map { it.toEntity() }
    }
    
    /**
     * 获取所有分类
     */
    fun getCategories(): List<String> {
        return ChannelManager.getCategories()
    }
    
    /**
     * 添加自定义源
     */
    fun addCustomSource(url: String) {
        ChannelManager.addSourceUrl(url)
    }
    
    /**
     * 获取源列表
     */
    fun getSourceUrls(): List<String> {
        return ChannelManager.getSourceUrls()
    }
    
    /**
     * 清除缓存
     */
    fun clearCache() {
        ChannelManager.clearCache()
    }
    
    /**
     * 获取统计信息
     */
    fun getStats(): ChannelStats {
        return ChannelStats(
            channelCount = ChannelManager.getChannelCount(),
            sourceCount = ChannelManager.getSourceCount(),
            categoryCount = ChannelManager.getCategories().size,
            cacheInfo = ChannelManager.getCacheInfo()
        )
    }
}

/**
 * 频道统计信息
 */
data class ChannelStats(
    val channelCount: Int,
    val sourceCount: Int,
    val categoryCount: Int,
    val cacheInfo: com.orca.tv.channel.CacheInfo
)
