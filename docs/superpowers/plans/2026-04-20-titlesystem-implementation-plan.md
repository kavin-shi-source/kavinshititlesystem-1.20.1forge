# 跨服称号同步系统实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 `superpowers:subagent-driven-development`（推荐）或 `superpowers:executing-plans` 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 在Velocity+Forge多服架构下实现玩家称号跨服同步，支持PNG图标、RGB渐变和动态跑马灯效果。

**架构：** Forge端作为权威业务层处理称号解锁/装备，通过Redis发布跨服事件；Velocity端消费事件并维护展示缓存，统一处理聊天、Tab和Velocitab展示。图标系统采用自定义字体+资源包方案，支持降级显示。

**技术栈：** Java 17、Minecraft Forge 1.20.1、Velocity API 3.3.0、Lettuce (Redis)、Adventure/MiniMessage、JUnit 5、Gson。

---

## 文件结构

### Forge端新文件
```
com.kavinshi.playertitle.icon/
├── IconDefinition.java              # 图标定义模型
├── IconManager.java                 # 图标扫描与管理
├── FontGenerator.java               # 字体映射生成
└── TitleResourcePack.java          # 资源包注册

com.kavinshi.playertitle.sync/
├── ClusterEventType.java           # 事件类型枚举
├── ClusterSyncEvent.java          # 跨服事件数据模型
├── ClusterRevisionService.java    # 玩家版本号生成器
├── TitleEventFactory.java         # 事件构造工厂
├── ClusterSyncGateway.java        # 同步网关接口
├── NoopClusterSyncGateway.java    # 本地空实现
└── RedisClusterSyncGateway.java   # Redis实现

com.kavinshi.playertitle.render/
├── TitleRenderer.java             # 称号渲染器
└── IconRenderer.java             # 图标渲染器（Forge端）
```

### Velocity端新项目
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
│       │   ├── IconRenderer.java          # 图标渲染器（Velocity端）
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

### 修改的现有文件
- `kavinshititlesystem-1.20.1forge/src/main/java/com/kavinshi/playertitle/config/JsonTitleConfigRepository.java` - 添加图标配置解析
- `kavinshititlesystem-1.20.1forge/src/main/java/com/kavinshi/playertitle/service/TitleProgressService.java` - 集成事件发布
- `kavinshititlesystem-1.20.1forge/src/main/java/com/kavinshi/playertitle/service/TitleEquipService.java` - 集成事件发布
- `kavinshititlesystem-1.20.1forge/build.gradle` - 添加Redis客户端依赖

---

## 阶段一：Forge端图标资源与同步基础（第1-2周）

### 任务1：创建图标资源管理器

**文件：**
- 创建：`kavinshititlesystem-1.20.1forge/src/main/java/com/kavinshi/playertitle/icon/IconDefinition.java`
- 创建：`kavinshititlesystem-1.20.1forge/src/main/java/com/kavinshi/playertitle/icon/IconManager.java`
- 测试：`kavinshititlesystem-1.20.1forge/src/test/java/com/kavinshi/playertitle/icon/IconManagerTest.java`

详细步骤见完整计划文档...

### 任务2：实现图标扫描管理器

**文件：**
- 创建：`kavinshititlesystem-1.20.1forge/src/main/java/com/kavinshi/playertitle/icon/IconManager.java`
- 测试：`kavinshititlesystem-1.20.1forge/src/test/java/com/kavinshi/playertitle/icon/IconManagerTest.java`

详细步骤见完整计划文档...

### 任务3：创建跨服事件模型

**文件：**
- 创建：`kavinshititlesystem-1.20.1forge/src/main/java/com/kavinshi/playertitle/sync/ClusterEventType.java`
- 创建：`kavinshititlesystem-1.20.1forge/src/main/java/com/kavinshi/playertitle/sync/ClusterSyncEvent.java`
- 测试：`kavinshititlesystem-1.20.1forge/src/test/java/com/kavinshi/playertitle/sync/ClusterSyncEventTest.java`

详细步骤见完整计划文档...

### 任务4：创建版本控制服务

