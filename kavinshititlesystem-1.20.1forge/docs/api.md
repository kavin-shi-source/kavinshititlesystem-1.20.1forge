# API 参考文档

## 📋 概述

本文档提供 Kavinshi PlayerTitle 系统的完整 API 参考，涵盖服务接口、事件模型、配置接口和扩展 API。

## 🎯 API 版本

**当前版本**：v1.0.0

**API 稳定性**：
- ✅ 稳定 API：称号服务、图标系统
- 🔄 开发中 API：事件总线扩展
- 🚧 实验性 API：自定义条件系统

## 🏗️ 核心服务 API

### 1. 称号服务接口

#### 1.1 TitleEquipService

**类路径**：`com.kavinshi.playertitle.service.TitleEquipService`

**描述**：处理玩家称号的装备和卸下操作。

**构造函数**：
```java
public TitleEquipService(
    TitleEventFactory eventFactory,
    ClusterEventBus eventBus
)
```

**方法**：

##### `equipTitle`
```java
public EquipResult equipTitle(UUID playerId, int titleId)
```

**参数**：
- `playerId` (`UUID`)：玩家唯一标识符
- `titleId` (`int`)：要装备的称号ID

**返回值**：`EquipResult` 枚举，包含以下可能值：
- `SUCCESS`：装备成功
- `ALREADY_EQUIPPED`：已经装备了该称号
- `TITLE_NOT_UNLOCKED`：称号未解锁
- `TITLE_NOT_FOUND`：称号不存在
- `CONDITION_NOT_MET`：装备条件不满足
- `ERROR`：未知错误

**使用示例**：
```java
TitleEquipService equipService = ...;
UUID playerId = ...;
int titleId = 1;

EquipResult result = equipService.equipTitle(playerId, titleId);
if (result == EquipResult.SUCCESS) {
    // 装备成功处理
}
```

##### `unequipTitle`
```java
public EquipResult unequipTitle(UUID playerId)
```

**参数**：
- `playerId` (`UUID`)：玩家唯一标识符

**返回值**：`EquipResult` 枚举
- `SUCCESS`：卸下成功
- `NOT_EQUIPPED`：玩家未装备任何称号
- `ERROR`：未知错误

##### `getEquippedTitleId`
```java
public Optional<Integer> getEquippedTitleId(UUID playerId)
```

**参数**：
- `playerId` (`UUID`)：玩家唯一标识符

**返回值**：`Optional<Integer>`，如果玩家装备了称号则返回称号ID，否则返回 `Optional.empty()`

#### 1.2 TitleProgressService

**类路径**：`com.kavinshi.playertitle.service.TitleProgressService`

**描述**：管理玩家称号解锁进度。

**构造函数**：
```java
public TitleProgressService(
    TitleEventFactory eventFactory,
    ClusterEventBus eventBus
)
```

**方法**：

##### `updateProgress`
```java
public ProgressUpdateResult updateProgress(
    UUID playerId,
    TitleConditionType conditionType,
    int delta
)
```

**参数**：
- `playerId` (`UUID`)：玩家唯一标识符
- `conditionType` (`TitleConditionType`)：条件类型
- `delta` (`int`)：进度变化值（正数增加，负数减少）

**返回值**：`ProgressUpdateResult` 对象，包含以下字段：
- `unlockedTitles` (`List<Integer>`)：本次更新解锁的新称号ID列表
- `updatedProgress` (`Map<Integer, Integer>`)：更新后的进度映射
- `anyUnlocked` (`boolean`)：是否有新称号解锁

**使用示例**：
```java
TitleProgressService progressService = ...;
UUID playerId = ...;

// 增加玩家等级
ProgressUpdateResult result = progressService.updateProgress(
    playerId,
    TitleConditionType.PLAYER_LEVEL,
    1
);

if (result.anyUnlocked()) {
    // 处理新解锁的称号
    for (int unlockedTitleId : result.getUnlockedTitles()) {
        // 通知玩家解锁了新称号
    }
}
```

##### `getPlayerProgress`
```java
public Map<Integer, Integer> getPlayerProgress(UUID playerId)
```

**参数**：
- `playerId` (`UUID`)：玩家唯一标识符

