# kavinshi's PlayerTitle 系统完整设计

## 项目概述

**目标：** 在 `Velocity + Forge` 多服架构下，实现玩家称号的跨服一致展示，支持自定义PNG图标、RGB渐变与动态跑马灯效果。

**核心场景：**
- 跨服聊天显示玩家当前称号（含图标）
- Proxy侧Tab列表显示玩家当前称号
- Velocitab-1.6.1适配显示
- 玩家切服、重连、代理重启后称号展示恢复
- 管理员可配置PNG图标作为称号前缀

**技术栈：**
- **Forge端：** Java 17 + Forge 1.20.1 + JUnit 5 + Gson + Lettuce
- **Velocity端：** Java 17 + Velocity API 3.3.0 + Adventure/MiniMessage + Lettuce
- **图标系统：** 自定义字体 + 资源包 + PUA编码映射
- **同步机制：** Redis Pub/Sub + 事件版本控制

## 架构总览

### 系统架构图

```
Forge 模组（权威业务层）
    ├── 称号业务逻辑（解锁、装备、配置）
    ├── 图标资源管理器（PNG→字体映射）
    ├── 资源包生成器（内置资源包）
    └── Redis事件网关（发布状态变化）
            ↓
        Redis（跨服事件总线）
            ↓
    Velocity 插件（展示协调层）
        ├── Redis事件订阅器
        ├── 代理展示缓存（带版本控制）
        ├── 图标渲染器（降级策略）
        ├── RGB效果渲染服务
        ├── 跨服聊天格式化器
        ├── Tab列表同步服务
        └── Velocitab适配桥接
            ↓
    展示层（最终用户可见）
        ├── 跨服聊天
        ├── 原生Tab列表
        └── Velocitab（可选）
```

### 设计原则

1. **单一权威：** 称号状态只在后端Forge模组写入
2. **展示分层：** 聊天、Tab、玩家列表展示统一在Proxy层处理
3. **事件驱动：** 只同步状态变化，不做全量高频广播
4. **可替换介质：** 跨服总线通过`ClusterSyncGateway`抽象，先对接Redis
5. **降级兼容：** PNG图标不可用时自动降级为Unicode字符
6. **版本控制：** 每个玩家拥有单调递增`revision`，解决事件乱序

## 第一部分：Forge端权威层设计

### 1.1 图标资源系统

#### 目录结构
```
config/titlesystem/
├── icons/                    # 管理员放置PNG图标的目录
│   ├── crown.png            (16x16或32x32 PNG)
│   ├── sword.png
│   └── star.png
├── titles.json              # 称号配置，引用图标文件名
└── font_mappings.json       # 自动生成，图标→PUA编码映射
```

#### 图标配置示例
```json
{
  "id": 1,
  "name": "Ancient Warden",
  "icon": "crown.png",           # 引用icons/目录下的文件名
  "fallbackChar": "✦",          # 资源包不可用时使用的Unicode字符
  "iconColor": "#FFD700",       # 图标颜色（HEX）
  "styleMode": "STATIC_GRADIENT",
  "baseColors": ["#FF0000", "#00FF00", "#0000FF"],
  "animationProfile": {
    "cycleMillis": 2000,
    "stepSize": 10
  }
}
```

#### 核心组件

**IconManager**
- 扫描`config/titlesystem/icons/`目录下的PNG文件
- 验证图片尺寸（16x16或32x32）
- 为每个图标分配PUA（Private Use Area）编码（如U+E000, U+E001）
- 生成图标定义列表

**FontGenerator**
- 将所有PNG图标打包成纹理图集`glyphs.png`
- 生成字体定义文件`assets/titlesystem/fonts/icons.json`
- 创建PUA编码到纹理坐标的映射

**TitleResourcePack**
- 动态注册内置资源包
- 包含`glyphs.png`和`icons.json`等资源文件
- 客户端自动加载，无需手动安装

### 1.2 跨服同步核心模块

#### 包结构
```
com.kavinshi.playertitle.sync/
├── ClusterEventType.java           # 事件类型枚举
├── ClusterSyncEvent.java          # 跨服事件数据模型
├── ClusterRevisionService.java    # 玩家版本号生成器
├── TitleEventFactory.java         # 事件构造工厂
├── ClusterSyncGateway.java        # 同步网关接口
├── NoopClusterSyncGateway.java    # 本地空实现（测试/开发）
└── RedisClusterSyncGateway.java   # Redis实现
```

