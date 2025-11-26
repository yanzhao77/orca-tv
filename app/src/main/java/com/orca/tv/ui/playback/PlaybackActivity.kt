package com.orca.tv.ui.playback

import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.orca.tv.R
import com.orca.tv.data.Channel
import com.orca.tv.data.ChannelDatabase
import kotlinx.coroutines.launch

/**
 * 播放界面 Activity
 * 集成 ExoPlayer，实现自动线路切换和错误处理
 */
class PlaybackActivity : FragmentActivity() {
    
    private var player: ExoPlayer? = null
    private lateinit var playerView: PlayerView
    
    private var currentChannel: Channel? = null
    private var currentLineIndex = 0
    
    private val channelDao by lazy {
        ChannelDatabase.getDatabase(this).channelDao()
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_playback)
        
        playerView = findViewById(R.id.player_view)
        
        // 获取频道信息
        val channelId = intent.getLongExtra("channel_id", -1)
        val channelName = intent.getStringExtra("channel_name") ?: "未知频道"
        
        if (channelId != -1L) {
            loadChannelAndPlay(channelId)
        } else {
            Toast.makeText(this, "频道信息错误", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
    
    /**
     * 加载频道并开始播放
     */
    private fun loadChannelAndPlay(channelId: Long) {
        lifecycleScope.launch {
            try {
                val channels = channelDao.getAllChannels()
                currentChannel = channels.find { it.id == channelId }
                
                if (currentChannel == null) {
                    Toast.makeText(this@PlaybackActivity, "频道不存在", Toast.LENGTH_SHORT).show()
                    finish()
                    return@launch
                }
                
                // 从上次成功的线路开始播放
                currentLineIndex = currentChannel!!.lastPlayedLineIndex
                
                initializePlayer()
                startPlayback()
                
            } catch (e: Exception) {
                Log.e(TAG, "加载频道失败: ${e.message}", e)
                Toast.makeText(this@PlaybackActivity, "加载失败", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
    
    /**
     * 初始化播放器
     */
    private fun initializePlayer() {
        player = ExoPlayer.Builder(this).build().apply {
            addListener(playerListener)
            playWhenReady = true
        }
        playerView.player = player
    }
    
    /**
     * 开始播放当前线路
     */
    private fun startPlayback() {
        val line = currentChannel?.lines?.getOrNull(currentLineIndex)
        
        if (line == null) {
            Toast.makeText(this, "没有可用的播放线路", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        
        Log.d(TAG, "播放线路 ${currentLineIndex + 1}/${currentChannel?.lines?.size}: ${line.url}")
        
        // 释放旧的播放资源
        player?.stop()
        player?.clearMediaItems()
        
        // 设置新的播放源
        val mediaItem = MediaItem.fromUri(line.url)
        player?.setMediaItem(mediaItem)
        player?.prepare()
        player?.play()
        
        // 显示提示
        val lineInfo = "线路 ${currentLineIndex + 1}/${currentChannel?.lines?.size}"
        Toast.makeText(this, lineInfo, Toast.LENGTH_SHORT).show()
    }
    
    /**
     * 尝试切换到下一条线路
     */
    private fun trySwitchToNextLine() {
        currentLineIndex++
        val nextLine = currentChannel?.lines?.getOrNull(currentLineIndex)
        
        if (nextLine != null) {
            // 如果还有下一条线路，则尝试播放
            Toast.makeText(this, "当前线路播放失败，正在尝试切换...", Toast.LENGTH_SHORT).show()
            startPlayback()
        } else {
            // 所有线路都已失败
            Toast.makeText(this, "${currentChannel?.name} 所有线路均无法播放", Toast.LENGTH_LONG).show()
            
            // 更新失败计数
            lifecycleScope.launch {
                currentChannel?.let {
                    val updatedChannel = it.copy(failCount = it.failCount + 1)
                    channelDao.update(updatedChannel)
                }
            }
            
            finish()
        }
    }
    
    /**
     * 手动切换线路（左右键）
     */
    private fun switchLine(direction: Int) {
        val totalLines = currentChannel?.lines?.size ?: 0
        if (totalLines <= 1) {
            Toast.makeText(this, "该频道只有一个线路", Toast.LENGTH_SHORT).show()
            return
        }
        
        currentLineIndex = (currentLineIndex + direction + totalLines) % totalLines
        startPlayback()
    }
    
    /**
     * 播放器事件监听
     */
    private val playerListener = object : Player.Listener {
        override fun onPlayerError(error: PlaybackException) {
            Log.e(TAG, "播放错误: ${error.message}", error)
            trySwitchToNextLine()
        }
        
        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_READY -> {
                    // 播放成功，记录成功的线路
                    lifecycleScope.launch {
                        currentChannel?.let {
                            val updatedChannel = it.copy(
                                lastPlayedLineIndex = currentLineIndex,
                                lastPlayedTime = System.currentTimeMillis(),
                                failCount = 0
                            )
                            channelDao.update(updatedChannel)
                        }
                    }
                }
                Player.STATE_ENDED -> {
                    // 播放结束
                    finish()
                }
            }
        }
    }
    
    /**
     * 处理遥控器按键
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                switchLine(-1)
                true
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                switchLine(1)
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }
    
    override fun onStop() {
        super.onStop()
        player?.pause()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        player?.release()
        player = null
    }
    
    companion object {
        private const val TAG = "PlaybackActivity"
    }
}
