# 方案 1 Velocity 端实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 `superpowers:subagent-driven-development`（推荐）或 `superpowers:executing-plans` 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 新建一个 Velocity 插件，消费 Redis 跨服事件并统一处理跨服聊天、Tab 展示、图标头衔以及 RGB 渐变/动态跑马灯。

**架构：** Velocity 插件不拥有称号业务真值，只维护在线展示缓存。它订阅 Redis 事件、按 `revision` 去重与校准，并把最终展示结果输出到聊天、原生 Tab 或 Velocitab 适配层。

**技术栈：** Java 17、Velocity API、Redis 客户端、Adventure/MiniMessage。

---

## 文件结构

- 创建：`velocity-playertitle/settings.gradle`
- 创建：`velocity-playertitle/build.gradle`
- 创建：`velocity-playertitle/src/main/resources/velocity-plugin.json`
- 创建：`velocity-playertitle/src/main/java/com/kavinshi/playertitle/proxy/ProxyTitlePlugin.java`
  - 插件入口。
- 创建：`velocity-playertitle/src/main/java/com/kavinshi/playertitle/proxy/sync/ProxyTitleView.java`
  - 在线玩家展示视图。
- 创建：`velocity-playertitle/src/main/java/com/kavinshi/playertitle/proxy/sync/ProxyTitleCache.java`
  - 带 revision 的缓存层。
- 创建：`velocity-playertitle/src/main/java/com/kavinshi/playertitle/proxy/sync/RedisClusterSubscriber.java`
  - Redis 订阅器。
- 创建：`velocity-playertitle/src/main/java/com/kavinshi/playertitle/proxy/render/TitleRenderService.java`
  - 图标、静态渐变、动态跑马灯渲染服务。
- 创建：`velocity-playertitle/src/main/java/com/kavinshi/playertitle/proxy/render/AnimatedFrameService.java`
  - 时间片动画帧更新。
- 创建：`velocity-playertitle/src/main/java/com/kavinshi/playertitle/proxy/chat/ProxyChatFormatter.java`
  - 跨服聊天称号渲染。
- 创建：`velocity-playertitle/src/main/java/com/kavinshi/playertitle/proxy/tab/ProxyTabSyncService.java`
  - 原生 Tab 展示同步。
- 创建：`velocity-playertitle/src/main/java/com/kavinshi/playertitle/proxy/velocitab/VelocitabBridge.java`
  - Velocitab 适配层。
- 创建：`velocity-playertitle/src/test/java/...`
  - 缓存去重、动态渲染、聊天格式、Velocitab 适配测试。

### 任务 1：建立 Velocity 插件骨架

**文件：**
- 创建：`velocity-playertitle/.../ProxyTitlePlugin.java`
- 创建：`velocity-playertitle/.../velocity-plugin.json`

- [ ] 步骤 1：创建最小插件工程与入口。
- [ ] 步骤 2：验证工程可解析。
- [ ] 步骤 3：确保插件不依赖 Forge 类。

### 任务 2：建立 Proxy 展示缓存

**文件：**
- 创建：`.../sync/ProxyTitleView.java`
- 创建：`.../sync/ProxyTitleCache.java`
- 测试：`.../sync/ProxyTitleCacheTest.java`

- [ ] 步骤 1：先写失败测试，覆盖 `eventId` 去重、`revision` 比较、快照覆盖。
- [ ] 步骤 2：运行测试确认失败。
- [ ] 步骤 3：实现最小缓存层。
- [ ] 步骤 4：运行测试确认通过。

### 任务 3：建立渲染服务

**文件：**
- 创建：`.../render/TitleRenderService.java`
- 创建：`.../render/AnimatedFrameService.java`
- 测试：`.../render/TitleRenderServiceTest.java`

- [ ] 步骤 1：先写失败测试，覆盖图标、静态渐变、动态跑马灯帧输出。
- [ ] 步骤 2：运行测试确认失败。
- [ ] 步骤 3：实现最小渲染逻辑。
- [ ] 步骤 4：运行测试确认通过。

### 任务 4：接入聊天展示

**文件：**
- 创建：`.../chat/ProxyChatFormatter.java`
- 测试：`.../chat/ProxyChatFormatterTest.java`

- [ ] 步骤 1：先写失败测试，覆盖普通称号、图标称号、静态渐变、动态称号单帧固化。
- [ ] 步骤 2：运行测试确认失败。
- [ ] 步骤 3：实现最小聊天格式化逻辑。
- [ ] 步骤 4：运行测试确认通过。

### 任务 5：接入原生 Tab 与 Velocitab 适配

**文件：**
- 创建：`.../tab/ProxyTabSyncService.java`
- 创建：`.../velocitab/VelocitabBridge.java`
- 测试：相关测试文件

- [ ] 步骤 1：先写失败测试，覆盖原生 Tab 展示值与 Velocitab 输出值。
- [ ] 步骤 2：运行测试确认失败。
- [ ] 步骤 3：实现最小同步与桥接逻辑。
- [ ] 步骤 4：运行测试确认通过。

### 任务 6：接入 Redis 与恢复策略

**文件：**
- 创建：`.../sync/RedisClusterSubscriber.java`
- 修改：缓存与入口类

- [ ] 步骤 1：实现 Redis 事件订阅。
- [ ] 步骤 2：实现重连后缓存待校准策略。
- [ ] 步骤 3：实现 `PLAYER_DISCONNECT` 清理与幽灵缓存清扫。
- [ ] 步骤 4：完成 Proxy 侧验证。
