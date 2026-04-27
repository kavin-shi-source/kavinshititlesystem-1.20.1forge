# Kavinshi PlayerTitle

Minecraft Forge 1.20.1 称号管理模组，支持跨服同步（MySQL + Velocity）。

## 架构

```
Forge 模组（权威业务） -> MySQL（持久化与并发控制） -> Velocity 插件（跨服展示协调） -> 聊天/Tab/Velocitab
```

## 模块结构

- `titlesystem-common` — 共享领域模型、事件、网络包
- `titlesystem-server` — 服务端业务逻辑、Forge 集成、数据库同步
- `titlesystem-client` — 客户端渲染、字体资源包、图标显示

## 环境要求

- Minecraft 1.20.1 / Forge 47.4.0+ / Java 17
- MySQL（跨服同步时必须）或 SQLite（仅单服运行）

## 构建

```bash
./gradlew build
./gradlew test
```

## 配置

- 称号配置：`config/playertitle/titles.json`
- 图标目录：`config/playertitle/icons/`（16x16 或 32x32 PNG）
- 集群配置：`config/playertitle/cluster.json`

## 状态

早期开发中，参考 solmochi/titlesystem 重写。