**文件：**
- 创建：`kavinshititlesystem-1.20.1forge/src/main/java/com/kavinshi/playertitle/sync/ClusterRevisionService.java`
- 测试：`kavinshititlesystem-1.20.1forge/src/test/java/com/kavinshi/playertitle/sync/ClusterRevisionServiceTest.java`

详细步骤见完整计划文档...

### 任务5：创建同步网关接口和空实现

**文件：**
- 创建：`kavinshititlesystem-1.20.1forge/src/main/java/com/kavinshi/playertitle/sync/ClusterSyncGateway.java`
- 创建：`kavinshititlesystem-1.20.1forge/src/main/java/com/kavinshi/playertitle/sync/NoopClusterSyncGateway.java`
- 测试：`kavinshititlesystem-1.20.1forge/src/test/java/com/kavinshi/playertitle/sync/NoopClusterSyncGatewayTest.java`

详细步骤见完整计划文档...

### 任务6：扩展配置解析支持图标

**文件：**
- 修改：`kavinshititlesystem-1.20.1forge/src/main/java/com/kavinshi/playertitle/config/JsonTitleConfigRepository.java`
- 测试：`kavinshititlesystem-1.20.1forge/src/test/java/com/kavinshi/playertitle/config/JsonTitleConfigRepositoryTest.java`

详细步骤见完整计划文档...

---

## 阶段二：Redis集成与Velocity项目骨架（第3周）

### 任务7：添加Redis客户端依赖

**文件：**
- 修改：`kavinshititlesystem-1.20.1forge/build.gradle`
- 测试：构建验证

详细步骤见完整计划文档...

### 任务8：创建Redis同步网关实现

**文件：**
- 创建：`kavinshititlesystem-1.20.1forge/src/main/java/com/kavinshi/playertitle/sync/RedisClusterSyncGateway.java`
- 创建：`kavinshititlesystem-1.20.1forge/src/main/java/com/kavinshi/playertitle/sync/RedisConfig.java`
- 测试：`kavinshititlesystem-1.20.1forge/src/test/java/com/kavinshi/playertitle/sync/RedisClusterSyncGatewayTest.java`

详细步骤见完整计划文档...

### 任务8：创建Velocity项目骨架

**文件：**
- 创建：`velocity-playertitle/` 目录结构
- 创建：`velocity-playertitle/build.gradle`
- 创建：`velocity-playertitle/settings.gradle`
- 创建：`velocity-playertitle/src/main/resources/velocity-plugin.json`

详细步骤见完整计划文档...

---

## 阶段三：Velocity核心实现（第4-5周）

### 任务9：创建Velocity插件入口

**文件：**
- 创建：`velocity-playertitle/src/main/java/com/kavinshi/playertitle/proxy/ProxyTitlePlugin.java`
- 创建：`velocity-playertitle/src/main/java/com/kavinshi/playertitle/proxy/sync/ProxyTitleView.java`
- 测试：`velocity-playertitle/src/test/java/com/kavinshi/playertitle/proxy/ProxyTitlePluginTest.java`

详细步骤见完整计划文档...

### 任务10：创建事件订阅器和缓存

**文件：**
- 创建：`velocity-playertitle/src/main/java/com/kavinshi/playertitle/proxy/sync/RedisClusterSubscriber.java`
- 创建：`velocity-playertitle/src/main/java/com/kavinshi/playertitle/proxy/sync/ProxyTitleCache.java`
- 测试：`velocity-playertitle/src/test/java/com/kavinshi/playertitle/proxy/sync/ProxyTitleCacheTest.java`

详细步骤见完整计划文档...

### 任务11：创建图标渲染器和RGB渲染服务

**文件：**
- 创建：`velocity-playertitle/src/main/java/com/kavinshi/playertitle/proxy/render/IconRenderer.java`
- 创建：`velocity-playertitle/src/main/java/com/kavinshi/playertitle/proxy/render/TitleRenderService.java`
- 测试：`velocity-playertitle/src/test/java/com/kavinshi/playertitle/proxy/render/TitleRenderServiceTest.java`

详细步骤见完整计划文档...

### 任务12：创建聊天格式化器和Tab同步服务

