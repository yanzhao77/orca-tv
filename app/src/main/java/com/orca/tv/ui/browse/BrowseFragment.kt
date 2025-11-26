package com.orca.tv.ui.browse

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.View
import androidx.core.content.ContextCompat
import androidx.leanback.app.BrowseSupportFragment
import androidx.leanback.widget.*
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.orca.tv.R
import com.orca.tv.data.Channel
import com.orca.tv.ui.playback.PlaybackActivity
import com.orca.tv.ui.settings.SettingsActivity

/**
 * Leanback BrowseFragment
 * 展示频道分类和列表
 */
class BrowseFragment : BrowseSupportFragment() {
    
    private lateinit var viewModel: ChannelViewModel
    private val rowsAdapter = ArrayObjectAdapter(ListRowPresenter())
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupUI()
        setupViewModel()
    }
    
    private fun setupUI() {
        // 设置标题
        title = getString(R.string.app_name)
        headersState = HEADERS_ENABLED
        isHeadersTransitionOnBackEnabled = true
        
        // 设置品牌颜色
        brandColor = ContextCompat.getColor(requireContext(), R.color.primary)
        
        // 设置搜索图标（可选）
        // searchAffordanceColor = ContextCompat.getColor(requireContext(), R.color.accent)
        
        adapter = rowsAdapter
        
        // 设置点击监听
        onItemViewClickedListener = ItemViewClickedListener()
    }
    
    private fun setupViewModel() {
        viewModel = ViewModelProvider(requireActivity())[ChannelViewModel::class.java]
        
        // 观察频道列表变化
        viewModel.channels.observe(viewLifecycleOwner) { channels ->
            if (channels.isNotEmpty()) {
                buildRows(channels)
            }
        }
    }
    
    /**
     * 构建行（按分类）
     */
    private fun buildRows(channels: List<Channel>) {
        rowsAdapter.clear()
        
        // 获取所有分类
        val categories = channels.map { it.category }.distinct()
        
        // 为每个分类创建一行
        for (category in categories) {
            val categoryChannels = channels.filter { it.category == category }
            
            val listRowAdapter = ArrayObjectAdapter(ChannelCardPresenter())
            categoryChannels.forEach { listRowAdapter.add(it) }
            
            val header = HeaderItem(category)
            rowsAdapter.add(ListRow(header, listRowAdapter))
        }
        
        // 添加设置行
        val settingsRowAdapter = ArrayObjectAdapter(GridItemPresenter())
        settingsRowAdapter.add(SettingsItem("设置", "配置后端 API 地址"))
        settingsRowAdapter.add(SettingsItem("刷新", "手动刷新频道列表"))
        
        val settingsHeader = HeaderItem("系统")
        rowsAdapter.add(ListRow(settingsHeader, settingsRowAdapter))
    }
    
    /**
     * 频道卡片 Presenter
     */
    private inner class ChannelCardPresenter : Presenter() {
        
        override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
            val cardView = ImageCardView(parent.context).apply {
                isFocusable = true
                isFocusableInTouchMode = true
                setMainImageDimensions(CARD_WIDTH, CARD_HEIGHT)
            }
            return ViewHolder(cardView)
        }
        
        override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) {
            val channel = item as Channel
            val cardView = viewHolder.view as ImageCardView
            
            cardView.titleText = channel.name
            cardView.contentText = "${channel.lines.size} 个线路"
            
            // 加载频道图标
            if (!channel.logo.isNullOrEmpty()) {
                Glide.with(cardView.context)
                    .load(channel.logo)
                    .placeholder(R.drawable.default_channel_icon)
                    .into(object : CustomTarget<Drawable>() {
                        override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                            cardView.mainImageView.setImageDrawable(resource)
                        }
                        override fun onLoadCleared(placeholder: Drawable?) {
                            cardView.mainImageView.setImageDrawable(placeholder)
                        }
                    })
            } else {
                cardView.mainImage = ContextCompat.getDrawable(cardView.context, R.drawable.default_channel_icon)
            }
        }
        
        override fun onUnbindViewHolder(viewHolder: ViewHolder) {
            val cardView = viewHolder.view as ImageCardView
            cardView.badgeImage = null
            cardView.mainImage = null
        }
    }
    
    /**
     * 设置项 Presenter
     */
    private inner class GridItemPresenter : Presenter() {
        override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
            val cardView = ImageCardView(parent.context).apply {
                isFocusable = true
                isFocusableInTouchMode = true
                setMainImageDimensions(CARD_WIDTH, CARD_HEIGHT)
            }
            return ViewHolder(cardView)
        }
        
        override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) {
            val settingsItem = item as SettingsItem
            val cardView = viewHolder.view as ImageCardView
            
            cardView.titleText = settingsItem.title
            cardView.contentText = settingsItem.description
            cardView.mainImage = ContextCompat.getDrawable(cardView.context, R.drawable.ic_settings)
        }
        
        override fun onUnbindViewHolder(viewHolder: ViewHolder) {
            val cardView = viewHolder.view as ImageCardView
            cardView.badgeImage = null
            cardView.mainImage = null
        }
    }
    
    /**
     * 点击事件监听
     */
    private inner class ItemViewClickedListener : OnItemViewClickedListener {
        override fun onItemClicked(
            itemViewHolder: Presenter.ViewHolder,
            item: Any,
            rowViewHolder: RowPresenter.ViewHolder,
            row: Row
        ) {
            when (item) {
                is Channel -> {
                    // 播放频道
                    val intent = Intent(requireContext(), PlaybackActivity::class.java).apply {
                        putExtra("channel_id", item.id)
                        putExtra("channel_name", item.name)
                    }
                    startActivity(intent)
                }
                is SettingsItem -> {
                    when (item.title) {
                        "设置" -> {
                            val intent = Intent(requireContext(), SettingsActivity::class.java)
                            startActivity(intent)
                        }
                        "刷新" -> {
                            viewModel.loadChannels(isRefresh = true)
                        }
                    }
                }
            }
        }
    }
    
    /**
     * 设置项数据类
     */
    private data class SettingsItem(val title: String, val description: String)
    
    companion object {
        private const val CARD_WIDTH = 313
        private const val CARD_HEIGHT = 176
    }
}
