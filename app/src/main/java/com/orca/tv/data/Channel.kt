package com.orca.tv.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * 频道数据模型
 */
@Entity(tableName = "channels")
data class Channel(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val name: String,                    // 频道名称
    val category: String = "其他",        // 频道分类（如：央视、卫视、体育）
    val logo: String? = null,            // 频道图标 URL
    val lines: List<ChannelLine>,        // 线路列表
    val epgId: String? = null,           // EPG 节目单 ID
    val isFavorite: Boolean = false,     // 是否收藏
    val lastPlayedLineIndex: Int = 0,    // 上次播放成功的线路索引
    val lastPlayedTime: Long = 0,        // 上次播放时间
    val failCount: Int = 0               // 连续失败次数（用于自动剔除）
)

/**
 * 频道线路（源）
 */
data class ChannelLine(
    val url: String,                     // 播放地址
    val source: String = "未知",          // 来源（如：iptv-api、本地源）
    val quality: String? = null,         // 清晰度（如：1080p、720p）
    val protocol: String = "http",       // 协议类型（http、rtmp、rtsp）
    val speed: Float = 0f,               // 测速结果（MB/s）
    val delay: Int = 0,                  // 延迟（ms）
    val lastSuccessTime: Long = 0        // 上次播放成功时间
)

/**
 * Room 类型转换器（用于存储 List<ChannelLine>）
 */
class Converters {
    private val gson = Gson()
    
    @TypeConverter
    fun fromChannelLineList(value: List<ChannelLine>): String {
        return gson.toJson(value)
    }
    
    @TypeConverter
    fun toChannelLineList(value: String): List<ChannelLine> {
        val listType = object : TypeToken<List<ChannelLine>>() {}.type
        return gson.fromJson(value, listType)
    }
}
