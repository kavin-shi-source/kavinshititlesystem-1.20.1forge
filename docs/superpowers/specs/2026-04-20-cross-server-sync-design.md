# kavinshi's playertitle 跨服同步设计

## 目标

在 `Velocity + Forge` 的多服架构下，实现玩家称号的跨服一致展示，覆盖以下场景：

- 跨服聊天显示玩家当前称号
- Proxy 侧 Tab 列表显示玩家当前称号
- 使用 `Velocitab-1.6.1` 时，Tab 列表仍可显示称号
- 玩家切服、重连、代理重启后，称号展示能够恢复

本设计聚焦于“跨服同步与展示链路”，不在本阶段处理 GUI、buff、combat lock。

## 结论

推荐采用以下架构：

- **后端 Forge 模组：** 作为称号业务权威层
- **Redis：** 作为跨服实时事件总线
- **Velocity 插件：** 作为跨服聊天、Tab、Velocitab 的展示协调层
- **Forge 本地存储 / 后续 MySQL：** 作为持久化层

不推荐只依赖插件消息通道，也不推荐只使用 MySQL 轮询实现实时跨服展示。

## 设计原则

- **单一权威：** 称号状态只在后端 Forge 模组写入
- **展示分层：** 聊天、Tab、玩家列表展示统一在 Proxy 层处理
- **事件驱动：** 只同步状态变化，不做全量高频广播
- **可替换介质：** 跨服总线通过 `ClusterSyncGateway` 抽象，先对接 Redis，后续可替换
- **冷启动可恢复：** 代理重启后能依靠玩家登录快照恢复在线缓存

## 边界与非目标

本设计只解决以下问题：

- 跨服称号状态同步
- Proxy 侧聊天称号展示
- Proxy 侧 Tab 列表称号展示
- Velocitab 适配
- 图标头衔展示
- RGB 静态渐变与动态跑马灯展示

本设计暂不解决以下问题：

- Forge 客户端本地 GUI
- 原版或客户端侧 HUD
- 光环、buff、combat lock
- 本地世界名牌渲染
- 其他聊天插件的深度兼容实现细节

职责边界如下：

- **Forge：** 权威业务状态
- **Velocity：** 展示缓存与展示渲染
- **Velocitab：** 只读展示层，不可反向修改业务状态

## 为什么推荐 Redis

### Redis 是否只能做内存缓存

不是。Redis 支持持久化，常用方式有：

- `RDB`：定时快照
- `AOF`：追加日志
- `AOF + RDB`：兼顾恢复速度与数据安全

本项目中，Redis 不承担“唯一主存储”的职责，而是承担“实时同步通道”的职责，因此对 Redis 的要求主要是低延迟和发布订阅能力，而不是复杂查询。

### Redis 会不会有性能压力

在这个项目中，Redis 的压力通常很低，因为同步的数据量很小，主要是：

- 玩家 UUID
- 当前称号 ID
- 称号显示文本
- 所在服务器
- 事件类型
- 时间戳

不会把完整称号配置、大量历史记录或高频全量快照塞进 Redis。正常做法是：

- **Redis：** 在线状态广播
- **持久化层：** 离线恢复与长期数据

## 方案对比

### 方案 A：Redis 事件总线 + 持久化存储（推荐）

特点：

- Forge 模组写入称号业务
- Redis 发布跨服事件
- Velocity 插件维护全服在线缓存
- 聊天与 Tab 从 Proxy 缓存读取

优点：

- 实时性最好
- 对 Velocity 最友好
- 易于覆盖跨服聊天与 Tab
- 后续可扩展 Web 面板、管理后台、审计日志

缺点：

- 需要额外增加一个 Velocity 插件
- 需要 Redis 运维

### 方案 B：纯 MySQL + 轮询

特点：

- 所有称号状态写入 MySQL
- Proxy 定时轮询数据库读取称号

优点：

- 持久化直观
- 运维上容易理解

缺点：

- 实时性差
- 聊天和 Tab 展示延迟明显
- 轮询实现别扭，扩展性差

### 方案 C：仅插件消息

特点：