**文件：**
- 创建：`velocity-playertitle/src/main/java/com/kavinshi/playertitle/proxy/chat/ProxyChatFormatter.java`
- 创建：`velocity-playertitle/src/main/java/com/kavinshi/playertitle/proxy/tab/ProxyTabSyncService.java`
- 测试：`velocity-playertitle/src/test/java/com/kavinshi/playertitle/proxy/chat/ProxyChatFormatterTest.java`

详细步骤见完整计划文档...

### 任务13：创建Velocitab桥接和配置

**文件：**
- 创建：`velocity-playertitle/src/main/java/com/kavinshi/playertitle/proxy/velocitab/VelocitabBridge.java`
- 创建：`velocity-playertitle/src/main/resources/config.yml`
- 测试：`velocity-playertitle/src/test/java/com/kavinshi/playertitle/proxy/velocitab/VelocitabBridgeTest.java`

详细步骤见完整计划文档...

### 任务14：集成所有组件到插件入口

**文件：**
- 修改：`velocity-playertitle/src/main/java/com/kavinshi/playertitle/proxy/ProxyTitlePlugin.java`
- 创建：`velocity-playertitle/src/main/java/com/kavinshi/playertitle/proxy/PluginConfig.java`
- 测试：`velocity-playertitle/src/test/java/com/kavinshi/playertitle/proxy/IntegrationTest.java`

详细步骤见完整计划文档...

---

## 阶段四：Forge端业务集成与端到端测试（第6周）

### 任务15：集成事件发布到Forge业务服务

**文件：**
- 修改：`kavinshititlesystem-1.20.1forge/src/main/java/com/kavinshi/playertitle/service/TitleProgressService.java`
- 修改：`kavinshititlesystem-1.20.1forge/src/main/java/com/kavinshi/playertitle/service/TitleEquipService.java`
- 创建：`kavinshititlesystem-1.20.1forge/src/main/java/com/kavinshi/playertitle/sync/TitleEventFactory.java`

详细步骤见完整计划文档...

### 任务16：创建端到端测试场景

**文件：**
- 创建：`kavinshititlesystem-1.20.1forge/src/test/java/com/kavinshi/playertitle/e2e/CrossServerSyncTest.java`
- 创建：`velocity-playertitle/src/test/java/com/kavinshi/playertitle/proxy/e2e/ProxyIntegrationTest.java`

详细步骤见完整计划文档...

### 任务17：保存完整实现计划

- [ ] **步骤1：将计划保存到文件**
- [ ] **步骤2：Commit计划文档**

```bash
cd "E:\java-xuexi\solstitlesystem-1.20.1-1.3.0"
& "C:\Program Files\Git\bin\git.exe" add docs/superpowers/plans/2026-04-20-titlesystem-implementation-plan.md
& "C:\Program Files\Git\bin\git.exe" commit -m "plan: add comprehensive implementation plan for cross-server title sync system"
```

---

## 计划审查与执行

### 下一步操作

1. **计划审查循环**：调度 plan-document-reviewer 子代理审查此计划
2. **用户确认**：审查通过后，用户确认执行方式
3. **开始实施**：根据选择的执行方式开始代码编写

**执行选项：**

**1. 子代理驱动（推荐）** - 每个任务调度一个新的子代理，任务间进行审查，快速迭代
   - **必需子技能：** 使用 `superpowers:subagent-driven-development`

**2. 内联执行** - 在当前会话中使用 executing-plans 执行任务，批量执行并设有检查点
   - **必需子技能：** 使用 `superpowers:executing-plans`

### 关键里程碑

- **里程碑1（任务1-6完成）**：Forge端图标资源系统和同步基础
- **里程碑2（任务7-8完成）**：Redis集成和Velocity项目骨架
- **里程碑3（任务9-14完成）**：Velocity展示层完整实现
- **里程碑4（任务15-16完成）**：端到端集成和测试验证

### 风险缓解

1. **PNG图标兼容性**：提供Unicode备选方案，分阶段实施
2. **Redis性能**：事件精简设计，连接池优化
3. **聊天插件冲突**：提供多种集成模式（完全接管/占位符）
4. **资源包分发**：内置资源包 + 可选外部包

---
*计划版本：1.0*
*最后更新：2026-04-20*
*基于设计文档：2026-04-20-titlesystem-complete-design.md*