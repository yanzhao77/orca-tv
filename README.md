# Orca-tv Android 项目

这是 Orca-tv Android 电视直播应用的完整项目源码。

## 项目结构

```
orca-tv-android/
├── app/                                # 主应用模块
│   ├── src/main/
│   │   ├── java/com/orca/tv/
│   │   │   ├── data/                   # 数据层
│   │   │   │   ├── Channel.kt          # 频道数据模型
│   │   │   │   ├── ChannelDatabase.kt  # Room 数据库
│   │   │   │   ├── M3UParser.kt        # M3U 解析器
│   │   │   │   └── ChannelRepository.kt # 数据仓库（三级回退）
│   │   │   ├── ui/                     # UI 层
│   │   │   │   ├── browse/             # 主浏览界面
│   │   │   │   │   ├── BrowseActivity.kt
│   │   │   │   │   ├── BrowseFragment.kt
│   │   │   │   │   └── ChannelViewModel.kt
│   │   │   │   ├── playback/           # 播放界面
│   │   │   │   │   └── PlaybackActivity.kt
│   │   │   │   └── settings/           # 设置界面
│   │   │   │       ├── SettingsActivity.kt
│   │   │   │       └── SettingsFragment.kt
│   │   │   └── player/                 # 播放核心（预留）
│   │   ├── res/                        # 资源文件
│   │   │   ├── layout/                 # 布局文件
│   │   │   ├── values/                 # 字符串、颜色、主题
│   │   │   ├── drawable/               # 图标资源
│   │   │   └── xml/                    # 设置 XML
│   │   ├── assets/                     # 资产文件
│   │   │   └── default_channels.m3u    # 内置默认频道列表
│   │   └── AndroidManifest.xml
│   ├── build.gradle                    # 应用模块构建配置
│   └── proguard-rules.pro              # 混淆规则
├── build.gradle                        # 项目级构建配置
├── settings.gradle                     # 项目设置
├── gradle.properties                   # Gradle 属性
└── README.md                           # 本文件
```

## 核心功能

### 1. 三级容错回退机制
- **后端 API**: 优先从配置的 iptv-api 服务加载最新频道列表
- **本地缓存**: 网络失败时自动加载上次成功的缓存
- **内置默认源**: 首次启动或缓存失效时，加载内置的基础频道列表

### 2. 智能播放与线路切换
- 自动检测播放错误并切换到下一条线路
- 记录上次播放成功的线路，下次优先使用
- 支持遥控器左右键手动切换线路
- 连续失败的频道自动标记并降低优先级

### 3. Leanback TV 界面
- 基于 Google Leanback 库构建，专为 Android TV 优化
- 支持遥控器导航和焦点管理
- 频道分类展示（央视、卫视等）
- 频道卡片展示频道图标、名称和线路数量

### 4. 数据持久化
- 使用 Room 数据库存储频道列表和用户偏好
- SharedPreferences 存储 API 地址等配置
- 支持收藏功能和播放历史记录

## 技术栈

| 模块 | 技术/库 | 版本 |
|------|---------|------|
| 开发语言 | Kotlin | 1.9.20 |
| UI 框架 | Jetpack Leanback | 1.0.0 |
| 播放器 | Media3 (ExoPlayer) | 1.2.1 |
| 数据库 | Room | 2.6.1 |
| 网络请求 | OkHttp + Retrofit | 4.12.0 / 2.9.0 |
| 数据解析 | Gson | 2.10.1 |
| 图片加载 | Glide | 4.16.0 |
| 协程 | Kotlinx Coroutines | 1.7.3 |

## 构建与运行

### 环境要求
- Android Studio Hedgehog (2023.1.1) 或更高版本
- JDK 17
- Android SDK API 34
- Gradle 8.1+

### 构建步骤

1. **克隆或下载项目**
   ```bash
   # 如果从 Git 克隆
   git clone <repository-url>
   cd orca-tv-android
   ```

2. **使用 Android Studio 打开项目**
   - 启动 Android Studio
   - 选择 "Open an Existing Project"
   - 选择 `orca-tv-android` 目录

3. **同步 Gradle**
   - Android Studio 会自动提示同步 Gradle
   - 点击 "Sync Now" 等待依赖下载完成

4. **连接 Android TV 设备或模拟器**
   - 真机：通过 ADB 连接（推荐使用网络 ADB）
   - 模拟器：创建一个 Android TV 模拟器（API 21+）

5. **运行应用**
   - 点击工具栏的 "Run" 按钮（绿色三角形）
   - 或使用快捷键 `Shift + F10`

### 构建 APK

```bash
# Debug 版本
./gradlew assembleDebug

# Release 版本（需要配置签名）
./gradlew assembleRelease
```

生成的 APK 文件位于：`app/build/outputs/apk/`

## 配置说明

### 1. 配置后端 API 地址

首次启动后，进入 **设置** 页面，配置您的 `iptv-api` 后端地址：

```
http://192.168.1.10:8000
```

### 2. 修改内置默认频道列表

编辑文件：`app/src/main/assets/default_channels.m3u`

添加您自己的频道源，格式如下：

```m3u
#EXTINF:-1 tvg-id="频道ID" tvg-name="频道名称" tvg-logo="图标URL" group-title="分类",频道显示名称
http://播放地址.m3u8
```

## 已知问题与限制

1. **图标资源**: 当前使用占位符图标，建议替换为实际的应用图标和 banner
2. **EPG 支持**: EPG 节目指南功能已预留接口，但未完整实现
3. **播放器控制**: 自定义播放器控制界面较为简单，可根据需求扩展
4. **错误处理**: 部分边界情况的错误处理可能需要进一步优化

## 开发建议

### 下一步开发优先级

1. **完善 UI**
   - 替换应用图标和 banner
   - 优化频道卡片样式
   - 添加加载动画和进度提示

2. **增强播放功能**
   - 实现 EPG 节目指南显示
   - 添加播放器手势控制（音量、进度）
   - 支持多音轨和字幕选择

3. **用户体验优化**
   - 添加频道搜索功能
   - 实现收藏夹管理
   - 支持自定义频道排序

4. **性能优化**
   - 优化启动速度
   - 实现列表懒加载
   - 添加图片缓存策略

## 许可证

本项目采用 MIT License。详见根目录的 LICENSE 文件。

## 致谢

本项目基于以下优秀的开源项目：
- [Guovin/iptv-api](https://github.com/Guovin/iptv-api) - 后端源管理系统
- [mytv-android/mytv-android](https://github.com/mytv-android/mytv-android) - 设计灵感来源
- Google ExoPlayer - 播放器核心
- Android Jetpack Leanback - TV UI 框架

---

**开发者**: Manus AI  
**最后更新**: 2025-11-26