**返回值**：`Map<Integer, Integer>`，键为称号ID，值为当前进度值

### 2. 图标系统 API

#### 2.1 IconManager

**类路径**：`com.kavinshi.playertitle.icon.IconManager`

**描述**：管理 PNG 图标文件。

**构造函数**：
```java
public IconManager(Path configDirectory)
```

**方法**：

##### `scanIcons`
```java
public void scanIcons() throws IOException
```

**描述**：扫描配置目录中的 PNG 图标文件。

**异常**：
- `IOException`：如果扫描过程中发生 I/O 错误

**使用示例**：
```java
Path configDir = Path.of("config", "playertitle", "icons");
IconManager iconManager = new IconManager(configDir);

try {
    iconManager.scanIcons();
    int iconCount = iconManager.getIconCount();
    System.out.println("Loaded " + iconCount + " icons");
} catch (IOException e) {
    // 处理扫描错误
}
```

##### `getIcon`
```java
public IconDefinition getIcon(String id)
```

**参数**：
- `id` (`String`)：图标ID（文件名不带扩展名）

**返回值**：`IconDefinition` 对象，如果未找到则返回 `null`

##### `getAllIcons`
```java
public Map<String, IconDefinition> getAllIcons()
```

**返回值**：所有图标的不可变映射，键为图标ID，值为图标定义

#### 2.2 ClientIconManager

**类路径**：`com.kavinshi.playertitle.client.ClientIconManager`

**描述**：客户端图标管理器，扩展自 `IconManager`，添加字体资源包生成功能。

**构造函数**：
```java
public ClientIconManager(Path configDir)
```

**方法**：

##### `generateFontResourcePack`
```java
public void generateFontResourcePack(Path outputDir) throws IOException
```

**参数**：
- `outputDir` (`Path`)：资源包输出目录

**描述**：生成包含所有图标的字体资源包。

**使用示例**：
```java
Path configDir = Path.of("config", "playertitle", "icons");
ClientIconManager clientIconManager = new ClientIconManager(configDir);
clientIconManager.scanIcons();

Path outputDir = Path.of("config", "playertitle", "resourcepack");
clientIconManager.generateFontResourcePack(outputDir);
```

#### 2.3 TitleIconResolver

**类路径**：`com.kavinshi.playertitle.title.TitleIconResolver`

**描述**：解析称号定义中的图标配置。

**方法**：

##### `resolveIcon`
```java
public Component resolveIcon(TitleDefinition titleDefinition)
```

**参数**：
- `titleDefinition` (`TitleDefinition`)：称号定义

**返回值**：`Component` 对象，包含图标和颜色的文本组件

##### `getIconUnicodeChar`
```java
public char getIconUnicodeChar(String iconId)
```

**参数**：
- `iconId` (`String`)：图标ID

**返回值**：对应的 Unicode 字符，如果未找到则返回默认字符

### 3. 事件总线 API

#### 3.1 ClusterEventBus

**接口路径**：`com.kavinshi.playertitle.sync.ClusterEventBus`

**描述**：集群事件总线接口。

**方法**：

##### `start`
```java
void start() throws Exception
```

**描述**：启动事件总线。

##### `stop`
```java
void stop() throws Exception
```

**描述**：停止事件总线。

##### `publish`
```java
void publish(ClusterSyncEvent event)
```

**参数**：
- `event` (`ClusterSyncEvent`)：要发布的事件

##### `subscribe`
```java
void subscribe(Consumer<ClusterSyncEvent> handler)
```

**参数**：
- `handler` (`Consumer<ClusterSyncEvent>`)：事件处理器

##### `getImplementationName`
```java
String getImplementationName()
```

**返回值**：实现名称，如 `"RedisEventBus"` 或 `"LocalEventBus"`

#### 3.2 事件实现

##### LocalEventBus
```java
public class LocalEventBus implements ClusterEventBus
```
本地事件总线，仅适用于单服务器环境。

##### RedisEventBus
```java
public class RedisEventBus implements ClusterEventBus
```
基于 Redis Pub/Sub 的分布式事件总线。

**构造函数**：
```java
public RedisEventBus(
    String host,
    int port,
    String password,
    int database
)
```

## 📨 事件系统 API

