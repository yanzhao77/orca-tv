package com.orca.tv.channel.parser

import com.orca.tv.channel.model.*
import java.util.UUID

/**
 * M3U 文件解析器
 * 支持标准 M3U 和扩展 M3U (#EXTM3U) 格式
 */
class M3UParser {
    
    companion object {
        private const val TAG = "M3UParser"
        private const val EXTM3U_HEADER = "#EXTM3U"
        private const val EXTINF_PREFIX = "#EXTINF:"
        private const val GENRE_MARKER = "#genre#"
    }
    
    /**
     * 解析 M3U 文件内容
     */
    fun parse(content: String): List<Channel> {
        val lines = content.lines().map { it.trim() }
        
        // 检测格式
        val isExtM3U = lines.firstOrNull()?.startsWith(EXTM3U_HEADER) == true
        
        return if (isExtM3U) {
            parseExtM3U(lines)
        } else {
            parseSimpleM3U(lines)
        }
    }
    
    /**
     * 解析扩展 M3U 格式
     */
    private fun parseExtM3U(lines: List<String>): List<Channel> {
        val channelMap = mutableMapOf<String, MutableList<ChannelSource>>()
        val channelInfoMap = mutableMapOf<String, ChannelInfo>()
        var currentCategory = "未分类"
        var currentChannelInfo: ChannelInfo? = null
        
        for (line in lines) {
            when {
                line.startsWith(EXTM3U_HEADER) -> continue
                
                line.contains(GENRE_MARKER) -> {
                    // 分类标记：央视频道,#genre#
                    currentCategory = line.substringBefore(",").trim()
                }
                
                line.startsWith(EXTINF_PREFIX) -> {
                    // 频道信息行
                    currentChannelInfo = parseExtInf(line, currentCategory)
                }
                
                line.isNotBlank() && !line.startsWith("#") -> {
                    // URL 行
                    currentChannelInfo?.let { info ->
                        val source = ChannelSource.fromUrl(line, SourceOrigin.REMOTE)
                        val channelName = info.name
                        
                        // 添加到频道源列表
                        channelMap.getOrPut(channelName) { mutableListOf() }.add(source)
                        
                        // 保存频道信息
                        if (!channelInfoMap.containsKey(channelName)) {
                            channelInfoMap[channelName] = info
                        }
                    }
                    currentChannelInfo = null
                }
            }
        }
        
        // 转换为 Channel 对象
        return channelMap.map { (name, sources) ->
            val info = channelInfoMap[name] ?: ChannelInfo(name, currentCategory)
            createChannel(name, sources, info)
        }
    }
    
    /**
     * 解析简单 M3U 格式（每行一个频道）
     */
    private fun parseSimpleM3U(lines: List<String>): List<Channel> {
        val channelMap = mutableMapOf<String, MutableList<ChannelSource>>()
        var currentCategory = "未分类"
        
        for (line in lines) {
            when {
                line.contains(GENRE_MARKER) -> {
                    currentCategory = line.substringBefore(",").trim()
                }
                
                line.contains(",http") || line.contains(",rtmp") || line.contains(",rtsp") -> {
                    // 格式：频道名称,URL
                    val parts = line.split(",", limit = 2)
                    if (parts.size == 2) {
                        val name = parts[0].trim()
                        val url = parts[1].trim()
                        val source = ChannelSource.fromUrl(url, SourceOrigin.REMOTE)
                        channelMap.getOrPut(name) { mutableListOf() }.add(source)
                    }
                }
                
                line.startsWith("http") || line.startsWith("rtmp") || line.startsWith("rtsp") -> {
                    // 纯 URL 行（使用 URL 作为名称）
                    val url = line.trim()
                    val name = extractNameFromUrl(url)
                    val source = ChannelSource.fromUrl(url, SourceOrigin.REMOTE)
                    channelMap.getOrPut(name) { mutableListOf() }.add(source)
                }
            }
        }
        
        // 转换为 Channel 对象
        return channelMap.map { (name, sources) ->
            val info = ChannelInfo(name, currentCategory)
            createChannel(name, sources, info)
        }
    }
    
    /**
     * 解析 #EXTINF 行
     * 格式：#EXTINF:-1 tvg-id="CCTV1" tvg-name="CCTV-1" tvg-logo="..." group-title="央视",CCTV-1综合
     */
    private fun parseExtInf(line: String, defaultCategory: String): ChannelInfo {
        val attributes = parseAttributes(line)
        val name = line.substringAfterLast(",").trim()
        
        return ChannelInfo(
            name = name,
            category = attributes["group-title"] ?: defaultCategory,
            tvgId = attributes["tvg-id"],
            tvgName = attributes["tvg-name"],
            tvgLogo = attributes["tvg-logo"]
        )
    }
    
    /**
     * 解析属性
     * 例如：tvg-id="CCTV1" tvg-name="CCTV-1"
     */
    private fun parseAttributes(line: String): Map<String, String> {
        val attributes = mutableMapOf<String, String>()
        val regex = """(\w+(?:-\w+)*)="([^"]*)"""".toRegex()
        
        regex.findAll(line).forEach { match ->
            val key = match.groupValues[1]
            val value = match.groupValues[2]
            attributes[key] = value
        }
        
        return attributes
    }
    
    /**
     * 从 URL 提取频道名称
     */
    private fun extractNameFromUrl(url: String): String {
        // 尝试从 URL 中提取有意义的名称
        val path = url.substringAfterLast("/").substringBeforeLast(".")
        return if (path.isNotBlank()) path else "未命名频道"
    }
    
    /**
     * 创建 Channel 对象
     */
    private fun createChannel(
        name: String,
        sources: List<ChannelSource>,
        info: ChannelInfo
    ): Channel {
        return Channel(
            id = generateChannelId(name, info.category),
            name = name,
            displayName = info.tvgName ?: name,
            category = info.category,
            logo = info.tvgLogo,
            sources = sources,
            epgId = info.tvgId
        )
    }
    
    /**
     * 生成频道 ID
     */
    private fun generateChannelId(name: String, category: String): String {
        return "${category}_${name}".hashCode().toString()
    }
}

/**
 * 频道信息（解析中间数据）
 */
data class ChannelInfo(
    val name: String,
    val category: String = "未分类",
    val tvgId: String? = null,
    val tvgName: String? = null,
    val tvgLogo: String? = null
)

/**
 * M3U 写入器
 */
class M3UWriter {
    
    /**
     * 将频道列表写入 M3U 格式
     */
    fun write(channels: List<Channel>): String {
        val builder = StringBuilder()
        builder.appendLine("#EXTM3U")
        
        // 按分类分组
        val groupedChannels = channels.groupBy { it.category }
        
        for ((category, channelList) in groupedChannels) {
            builder.appendLine()
            builder.appendLine("$category,#genre#")
            
            for (channel in channelList) {
                for (source in channel.sources) {
                    builder.append("#EXTINF:-1")
                    
                    // 添加属性
                    if (channel.epgId != null) {
                        builder.append(""" tvg-id="${channel.epgId}"""")
                    }
                    builder.append(""" tvg-name="${channel.displayName}"""")
                    if (channel.logo != null) {
                        builder.append(""" tvg-logo="${channel.logo}"""")
                    }
                    builder.append(""" group-title="$category"""")
                    
                    builder.append(",${channel.displayName}")
                    builder.appendLine()
                    builder.appendLine(source.url)
                }
            }
        }
        
        return builder.toString()
    }
}
