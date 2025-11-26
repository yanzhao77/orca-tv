package com.orca.tv.data

import android.util.Log

/**
 * M3U/M3U8 播放列表解析器
 * 
 * 支持标准的 EXTINF 格式：
 * #EXTINF:-1 tvg-id="CCTV1" tvg-name="CCTV-1综合" tvg-logo="http://..." group-title="央视",CCTV-1综合
 * http://example.com/stream.m3u8
 */
class M3UParser {
    
    companion object {
        private const val TAG = "M3UParser"
        
        /**
         * 解析 M3U 文本内容
         */
        fun parse(content: String): List<Channel> {
            val channels = mutableListOf<Channel>()
            val lines = content.lines()
            
            var currentExtinf: ExtinfInfo? = null
            
            for (line in lines) {
                val trimmedLine = line.trim()
                
                when {
                    // 跳过空行和注释（非 EXTINF）
                    trimmedLine.isEmpty() || trimmedLine.startsWith("#EXTM3U") -> continue
                    
                    // 解析 EXTINF 行
                    trimmedLine.startsWith("#EXTINF:") -> {
                        currentExtinf = parseExtinf(trimmedLine)
                    }
                    
                    // 解析播放地址
                    !trimmedLine.startsWith("#") && currentExtinf != null -> {
                        val channel = createChannel(currentExtinf, trimmedLine)
                        
                        // 检查是否已存在同名频道（合并线路）
                        val existingChannel = channels.find { it.name == channel.name }
                        if (existingChannel != null) {
                            // 合并线路
                            val updatedLines = existingChannel.lines.toMutableList()
                            updatedLines.addAll(channel.lines)
                            channels[channels.indexOf(existingChannel)] = existingChannel.copy(lines = updatedLines)
                        } else {
                            channels.add(channel)
                        }
                        
                        currentExtinf = null
                    }
                }
            }
            
            Log.d(TAG, "解析完成，共 ${channels.size} 个频道")
            return channels
        }
        
        /**
         * 解析 EXTINF 行
         * 格式：#EXTINF:-1 tvg-id="CCTV1" tvg-name="CCTV-1综合" tvg-logo="http://..." group-title="央视",CCTV-1综合
         */
        private fun parseExtinf(line: String): ExtinfInfo {
            val info = ExtinfInfo()
            
            // 提取属性
            val tvgIdMatch = Regex("""tvg-id="([^"]*)"""").find(line)
            info.epgId = tvgIdMatch?.groupValues?.get(1)
            
            val tvgNameMatch = Regex("""tvg-name="([^"]*)"""").find(line)
            val tvgName = tvgNameMatch?.groupValues?.get(1)
            
            val tvgLogoMatch = Regex("""tvg-logo="([^"]*)"""").find(line)
            info.logo = tvgLogoMatch?.groupValues?.get(1)
            
            val groupTitleMatch = Regex("""group-title="([^"]*)"""").find(line)
            info.category = groupTitleMatch?.groupValues?.get(1) ?: "其他"
            
            // 提取频道名称（逗号后的部分）
            val commaIndex = line.lastIndexOf(',')
            info.name = if (commaIndex != -1) {
                line.substring(commaIndex + 1).trim()
            } else {
                tvgName ?: "未知频道"
            }
            
            return info
        }
        
        /**
         * 创建频道对象
         */
        private fun createChannel(extinf: ExtinfInfo, url: String): Channel {
            val line = ChannelLine(
                url = url,
                source = "iptv-api",
                protocol = when {
                    url.startsWith("rtmp://") -> "rtmp"
                    url.startsWith("rtsp://") -> "rtsp"
                    else -> "http"
                }
            )
            
            return Channel(
                name = extinf.name,
                category = extinf.category,
                logo = extinf.logo,
                lines = listOf(line),
                epgId = extinf.epgId
            )
        }
    }
    
    /**
     * EXTINF 信息临时存储
     */
    private data class ExtinfInfo(
        var name: String = "",
        var category: String = "其他",
        var logo: String? = null,
        var epgId: String? = null
    )
}
