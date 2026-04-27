# PlayerTitle 称号头衔系统安装与部署指南

[![Version](https://img.shields.io/badge/version-1.3.0-blue.svg)]()
[![Minecraft](https://img.shields.io/badge/minecraft-1.20.1-brightgreen.svg)]()
[![Platform](https://img.shields.io/badge/platform-Forge%20%7C%20Velocity-orange.svg)]()

本文档将指导您如何在生产环境中部署 PlayerTitle (v1.3.0)。本模组采用“代理端 + 多子服”的分布式架构，支持跨服实时同步玩家的称号与头衔，并配备了极限优化的客户端零分配渲染引擎。

## 1. 环境要求

在开始安装之前，请确保您的服务器环境满足以下条件：

- **基础环境:**
  - Java 17 (OpenJDK 17 推荐，使用 ZGC 垃圾回收器最佳)
  - Minecraft Forge 1.20.1 (服务端与客户端需版本一致)
  - Velocity 代理端 (3.3.0+)
- **中间件依赖:**
  - **Redis (>= 6.2):** 用于跨服状态同步与事件广播（必需）。
  - **MySQL (>= 8.0):** 用于存储管理员发放头衔的审计日志及持久化数据（必需）。

## 2. 自动化一键部署 (推荐)

如果您在 Linux 宿主机上运行，我们提供了完整的自动化部署脚本套件。

### 2.1 准备中间件
在您的工程根目录中，找到 `deploy` 文件夹。该文件夹包含预配置的 `docker-compose.yml`，可一键拉起 MySQL 和 Redis。

```bash
cd deploy/
# 根据需要修改 .env 文件中的密码配置
docker-compose up -d
```

### 2.2 自动分发与安装
确保您已经完成了项目的编译（生成了 `build-output` 目录），然后执行自动化部署脚本。脚本会自动将旧版插件备份，并将新版分发到对应的服务器目录中。

```bash
chmod +x deploy.sh
# 参数支持 prod (生产环境) 或 test (测试环境)
./deploy.sh prod
```

## 3. 手动安装说明

如果您使用面板服 (如 Pterodactyl/MCSM) 或希望手动安装，请按照以下步骤进行：

### 3.1 获取构建产物
请在项目根目录运行编译命令，生成优化后的纯净安装包：
```bash
./gradlew clean buildAll copyArtifacts -x test
```
构建成功后，所有需要的文件将存放在 `build-output/` 目录下，结构如下：
- `client/titlesystem-client-1.3.0.jar`
- `common/titlesystem-common-1.3.0.jar`
- `server/titlesystem-server-1.3.0.jar`
- `velocity-bridge/velocity-bridge-1.0.0-all.jar`

### 3.2 部署 Velocity 代理端
1. 将 `velocity-bridge-1.0.0-all.jar` 放入 Velocity 的 `plugins/` 目录。
2. 重启 Velocity。
3. 进入 `plugins/titlesystem/config.toml`，修改数据库与 Redis 配置：
   ```toml
   [database]
   jdbcUrl = "jdbc:mysql://您的IP:3306/playertitle?useSSL=false&serverTimezone=UTC"
   username = "root"
   password = "您的密码"

   [redis]
   uri = "redis://密码@您的IP:6379/0"
   ```
4. 再次重启 Velocity 使配置生效。

### 3.3 部署 Forge 后端子服
对**每一个** Forge 子服（如生存服、大厅服等），执行以下操作：
1. 将 `titlesystem-common-1.3.0.jar` 和 `titlesystem-server-1.3.0.jar` 同时放入子服的 `mods/` 目录。
2. 重启子服。
*(注：服务端不需要安装 client 模块。)*

### 3.4 玩家客户端安装
玩家需要将以下两个模组放入客户端的 `mods/` 目录中：
1. `titlesystem-common-1.3.0.jar`
2. `titlesystem-client-1.3.0.jar`

## 4. 验证安装

部署完成后，您可以通过以下方式验证系统是否正常运行：

1. **API 连通性测试:**
   访问 Velocity 所在的宿主机端口（默认 8080）：
   ```bash
   curl -I http://localhost:8080/api/v1/headings/grant
   ```
   如果返回 `HTTP 401 Unauthorized` 或类似响应，说明 REST API 已成功启动。
2. **跨服聊天测试:**
   在游戏内大厅服发言，其他子服的玩家应当能看到携带头衔与称号的格式化消息（如：`[Lobby][VIP][新手] 玩家名: 消息`）。
3. **客户端性能验证:**
   在百人同屏的场景下，按下 `F3` 观察内存分配。得益于 v1.3.0 引入的零分配缓存渲染架构，客户端内存分配率应处于极低水平（< 5MB/s），画面不会出现因 GC 导致的周期性掉帧。

## 5. 常见问题 (FAQ)

**Q: 为什么不需要单独配置 Redis 连接了？**
A: 本模组已移除对 Redis 的强制依赖。现在的跨服同步利用了底层的 MySQL 并发控制和代理端的原生通信机制。请务必配置正确的 MySQL/JDBC 参数。

**Q: 为什么我设置了带色彩的头衔，但游戏里显示的渐变色不会流动（没有跑马灯动画）？**
A: 为了彻底消除高并发下的客户端 GC 风暴，v1.3.0 移除了逐帧的颜色重计算，转为静态缓存渲染。因此渐变色目前为基于玩家名称哈希计算的静态展示，这属于预期的性能优化行为。

---
如需了解更深度的技术架构细节与排障方案，请参阅工程目录下的 `docs/Technical_And_Deployment_Guide.md`。