### 1. 事件基类

#### ClusterSyncEvent

**类路径**：`com.kavinshi.playertitle.sync.ClusterSyncEvent`

**描述**：所有跨服同步事件的基类。

**字段**：

| 字段名 | 类型 | 说明 |
|--------|------|------|
| `playerId` | `UUID` | 玩家唯一标识符 |
| `eventType` | `ClusterEventType` | 事件类型 |
| `revision` | `long` | 版本号 |
| `timestamp` | `Instant` | 时间戳 |
| `serverId` | `String` | 源服务器ID |

**方法**：

##### `getPlayerId`
```java
public UUID getPlayerId()
```

##### `getEventType`
```java
public ClusterEventType getEventType()
```

##### `getRevision`
```java
public long getRevision()
```

##### `getTimestamp`
```java
public Instant getTimestamp()
```

##### `getServerId`
```java
public String getServerId()
```

### 2. 具体事件类型

#### 2.1 TitleAssignedEvent

**类路径**：`com.kavinshi.playertitle.sync.TitleAssignedEvent`

**描述**：玩家解锁新称号的事件。

**构造函数**：
```java
public TitleAssignedEvent(
    UUID playerId,
    long revision,
    String serverId,
    int titleId,
    Instant unlockedAt
)
```

**字段**：

| 字段名 | 类型 | 说明 |
|--------|------|------|
| `titleId` | `int` | 解锁的称号ID |
| `unlockedAt` | `Instant` | 解锁时间 |

#### 2.2 TitleRemovedEvent

**类路径**：`com.kavinshi.playertitle.sync.TitleRemovedEvent`

**描述**：玩家称号被移除的事件。

**构造函数**：
```java
public TitleRemovedEvent(
    UUID playerId,
    long revision,
    String serverId,
    int titleId
)
```

**字段**：

| 字段名 | 类型 | 说明 |
|--------|------|------|
| `titleId` | `int` | 被移除的称号ID |

#### 2.3 TitleEquipStateChangedEvent

**类路径**：`com.kavinshi.playertitle.sync.TitleEquipStateChangedEvent`

**描述**：玩家装备状态变更的事件。

**构造函数**：
```java
public TitleEquipStateChangedEvent(
    UUID playerId,
    long revision,
    String serverId,
    Optional<Integer> equippedTitleId
)
```

**字段**：

| 字段名 | 类型 | 说明 |
|--------|------|------|
| `equippedTitleId` | `Optional<Integer>` | 装备的称号ID，如果为空表示卸下 |

#### 2.4 TitleProgressUpdatedEvent

**类路径**：`com.kavinshi.playertitle.sync.TitleProgressUpdatedEvent`

**描述**：玩家称号进度更新的事件。

**构造函数**：
```java
public TitleProgressUpdatedEvent(
    UUID playerId,
    long revision,
    String serverId,
    TitleConditionType conditionType,
    int delta,
    Map<Integer, Integer> updatedProgress,
    List<Integer> newlyUnlocked
)
```

**字段**：

| 字段名 | 类型 | 说明 |
|--------|------|------|
| `conditionType` | `TitleConditionType` | 更新的条件类型 |
| `delta` | `int` | 进度变化值 |
| `updatedProgress` | `Map<Integer, Integer>` | 更新后的进度映射 |
| `newlyUnlocked` | `List<Integer>` | 新解锁的称号ID列表 |

#### 2.5 TitleUpdatedEvent

**类路径**：`com.kavinshi.playertitle.sync.TitleUpdatedEvent`

**描述**：称号定义更新的服务器级事件。

**构造函数**：
```java
public TitleUpdatedEvent(
    long revision,
    String serverId,
    List<TitleDefinition> updatedTitles
)
```

**字段**：

| 字段名 | 类型 | 说明 |
|--------|------|------|
| `updatedTitles` | `List<TitleDefinition>` | 更新的称号定义列表 |

### 3. 事件工厂 API

#### TitleEventFactory

**类路径**：`com.kavinshi.playertitle.sync.TitleEventFactory`

**描述**：创建称号相关事件的工厂类。

**构造函数**：
```java
public TitleEventFactory(ClusterRevisionService revisionService)
```

**方法**：

