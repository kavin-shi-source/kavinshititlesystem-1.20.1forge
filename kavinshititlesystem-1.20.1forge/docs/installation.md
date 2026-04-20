# 安装配置指南

## 📋 概述

本文档提供 Kavinshi PlayerTitle 跨服称号系统的完整安装和配置指南，涵盖服务端模组、客户端模组以及 Velocity 插件的安装步骤。

## 🎯 系统要求

### 最低配置

| 组件 | 要求 | 说明 |
|------|------|------|
| **操作系统** | Windows 10 / Linux / macOS | 64位操作系统 |
| **Java** | JDK 17 或更高版本 | 必须使用 Java 17 |
| **Minecraft** | 1.20.1 | 客户端和服务端版本必须一致 |
| **Forge** | 47.2.0 或更高版本 | 服务端模组框架 |
| **内存** | 服务器：4GB+，客户端：2GB+ | 建议分配更多内存以获得更好性能 |
| **磁盘空间** | 至少 100MB 可用空间 | 用于存储配置文件和图标 |

### 推荐配置

| 组件 | 推荐配置 | 说明 |
|------|----------|------|
| **操作系统** | Linux (Ubuntu 22.04 LTS) | 生产环境推荐使用 Linux |
| **Java** | JDK 17.0.8+ | 使用长期支持版本 |
| **内存** | 服务器：8GB+，客户端：4GB+ | 大型服务器网络需要更多内存 |
| **Redis** | Redis 7.0+ | 用于跨服事件同步 |
| **网络** | 稳定的网络连接 | 低延迟网络提升跨服体验 |

## 🚀 安装步骤

### 第一阶段：服务端模组安装

#### 步骤 1：下载模组文件

从以下渠道获取最新版本的模组文件：

1. **官方发布页**：下载 `kavinshi-playertitle-X.X.X.jar`
2. **构建系统**：自行构建（见开发指南）
3. **镜像站点**：备用下载链接

#### 步骤 2：安装到 Forge 服务器

1. 停止 Minecraft 服务器
2. 将模组文件复制到服务器的 `mods` 目录：
   ```
   /path/to/your/server/mods/kavinshi-playertitle-X.X.X.jar
   ```
3. 启动服务器，等待模组初始化完成
4. 检查服务器日志，确认模组加载成功：

   ```
   [Server thread/INFO] [com.kavinshi.playertitle.KavinshiPlayerTitleMod/]: 
   Kavinshi PlayerTitle mod v1.0.0 initialized
   [Server thread/INFO] [com.kavinshi.playertitle.bootstrap.RewriteBootstrap/]: 
   PlayerTitle components initialized successfully
   ```

#### 步骤 3：验证安装

1. 在游戏中执行命令：`/playertitle version`
2. 预期输出：显示模组版本信息
3. 检查配置文件是否生成：
   ```
   /config/playertitle/titles.json
   /config/playertitle/icons/
   ```

### 第二阶段：客户端模组安装

#### 步骤 1：下载客户端模组

**注意**：客户端模组版本必须与服务端模组版本完全一致。

#### 步骤 2：安装到客户端

1. 关闭 Minecraft 客户端
2. 将模组文件复制到客户端的 `mods` 目录：
   ```
   %appdata%/.minecraft/mods/kavinshi-playertitle-X.X.X.jar
   ```
3. 启动 Minecraft 客户端
4. 选择 Forge 1.20.1 配置文件启动

#### 步骤 3：验证客户端安装

1. 进入游戏后，检查资源包列表：
   - 应出现 "PlayerTitle Fonts" 资源包
   - 资源包应处于启用状态

2. 检查游戏日志：
   ```
   [Client thread/INFO] [com.kavinshi.playertitle.client.ClientModInitializer/]: 
   PlayerTitle font resource pack registered: kavinshiplayertitle_fonts
   ```

### 第三阶段：Velocity 插件安装（可选）

#### 步骤 1：下载 Velocity 插件