- 后端服通过插件消息把状态推给 Proxy 或其他服

优点：

- 轻量
- 适合最小演示

缺点：

- 只适合在线玩家
- 离线恢复弱
- 代理重启后状态重建差
- 不适合你当前需求

## 跨服总线设计

Forge 模组在以下时机发出跨服事件：

- 玩家登录后发送 `STATE_SNAPSHOT`
- 解锁称号后发送 `TITLE_UNLOCKED`
- 装备称号后发送 `TITLE_EQUIPPED`
- 卸下称号后发送 `TITLE_UNEQUIPPED`
- 玩家离线时发送 `PLAYER_DISCONNECT`

推荐事件载荷字段：

- `schemaVersion`
- `eventId`
- `eventType`
- `revision`
- `playerUuid`
- `playerName`
- `serverName`
- `titleId`
- `titleDisplay`
- `titleWeight`
- `titleColor`
- `timestamp`

说明：

- `schemaVersion` 用于后续协议升级
- `eventId` 用于消费去重
- `revision` 为每个玩家单调递增版本号，解决重复和乱序
- `titleDisplay` 供聊天与 Tab 直接展示
- `titleWeight` 供排序使用，避免按文本排序
- `titleColor` 供 Tab 或名字样式渲染

### 展示字段扩展

为支持图标头衔、静态渐变与动态跑马灯，称号定义与跨服事件需要补充以下字段：

- `icon`
- `iconColor`
- `styleMode`
- `baseColors`
- `animationProfile`

推荐约束如下：

- `icon`：称号前的图标字符或短文本
- `iconColor`：图标颜色
- `styleMode`：`PLAIN` / `STATIC_GRADIENT` / `ANIMATED_CHROMA`
- `baseColors`：渐变或跑马灯使用的颜色组
- `animationProfile`：速度、步长、周期等动态参数

### RGB 规则

本项目支持两种 RGB 展示能力：

- **静态渐变：** 固定渐变色，不随时间变化
- **动态跑马灯：** 颜色随时间片循环流动

统一规则如下：

- **聊天：** 动态跑马灯按“发送瞬间帧”固化，不做消息发出后的持续刷新
- **Tab / Velocitab / Proxy 玩家列表：** 支持按时间片持续刷新动态帧
- **Forge：** 只存储展示定义，不负责生成动态帧
- **Velocity：** 负责最终渲染静态渐变和动态跑马灯

### 一致性规则

- **先落库，后发事件：** Forge 必须先完成本地状态保存，再发布跨服事件
- **UUID 为唯一键：** `playerUuid` 是唯一身份标识，`playerName` 仅用于展示
- **Proxy 缓存不是权威源：** 一切冲突都以 Forge 持久化状态为准
- **快照优先级最高：** `STATE_SNAPSHOT` 视为该玩家某一版本的全量真值
- **版本优先于时间：** Proxy 侧以 `revision` 判定新旧，不依赖机器时间
- **消费必须幂等：** 同一 `eventId` 或较旧 `revision` 不得重复应用

### 事件类型补充

除以下事件外：

- `STATE_SNAPSHOT`
- `TITLE_UNLOCKED`
- `TITLE_EQUIPPED`
- `TITLE_UNEQUIPPED`
- `PLAYER_DISCONNECT`

还应补充：

- `TITLE_REVOKED`

用于配置重载、管理员撤销或非法装备降级场景。

## 恢复与补偿策略

仅依赖 Redis `Pub/Sub` 不足以覆盖全部恢复场景，因此需要补偿机制。

### Proxy 重启恢复

- 玩家重新登录后，Forge 立即发送 `STATE_SNAPSHOT`
- 玩家切服时，目标 Forge 服再次发送 `STATE_SNAPSHOT`
- 可选：Velocity 插件定期请求在线玩家快照，修复长时间缓存缺失

### Redis 断连恢复

- Velocity 插件检测到 Redis 重连后，标记缓存进入“待校准”状态
- 待校准期间，新的快照优先覆盖旧缓存
- 若超过阈值未收到快照，可将对应玩家回退为“无称号展示”而不是继续展示旧值

### Forge 重启恢复

