# 最终方案整合计划

> **面向 AI 代理的工作者：** 必需子技能：使用 `superpowers:subagent-driven-development`（推荐）或 `superpowers:executing-plans` 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 把当前 Forge 重写工程、跨服同步规格、方案 1 的 Forge 子计划与 Velocity 子计划整合成统一路线，并明确实现顺序。

**架构：** 统一路线为 `Forge 模组（权威业务） -> Redis（跨服事件） -> Velocity 插件（展示协调） -> 聊天 / Tab / Velocitab`。称号展示支持图标头衔、静态渐变与动态跑马灯，其中动态跑马灯仅在 Proxy 侧持续刷新，聊天只取发送瞬间帧。

**技术栈：** Java 17、Forge、Velocity、Redis、Adventure、JUnit 5、Gson。

---

## 关联文档

- 规格：`docs/superpowers/specs/2026-04-20-cross-server-sync-design.md`
- 总计划：`docs/superpowers/plans/2026-04-20-solstitlesystem-mvp-server.md`
- Forge 子计划：`docs/superpowers/plans/2026-04-20-scheme1-forge-plan.md`
- Velocity 子计划：`docs/superpowers/plans/2026-04-20-scheme1-velocity-plan.md`

## 最终方案

- **阶段 1：Forge 权威层**
  - 完成称号业务、配置、持久化、事件模型
  - 补齐图标与 RGB 样式定义
  - 定义 `ClusterSyncGateway`

- **阶段 2：Forge 跨服输出层**
  - 生成 `STATE_SNAPSHOT`、`TITLE_EQUIPPED`、`TITLE_UNEQUIPPED`、`TITLE_REVOKED`
  - 引入 `schemaVersion`、`eventId`、`revision`

- **阶段 3：Velocity 展示协调层**
  - 消费 Redis 事件
  - 维护在线展示缓存
  - 渲染聊天称号
  - 渲染原生 Tab

- **阶段 4：Velocitab 适配层**
  - 输出 `title_display`
  - 输出 `title_icon`
  - 输出 `title_weight`
  - 输出样式模式与颜色值

## 实施顺序

### 任务 1：完成 Forge 子计划

- [ ] 按 `2026-04-20-scheme1-forge-plan.md` 执行。

### 任务 2：补充 Redis 真实适配器

- [ ] 在 Forge 端以 `NoopClusterSyncGateway` 为基线，增加 Redis 实现。
- [ ] 在 Velocity 端增加 Redis 订阅器。

### 任务 3：完成 Velocity 子计划

- [ ] 按 `2026-04-20-scheme1-velocity-plan.md` 执行。

### 任务 4：联调与验证

- [ ] 验证图标头衔在聊天展示中可用。
- [ ] 验证静态渐变在聊天与 Tab 中可用。
- [ ] 验证动态跑马灯在 Tab 与 Velocitab 中可用。
- [ ] 验证聊天动态称号按发送瞬间帧固化。
- [ ] 验证切服、重连、Proxy 重启、Redis 重连。