从官方发布页下载：`kavinshi-playertitle-velocity-X.X.X.jar`

#### 步骤 2：安装到 Velocity

1. 停止 Velocity 代理服务
2. 将插件文件复制到 Velocity 的 `plugins` 目录：
   ```
   /path/to/velocity/plugins/kavinshi-playertitle-velocity-X.X.X.jar
   ```
3. 启动 Velocity 服务

#### 步骤 3：配置 Velocity

编辑 Velocity 配置文件 `velocity.toml`：

```toml
[servers]
# 配置你的游戏服务器
survival = "127.0.0.1:25565"
creative = "127.0.0.1:25566"

[player-info]
# 启用标签页支持
forwarding-mode = "modern"
```

#### 步骤 4：验证 Velocity 插件

1. 检查 Velocity 日志：
   ```
   [INFO] [kavinshi-playertitle]: PlayerTitle Velocity plugin v1.0.0 enabled
   [INFO] [kavinshi-playertitle]: Connected to Redis event bus
   ```

2. 在 Velocity 代理的游戏中验证：
   - 标签页应显示玩家称号
   - 跨服聊天应同步称号信息

## ⚙️ 配置说明

### 核心配置文件

#### 1. 称号定义配置 (`titles.json`)

**文件位置**：`config/playertitle/titles.json`

**完整示例**：
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
      },
      {
        "type": "PLAYTIME_HOURS",
        "value": 10
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
  },
  {
    "id": 2,
    "name": "资深冒险家",
    "displayOrder": 200,
    "color": "#FFAA00",
    "conditions": [
      {
        "type": "QUEST_COMPLETED",
        "value": 50
      }
    ],
    "category": "冒险",
    "icon": "sword.png",
    "iconColor": "#C0C0C0",
    "styleMode": "GRADIENT",
    "baseColors": ["#FF0000", "#FFFF00", "#00FF00"],
    "animationProfile": {
      "type": "PULSE",
      "speed": 2.0,
      "intensity": 0.8
    }
  }
]
```

**字段说明**：

| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| `id` | integer | 是 | 称号的唯一标识符 |
| `name` | string | 是 | 称号显示名称 |
| `displayOrder` | integer | 是 | 显示顺序，数字越小越靠前 |
| `color` | string | 是 | 称号文字颜色，HEX格式 |
| `conditions` | array | 是 | 解锁条件数组 |
| `category` | string | 否 | 称号分类，用于分组显示 |
| `icon` | string | 否 | 图标文件名（不带路径） |
| `iconColor` | string | 否 | 图标颜色，HEX格式 |
| `styleMode` | string | 是 | 样式模式：STATIC, RANDOM, SEQUENTIAL, GRADIENT |
| `baseColors` | array | 否 | 基础颜色列表，用于特定样式模式 |
| `animationProfile` | object | 否 | 动画配置 |

#### 2. 图标配置

**图标目录**：`config/playertitle/icons/`

**图标要求**：
- **格式**：PNG
- **尺寸**：16×16 或 32×32 像素
- **命名**：英文或数字，不要包含中文或特殊字符
- **示例文件**：
  - `crown.png` - 皇冠图标
  - `star.png` - 星星图标
  - `sword.png` - 剑图标
  - `shield.png` - 盾牌图标

**自动扫描**：
系统启动时会自动扫描图标目录，为每个 PNG 文件分配唯一的 Unicode 字符（U+E000 到 U+F8FF）。

#### 3. Redis 配置

**配置文件**：`config/playertitle/redis.properties`

**示例配置**：
```properties
# Redis 连接配置
redis.enabled=true
redis.host=127.0.0.1
redis.port=6379
redis.password=
redis.database=0
redis.timeout=5000
redis.pool.maxTotal=8
redis.pool.maxIdle=4
redis.pool.minIdle=1
```

**说明**：
- `redis.enabled`：是否启用 Redis 事件总线
- 如果禁用，将使用本地事件总线（仅单服务器有效）

#### 4. 数据库配置（可选）

**配置文件**：`config/playertitle/database.properties`

**示例配置**：
```properties
# MySQL 数据库配置
database.type=mysql
database.host=127.0.0.1
database.port=3306
database.name=playertitle
database.username=root
database.password=your_password
database.pool.size=5
```

### 高级配置

#### 1. 缓存配置

```properties
# 玩家状态缓存配置
cache.playerState.enabled=true
cache.playerState.expireMinutes=30
cache.playerState.maxSize=1000