##### `createTitleAssignedEvent`
```java
public TitleAssignedEvent createTitleAssignedEvent(
    UUID playerId,
    int titleId
)
```

##### `createTitleRemovedEvent`
```java
public TitleRemovedEvent createTitleRemovedEvent(
    UUID playerId,
    int titleId
)
```

##### `createTitleEquipStateChangedEvent`
```java
public TitleEquipStateChangedEvent createTitleEquipStateChangedEvent(
    UUID playerId,
    Optional<Integer> equippedTitleId
)
```

##### `createTitleProgressUpdatedEvent`
```java
public TitleProgressUpdatedEvent createTitleProgressUpdatedEvent(
    UUID playerId,
    TitleConditionType conditionType,
    int delta,
    Map<Integer, Integer> updatedProgress,
    List<Integer> newlyUnlocked
)
```

##### `createTitleUpdatedEvent`
```java
public TitleUpdatedEvent createTitleUpdatedEvent(
    List<TitleDefinition> updatedTitles
)
```

## 🔌 扩展 API

### 1. 自定义条件系统

#### TitleCondition

**接口路径**：`com.kavinshi.playertitle.title.TitleCondition`

**描述**：称号解锁条件接口。

**方法**：

##### `getType`
```java
TitleConditionType getType()
```

##### `getValue`
```java
int getValue()
```

##### `check`
```java
boolean check(UUID playerId, int currentProgress)
```

**实现示例**：
```java
public class CustomCondition implements TitleCondition {
    private final TitleConditionType type;
    private final int value;
    
    public CustomCondition(TitleConditionType type, int value) {
        this.type = type;
        this.value = value;
    }
    
    @Override
    public TitleConditionType getType() {
        return type;
    }
    
    @Override
    public int getValue() {
        return value;
    }
    
    @Override
    public boolean check(UUID playerId, int currentProgress) {
        // 自定义条件检查逻辑
        return currentProgress >= value;
    }
}
```

#### TitleConditionType

**枚举路径**：`com.kavinshi.playertitle.title.TitleConditionType`

**预定义类型**：

| 枚举值 | 说明 |
|--------|------|
| `PLAYER_LEVEL` | 玩家等级 |
| `PLAYTIME_HOURS` | 在线时间（小时） |
| `QUEST_COMPLETED` | 完成任务数量 |
| `MOBS_KILLED` | 击杀生物数量 |
| `BLOCKS_MINED` | 挖掘方块数量 |
| `CUSTOM` | 自定义条件 |

**扩展自定义类型**：
```java
// 注册自定义条件类型
TitleConditionType.register("ACHIEVEMENT_COMPLETED", "成就完成");
```

### 2. 自定义样式系统

#### TitleStyleMode

**枚举路径**：`com.kavinshi.playertitle.title.TitleStyleMode`

**预定义模式**：

| 枚举值 | 说明 |
|--------|------|
| `STATIC` | 静态颜色 |
| `RANDOM` | 随机颜色 |
| `SEQUENTIAL` | 顺序切换颜色 |
| `GRADIENT` | 渐变颜色 |

#### TitleAnimationProfile

**类路径**：`com.kavinshi.playertitle.title.TitleAnimationProfile`

**字段**：

| 字段名 | 类型 | 说明 |
|--------|------|------|
| `type` | `AnimationType` | 动画类型 |
| `speed` | `float` | 动画速度 |
| `intensity` | `float` | 动画强度 |
| `customData` | `Map<String, Object>` | 自定义动画数据 |

#### AnimationType

**枚举路径**：`com.kavinshi.playertitle.title.AnimationType`

**预定义类型**：

| 枚举值 | 说明 |
|--------|------|
| `NONE` | 无动画 |
| `PULSE` | 脉动效果 |
| `BLINK` | 闪烁效果 |
| `BREATHE` | 呼吸效果 |
| `WAVE` | 波浪效果 |

## 📊 数据模型 API

### 1. TitleDefinition

**类路径**：`com.kavinshi.playertitle.title.TitleDefinition`

**描述**：称号定义数据模型。