#### 事件模型
```java
public class ClusterSyncEvent {
    // 协议控制
    private int schemaVersion = 1;
    private String eventId;           // UUID，用于去重
    private int revision;            // 玩家单调递增版本号
    
    // 玩家信息
    private UUID playerUuid;
    private String playerName;
    private String serverName;
    
    // 称号信息
    private int titleId;
    private String titleDisplay;
    private int titleWeight;
    private String titleColor;
    
    // 图标与样式
    private String iconId;           // "crown.png"
    private String iconChar;         // "\ue000" (PUA编码)
    private String iconColor;        // "#FFD700"
    private TitleStyleMode styleMode;
    private List<String> baseColors;
    private TitleAnimationProfile animationProfile;
    
    // 元数据
    private ClusterEventType eventType;
    private long timestamp;
}
```

#### 事件类型
| 事件类型 | 触发时机 | 载荷内容 |
|----------|----------|----------|
| `STATE_SNAPSHOT` | 玩家登录、切服 | 玩家当前完整状态 |
| `TITLE_UNLOCKED` | 解锁新称号 | 解锁的称号ID和详情 |
| `TITLE_EQUIPPED` | 装备称号 | 装备的称号信息 |
| `TITLE_UNEQUIPPED` | 卸下称号 | 卸下的称号ID |
| `TITLE_REVOKED` | 称号被撤销 | 被撤销的称号ID |
| `PLAYER_DISCONNECT` | 玩家离线 | 玩家UUID |

### 1.3 业务服务集成

**TitleProgressService扩展**
- 在`unlockTitle`方法中生成`TITLE_UNLOCKED`事件素材
- 调用`TitleEventFactory.createUnlockEvent()`

**TitleEquipService扩展**
- 在`equipTitle`方法中生成`TITLE_EQUIPPED`事件
- 在`unequipTitle`方法中生成`TITLE_UNEQUIPPED`事件

**PlayerStateLifecycleHandler扩展**
- 在玩家登录时生成`STATE_SNAPSHOT`事件
- 通过`ClusterSyncGateway`发布事件

### 1.4 Redis客户端集成

**依赖：** `io.lettuce:lettuce-core`

**配置示例：**
```properties
# titlesystem-redis.properties
redis.host=localhost
redis.port=6379
redis.password=
redis.channel.prefix=titlesystem:events
redis.timeout.ms=5000
```

**RedisClusterSyncGateway实现要点：**
- 连接池管理
- JSON序列化/反序列化
- 异步发布，错误重试
- 连接状态监控

## 第二部分：Velocity展示协调层设计

### 2.1 项目结构
```
velocity-playertitle/
├── build.gradle                    # 构建配置
├── settings.gradle
├── src/main/java/
│   └── com/kavinshi/playertitle/proxy/
│       ├── ProxyTitlePlugin.java           # 插件入口
│       ├── sync/
│       │   ├── ProxyTitleView.java         # 展示视图模型
│       │   ├── ProxyTitleCache.java        # 带版本的缓存
│       │   └── RedisClusterSubscriber.java # Redis事件订阅器
│       ├── render/
│       │   ├── TitleRenderService.java     # 称号渲染服务
│       │   ├── IconRenderer.java          # 图标渲染器
│       │   └── AnimatedFrameService.java   # 动态跑马灯帧更新
│       ├── chat/
│       │   └── ProxyChatFormatter.java    # 聊天格式化器
│       ├── tab/
│       │   └── ProxyTabSyncService.java   # Tab展示同步
│       └── velocitab/
│           └── VelocitabBridge.java       # Velocitab适配桥接
└── src/main/resources/
    └── velocity-plugin.json               # 插件元数据
```

### 2.2 代理展示缓存

**ProxyTitleCache核心逻辑：**
```java
public class ProxyTitleCache {
    // UUID -> (revision, ProxyTitleView)
    private final ConcurrentHashMap<UUID, CacheEntry> cache;
    
    public boolean applyEvent(ClusterSyncEvent event) {
        UUID playerId = event.getPlayerUuid();
        int eventRevision = event.getRevision();
        
        return cache.compute(playerId, (uuid, existing) -> {
            // 去重：跳过旧版本事件
            if (existing != null && existing.revision >= eventRevision) {
                return existing; // 幂等处理
            }
            
            // 应用事件更新展示视图
            ProxyTitleView newView = applyEventToView(event, existing);
            return new CacheEntry(eventRevision, newView);
        });
    }
}
```