- 玩家重新加入 Forge 服时，从持久化层恢复称号状态
- Forge 恢复完成后再发布 `STATE_SNAPSHOT`

### 代理缓存过期

- `PLAYER_DISCONNECT` 到达后移除缓存
- 若未收到离线事件，则以 Proxy 在线玩家列表为准定期清理幽灵缓存

## 安全与格式约束

- Redis 通道必须按环境隔离，例如区分测试服、正式服
- 事件频道名应按项目命名空间隔离，避免混用
- `titleDisplay` 必须限制最大长度
- `titleDisplay` 中的颜色或格式码应走白名单
- Proxy 层不直接信任任意外部事件源，只接受本项目约定通道

## 方案 1 设计

### 场景

前端为 `Velocity_server`，后端为纯 Forge 模组。

### 组件职责

- **Forge 模组**
  - 处理称号进度、解锁、装备、卸下
  - 保存玩家称号持久化状态
  - 发布 Redis 事件

- **Velocity 插件**
  - 订阅 Redis 事件
  - 建立 `UUID -> 在线称号视图` 缓存
  - 接管跨服聊天格式
  - 接管 Proxy 侧 Tab 展示

### 数据流

1. 玩家在某个 Forge 服达成称号条件
2. Forge 模组写入本地状态
3. Forge 模组发布跨服事件到 Redis
4. Velocity 插件收到事件并更新缓存
5. 后续聊天、Tab、Proxy 玩家列表统一读取 Proxy 缓存

### 聊天兼容策略

若网络中已有聊天插件，则应明确以下 3 种接入模式之一：

- **完全接管：** 由本插件统一负责跨服聊天格式
- **占位符模式：** 本插件只提供称号占位值，由现有聊天插件渲染
- **桥接模式：** 本插件在现有聊天插件输出前后追加称号组件

首期推荐采用“完全接管”或“占位符模式”，避免多插件同时改写消息。

### 聊天显示

由 Velocity 插件监听聊天事件，在转发前渲染称号前缀，例如：

```text
[Ancient Warden] PlayerName: hello
```

这样可以确保跨服聊天统一格式，而不是每个后端服各自拼接。

推荐最终格式支持以下元素：

- 图标
- 称号文本
- 静态渐变
- 动态跑马灯的单帧结果

例如：

```text
[✦ Ancient Warden] PlayerName: hello
```

### Tab 显示

若不使用第三方 Tab 插件，则由 Velocity 插件直接控制 Proxy 侧的玩家列表显示。Velocity API 提供 `Player#getTabList()` 与 header/footer 能力，可用于管理代理层 Tab 展示：

