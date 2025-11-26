package com.orca.tv.channel.api

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.orca.tv.channel.ChannelManager
import com.orca.tv.channel.model.Channel
import com.orca.tv.channel.model.ChannelGroup
import com.orca.tv.channel.parser.M3UWriter

/**
 * 本地 API 接口
 * 提供类似 REST API 的接口，但完全在本地运行
 */
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
     * 按分组获取频道（JSON 格式）
     */
    fun getChannelsByGroup(group: String): String
    
    /**
     * 获取 M3U 格式播放列表
     */
    fun getM3UPlaylist(): String
    
    /**
     * 搜索频道（JSON 格式）
     */
    fun searchChannels(query: String): String
    
    /**
     * 获取统计信息（JSON 格式）
     */
    fun getStats(): String
}

/**
 * 本地 API 实现
 */
class ChannelApiImpl : ChannelApi {
    
    private val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .create()
    
    private val m3uWriter = M3UWriter()
    
    override fun getChannelsJson(): String {
        val channels = ChannelManager.getAllChannels()
        return gson.toJson(mapOf(
            "total" to channels.size,
            "channels" to channels
        ))
    }
    
    override fun getGroups(): List<String> {
        return ChannelManager.getCategories()
    }
    
    override fun getChannelsByGroup(group: String): String {
        val channels = ChannelManager.getChannelsByCategory(group)
        return gson.toJson(mapOf(
            "group" to group,
            "total" to channels.size,
            "channels" to channels
        ))
    }
    
    override fun getM3UPlaylist(): String {
        val channels = ChannelManager.getAllChannels()
        return m3uWriter.write(channels)
    }
    
    override fun searchChannels(query: String): String {
        val channels = ChannelManager.searchChannels(query)
        return gson.toJson(mapOf(
            "query" to query,
            "total" to channels.size,
            "channels" to channels
        ))
    }
    
    override fun getStats(): String {
        val channelCount = ChannelManager.getChannelCount()
        val sourceCount = ChannelManager.getSourceCount()
        val categories = ChannelManager.getCategories()
        val cacheInfo = ChannelManager.getCacheInfo()
        
        return gson.toJson(mapOf(
            "channel_count" to channelCount,
            "source_count" to sourceCount,
            "category_count" to categories.size,
            "categories" to categories,
            "cache" to mapOf(
                "is_valid" to cacheInfo.isValid,
                "size_bytes" to cacheInfo.size,
                "last_modified" to cacheInfo.lastModified
            )
        ))
    }
}

/**
 * API 响应包装器
 */
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val error: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