**恢复策略：**
1. **Redis重连恢复：** 检测到Redis连接恢复后，标记缓存为"待校准"状态
2. **快照优先：** `STATE_SNAPSHOT`事件覆盖所有增量事件
3. **幽灵清理：** 定期对比Velocity在线玩家列表，清理无效缓存
4. **超时降级：** 长时间未更新的缓存项自动清除

### 2.3 图标渲染器

**图标渲染策略矩阵：**

| 场景 | 客户端支持 | Velocity处理 | 输出结果 |
|------|-----------|-------------|----------|
| **Forge客户端聊天** | ✅ 有资源包 | 传输PUA字符`\ue000` | 显示PNG图标 |
| **Velocity代理聊天** | ❌ 无资源包 | 转换为Unicode备用字符 | 显示`✦`字符 |
| **Tab列表展示** | 混合 | 使用MiniMessage组件 | 带颜色的文本 |

**IconRenderer实现：**
```java
public class IconRenderer {
    private final Map<String, IconMapping> iconMappings;
    
    public Component renderIcon(String iconId, String iconColor) {
        IconMapping mapping = iconMappings.get(iconId);
        if (mapping == null) {
            return Component.text(""); // 无图标
        }
        
        // 策略1：如果聊天插件支持Base64图片
        if (canRenderImages()) {
            return renderAsImage(mapping);
        }
        
        // 策略2：使用Unicode备选字符（降级方案）
        return Component.text(mapping.fallbackChar())
                       .color(NamedTextColor.hex(iconColor));
    }
}
```

### 2.4 RGB效果渲染服务

**静态渐变渲染：**
```java
public Component renderStaticGradient(String text, List<String> colors) {
    int length = text.length();
    List<Component> parts = new ArrayList<>();
    
    for (int i = 0; i < length; i++) {
        // 计算当前字符的颜色插值
        TextColor color = interpolateColor(colors, i, length);
        parts.add(Component.text(text.charAt(i)).color(color));
    }
    
    return Component.join(JoinConfiguration.noSeparators(), parts);
}
```

**动态跑马灯处理：**
- **聊天消息：** 使用发送瞬间的固定帧（固化）
- **Tab展示：** `AnimatedFrameService`持续更新帧
- **Velocitab：** 提供当前帧的快照值

### 2.5 跨服聊天格式化

**消息格式：** `[图标 称号] 玩家名: 消息内容`

**示例：**
- 完整支持：`[✦ Ancient Warden] PlayerName: hello`
- 无图标：`[Ancient Warden] PlayerName: hello`
- 无称号：`PlayerName: hello`

**ProxyChatFormatter关键逻辑：**
```java
public Component formatChatMessage(UUID sender, String originalMessage) {
    ProxyTitleView view = cache.getView(sender);
    if (view == null || view.titleId() < 0) {
        return Component.text(originalMessage); // 无称号
    }
    
    // 构建称号前缀组件
    Component titlePrefix = buildTitlePrefix(view);
    
    // 返回完整消息
    return Component.join(
        JoinConfiguration.separator(Component.text(" ")),
        titlePrefix,
        Component.text(view.playerName()),
        Component.text(": " + originalMessage)
    );
}
```

### 2.6 Tab列表同步

**显示格式：** `图标 玩家名 (称号)`

**示例：** `✦ PlayerName (Ancient Warden)`

**更新时机：**
1. 接收到玩家称号变化事件
2. 玩家加入/离开服务器
3. 动态跑马灯帧更新
4. 缓存校准完成

### 2.7 Velocitab适配

**提供的Placeholder：**
- `%title_display%` - 称号显示文本
- `%title_weight%` - 称号权重（用于排序）
- `%title_color%` - 称号颜色（HEX）
- `%title_icon%` - 图标ID或字符
- `%title_style_mode%` - 样式模式（PLAIN/STATIC_GRADIENT/ANIMATED_CHROMA）

**排序优先级：**
1. `title_weight`（降序）
2. 权限组权重（如果适用）
3. 服务器分组
4. 玩家名（字母序）

### 2.8 构建配置