**构造函数**：
```java
public TitleDefinition(
    int id,
    String name,
    int displayOrder,
    int color,
    List<TitleCondition> conditions,
    String category,
    String icon,
    String iconColor,
    TitleStyleMode styleMode,
    List<String> baseColors,
    TitleAnimationProfile animationProfile
)
```

**方法**：

##### `getId`
```java
public int getId()
```

##### `getName`
```java
public String getName()
```

##### `getDisplayOrder`
```java
public int getDisplayOrder()
```

##### `getColor`
```java
public int getColor()
```

##### `getConditions`
```java
public List<TitleCondition> getConditions()
```

##### `getCategory`
```java
public String getCategory()
```

##### `getIcon`
```java
public String getIcon()
```

##### `getIconColor`
```java
public String getIconColor()
```

##### `getStyleMode`
```java
public TitleStyleMode getStyleMode()
```

##### `getBaseColors`
```java
public List<String> getBaseColors()
```

##### `getAnimationProfile`
```java
public TitleAnimationProfile getAnimationProfile()
```

### 2. IconDefinition

**类路径**：`com.kavinshi.playertitle.icon.IconDefinition`

**描述**：图标定义数据模型。

**构造函数**：
```java
public IconDefinition(
    String id,
    String name,
    Path pngPath,
    char unicodeChar,
    int width,
    int height,
    int ascent,
    int descent
)
```

**方法**：

##### `getId`
```java
public String getId()
```

##### `getName`
```java
public String getName()
```

##### `getPngPath`
```java
public Path getPngPath()
```

##### `getUnicodeChar`
```java
public char getUnicodeChar()
```

##### `getWidth`
```java
public int getWidth()
```

##### `getHeight`
```java
public int getHeight()
```

##### `getAscent`
```java
public int getAscent()
```

##### `getDescent`
```java
public int getDescent()
```

### 3. PlayerTitleState

**类路径**：`com.kavinshi.playertitle.player.PlayerTitleState`

**描述**：玩家称号状态数据模型。

**构造函数**：
```java
public PlayerTitleState(
    UUID playerId,
    Map<Integer, Integer> titleProgress,
    Optional<Integer> equippedTitleId,
    Instant lastUpdated
)
```

**方法**：

##### `getPlayerId`
```java
public UUID getPlayerId()
```

##### `getTitleProgress`
```java
public Map<Integer, Integer> getTitleProgress()
```

##### `getEquippedTitleId`
```java
public Optional<Integer> getEquippedTitleId()
```

##### `getLastUpdated`
```java
public Instant getLastUpdated()
```

## 🔧 实用工具 API

### 1. 配置加载器

#### JsonTitleConfigRepository

**类路径**：`com.kavinshi.playertitle.config.JsonTitleConfigRepository`

**描述**：从 JSON 文件加载称号配置。

**构造函数**：
```java
public JsonTitleConfigRepository(Path configPath)
```

**方法**：

##### `loadAll`
```java
public List<TitleDefinition> loadAll() throws IOException
```

##### `saveAll`
```java
public void saveAll(List<TitleDefinition> titles) throws IOException
```

##### `reload`
```java
public void reload() throws IOException
```

### 2. 版本服务

#### ClusterRevisionService

**类路径**：`com.kavinshi.playertitle.sync.ClusterRevisionService`

**描述**：管理集群事件的版本号。

**方法**：

##### `nextRevision`
```java
public long nextRevision(UUID playerId)
```

**参数**：
- `playerId` (`UUID`)：玩家唯一标识符

**返回值**：下一个版本号

##### `getCurrentRevision`
```java
public long getCurrentRevision(UUID playerId)
```

**参数**：
- `playerId` (`UUID`)：玩家唯一标识符

**返回值**：当前版本号

## 📝 使用示例

### 1. 完整的称号装备流程

```java
// 初始化服务
TitleEventFactory eventFactory = new TitleEventFactory(revisionService);
ClusterEventBus eventBus = new RedisEventBus("localhost", 6379, null, 0);
TitleEquipService equipService = new TitleEquipService(eventFactory, eventBus);

// 启动事件总线
eventBus.start();

// 装备称号
UUID playerId = UUID.fromString("player-uuid-here");
int titleId = 1;

EquipResult result = equipService.equipTitle(playerId, titleId);
switch (result) {
    case SUCCESS:
        System.out.println("称号装备成功");
        break;
    case TITLE_NOT_UNLOCKED:
        System.out.println("称号未解锁");
        break;
    // 处理其他结果
}

// 获取当前装备的称号
Optional<Integer> equippedTitleId = equipService.getEquippedTitleId(playerId);
equippedTitleId.ifPresent(id -> {
    System.out.println("玩家当前装备的称号ID: " + id);
});

// 清理资源
eventBus.stop();
```

