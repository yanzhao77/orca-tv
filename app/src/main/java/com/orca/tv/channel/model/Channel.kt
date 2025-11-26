package com.orca.tv.channel.model

/**
 * 频道数据模型
 */
data class Channel(
    val id: String,                    // 频道唯一 ID
    val name: String,                  // 频道名称
    val displayName: String,           // 显示名称
    val category: String,              // 分类（央视、卫视等）
    val logo: String?,                 // 台标 URL
    val sources: List<ChannelSource>,  // 多个线路
    val epgId: String?,                // EPG ID
    val metadata: Map<String, String> = emptyMap()  // 扩展元数据
) {
    /**
     * 获取首选播放源
     */
    fun getPreferredSource(): ChannelSource? {
        return sources.maxByOrNull { it.priority }
    }
    
    /**
     * 按优先级排序的源列表
     */
    fun getSortedSources(): List<ChannelSource> {
        return sources.sortedByDescending { it.priority }
    }
    
    /**
     * 转换为旧版 Channel 实体（兼容性）
     */
    fun toEntity(): com.orca.tv.data.Channel {
        return com.orca.tv.data.Channel(
            id = id,
            name = displayName,
            category = category,
            logo = logo,
            urls = sources.map { it.url },
            lastPlayedUrl = getPreferredSource()?.url,
            isFavorite = false
        )
    }
}

/**
 * 频道源（线路）
 */
data class ChannelSource(
    val id: String,                    // 源 ID
    val url: String,                   // 播放 URL
    val origin: SourceOrigin,          // 来源类型
    val quality: Quality?,             // 清晰度
    val protocol: Protocol,            // 协议类型
    val ipvType: IpvType,              // IPv4/IPv6
    val priority: Int = 0,             // 优先级（越高越优先）
    val testResult: TestResult? = null // 测速结果
) {
    companion object {
        /**
         * 从 URL 创建默认源
         */
        fun fromUrl(url: String, origin: SourceOrigin = SourceOrigin.REMOTE): ChannelSource {
            return ChannelSource(
                id = url.hashCode().toString(),
                url = url,
                origin = origin,
                quality = detectQuality(url),
                protocol = detectProtocol(url),
                ipvType = IpvType.UNKNOWN,
                priority = 0
            )
        }
        
        private fun detectQuality(url: String): Quality? {
            return when {
                url.contains("4k", ignoreCase = true) || url.contains("uhd", ignoreCase = true) -> Quality.UHD
                url.contains("1080", ignoreCase = true) || url.contains("fhd", ignoreCase = true) -> Quality.FHD
                url.contains("720", ignoreCase = true) || url.contains("hd", ignoreCase = true) -> Quality.HD
                url.contains("480", ignoreCase = true) || url.contains("sd", ignoreCase = true) -> Quality.SD
                else -> null
            }
        }
        
        private fun detectProtocol(url: String): Protocol {
            return when {
                url.startsWith("rtmp://", ignoreCase = true) -> Protocol.RTMP
                url.startsWith("rtsp://", ignoreCase = true) -> Protocol.RTSP
                url.startsWith("udp://", ignoreCase = true) -> Protocol.UDP
                url.startsWith("https://", ignoreCase = true) -> Protocol.HTTPS
                url.startsWith("http://", ignoreCase = true) -> Protocol.HTTP
                else -> Protocol.HTTP
            }
        }
    }
}

/**
 * 来源类型
 */
enum class SourceOrigin {
    LOCAL,      // 本地源
    REMOTE,     // 远程源
    WHITELIST,  // 白名单
    SUBSCRIBE,  // 订阅源
    HISTORY,    // 历史缓存
    BUILTIN     // 内置源
}

/**
 * 清晰度
 */
enum class Quality(val displayName: String) {
    SD("标清"),
    HD("高清"),
    FHD("全高清"),
    UHD("超高清");
    
    companion object {
        fun fromResolution(width: Int, height: Int): Quality {
            return when {
                width >= 3840 && height >= 2160 -> UHD
                width >= 1920 && height >= 1080 -> FHD
                width >= 1280 && height >= 720 -> HD
                else -> SD
            }
        }
    }
}

/**
 * 协议类型
 */
enum class Protocol {
    HTTP, HTTPS, RTMP, RTSP, UDP
}

/**
 * IP 类型
 */
enum class IpvType {
    IPV4, IPV6, UNKNOWN
}

/**
 * 测速结果
 */
data class TestResult(
    val delay: Long,                   // 延迟（ms）
    val speed: Double,                 // 速率（MB/s）
    val resolution: String?,           // 分辨率
    val testTime: Long                 // 测试时间戳
) {
    /**
     * 是否有效
     */
    fun isValid(maxDelay: Long = 5000, minSpeed: Double = 0.5): Boolean {
        return delay in 0..maxDelay && speed >= minSpeed
    }
    
    /**
     * 计算得分（用于排序）
     */
    fun calculateScore(): Double {
        val delayScore = if (delay > 0) (1.0 / delay) * 1000 else 0.0
        val speedScore = speed * 10
        return delayScore + speedScore
    }
}

/**
 * 频道分组
 */
data class ChannelGroup(
    val name: String,                  // 分组名称
    val channels: List<Channel>        // 频道列表
) {
    val size: Int get() = channels.size
}
