package com.orca.tv.ui.browse

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import com.orca.tv.R

/**
 * 主浏览界面 Activity
 * 使用 Leanback BrowseFragment 展示频道列表
 */
class BrowseActivity : FragmentActivity() {
    
    private lateinit var viewModel: ChannelViewModel
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_browse)
        
        // 初始化 ViewModel
        viewModel = ViewModelProvider(this)[ChannelViewModel::class.java]
        
        // 观察数据变化
        observeViewModel()
        
        // 加载频道列表
        viewModel.loadChannels()
        
        // 添加 BrowseFragment
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.browse_fragment_container, BrowseFragment())
                .commit()
        }
    }
    
    private fun observeViewModel() {
        // 观察错误状态
        viewModel.errorState.observe(this) { error ->
            if (error.isNotEmpty()) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show()
            }
        }
        
        // 观察加载状态
        viewModel.isLoading.observe(this) { isLoading ->
            // 可以在这里显示/隐藏加载动画
        }
    }
}
