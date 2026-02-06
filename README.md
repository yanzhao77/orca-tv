# Orca-tv

**智能、容错的 Android TV 直播应用**

---

## 🌟 项目简介

Orca-tv 是一款专为 Android TV 设计的开源电视直播应用。它解决了公共 IPTV 源不稳定的核心痛点，通过内置的 **ChannelManager** 模块，实现了直播源的自动管理、多级容错和毫秒级响应，为您提供流畅、可靠的观看体验。

**核心理念**: 将复杂的直播源管理工作完全本地化，不再依赖任何外部后端服务。

---

## ✨ 主要功能

| 功能 | 描述 |
|---|---|
| **📺 本地化频道管理** | 内置 **ChannelManager** 模块，取代外部 API，实现频道数据的本地化解析、管理和缓存。 |
| **🚀 三级容错回退** | `远程源` → `本地源` → `历史缓存`，确保在任何网络环境下都能提供可看的频道。 |
| **⚡️ 毫秒级响应** | 所有频道操作（加载、搜索、过滤）均在内存中完成，响应速度极快。 |
| **🔗 多线路自动切换** | 播放失败时自动切换到下一条可用线路，支持手动切换。 |
| **📡 M3U 解析器** | 强大的 M3U 解析器，兼容标准和扩展格式，自动合并同名频道的多条线路。 |
| **📱 Leanback TV UI** | 基于 Google Leanback 框架构建，专为电视遥控器操作优化。 |
| **🔧 高度可配置** | 支持自定义源、黑白名单、更新频率等高级配置。 |
| **🌐 离线可用** | 首次加载后，即使没有网络也能使用历史缓存观看。 |

---

## 🏗️ 项目架构

### 核心模块: ChannelManager

Orca-tv 的核心是 **ChannelManager**，一个完全在 App 内部运行的 Kotlin 模块。它取代了传统的“App → 后端 API”模式，将所有频道管理工作在本地完成。

**数据流程**:
```
远程 M3U 文件 → ChannelManager → M3U 解析器 → Channel 列表 → UI
```

**主要组件**:
- **ChannelManager**: 单例，负责统一管理所有频道数据。
- **M3UParser**: 解析 M3U 文件，生成频道对象。
- **ChannelSourceManager**: 管理远程源、本地源和缓存。
- **ChannelApi**: 提供本地 API 接口，供 UI 层调用。

### 技术栈

| 模块 | 技术/库 | 版本 |
|---|---|---|
| 开发语言 | Kotlin | 1.9.20 |
| UI 框架 | Jetpack Leanback | 1.0.0 |
| 播放器 | Media3 (ExoPlayer) | 1.2.1 |
| 数据库 | Room | 2.6.1 |
| 网络请求 | OkHttp + Retrofit | 4.12.0 / 2.9.0 |
| 数据解析 | Gson | 2.10.1 |
| 图片加载 | Glide | 4.16.0 |
| 协程 | Kotlinx Coroutines | 1.7.3 |

---

## 🚀 快速开始

### 环境要求
- Android Studio Hedgehog (2023.1.1) 或更高版本
- JDK 17
- Android SDK API 34

### 构建步骤

1. **克隆项目**
   ```bash
   git clone https://github.com/yanzhao77/orca-tv.git
   cd orca-tv
   ```

2. **使用 Android Studio 打开**
   - 启动 Android Studio
   - 选择 "Open an Existing Project"
   - 选择 `orca-tv` 目录

3. **同步并运行**
   - 等待 Gradle 同步完成
   - 连接 Android TV 设备或模拟器
   - 点击 "Run" 按钮

### 构建 APK

```bash
# Debug 版本
./gradlew assembleDebug

# Release 版本（需要配置签名）
./gradlew assembleRelease
```

生成的 APK 文件位于 `app/build/outputs/apk/` 目录。

---

## 🔧 配置说明

### 1. 配置直播源

首次启动后，进入 **设置** 页面，您可以添加或修改 M3U 直播源 URL。

**默认源**:
- `https://raw.githubusercontent.com/Guovin/iptv-api/main/output/result.m3u`
- `https://iptv-org.github.io/iptv/index.m3u`

### 2. 修改内置频道

如果需要在没有网络的情况下使用，可以编辑内置的频道列表：

`app/src/main/assets/default_channels.m3u`

---

## 🤝 贡献

欢迎任何形式的贡献！您可以：
- 提交 Issue 报告 Bug 或提出建议
- 发起 Pull Request 修复 Bug 或添加新功能
- 帮助完善文档

## 📝 许可证

本项目采用 **MIT License**。详见根目录的 `LICENSE` 文件。

## 🙏 致谢

- **Guovin/iptv-api**: 提供了高质量的直播源和设计灵感。
- **iptv-org/iptv**: 全球公开的 IPTV 源仓库。
- **Google ExoPlayer**: 强大的播放器核心。
- **Android Jetpack**: 现代 Android 开发的基础。

---

**免责声明**: 本项目仅供学习和研究使用，所有直播源均来自互联网。请勿用于任何商业用途。对于因使用本项目而产生的任何法律问题，开发者概不负责。
