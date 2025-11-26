package com.orca.tv.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

/**
 * 频道数据仓库
 * 实现三级回退机制：API -> 本地缓存 -> 内置默认源
 */
class ChannelRepository(private val context: Context) {
    
    private val channelDao = ChannelDatabase.getDatabase(context).channelDao()
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    
    companion object {
        private const val TAG = "ChannelRepository"
        private const val DEFAULT_M3U_FILE = "default_channels.m3u"
    }
    
    /**
     * 从远程 API 加载频道列表
     */
    suspend fun loadFromRemote(): Result<List<Channel>> = withContext(Dispatchers.IO) {
        try {
            val apiUrl = getApiUrl()
            if (apiUrl.isEmpty()) {
                return@withContext Result.failure(Exception("未配置后端 API 地址"))
            }
            
            Log.d(TAG, "从远程 API 加载: $apiUrl")
            
            val request = Request.Builder()
                .url(apiUrl)
                .build()
            
            val response = httpClient.newCall(request).execute()
            
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("HTTP ${response.code}"))
            }
            
            val m3uContent = response.body?.string() ?: ""
            val channels = M3UParser.parse(m3uContent)
            
            // 保存到本地缓存
            saveToCache(channels)
            
            Log.d(TAG, "从远程加载成功，共 ${channels.size} 个频道")
            Result.success(channels)
            
        } catch (e: Exception) {
            Log.e(TAG, "从远程加载失败: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * 从本地缓存加载频道列表
     */
    suspend fun loadFromCache(): Result<List<Channel>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "从本地缓存加载")
            val channels = channelDao.getAllChannels()
            
            if (channels.isEmpty()) {
                return@withContext Result.failure(Exception("缓存为空"))
            }
            
            Log.d(TAG, "从缓存加载成功，共 ${channels.size} 个频道")
            Result.success(channels)
            
        } catch (e: Exception) {
            Log.e(TAG, "从缓存加载失败: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * 加载内置的默认频道列表
     */
    suspend fun loadFromDefault(): Result<List<Channel>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "加载内置默认频道列表")
            
            val inputStream = context.assets.open(DEFAULT_M3U_FILE)
            val reader = BufferedReader(InputStreamReader(inputStream))
            val m3uContent = reader.readText()
            reader.close()
            
            val channels = M3UParser.parse(m3uContent)
            
            // 保存到缓存
            saveToCache(channels)
            
            Log.d(TAG, "加载默认列表成功，共 ${channels.size} 个频道")
            Result.success(channels)
            
        } catch (e: Exception) {
            Log.e(TAG, "加载默认列表失败: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * 保存频道列表到本地缓存
     */
    private suspend fun saveToCache(channels: List<Channel>) {
        try {
            channelDao.deleteAll()
            channelDao.insertAll(channels)
            Log.d(TAG, "保存到缓存成功")
        } catch (e: Exception) {
            Log.e(TAG, "保存到缓存失败: ${e.message}", e)
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
     * 获取 API 地址（从 SharedPreferences 读取）
     */
    private fun getApiUrl(): String {
        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        return prefs.getString("api_url", "") ?: ""
    }
}
