package com.orca.tv.channel.source

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * 本地源加载器
 * 负责加载 assets 中的本地 M3U 文件
 */
class LocalSourceLoader(
    private val context: Context
) {
    companion object {
        private const val TAG = "LocalSourceLoader"
        private const val DEFAULT_CHANNELS_FILE = "default_channels.m3u"
        private const val CUSTOM_CHANNELS_FILE = "custom_channels.m3u"
    }
    
    /**
     * 加载本地源
     */
    suspend fun load(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val results = mutableListOf<String>()
            
            // 1. 加载默认频道列表
            loadFromAssets(DEFAULT_CHANNELS_FILE).onSuccess { content ->
                results.add(content)
            }
            
            // 2. 加载自定义频道列表（如果存在）
            loadFromInternalStorage(CUSTOM_CHANNELS_FILE).onSuccess { content ->
                results.add(content)
            }
            
            if (results.isNotEmpty()) {
                Result.success(results.joinToString("\n\n"))
            } else {
                Result.failure(IOException("No local sources found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 从 assets 加载文件
     */
    private fun loadFromAssets(fileName: String): Result<String> {
        return try {
            val content = context.assets.open(fileName).bufferedReader().use { it.readText() }
            Result.success(content)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 从内部存储加载文件
     */
    private fun loadFromInternalStorage(fileName: String): Result<String> {
        return try {
            val file = context.filesDir.resolve(fileName)
            if (file.exists()) {
                val content = file.readText()
                Result.success(content)
            } else {
                Result.failure(IOException("File not found: $fileName"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 保存自定义频道列表
     */
    suspend fun saveCustomChannels(content: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val file = context.filesDir.resolve(CUSTOM_CHANNELS_FILE)
            file.writeText(content)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
