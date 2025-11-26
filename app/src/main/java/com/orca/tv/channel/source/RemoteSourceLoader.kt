package com.orca.tv.channel.source

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * 远程源加载器
 * 负责从远程 URL 下载 M3U 文件
 */
class RemoteSourceLoader {
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    companion object {
        private const val TAG = "RemoteSourceLoader"
    }
    
    /**
     * 加载远程源
     */
    suspend fun load(url: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(url)
                .get()
                .build()
            
            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                val content = response.body?.string()
                if (content != null) {
                    Result.success(content)
                } else {
                    Result.failure(IOException("Empty response body"))
                }
            } else {
                Result.failure(IOException("HTTP ${response.code}: ${response.message}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 批量加载远程源
     */
    suspend fun loadMultiple(urls: List<String>): List<Result<String>> = withContext(Dispatchers.IO) {
        urls.map { url -> load(url) }
    }
}
