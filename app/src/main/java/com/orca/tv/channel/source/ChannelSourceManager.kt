package com.orca.tv.channel.source

import android.content.Context
import com.orca.tv.channel.cache.SourceCache
import kotlinx.coroutines.*
import java.io.IOException

/**
 * 频道源管理器
 * 负责加载、更新和管理频道源
 */
class ChannelSourceManager(
    private val context: Context
) {
    private val remoteLoader = RemoteSourceLoader()
    private val localLoader = LocalSourceLoader(context)
    private val cache = SourceCache(context)
    
    companion object {
        private const val TAG = "ChannelSourceManager"
    }
    
    /**
     * 加载所有源
     * 优先级：远程源 > 本地源 > 缓存
     */
    suspend fun loadSources(sourceUrls: List<String>): Result<String> = withContext(Dispatchers.IO) {
        try {
            val results = mutableListOf<String>()
            
            // 1. 并行加载远程源
            if (sourceUrls.isNotEmpty()) {
                val remoteResults = sourceUrls.map { url ->
                    async {
                        remoteLoader.load(url).getOrNull()
                    }
                }.awaitAll().filterNotNull()
                
                results.addAll(remoteResults)
            }
            
            // 2. 加载本地源
            localLoader.load().onSuccess { content ->
                if (content.isNotBlank()) {
                    results.add(content)
                }
            }
            
            // 3. 如果远程和本地都失败，使用缓存
            if (results.isEmpty()) {
                cache.get().onSuccess { content ->
                    if (content.isNotBlank()) {
                        results.add(content)
                    }
                }
            } else {
                // 缓存最新结果
                val combined = results.joinToString("\n\n")
                cache.save(combined)
            }
            
            if (results.isNotEmpty()) {
                Result.success(results.joinToString("\n\n"))
            } else {
                Result.failure(IOException("No sources available"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 仅加载远程源
     */
    suspend fun loadRemoteSources(sourceUrls: List<String>): Result<String> = withContext(Dispatchers.IO) {
        try {
            val results = sourceUrls.map { url ->
                async {
                    remoteLoader.load(url).getOrNull()
                }
            }.awaitAll().filterNotNull()
            
            if (results.isNotEmpty()) {
                Result.success(results.joinToString("\n\n"))
            } else {
                Result.failure(IOException("Failed to load remote sources"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 仅加载本地源
     */
    suspend fun loadLocalSource(): Result<String> = withContext(Dispatchers.IO) {
        localLoader.load()
    }
    
    /**
     * 仅加载缓存
     */
    suspend fun loadCachedSource(): Result<String> = withContext(Dispatchers.IO) {
        cache.get()
    }
    
    /**
     * 清除缓存
     */
    fun clearCache() {
        cache.clear()
    }
}
