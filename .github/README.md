# GitHub Actions 自动构建说明

本项目配置了 GitHub Actions 自动构建 APK 并发布到 Releases。

## 工作流说明

### 1. Build and Release APK (`build-apk.yml`)

**触发条件**:
- 推送 `v*` 格式的标签（如 `v1.0.0`, `v1.1.0`）
- 手动触发（在 GitHub Actions 页面点击 "Run workflow"）

**执行步骤**:
1. 检出代码
2. 设置 JDK 17 环境
3. 构建 Debug APK
4. 构建 Release APK
5. 重命名 APK 文件（包含版本号）
6. 生成构建信息文件
7. 上传 APK 到 Artifacts
8. 如果是标签推送，自动创建 GitHub Release 并上传 APK

**产物**:
- `orca-tv-vX.X.X-debug.apk` - Debug 版本
- `orca-tv-vX.X.X-release-unsigned.apk` - Release 版本（未签名）
- `BUILD_INFO.txt` - 构建信息

### 2. PR Build Check (`pr-check.yml`)

**触发条件**:
- 向 `main` 或 `develop` 分支提交 Pull Request

**执行步骤**:
1. 检出代码
2. 设置 JDK 17 环境
3. 运行 Lint 检查
4. 构建 Debug APK
5. 上传 Lint 报告
6. 在 PR 中评论构建状态

## 使用方法

### 方法一：推送标签（推荐）

1. 更新版本号（在 `app/build.gradle` 中）
2. 提交代码
3. 创建并推送标签：

```bash
git tag v1.0.1
git push origin v1.0.1
```

4. GitHub Actions 会自动构建并创建 Release

### 方法二：手动触发

1. 访问 GitHub 仓库的 Actions 页面
2. 选择 "Build and Release APK" 工作流
3. 点击 "Run workflow" 按钮
4. 选择分支并点击 "Run workflow"

## 下载 APK

### 从 Releases 下载（推荐）

访问 https://github.com/yanzhao77/orca-tv/releases 下载最新版本的 APK。

### 从 Artifacts 下载

1. 访问 GitHub 仓库的 Actions 页面
2. 选择对应的工作流运行记录
3. 在 "Artifacts" 部分下载 APK

## 签名配置（可选）

如果需要对 Release APK 进行签名，需要配置以下 GitHub Secrets：

1. `KEYSTORE_FILE` - Keystore 文件的 Base64 编码
2. `KEYSTORE_PASSWORD` - Keystore 密码
3. `KEY_ALIAS` - Key 别名
4. `KEY_PASSWORD` - Key 密码

### 生成 Keystore

```bash
keytool -genkey -v -keystore orca-tv.keystore -alias orca-tv -keyalg RSA -keysize 2048 -validity 10000
```

### 将 Keystore 转换为 Base64

```bash
base64 orca-tv.keystore > keystore.base64
```

然后将 `keystore.base64` 的内容添加到 GitHub Secrets 中的 `KEYSTORE_FILE`。

## 故障排查

### 构建失败

1. 查看 Actions 日志，找到错误信息
2. 常见问题：
   - Gradle 版本不兼容：检查 `gradle-wrapper.properties`
   - 依赖下载失败：重新运行工作流
   - 代码错误：修复代码后重新推送

### APK 未上传到 Release

1. 确认是否推送了标签（不是普通提交）
2. 检查标签格式是否为 `v*`（如 `v1.0.0`）
3. 查看 Actions 日志中的 "Create Release" 步骤

## 版本管理建议

遵循 [语义化版本](https://semver.org/lang/zh-CN/) 规范：

- **主版本号 (MAJOR)**: 不兼容的 API 修改
- **次版本号 (MINOR)**: 向下兼容的功能性新增
- **修订号 (PATCH)**: 向下兼容的问题修正

示例：
- `v1.0.0` - 首次发布
- `v1.1.0` - 新增功能
- `v1.1.1` - 修复 Bug
- `v2.0.0` - 重大更新

## 更新 CHANGELOG

每次发布新版本前，更新 `CHANGELOG.md` 文件，记录本次更新的内容。

---

**注意**: 所有构建产物都会保留一定时间（Debug APK 30天，Release APK 90天），过期后会自动删除。建议及时下载重要版本的 APK。
