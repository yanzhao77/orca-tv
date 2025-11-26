package com.orca.tv.channel.cache

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/**
 * 源缓存管理器
 * 负责缓存频道源数据
 */
class SourceCache(
    private val context: Context
) {
    companion object {
        private const val TAG = "SourceCache"
        private const val CACHE_FILE_NAME = "channels_cache.m3u"
        private const val CACHE_EXPIRATION = 7 * 24 * 60 * 60 * 1000L // 7 天
    }
    
    private val cacheFile: File
        get() = context.cacheDir.resolve(CACHE_FILE_NAME)
    
    /**
     * 保存缓存
     */
    suspend fun save(content: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            cacheFile.writeText(content)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 获取缓存
     */
    suspend fun get(): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (!cacheFile.exists()) {
                return@withContext Result.failure(IOException("Cache file not found"))
            }
            
            // 检查缓存是否过期
            val lastModified = cacheFile.lastModified()
            val now = System.currentTimeMillis()
            if (now - lastModified > CACHE_EXPIRATION) {
                return@withContext Result.failure(IOException("Cache expired"))
            }
            
            val content = cacheFile.readText()
            Result.success(content)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 清除缓存
     */
    fun clear() {
        try {
            if (cacheFile.exists()) {
                cacheFile.delete()
            }
        } catch (e: Exception) {
            // Ignore
        }
    }
    
    /**
     * 检查缓存是否存在且有效
     */
    fun isValid(): Boolean {
        if (!cacheFile.exists()) {
            return false
        }
        
        val lastModified = cacheFile.lastModified()
        val now = System.currentTimeMillis()
        return (now - lastModified) <= CACHE_EXPIRATION
    }
    
    /**
     * 获取缓存大小（字节）
     */
    fun getSize(): Long {
        return if (cacheFile.exists()) cacheFile.length() else 0
    }
    
    /**
     * 获取缓存最后修改时间
     */
    fun getLastModified(): Long {
        return if (cacheFile.exists()) cacheFile.lastModified() else 0
    }
}
