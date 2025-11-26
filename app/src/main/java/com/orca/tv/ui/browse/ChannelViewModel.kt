package com.orca.tv.ui.browse

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.orca.tv.data.Channel
import com.orca.tv.data.ChannelRepository
import kotlinx.coroutines.launch

/**
 * 频道列表 ViewModel
 * 实现三级回退加载逻辑
 */
class ChannelViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository = ChannelRepository(application)
    
    private val _channels = MutableLiveData<List<Channel>>()
    val channels: LiveData<List<Channel>> = _channels
    
    private val _errorState = MutableLiveData<String>()
    val errorState: LiveData<String> = _errorState
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    /**
     * 加载频道列表
     * @param isRefresh 是否为手动刷新（手动刷新时不走回退逻辑）
     */
    fun loadChannels(isRefresh: Boolean = false) {
        viewModelScope.launch {
            _isLoading.value = true
            
            // 1. 尝试从后端 API 加载
            val remoteResult = repository.loadFromRemote()
            if (remoteResult.isSuccess) {
                _channels.value = remoteResult.getOrNull()
                _errorState.value = ""
                _isLoading.value = false
                return@launch
            }
            
            // 如果是手动刷新，则不走后续回退，直接提示错误
            if (isRefresh) {
                _errorState.value = "刷新失败，请检查网络或后端服务。"
                _isLoading.value = false
                return@launch
            }
            
            // 2. 尝试从本地缓存加载
            val cacheResult = repository.loadFromCache()
            if (cacheResult.isSuccess && cacheResult.getOrNull()?.isNotEmpty() == true) {
                _channels.value = cacheResult.getOrNull()
                _errorState.value = "网络连接失败，已加载本地缓存。" // 温馨提示
                _isLoading.value = false
                return@launch
            }
            
            // 3. 加载内置默认列表
            val defaultResult = repository.loadFromDefault()
            if (defaultResult.isSuccess) {
                _channels.value = defaultResult.getOrNull()
                _errorState.value = "首次加载，请在设置中配置您的后端服务地址。" // 引导提示
            } else {
                _errorState.value = "发生未知错误，无法加载任何频道。"
            }
            
            _isLoading.value = false
        }
    }
    
    /**
     * 按分类获取频道
     */
    fun getChannelsByCategory(category: String): List<Channel> {
        return _channels.value?.filter { it.category == category } ?: emptyList()
    }
    
    /**
     * 获取所有分类
     */
    fun getAllCategories(): List<String> {
        return _channels.value?.map { it.category }?.distinct() ?: emptyList()
    }
    
    /**
     * 切换收藏状态
     */
    fun toggleFavorite(channel: Channel) {
        viewModelScope.launch {
            val updatedChannel = channel.copy(isFavorite = !channel.isFavorite)
            repository.updateChannel(updatedChannel)
            
            // 更新本地列表
            val updatedList = _channels.value?.map {
                if (it.id == channel.id) updatedChannel else it
            }
            _channels.value = updatedList ?: emptyList()
        }
    }
}