- [Velocity Player API](https://jd.papermc.io/velocity/3.3.0/com/velocitypowered/api/proxy/Player.html)
- [Velocity TabList API](https://jd.papermc.io/velocity/3.4.0/com/velocitypowered/api/proxy/player/TabList.html)

### 适用结论

这是最推荐的标准架构，也是后续最容易维护的架构。

## 方案 2 设计

### 场景

前端玩家列表使用 `Velocitab-1.6.1`，同时跨服聊天和 Tab 列表都需要显示玩家称号。

### 关键判断

`Velocitab` 更适合负责 Tab 与 nametag 展示，不适合承担跨服聊天主逻辑。因此：

- **聊天：** 仍由自定义 Velocity 插件负责
- **Tab：** 交由 Velocitab 负责展示

### Velocitab 相关能力

Velocitab 支持：

- Placeholder
- MiniPlaceholders
- Plugin Message API

参考资料：

- [Velocitab Placeholders](https://github-wiki-see.page/m/WiIIiam278/Velocitab/wiki/placeholders)
- [Velocitab Plugin Message API](https://github-wiki-see.page/m/WiIIiam278/Velocitab/wiki/Plugin-Message-API)

### 重要限制

Velocitab 文档里提到的 `PAPIProxyBridge` 路径主要面向 `Spigot / Paper` 后端网络，不适合直接照搬到“后端纯 Forge”的场景。因此，本项目不以 `PAPIProxyBridge` 作为核心方案。

### 推荐接法

推荐以下接法：

- Forge 模组发布 Redis 事件
- Velocity 插件订阅 Redis 并维护称号缓存
- Velocity 插件把称号数据转成 Velocitab 可用的展示值
- Velocitab 负责渲染 Tab 样式和排序

### Velocitab 落点

Velocitab 适配层需要至少提供：

- `title_display`
- `title_weight`
- `title_color`
- `server_name`
- `title_icon`
- `title_style_mode`

排序建议按以下优先级组合：

1. `title_weight`
2. 权限组权重
3. 服务器分组
4. 玩家名

这样可以避免称号排序与权限组排序互相覆盖。

### 实现方式

可以分成两层：

- **数据层**
  - `title_display`
  - `title_weight`
  - `title_color`
  - `server_name`
  - `title_icon`
  - `title_style_mode`

- **展示层**
  - 聊天由 Velocity 插件渲染
  - Tab 由 Velocitab 使用上述数据渲染

### 图标与 RGB 的实现边界

推荐边界如下：

- **Forge 侧**
  - 保存称号的图标与样式定义
  - 把图标、颜色组、样式模式、动画参数放入快照和增量事件

- **Velocity 侧**
  - 根据样式定义生成聊天称号字符串
  - 生成 Tab 和 Velocitab 的当前帧展示结果
  - 维护动态跑马灯的时间片刷新

- **Velocitab**
  - 只消费 Velocity 插件准备好的数据
  - 不负责业务真值，不负责动画定义存储

### 为什么不建议让 Forge 直接驱动 Velocitab

虽然 Velocitab 支持 Plugin Message API，但对你这个架构来说，它更适合做“展示适配入口”，不适合作为总架构核心。原因是：

- 你的业务权威在 Forge
- 你的跨服展示中心在 Velocity
- 如果让 Forge 直接驱动 Velocitab，会让后端服承担过多 Proxy 语义

因此，正确边界是：

- Forge -> Redis -> Velocity 插件 -> Velocitab

而不是：

- Forge -> 直接控制 Velocitab

## 推荐实施顺序

### 第一阶段

先完成 `方案 1`：

- Forge 模组定义 `ClusterSyncGateway`
- Forge 模组在称号变化时发事件
- Velocity 插件订阅 Redis
- Velocity 插件渲染跨服聊天
- Velocity 插件维护 Tab 展示缓存
- Forge 模组补齐图标与 RGB 样式定义
- Velocity 插件实现静态渐变与动态跑马灯渲染
- 完成重复、乱序、重连、切服的验证

### 第二阶段

如果使用 Velocitab，再补 `方案 2` 的适配层：

- 把称号缓存转成 Velocitab 可消费的值
- 配置 Velocitab 的排序和显示格式
- 把图标与样式模式暴露给 Velocitab 适配层
- 补充 Velocitab 不可用时的降级路径

## 当前设计结论

- 你当前应优先做 `方案 1`
- `方案 2` 作为 `方案 1` 之上的展示适配层
- 统一技术路线为：

```text
Forge 模组（权威业务）
    ->
Redis（跨服事件）
    ->
Velocity 插件（展示协调）
    ->
聊天 / Tab / Velocitab
```

## 下一步实现边界

实现阶段将新增两个子系统：

- `Forge 侧 ClusterSyncGateway`
- `Velocity 侧 Proxy 展示插件`

其中：

- Forge 侧负责发事件
- Velocity 侧负责消费事件与渲染展示
- Velocitab 仅作为可选的展示增强层
- 图标与 RGB 样式定义在 Forge 侧建模，在 Velocity 侧渲染

## 验证矩阵

实现完成后，至少要验证以下场景：

- 正常解锁后跨服聊天立即显示称号
- 正常装备后 Tab 列表立即显示称号
- 重复事件不会导致重复应用
- 旧事件不会覆盖新状态
- 玩家切服后称号展示保持一致
- Proxy 重启后在线玩家能通过快照恢复
- Redis 短暂断连后缓存能重新校准
- 称号被撤销后聊天与 Tab 同步降级
- Velocitab 不可用时，聊天链路仍正常
