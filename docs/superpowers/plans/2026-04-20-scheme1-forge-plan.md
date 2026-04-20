# 方案 1 Forge 端实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 `superpowers:subagent-driven-development`（推荐）或 `superpowers:executing-plans` 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 在 `kavinshititlesystem-1.20.1forge` 中完成方案 1 的 Forge 端能力，包括图标头衔、RGB 样式定义、跨服事件模型与 Redis 端口发布。

**架构：** Forge 端继续作为称号业务权威层，负责配置、持久化、解锁、装备以及快照/增量事件生成。Forge 不渲染动态 RGB 帧，只负责输出图标、颜色组、样式模式和动画参数，由 Proxy 层最终展示。

**技术栈：** Java 17、Minecraft Forge 1.20.1、JUnit 5、Gson、Redis 客户端（后续接入）。

---

## 文件结构

- 修改：`kavinshititlesystem-1.20.1forge/src/main/java/com/kavinshi/playertitle/title/TitleDefinition.java`
  - 扩展图标与 RGB 样式定义。
- 创建：`kavinshititlesystem-1.20.1forge/src/main/java/com/kavinshi/playertitle/title/TitleStyleMode.java`
  - 展示模式枚举。
- 创建：`kavinshititlesystem-1.20.1forge/src/main/java/com/kavinshi/playertitle/title/TitleAnimationProfile.java`
  - 动态跑马灯参数。
- 修改：`kavinshititlesystem-1.20.1forge/src/main/java/com/kavinshi/playertitle/config/JsonTitleConfigRepository.java`
  - 解析图标、样式、颜色组、动画参数。
- 创建：`kavinshititlesystem-1.20.1forge/src/main/java/com/kavinshi/playertitle/sync/ClusterEventType.java`
  - 跨服事件类型。
- 创建：`kavinshititlesystem-1.20.1forge/src/main/java/com/kavinshi/playertitle/sync/ClusterSyncEvent.java`
  - 跨服事件模型。
- 创建：`kavinshititlesystem-1.20.1forge/src/main/java/com/kavinshi/playertitle/sync/ClusterSyncGateway.java`
  - 跨服同步端口。
- 创建：`kavinshititlesystem-1.20.1forge/src/main/java/com/kavinshi/playertitle/sync/NoopClusterSyncGateway.java`
  - 本地空实现。
- 创建：`kavinshititlesystem-1.20.1forge/src/main/java/com/kavinshi/playertitle/sync/ClusterRevisionService.java`
  - 每玩家单调递增 revision。
- 创建：`kavinshititlesystem-1.20.1forge/src/main/java/com/kavinshi/playertitle/sync/TitleEventFactory.java`
  - 构造快照与增量事件。
- 修改：`kavinshititlesystem-1.20.1forge/src/main/java/com/kavinshi/playertitle/service/TitleProgressService.java`
  - 输出解锁事件素材。
- 修改：`kavinshititlesystem-1.20.1forge/src/main/java/com/kavinshi/playertitle/service/TitleEquipService.java`
  - 输出装备/卸下事件素材。
- 创建：`kavinshititlesystem-1.20.1forge/src/test/java/com/kavinshi/playertitle/title/TitleDisplayStyleTest.java`
  - 图标与 RGB 配置解析测试。
- 创建：`kavinshititlesystem-1.20.1forge/src/test/java/com/kavinshi/playertitle/sync/ClusterSyncEventTest.java`
  - 事件模型、一致性与幂等字段测试。
- 创建：`kavinshititlesystem-1.20.1forge/src/test/java/com/kavinshi/playertitle/sync/TitleEventFactoryTest.java`
  - 快照与增量事件生成测试。

### 任务 1：扩展称号展示定义

**文件：**
- 创建：`.../title/TitleStyleMode.java`
- 创建：`.../title/TitleAnimationProfile.java`
- 修改：`.../title/TitleDefinition.java`
- 测试：`.../title/TitleDisplayStyleTest.java`

- [ ] 步骤 1：先写失败测试，覆盖图标、静态渐变、动态跑马灯配置字段。
- [ ] 步骤 2：运行测试并确认失败。
- [ ] 步骤 3：实现最小展示模型，不渲染动画帧，只保存定义。
- [ ] 步骤 4：运行测试确认通过。

### 任务 2：扩展 JSON 配置解析

**文件：**
- 修改：`.../config/JsonTitleConfigRepository.java`
- 测试：`.../config/JsonTitleConfigRepositoryTest.java`

- [ ] 步骤 1：补失败测试，覆盖 `icon`、`styleMode`、`baseColors`、`animationProfile` 解析。
- [ ] 步骤 2：运行测试确认失败。
- [ ] 步骤 3：实现最小解析逻辑。
- [ ] 步骤 4：运行测试确认通过。

### 任务 3：建立跨服事件模型

**文件：**
- 创建：`.../sync/ClusterEventType.java`
- 创建：`.../sync/ClusterSyncEvent.java`
- 创建：`.../sync/ClusterRevisionService.java`
- 测试：`.../sync/ClusterSyncEventTest.java`

- [ ] 步骤 1：先写失败测试，覆盖 `schemaVersion`、`eventId`、`revision`、标题展示字段。
- [ ] 步骤 2：运行测试确认失败。
- [ ] 步骤 3：实现最小事件模型与版本生成逻辑。
- [ ] 步骤 4：运行测试确认通过。

### 任务 4：建立事件工厂与网关端口

**文件：**
- 创建：`.../sync/ClusterSyncGateway.java`
- 创建：`.../sync/NoopClusterSyncGateway.java`
- 创建：`.../sync/TitleEventFactory.java`
- 测试：`.../sync/TitleEventFactoryTest.java`

- [ ] 步骤 1：先写失败测试，覆盖 `STATE_SNAPSHOT`、`TITLE_EQUIPPED`、`TITLE_UNEQUIPPED`、`TITLE_REVOKED`。
- [ ] 步骤 2：运行测试确认失败。
- [ ] 步骤 3：实现最小事件工厂与空网关。
- [ ] 步骤 4：运行测试确认通过。

### 任务 5：把事件接入进度与装备服务

**文件：**
- 修改：`.../service/TitleProgressService.java`
- 修改：`.../service/TitleEquipService.java`
- 修改：相关测试文件

- [ ] 步骤 1：先补失败测试，验证解锁/装备/卸下会生成事件素材。
- [ ] 步骤 2：运行测试确认失败。
- [ ] 步骤 3：以最小改动把服务成功路径接上事件生成点。
- [ ] 步骤 4：运行测试确认通过。

### 任务 6：Forge 端验证

**文件：**
- 测试：`src/test/java/...`

- [ ] 步骤 1：运行 `gradlew test`。
- [ ] 步骤 2：验证事件模型包含图标与 RGB 样式定义。
- [ ] 步骤 3：确认 Forge 端仍不承担动画帧渲染。
