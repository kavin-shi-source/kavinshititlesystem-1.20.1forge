# Kavinshi PlayerTitle 跨服称号系统

[![License](https://img.shields.io/badge/license-MIT-blue.svg)]()
[![Minecraft](https://img.shields.io/badge/Minecraft-1.20.1-green.svg)]()
[![Forge](https://img.shields.io/badge/Forge-47.2.0-orange.svg)]()

一个基于 Minecraft Forge 和 Velocity 的跨服务器玩家称号同步系统，支持自定义图标、动画效果和全局标签页展示。

## 🎯 项目背景

在大型 Minecraft 服务器网络中，玩家经常需要在多个子服之间切换。传统的称号系统只能在单一服务器内使用，当玩家切换服务器时称号无法同步。本系统解决了以下问题：

1. **跨服称号同步**：玩家在不同子服之间切换时保持称号一致
2. **全局标签页展示**：在 Velocity 代理的标签页中显示所有服务器的玩家称号
3. **自定义图标支持**：通过 PNG 图片文件创建自定义称号图标
4. **实时事件同步**：使用 Redis 作为事件总线实现实时跨服通信

## ✨ 特性

### 🎨 丰富的称号系统
- **称号解锁条件**：支持多种条件类型（等级、在线时间、成就等）
- **动画效果**：渐变、闪烁、呼吸等多种动画效果
- **自定义样式**：支持静态、随机、顺序等多种显示模式
- **图标系统**：支持 16×16 和 32×32 PNG 格式图标

### 🔄 跨服同步能力
- **实时事件总线**：基于 Redis Pub/Sub 的实时事件系统
- **版本控制**：事件版本机制确保幂等性和顺序一致性
- **集群感知**：自动识别服务器集群中的节点变化

### 🎯 客户端支持
- **动态字体资源包**：自动生成并注册包含自定义图标的字体资源包
- **图标渲染**：在聊天和标签页中正确渲染自定义图标
- **客户端集成**：与 Forge 客户端无缝集成

### ⚙️ 配置管理
- **JSON 配置**：所有称号配置使用 JSON 格式，易于编辑
- **热重载**：支持配置文件热重载，无需重启服务器
- **图标扫描**：自动扫描并加载图标目录中的 PNG 文件

## 🚀 快速开始

### 环境要求

- **Minecraft 服务器**：1.20.1
- **Forge**：47.2.0 或更高版本
- **Java**：17 或更高版本
- **Redis**（可选）：用于跨服事件同步

### 安装步骤

#### 1. 服务端模组安装

1. 下载最新版本的 `kavinshi-playertitle-X.X.X.jar`
2. 将文件放入 Forge 服务器的 `mods` 目录
3. 启动服务器，首次运行会生成配置文件

#### 2. 客户端模组安装

1. 下载相同版本的客户端模组文件
2. 将文件放入 Minecraft 客户端的 `mods` 目录
3. 启动游戏，系统会自动生成字体资源包

#### 3. Velocity 插件安装（可选）

1. 下载 `kavinshi-playertitle-velocity-X.X.X.jar`
2. 将文件放入 Velocity 代理的 `plugins` 目录
3. 重启 Velocity 服务

### 基本配置

#### 称号配置文件

配置文件位于 `config/playertitle/titles.json`，示例：

```json
[
  {
    "id": 1,
    "name": "新手玩家",
    "displayOrder": 100,
    "color": "#55FF55",
    "conditions": [
      {
        "type": "PLAYER_LEVEL",
        "value": 5
      }
    ],
    "category": "新手",
    "icon": "crown.png",
    "iconColor": "#FFD700",
    "styleMode": "STATIC",
    "baseColors": ["#FF0000", "#00FF00", "#0000FF"],
    "animationProfile": {
      "type": "NONE",
      "speed": 1.0
    }
  }
]
```

#### 图标配置

将 PNG 图标文件放入 `config/playertitle/icons/` 目录：
- 支持 16×16 和 32×32 像素
- 系统会自动扫描并分配 Unicode 字符
- 图标文件名即为称号配置中的 `icon` 字段值

## 📚 文档

- [架构设计](./docs/architecture.md) - 系统架构和技术选型
- [安装配置指南](./docs/installation.md) - 详细安装和配置步骤
- [API 参考](./docs/api.md) - 事件系统和称号服务 API
- [事件系统文档](./docs/events.md) - 跨服事件通信机制
- [图标系统文档](./docs/icons.md) - 自定义图标使用指南
- [贡献指南](./docs/contributing.md) - 开发贡献指南

## 🏗️ 系统架构

### 组件图

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Forge 服务器  │    │   Redis 集群    │    │  Velocity 代理   │
│                 │    │                 │    │                 │
│  ┌───────────┐  │    │  ┌───────────┐  │    │  ┌───────────┐  │
│  │ 称号服务   │◄─┼────┼─►│ 事件总线   │◄─┼────┼─►│ 标签页插件  │  │
│  └───────────┘  │    │  └───────────┘  │    │  └───────────┘  │
│  ┌───────────┐  │    │                 │    │  ┌───────────┐  │
│  │ 图标管理器 │  │    │                 │    │  │ 聊天适配器  │  │
│  └───────────┘  │    │                 │    │  └───────────┘  │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                        │
         ▼                        ▼
┌─────────────────┐    ┌─────────────────┐
│   Forge 客户端   │    │   其他 Forge    │
│                 │    │     服务器       │
│  ┌───────────┐  │    │                 │
│  │ 字体资源包 │  │    │                 │
│  └───────────┘  │    │                 │
└─────────────────┘    └─────────────────┘
```

### 技术栈

- **服务端框架**：Minecraft Forge 1.20.1
- **网络代理**：Velocity 3.3.0
- **事件总线**：Redis Pub/Sub
- **数据存储**：JSON 配置文件 + MySQL（可选）
- **构建工具**：Gradle
- **编程语言**：Java 17

## 🔧 开发指南

### 项目结构

```
src/main/java/com/kavinshi/playertitle/
├── bootstrap/          # 启动和初始化
├── client/            # 客户端专用代码
├── config/            # 配置管理
├── icon/              # 图标系统
├── player/            # 玩家状态管理
├── service/           # 业务服务
├── sync/              # 跨服同步
└── title/             # 称号核心逻辑
```

### 构建项目

```bash
# 克隆项目
git clone <repository-url>
cd kavinshititlesystem-1.20.1forge

# 构建模组
./gradlew build

# 运行测试
./gradlew test

# 生成文档
./gradlew javadoc
```

### 开发环境

1. **Java 开发工具包**：JDK 17
2. **集成开发环境**：IntelliJ IDEA 或 Eclipse
3. **Minecraft 开发环境**：Forge MDK
4. **版本控制**：Git

## 🤝 贡献指南

我们欢迎所有形式的贡献！请参阅 [贡献指南](./docs/contributing.md) 了解详细信息。

### 报告问题

请在 GitHub Issues 中报告问题，包括：
- 问题描述
- 重现步骤
- 期望行为
- 实际行为
- 相关日志

### 提交代码

1. Fork 本仓库
2. 创建功能分支 (`git checkout -b feature/amazing-feature`)
3. 提交更改 (`git commit -m 'Add some amazing feature'`)
4. 推送到分支 (`git push origin feature/amazing-feature`)
5. 创建 Pull Request

## 📄 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](./LICENSE) 文件了解详情。

## 🙏 致谢

- **Minecraft Forge** - 提供强大的模组开发框架
- **Velocity** - 高性能 Minecraft 代理
- **Redis** - 快速的内存数据存储
- **solmochi/titlesystem** - 提供基础称号系统参考实现
- **所有贡献者** - 感谢你们的宝贵贡献

## 📞 联系支持

- **GitHub Issues**：[问题报告](https://github.com/your-org/kavinshi-playertitle/issues)
- **Discord 频道**：[加入讨论](https://discord.gg/your-invite-link)
- **文档网站**：[在线文档](https://docs.kavinshi.com/playertitle)

---

**注意**：本项目仍在积极开发中，API 可能会有变动。生产环境使用前请充分测试。