**build.gradle核心依赖：**
```groovy
dependencies {
    // Velocity API
    implementation 'com.velocitypowered:velocity-api:3.3.0'
    annotationProcessor 'com.velocitypowered:velocity-api:3.3.0'
    
    // Redis客户端
    implementation 'io.lettuce:lettuce-core:6.3.2.RELEASE'
    
    // Adventure文本处理
    implementation 'net.kyori:adventure-api:4.16.0'
    implementation 'net.kyori:adventure-text-minimessage:4.16.0'
    
    // 配置处理
    implementation 'org.spongepowered:configurate-yaml:4.1.2'
    
    // 测试
    testImplementation 'org.junit.jupiter:junit-jupiter:5.10.0'
    testImplementation 'org.mockito:mockito-core:5.5.0'
}
```

## 第三部分：实施顺序与任务分解

### 阶段一：Forge权威层完善（第1-2周）

**目标：** 完成称号业务核心，建立跨服事件模型，实现图标资源系统。

**任务分解：**
1. **图标资源管理器** - PNG图标扫描、验证、PUA编码分配
2. **字体映射生成器** - 生成纹理图集和字体定义文件
3. **资源包注册器** - 动态注册内置资源包
4. **跨服同步核心模块** - 事件模型、版本控制、网关接口
5. **业务服务集成** - 将事件发布接入解锁/装备服务
6. **配置扩展** - 支持图标引用和RGB样式配置

### 阶段二：Redis真实适配与Velocity骨架（第3周）

**目标：** 接入真实Redis客户端，创建Velocity插件项目骨架。

**任务分解：**
1. **Redis客户端集成** - Lettuce客户端配置与连接管理
2. **Redis事件序列化** - 事件对象到JSON的序列化/反序列化
3. **Velocity项目创建** - 构建配置、插件入口、依赖管理
4. **事件模型共享** - 创建共享DTO模块或复制事件定义

### 阶段三：Velocity展示协调层实现（第4-5周）

**目标：** 实现事件消费、缓存管理、聊天和Tab展示。

**任务分解：**
1. **Redis事件订阅器** - 订阅事件、反序列化、分发
2. **代理展示缓存** - 带版本控制的缓存实现
3. **图标渲染器** - 图标降级策略和渲染逻辑
4. **RGB效果渲染服务** - 静态渐变和动态跑马灯渲染
5. **跨服聊天格式化器** - 聊天消息前缀添加
6. **Tab列表同步服务** - 原生Tab展示更新
7. **恢复策略实现** - Redis重连、缓存校准、幽灵清理

### 阶段四：Velocitab适配与联调验证（第6周）

**目标：** 完成Velocitab集成，进行端到端测试验证。

**任务分解：**
1. **Velocitab桥接器** - 提供占位符和MiniPlaceholders
2. **配置模板生成** - 生成Velocitab配置示例
3. **端到端测试场景** - 设计完整测试用例
4. **异常处理完善** - 网络异常、配置错误处理
5. **文档与部署指南** - 安装、配置、运维文档

## 第四部分：验证矩阵与质量保证

### 4.1 功能验证场景

| 场景 | 预期结果 | 验证方法 |
|------|----------|----------|
| **正常解锁称号** | 聊天立即显示新称号 | 解锁后发送聊天消息 |
| **装备/卸下称号** | Tab列表实时更新 | 观察Tab列表变化 |
| **玩家切服** | 称号保持一致 | 切换服务器后检查 |
| **Redis断连恢复** | 自动重连并校准 | 模拟Redis服务重启 |
| **资源包缺失** | 降级为Unicode字符 | 移除资源包后测试 |
| **Velocity重启** | 通过快照恢复缓存 | 重启Velocity服务 |
| **事件乱序** | 以reversion为准 | 模拟乱序事件发送 |
| **重复事件** | 幂等处理，不重复应用 | 发送相同事件多次 |

### 4.2 性能指标

| 指标 | 目标值 | 测量方法 |
|------|--------|----------|
| **事件发布延迟** | < 50ms | 从业务变化到Redis发布 |
| **事件处理延迟** | < 100ms | 从Redis接收到缓存更新 |
| **聊天渲染延迟** | < 10ms | 消息格式化时间 |
| **内存占用** | < 50MB | 缓存1000名玩家数据 |
| **Redis连接池** | 10-20连接 | 根据服务器数量调整 |