### 2. 自定义事件处理器

```java
// 创建事件总线
ClusterEventBus eventBus = new LocalEventBus();
eventBus.start();

// 订阅事件
eventBus.subscribe(event -> {
    switch (event.getEventType()) {
        case TITLE_ASSIGNED:
            TitleAssignedEvent assignedEvent = (TitleAssignedEvent) event;
            System.out.println("玩家 " + assignedEvent.getPlayerId() + 
                             " 解锁了称号 " + assignedEvent.getTitleId());
            break;
            
        case TITLE_EQUIP_STATE_CHANGED:
            TitleEquipStateChangedEvent equipEvent = (TitleEquipStateChangedEvent) event;
            equipEvent.getEquippedTitleId().ifPresentOrElse(
                titleId -> System.out.println("玩家装备了称号 " + titleId),
                () -> System.out.println("玩家卸下了称号")
            );
            break;
            
        // 处理其他事件类型
    }
});

// 发布事件
TitleAssignedEvent event = eventFactory.createTitleAssignedEvent(
    UUID.randomUUID(),
    1
);
eventBus.publish(event);
```

### 3. 自定义条件实现

```java
// 注册自定义条件类型
TitleConditionType.register("DUNGEON_CLEARED", "地牢通关");

// 创建自定义条件
TitleCondition dungeonCondition = new TitleCondition() {
    @Override
    public TitleConditionType getType() {
        return TitleConditionType.valueOf("DUNGEON_CLEARED");
    }
    
    @Override
    public int getValue() {
        return 5; // 需要通关5次
    }
    
    @Override
    public boolean check(UUID playerId, int currentProgress) {
        // 从数据库或缓存中获取玩家的地牢通关次数
        int clearedCount = getDungeonClearedCount(playerId);
        return clearedCount >= getValue();
    }
    
    private int getDungeonClearedCount(UUID playerId) {
        // 实现获取逻辑
        return 3;
    }
};

// 在称号配置中使用
TitleDefinition title = new TitleDefinition(
    100,
    "地牢大师",
    500,
    0xFFA500,
    List.of(dungeonCondition),
    "挑战",
    "dungeon.png",
    "#8B4513",
    TitleStyleMode.STATIC,
    List.of(),
    new TitleAnimationProfile(AnimationType.NONE, 1.0f, 1.0f, Map.of())
);
```

## 🔍 API 使用注意事项

### 1. 线程安全

**线程安全的 API**：
- `IconManager` 的所有方法
- `TitleEquipService` 和 `TitleProgressService` 的公共方法
- `ClusterEventBus` 的事件发布方法

**非线程安全的 API**：
- `TitleDefinition` 和 `IconDefinition` 等数据模型对象
- 配置加载过程中的临时对象

### 2. 性能考虑

**高性能操作**：
- 称号查询：使用缓存加速
- 事件发布：支持批量发布
- 图标渲染：客户端本地缓存

**高成本操作**：
- 配置文件重载
- 图标目录扫描
- 玩家状态持久化

### 3. 错误处理

**检查异常**：
- `IOException`：文件操作错误
- `SQLException`：数据库操作错误

**运行时异常**：
- `IllegalArgumentException`：参数错误
- `IllegalStateException`：状态错误
- `UnsupportedOperationException`：不支持的操作

### 4. 资源管理

**需要显式清理的资源**：
- `ClusterEventBus`：需要调用 `stop()` 方法
- 数据库连接：使用连接池管理
- 文件句柄：使用 try-with-resources

**自动管理的资源**：
- 内存缓存：使用软引用和弱引用
- 线程池：系统自动管理生命周期
- 网络连接：自动重连机制

---

**文档版本**：v1.0.0  
**最后更新**：2026-04-20  
**维护者**：API 文档团队