﻿# SolsTitleSystem MVP 服务端重写实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 `superpowers:subagent-driven-development`（推荐）或 `superpowers:executing-plans` 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 在 `kavinshititlesystem-1.20.1forge` 目录中建立一个可持续开发的 Forge 1.20.1 工程，先落地“可持久化、可重连恢复、可同步”的称号系统 MVP 服务端能力，并在架构上预留跨服同步能力。

**架构：** 采用“兼容式重写”路线，保留原模组的核心数据流：配置加载 -> 注册表 -> 玩家状态 -> 服务端事件统计 -> 解锁/装备 -> 当前服同步。跨服同步不在第一批就接入具体中间件，但从第一天开始抽象为独立端口，所有关键状态变化都通过统一事件模型发布，避免未来返工。首期不实现 buff 与 combat lock，仅保留可插拔规则接口。

**技术栈：** Java 17、Minecraft Forge 1.20.1、ForgeGradle、JUnit 5（纯 Java 单元测试）、Gson。

---

## 文件结构

**实现守则：**

- 不直接复制反编译后的实现代码，只参考其结构、命名与数据流。
- 所有玩家状态变更必须经过服务层，不允许命令层或网络层直接改状态。
- 配置中的称号 `id` 视为稳定主键；重载后若已装备称号不存在，必须自动降级为未装备状态。
- 当前服客户端同步与跨服同步是两条链路，不能复用同一套传输职责。

- 创建：`kavinshititlesystem-1.20.1forge/settings.gradle`
- 创建：`kavinshititlesystem-1.20.1forge/build.gradle`
- 创建：`kavinshititlesystem-1.20.1forge/gradle.properties`
- 创建：`kavinshititlesystem-1.20.1forge/src/main/resources/META-INF/mods.toml`
- 创建：`kavinshititlesystem-1.20.1forge/src/main/resources/pack.mcmeta`
- 创建：`kavinshititlesystem-1.20.1forge/src/main/java/com/kavinshi/playertitle/KavinshiPlayerTitleMod.java`
- 创建：`kavinshititlesystem-1.20.1forge/src/main/java/com/kavinshi/playertitle/bootstrap/RewriteBootstrap.java`
- 创建：`kavinshititlesystem-1.20.1forge/src/main/java/com/kavinshi/playertitle/title/TitleDefinition.java`
- 创建：`kavinshititlesystem-1.20.1forge/src/main/java/com/kavinshi/playertitle/title/TitleCondition.java`
- 创建：`kavinshititlesystem-1.20.1forge/src/main/java/com/kavinshi/playertitle/title/TitleConditionType.java`
- 创建：`kavinshititlesystem-1.20.1forge/src/main/java/com/kavinshi/playertitle/title/TitleRegistry.java`
- 创建：`kavinshititlesystem-1.20.1forge/src/main/java/com/kavinshi/playertitle/player/PlayerTitleState.java`
- 创建：`kavinshititlesystem-1.20.1forge/src/main/java/com/kavinshi/playertitle/player/PlayerTitleStateRepository.java`
- 创建：`kavinshititlesystem-1.20.1forge/src/main/java/com/kavinshi/playertitle/player/ForgePlayerTitleStateStore.java`
- 创建：`kavinshititlesystem-1.20.1forge/src/main/java/com/kavinshi/playertitle/player/PlayerStateLifecycleHandler.java`
- 创建：`kavinshititlesystem-1.20.1forge/src/main/java/com/kavinshi/playertitle/config/TitleConfigRepository.java`
- 创建：`kavinshititlesystem-1.20.1forge/src/main/java/com/kavinshi/playertitle/config/JsonTitleConfigRepository.java`
- 创建：`kavinshititlesystem-1.20.1forge/src/main/java/com/kavinshi/playertitle/service/TitleProgressService.java`
- 创建：`kavinshititlesystem-1.20.1forge/src/main/java/com/kavinshi/playertitle/service/TitleEquipService.java`
- 创建：`kavinshititlesystem-1.20.1forge/src/main/java/com/kavinshi/playertitle/sync/ClusterEventType.java`
- 创建：`kavinshititlesystem-1.20.1forge/src/main/java/com/kavinshi/playertitle/sync/ClusterSyncEvent.java`
- 创建：`kavinshititlesystem-1.20.1forge/src/main/java/com/kavinshi/playertitle/sync/ClusterSyncGateway.java`
- 创建：`kavinshititlesystem-1.20.1forge/src/main/java/com/kavinshi/playertitle/sync/NoopClusterSyncGateway.java`
- 创建：`kavinshititlesystem-1.20.1forge/src/main/java/com/kavinshi/playertitle/network/TitleNetwork.java`
- 创建：`kavinshititlesystem-1.20.1forge/src/main/java/com/kavinshi/playertitle/network/PlayerTitleStateSyncService.java`
- 创建：`kavinshititlesystem-1.20.1forge/src/main/java/com/kavinshi/playertitle/network/TitleRegistrySyncService.java`
- 创建：`kavinshititlesystem-1.20.1forge/src/main/java/com/kavinshi/playertitle/network/EquippedTitleBroadcastService.java`
- 创建：`kavinshititlesystem-1.20.1forge/src/main/java/com/kavinshi/playertitle/network/packet/SyncTitleRegistryPacket.java`
- 创建：`kavinshititlesystem-1.20.1forge/src/main/java/com/kavinshi/playertitle/network/packet/SyncTitleStatePacket.java`
- 创建：`kavinshititlesystem-1.20.1forge/src/main/java/com/kavinshi/playertitle/network/packet/SyncEquippedTitlePacket.java`
- 创建：`kavinshititlesystem-1.20.1forge/src/main/java/com/kavinshi/playertitle/command/TitleAdminCommand.java`
- 创建：`kavinshititlesystem-1.20.1forge/src/main/java/com/kavinshi/playertitle/handler/ServerEventHandler.java`
- 创建：`kavinshititlesystem-1.20.1forge/src/test/java/com/kavinshi/playertitle/title/TitleConditionTest.java`
- 创建：`kavinshititlesystem-1.20.1forge/src/test/java/com/kavinshi/playertitle/title/TitleDefinitionTest.java`
- 创建：`kavinshititlesystem-1.20.1forge/src/test/java/com/kavinshi/playertitle/title/TitleRegistryTest.java`
- 创建：`kavinshititlesystem-1.20.1forge/src/test/java/com/kavinshi/playertitle/player/PlayerTitleStateTest.java`
- 创建：`kavinshititlesystem-1.20.1forge/src/test/java/com/kavinshi/playertitle/service/TitleProgressServiceTest.java`
- 创建：`kavinshititlesystem-1.20.1forge/src/test/java/com/kavinshi/playertitle/service/TitleEquipServiceTest.java`

