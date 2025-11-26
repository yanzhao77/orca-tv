# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Planned
- EPG 节目指南显示
- 频道搜索功能
- 收藏夹管理
- 自定义频道排序
- 播放器手势控制

## [1.0.0] - 2025-11-26

### Added
- ✨ 三级容错回退机制（API → 缓存 → 内置源）
- ✨ 智能播放与自动线路切换
- ✨ Leanback TV 界面，专为 Android TV 优化
- ✨ M3U 播放列表解析器
- ✨ Room 数据库持久化
- ✨ ExoPlayer 播放器集成
- ✨ 频道分类展示（央视、卫视、地方台等）
- ✨ 多线路支持，自动切换失败线路
- ✨ 播放历史记录
- ✨ 设置界面，支持配置后端 API 地址
- ✨ 内置 17 个默认频道作为回退源

### Technical
- 🔧 Kotlin 1.9.20
- 🔧 Leanback 1.0.0
- 🔧 Media3 (ExoPlayer) 1.2.1
- 🔧 Room 2.6.1
- 🔧 MVVM 架构
- 🔧 协程支持
- 🔧 Retrofit + OkHttp 网络请求
- 🔧 Glide 图片加载

### Documentation
- 📚 完整的 README.md
- 📚 开发指南 (DEVELOPMENT_GUIDE.md)
- 📚 架构设计文档
- 📚 集成指南

---

## Version History

- **v1.0.0** (2025-11-26) - 首次发布