# 称号定义缓存配置
cache.titleDefinitions.enabled=true
cache.titleDefinitions.expireMinutes=60

# Redis 二级缓存配置
cache.redis.enabled=true
cache.redis.expireHours=24
```

#### 2. 事件总线配置

```properties
# 事件总线配置
eventbus.type=redis  # redis 或 local
eventbus.retry.maxAttempts=3
eventbus.retry.delayMillis=1000
eventbus.batch.enabled=true
eventbus.batch.size=10
eventbus.batch.delayMillis=100
```

#### 3. 性能调优配置

```properties
# 线程池配置
threadpool.coreSize=4
threadpool.maxSize=8
threadpool.queueCapacity=100
threadpool.keepAliveSeconds=60

# 内存配置
memory.maxCachedPlayers=500
memory.iconCache.size=50
```

## 🔧 配置验证

### 1. 配置文件验证

系统启动时会自动验证配置文件格式，错误示例：

```json
[
  {
    "id": "invalid",  // 错误：id 应该是数字
    "name": "测试称号",
    "displayOrder": 100,
    "color": "not_hex",  // 错误：颜色格式不正确
    "conditions": []
  }
]
```

**验证错误处理**：
- 格式错误：跳过该称号，记录错误日志
- 必填字段缺失：使用默认值或跳过
- 条件配置错误：忽略该条件

### 2. 图标文件验证

**验证规则**：
1. 文件必须是 PNG 格式
2. 尺寸必须是 16×16 或 32×32
3. 文件名不能包含特殊字符
4. 文件大小不超过 100KB

**验证失败处理**：
- 记录警告日志
- 跳过无效文件
- 继续处理其他文件

## 🚨 故障排除

### 常见问题

#### 问题 1：模组无法加载

**症状**：
- 服务器启动时报错
- 模组不出现在已加载模组列表中

**解决方案**：
1. 检查 Forge 版本是否匹配
2. 检查 Java 版本是否为 17
3. 查看日志文件中的详细错误信息
4. 确保模组文件没有损坏

#### 问题 2：配置文件不生成

**症状**：
- `config/playertitle` 目录不存在
- 没有生成 `titles.json` 文件

**解决方案**：
1. 检查服务器是否有写权限
2. 手动创建目录：`config/playertitle`
3. 从示例配置复制文件
4. 重启服务器

#### 问题 3：图标不显示

**症状**：
- 称号前面的图标显示为方框
- 客户端资源包未加载

**解决方案**：
1. 检查客户端是否安装了模组
2. 验证资源包是否启用
3. 检查图标文件格式和尺寸
4. 查看客户端日志中的字体加载信息

#### 问题 4：跨服同步失效

**症状**：
- 玩家切换服务器后称号不同步
- Velocity 标签页不显示称号

**解决方案**：
1. 检查 Redis 连接是否正常
2. 验证 Velocity 插件是否正确安装
3. 检查事件总线配置
4. 查看跨服事件日志

### 日志分析

#### 关键日志信息

**正常启动日志**：
```
[INFO] 加载了 15 个称号定义
[INFO] 扫描到 8 个图标文件
[INFO] Redis 事件总线连接成功
[INFO] 玩家状态存储初始化完成
```

**错误日志示例**：
```
[ERROR] 配置文件解析失败：titles.json (第 25 行)
[WARN] 图标文件无效：invalid.png (尺寸 64x64)
[ERROR] Redis 连接失败：Connection refused
```

#### 日志文件位置

- **服务端日志**：`logs/latest.log`
- **客户端日志**：`.minecraft/logs/latest.log`
- **Velocity 日志**：`logs/velocity.log`

## 🔄 配置热重载

### 1. 手动重载

**服务端命令**：
```
/playertitle reload
```

**命令权限**：
- `playertitle.reload` - 重载配置权限

### 2. 自动重载

**配置文件监控**：
系统会监控以下文件的更改：
- `titles.json`
- `categories.json`
- `icons/` 目录

**重载行为**：
- 新增称号：立即生效
- 修改称号：立即生效
- 删除称号：已解锁玩家不受影响
- 新增图标：需要客户端重新登录

### 3. 重载验证

**验证步骤**：
1. 执行重载命令
2. 检查控制台输出
3. 验证配置是否正确加载
4. 测试功能是否正常

## 📊 性能调优

### 1. 服务器调优

**内存调优**：
```properties
# JVM 参数示例
-Xms4G -Xmx8G -XX:+UseG1GC -XX:MaxGCPauseMillis=200
```

**线程池调优**：
- 根据 CPU 核心数调整线程池大小
- 监控线程池使用情况
- 避免线程饥饿

### 2. Redis 调优

**连接池调优**：
```properties
redis.pool.maxTotal=8
redis.pool.maxIdle=4
redis.pool.minIdle=1
```

**网络调优**：
- 使用低延迟网络
- 启用连接保持
- 配置合理的超时时间

### 3. 数据库调优（如果使用）

**索引优化**：
```sql
CREATE INDEX idx_player_id ON player_titles(player_id);
CREATE INDEX idx_title_id ON player_titles(title_id);
```

**查询优化**：
- 批量查询代替单条查询
- 合理使用缓存
- 避免全表扫描

## 🔐 安全配置

### 1. 权限配置

**默认权限节点**：

| 权限节点 | 说明 | 默认 |
|----------|------|------|
| `playertitle.use` | 使用称号系统 | true |
| `playertitle.reload` | 重载配置 | op |
| `playertitle.admin` | 管理命令 | op |

**权限配置示例**：
```yaml
# LuckPerms 权限配置示例
permissions:
  - "playertitle.use"
  - "playertitle.reload"
  - "playertitle.admin"