## 任务 1：建立工程骨架

**文件：**
- 创建：`kavinshititlesystem-1.20.1forge/settings.gradle`
- 创建：`kavinshititlesystem-1.20.1forge/build.gradle`
- 创建：`kavinshititlesystem-1.20.1forge/gradle.properties`
- 创建：`kavinshititlesystem-1.20.1forge/src/main/resources/META-INF/mods.toml`
- 创建：`kavinshititlesystem-1.20.1forge/src/main/resources/pack.mcmeta`
- 创建：`kavinshititlesystem-1.20.1forge/src/main/java/com/kavinshi/playertitle/KavinshiPlayerTitleMod.java`
- 创建：`kavinshititlesystem-1.20.1forge/src/main/java/com/kavinshi/playertitle/bootstrap/RewriteBootstrap.java`

- [ ] 步骤 1：创建 Gradle 工程基础文件，锁定 Forge 1.20.1 与 Java 17。
- [ ] 步骤 2：补齐最小资源文件与模组入口，保证工程结构可继续扩展。
- [ ] 步骤 3：补一个最小 `RewriteBootstrap`，避免入口类从第一天开始承担装配职责。
- [ ] 步骤 4：运行 `gradlew tasks` 或等效命令，验证构建脚手架能被解析。
- [ ] 步骤 5：记录未完成项，避免在本任务中提前引入业务逻辑。

## 任务 2：落地核心领域模型

**文件：**
- 创建：`kavinshititlesystem-1.20.1forge/src/main/java/com/kavinshi/playertitle/title/TitleDefinition.java`
- 创建：`kavinshititlesystem-1.20.1forge/src/main/java/com/kavinshi/playertitle/title/TitleCondition.java`
- 创建：`kavinshititlesystem-1.20.1forge/src/main/java/com/kavinshi/playertitle/title/TitleConditionType.java`
- 创建：`kavinshititlesystem-1.20.1forge/src/main/java/com/kavinshi/playertitle/title/TitleRegistry.java`
- 创建：`kavinshititlesystem-1.20.1forge/src/main/java/com/kavinshi/playertitle/player/PlayerTitleState.java`
- 测试：`kavinshititlesystem-1.20.1forge/src/test/java/com/kavinshi/playertitle/title/TitleConditionTest.java`
- 测试：`kavinshititlesystem-1.20.1forge/src/test/java/com/kavinshi/playertitle/title/TitleDefinitionTest.java`
- 测试：`kavinshititlesystem-1.20.1forge/src/test/java/com/kavinshi/playertitle/title/TitleRegistryTest.java`
- 测试：`kavinshititlesystem-1.20.1forge/src/test/java/com/kavinshi/playertitle/player/PlayerTitleStateTest.java`

