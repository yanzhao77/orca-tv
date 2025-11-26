package com.orca.tv.ui.browse

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.orca.tv.channel.ChannelManager
import com.orca.tv.data.Channel
import com.orca.tv.data.ChannelRepositoryV2
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * 频道列表 ViewModel V2
 * 使用 ChannelManager 实现本地化频道管理
 */
class ChannelViewModelV2(application: Application) : AndroidViewModel(application) {
    
    private val repository = ChannelRepositoryV2(application)
    
    private val _channels = MutableLiveData<List<Channel>>()
    val channels: LiveData<List<Channel>> = _channels
    
    private val _errorState = MutableLiveData<String>()
    val errorState: LiveData<String> = _errorState
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    init {
        // 初始化 ChannelManager
        repository.initialize()
        
        // 监听 ChannelManager 的状态变化
        observeChannelManager()
        
        // 自动加载频道列表
        loadChannels()
    }
    
    /**
     * 监听 ChannelManager 的状态变化
     */
    private fun observeChannelManager() {
        viewModelScope.launch {
            // 监听频道列表变化
            ChannelManager.channels.collectLatest { channels ->
                _channels.value = channels.map { it.toEntity() }
            }
        }
        
        viewModelScope.launch {
            // 监听加载状态
            ChannelManager.isLoading.collectLatest { isLoading ->
                _isLoading.value = isLoading
            }
        }
        
        viewModelScope.launch {
            // 监听错误信息
            ChannelManager.error.collectLatest { error ->
                _errorState.value = error ?: ""
            }
        }
    }
    
    /**
     * 加载频道列表
     * @param forceRefresh 是否强制刷新
     */
    fun loadChannels(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            val result = repository.loadChannels(forceRefresh)
            
            result.onSuccess { channels ->
                _channels.value = channels
                _errorState.value = ""
            }
            
            result.onFailure { exception ->
                _errorState.value = exception.message ?: "加载失败"
            }
        }
    }
    
    /**
     * 刷新频道列表
     */
    fun refreshChannels() {
        loadChannels(forceRefresh = true)
    }
    
    /**
     * 按分类获取频道
     */
    fun getChannelsByCategory(category: String): List<Channel> {
        return repository.getChannelsByCategory(category)
    }
    
    /**
     * 获取所有分类
     */
    fun getCategories(): List<String> {
        return repository.getCategories()
    }
    
    /**
     * 搜索频道
     */
    fun searchChannels(query: String): List<Channel> {
        return repository.searchChannels(query)
    }
    
    /**
     * 添加自定义源
     */
    fun addCustomSource(url: String) {
        repository.addCustomSource(url)
        // 添加后自动刷新
        refreshChannels()
    }
    
    /**
     * 获取源列表
     */
    fun getSourceUrls(): List<String> {
        return repository.getSourceUrls()
    }
    
    /**
     * 清除缓存
     */
    fun clearCache() {
        repository.clearCache()
    }
    
    /**
     * 获取统计信息
     */
    fun getStats(): String {
        val stats = repository.getStats()
        return """
            频道数量: ${stats.channelCount}
            线路数量: ${stats.sourceCount}
            分类数量: ${stats.categoryCount}
            缓存状态: ${if (stats.cacheInfo.isValid) "有效" else "无效"}
        """.trimIndent()
    }
}