```

### 2. 数据安全

**加密配置**：
```properties
# 敏感数据加密
encryption.enabled=true
encryption.algorithm=AES/GCM/NoPadding
encryption.keyFile=/secure/keyfile.dat
```

**备份配置**：
```properties
# 自动备份配置
backup.enabled=true
backup.intervalHours=24
backup.retentionDays=7
backup.location=/backup/playertitle
```

## 🚀 生产环境部署

### 1. 部署清单

**预部署检查**：
- [ ] 所有服务器 Java 版本一致
- [ ] 模组版本一致
- [ ] 配置文件同步
- [ ] Redis 集群配置正确
- [ ] 网络防火墙开放必要端口

**端口要求**：

| 服务 | 端口 | 协议 | 说明 |
|------|------|------|------|
| Minecraft | 25565 | TCP | 游戏服务器 |
| Redis | 6379 | TCP | 事件总线 |
| Velocity | 25577 | TCP | 代理服务器 |

### 2. 监控部署

**监控指标**：
- 服务器性能指标
- Redis 连接状态
- 事件吞吐量
- 玩家在线状态

**告警配置**：
- Redis 连接失败
- 事件处理延迟过高
- 内存使用率超过阈值

### 3. 灾难恢复

**备份策略**：
- 每日自动备份配置文件
- 实时备份玩家数据
- 异地备份关键数据

**恢复流程**：
1. 停止所有服务
2. 恢复配置文件
3. 恢复玩家数据
4. 验证数据完整性
5. 逐步重启服务

---

**文档版本**：1.0.0  
**最后更新**：2026-04-20  
**维护者**：技术文档团队