### 4.3 兼容性测试

**聊天插件兼容模式：**
1. **完全接管模式** - 本插件负责所有聊天格式化
2. **占位符模式** - 提供称号占位值，由现有插件渲染
3. **桥接模式** - 在现有插件输出前后追加称号组件

**客户端兼容性：**
1. **有资源包客户端** - 显示PNG图标
2. **无资源包客户端** - 显示Unicode字符
3. **旧版本客户端** - 降级为纯文本显示

## 第五部分：风险与缓解措施

| 风险 | 可能性 | 影响 | 缓解措施 |
|------|--------|------|----------|
| **PNG图标渲染兼容性** | 中 | 高 | 提供Unicode备选方案，分阶段实施 |
| **Redis性能瓶颈** | 低 | 中 | 事件精简设计，连接池优化，监控告警 |
| **跨服聊天插件冲突** | 中 | 高 | 提供多种集成模式，配置开关 |
| **资源包分发问题** | 中 | 中 | 内置资源包 + 可选外部包，提供安装指南 |
| **版本升级兼容性** | 低 | 高 | Schema版本控制，向后兼容设计 |
| **网络分区问题** | 低 | 高 | 本地缓存降级，重试机制，手动同步命令 |
| **内存泄漏风险** | 低 | 高 | 定期缓存清理，弱引用使用，监控内存使用 |

## 第六部分：部署与运维

### 6.1 部署要求

**服务器要求：**
- Redis 6.0+（支持Pub/Sub）
- Velocity 3.3.0+
- Forge 1.20.1服务器

**网络要求：**
- Forge服务器可访问Redis
- Velocity服务器可访问Redis
- 客户端可下载资源包（如果使用外部包）

### 6.2 配置示例

**Forge端配置（titlesystem-redis.properties）：**
```properties
redis.host=127.0.0.1
redis.port=6379
redis.password=
redis.channel=titlesystem:events:prod
redis.timeout.ms=5000
redis.pool.size=10
```

**Velocity端配置（velocity-playertitle.conf）：**
```yaml
redis:
  host: "127.0.0.1"
  port: 6379
  password: ""
  channel: "titlesystem:events:prod"
  
chat:
  enabled: true
  format: "[{icon} {title}] {player}: {message}"
  icon-fallback: true
  
tab:
  enabled: true
  format: "{icon} {player} ({title})"
  
velocitab:
  enabled: true
  placeholders:
    title-display: "{title}"
    title-weight: "{weight}"
    title-icon: "{icon}"
```

### 6.3 监控与告警

**监控指标：**
- Redis连接状态
- 事件发布/消费速率
- 缓存命中率
- 内存使用情况
- 玩家称号覆盖率

**告警阈值：**
- Redis连接失败 > 3次/分钟
- 事件处理延迟 > 200ms
- 内存使用 > 80%
- 缓存校准失败率 > 10%

## 附录

### A. Unicode备选字符表

| 图标类型 | Unicode字符 | 示例 |
|----------|-------------|------|
| 皇冠 | U+2726 (✦) | ✦ |
| 剑 | U+2694 (⚔️) | ⚔️ |
| 盾 | U+26E8 (⛨) | ⛨ |
| 星星 | U+2605 (★) | ★ |
| 勋章 | U+1F396 (🎖️) | 🎖️ |

### B. 事件Schema版本历史

| 版本 | 变更内容 | 兼容性 |
|------|----------|--------|
| v1.0 | 初始版本，基础字段 | - |
| v1.1 | 添加图标相关字段 | 向后兼容 |
| v1.2 | 添加RGB样式字段 | 向后兼容 |
| v2.0 | 协议重构（如需要） | 向前不兼容 |

### C. 故障排除指南

**常见问题：**
1. **图标不显示** - 检查资源包是否加载，客户端日志
2. **称号不同步** - 检查Redis连接，查看事件日志
3. **聊天格式错误** - 检查聊天插件冲突，调整集成模式
4. **内存占用过高** - 检查缓存清理策略，调整超时时间

**调试命令：**
- `/title debug cache` - 查看缓存状态
- `/title debug events` - 查看最近事件
- `/title debug redis` - 测试Redis连接
- `/title sync <player>` - 手动触发同步

---
*设计文档版本：1.0*
*最后更新：2026-04-20*
*设计状态：已批准*