- [ ] 步骤 1：先写失败测试，覆盖 hostile 击杀、指定实体、生存时间、多条件组合、注册表排序、玩家状态解锁/装备/撤销流程。
- [ ] 步骤 2：运行测试并确认失败，证明模型尚未实现。
- [ ] 步骤 3：实现 `TitleCondition`、`TitleDefinition`、`TitleRegistry`、`PlayerTitleState` 的最小代码。
- [ ] 步骤 4：再次运行测试，确认领域模型测试通过。
- [ ] 步骤 5：检查模型字段是否覆盖跨服所需最小状态：玩家 UUID、已解锁称号、当前装备、进度快照。
- [ ] 步骤 6：冻结领域事件的最小输出形态，后续服务统一复用，避免各自定义结果对象。

## 任务 3：实现配置加载与注册表装配

- [ ] 定义配置读取接口，隔离 JSON、数据库或远程配置来源。
- [ ] 实现 JSON 读取器，先支持 MVP 所需字段。
- [ ] 用小样本称号配置验证装配流程。
- [ ] 保证注册表加载顺序稳定，便于后续客户端显示与跨服一致性。
- [ ] 明确配置重载一致性规则：稳定 `id`、非法装备降级、删除称号后的处理策略。

## 任务 4：实现玩家状态持久化与生命周期桥接

- [ ] 定义状态仓储端口，明确读取、保存、复制、清理职责。
- [ ] 实现 Forge 持久化适配器，优先保证下线、重连、重生后的状态闭环。
- [ ] 接上登录、克隆、重生、换维度等生命周期桥接。
- [ ] 验证死亡后生存时间重置、重连后解锁状态保留、已装备状态可恢复。

## 任务 5：实现进度计算与解锁服务

- [ ] 先写失败测试，覆盖击杀累计、周期性生存时间推进、首次解锁、重复解锁去重。
- [ ] 实现最小服务逻辑，只处理 MVP 事件输入与状态输出。
- [ ] 让解锁结果输出为显式结果对象，避免直接耦合网络与命令层。
- [ ] 在服务中追加跨服事件构造点，但先不接入真实中间件。

## 任务 6：实现装备服务与跨服同步端口

- [ ] 先写失败测试，覆盖未解锁不可装备、已解锁可装备、重复装备幂等、卸下称号。
- [ ] 实现装备服务，本期不引入 buff 与 combat lock，只保留规则扩展接口。
- [ ] 定义跨服事件模型，至少覆盖 `TITLE_UNLOCKED`、`TITLE_EQUIPPED`、`TITLE_UNEQUIPPED`、`STATE_SNAPSHOT`。
- [ ] 在服务成功路径中通过 `ClusterSyncGateway` 发布事件，默认接 `Noop` 实现。

## 任务 7：接上 Forge 事件、命令与当前服同步

- [ ] 接入服务端事件入口，只桥接 MVP 所需事件，不提前做 GUI。
- [ ] 实现网络通道与消息注册，显式区分注册表、自身状态、外显称号三类同步。
- [ ] 实现最小管理员命令，至少支持 `reload`、`grant`、`revoke`、`check`，且全部走服务层。
- [ ] 实现当前服客户端同步服务，让状态变化能即时下发。
- [ ] 将“当前服同步”和“跨服同步”分开，不让网络包直接承担跨服职责。

## 任务 8：验证与收尾

- [ ] 运行单元测试，确认核心领域逻辑通过。
- [ ] 补充最小验证：配置解析、注册表排序稳定性、状态持久化、事件桥接幂等。
- [ ] 运行 Gradle 构建或编译任务，确认工程可解析。
- [ ] 检查是否存在把反编译代码直接复制进新工程的情况，若有则替换为重写实现。
- [ ] 列出下一阶段待办：buff、combat lock、客户端 GUI、真实跨服适配器。
