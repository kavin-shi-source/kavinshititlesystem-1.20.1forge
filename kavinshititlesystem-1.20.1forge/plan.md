# 事件系统改造与三模块架构实施计划

## 项目概述
将现有的单模组Minecraft Forge称号系统改造为三模块架构，支持跨服务器通信和客户端-服务器分离。实现完整的事件系统，支持LOCAL、REDIS、VELOCITY三种通信模式。

## 实施目标
1. 创建三模块Gradle项目结构：titlesystem-common、titlesystem-server、titlesystem-client
2. 实现完整的事件总线系统，支持三种通信模式
3. 建立服务器-客户端网络通信层
4. 保持向后兼容性，确保现有功能正常工作
5. 添加配置系统支持模式选择

## 技术栈
- Minecraft Forge 1.20.1
- Java 17
- Gradle 8.1+
- Redis (Jedis客户端) - 用于REDIS模式
- Velocity Proxy API - 用于VELOCITY模式
- Jackson JSON - 用于事件序列化

## 任务列表

### 任务 1/8：创建三模块Gradle项目结构
**目标：** 设置多模块Gradle项目，定义模块依赖关系
**完成标准：**
- `settings.gradle` 包含三个子模块定义
- 每个模块有自己的`build.gradle`文件，正确配置依赖
- 项目能成功构建：`./gradlew build` 通过
- Common模块包含共享代码，Server/Client模块依赖Common

**实施步骤：**
1. 修改根目录`settings.gradle`，添加三个子模块
2. 为每个模块创建`build.gradle`文件：
   - Common模块：仅包含共享接口和工具类
   - Server模块：依赖Common，添加Forge Server依赖
   - Client模块：依赖Common，添加Forge Client依赖
3. 配置模块间的依赖关系
4. 运行构建验证：`./gradlew build`
5. 确保现有代码仍然能编译

**验证命令：**
```bash
./gradlew build
./gradlew test
```

### 任务 2/8：迁移现有代码到三模块结构
**目标：** 将现有代码分类迁移到适当的模块
**完成标准：**
- Common模块：包含ClusterEventBus接口、所有事件类、EventSerializer、配置类
- Server模块：包含LocalEventBus、RedisEventBus、VelocityEventBus实现、服务器业务逻辑
- Client模块：包含客户端Packet处理器、UI事件类、渲染逻辑
- 迁移后所有测试通过

**实施步骤：**
1. 创建模块目录结构：`src/main/java/com/kavinshi/playertitle/`
2. 分析现有代码，按职责分类：
   - Common：sync包下的接口和抽象类、事件基类
   - Server：服务实现、数据存储、事件总线实现
   - Client：客户端渲染、UI组件、资源管理
3. 逐个文件迁移，确保包结构一致
4. 更新import语句和包声明
5. 运行测试验证迁移正确性

**验证命令：**
```bash
./gradlew test
# 每个模块单独测试
./gradlew :titlesystem-common:test
./gradlew :titlesystem-server:test  
./gradlew :titlesystem-client:test
```

### 任务 3/8：完成RedisEventBus实现
**目标：** 补全RedisEventBus中的TODO部分，实现完整的Redis Pub/Sub通信
**完成标准：**
- RedisEventBus能成功连接Redis服务器
- 能发布和订阅事件，事件能跨服务器传递
- 包含完整的序列化/反序列化逻辑
- 添加Jedis依赖到Server模块build.gradle
- 编写单元测试验证Redis功能

**实施步骤：**
1. 在Server模块的`build.gradle`中添加Jedis依赖：
   ```gradle
   implementation 'redis.clients:jedis:5.0.0'
   ```
2. 实现RedisEventBus中的TODO方法：
   - `publish()`方法的Redis发布逻辑
   - `start()`方法的Jedis连接池初始化
   - `stop()`方法的资源清理
   - `serializeEvent()`和`deserializeEvent()`使用Jackson
   - `handleRedisMessage()`的消息处理
3. 创建Redis配置类`RedisConfig`
4. 编写RedisEventBus测试，模拟Redis服务器或使用测试容器
5. 验证事件能通过Redis发布和接收

**验证命令：**
```bash
# 需要运行Redis实例或使用测试容器
./gradlew :titlesystem-server:test --tests "*RedisEventBus*"
```

### 任务 4/8：实现VelocityEventBus
**目标：** 创建Velocity代理的事件总线实现，支持通过Velocity插件消息通信
**完成标准：**
- VelocityEventBus实现ClusterEventBus接口
- 能通过Velocity Plugin Messaging发送和接收事件
- 包含Velocity API依赖管理
- 有完整的配置类VelocityConfig
- 编写单元测试（模拟测试）

**实施步骤：**
1. 研究Velocity Plugin Messaging API
2. 创建VelocityEventBus类，实现ClusterEventBus接口
3. 实现事件通过Velocity通道发送：
   - 使用Velocity的`Server`API发送插件消息
   - 注册插件消息监听器接收事件
4. 创建VelocityConfig配置类
5. 添加Velocity API依赖到Server模块（注意版本兼容性）
6. 编写模拟测试，验证事件流转
7. 处理网络异常和重连逻辑

**验证命令：**
```bash
./gradlew :titlesystem-server:test --tests "*VelocityEventBus*"
```

### 任务 5/8：开发Forge网络包系统
**目标：** 建立服务器-客户端网络通信层，支持实时数据同步
**完成标准：**
- 定义5种网络Packet类型，覆盖所有同步场景
- 服务器能向客户端发送Packet，客户端能正确处理
- 支持增量更新和全量同步
- 网络层有错误处理和重试机制
- 编写网络层单元测试

**实施步骤：**
1. 在Common模块定义Packet接口和基类
2. 实现5种Packet类型：
   - `TitleDataSyncPacket`：称号数据同步
   - `TitleEquipUpdatePacket`：装备状态更新
   - `TitleProgressSyncPacket`：进度百分比更新
   - `IconResourcePackPacket`：图标资源包通知
   - `PlayerTitleStatePacket`：玩家状态同步
3. 在Server模块注册Packet发送逻辑
4. 在Client模块注册Packet接收处理器
5. 实现Packet的编码/解码（使用FriendlyByteBuf）
6. 添加网络通道注册和初始化逻辑
7. 编写测试验证Packet序列化/反序列化

**验证命令：**
```bash
./gradlew :titlesystem-common:test --tests "*Packet*"
./gradlew :titlesystem-server:test --tests "*Network*"
./gradlew :titlesystem-client:test --tests "*PacketHandler*"
```

### 任务 6/8：实现客户端事件处理器和UI系统
**目标：** 客户端能接收和处理服务器事件，更新UI显示
**完成标准：**
- 客户端能接收所有5种网络Packet并更新本地状态
- 实现客户端内部事件总线，管理UI状态
- 称号和图标能正确显示，支持动画
- 资源包生成和加载功能正常工作
- 客户端测试验证UI逻辑

**实施步骤：**
1. 在Client模块创建Packet处理器类
2. 实现客户端内部事件系统：
   - `ClientEventBus`管理UI事件
   - `TitleDisplayManager`控制称号显示
   - `IconRenderer`处理图标渲染
3. 完善ClientIconManager，确保资源包生成正确
4. 实现动画系统：支持称号显示动画、进度条动画
5. 创建客户端配置界面（可选）
6. 编写客户端单元测试和集成测试
7. 验证端到端流程：服务器事件→网络Packet→客户端更新→UI渲染

**验证命令：**
```bash
./gradlew :titlesystem-client:test
# 启动Minecraft客户端测试实际渲染
```

### 任务 7/8：扩展配置系统支持模式选择
**目标：** 创建可配置的通信模式选择系统
**完成标准：**
- 支持LOCAL、REDIS、VELOCITY三种模式配置
- 配置文件能动态加载，无需重启游戏
- 配置变更能正确切换事件总线实现
- 有完整的配置验证和错误处理
- 编写配置系统测试

**实施步骤：**
1. 创建`CommunicationMode`枚举
2. 创建`SyncConfig`配置类，包含三种模式的配置
3. 实现`EventBusFactory`，根据配置创建对应事件总线
4. 创建配置管理器`ConfigManager`，支持热重载
5. 添加配置文件示例和文档
6. 实现配置验证：检查Redis连接、Velocity连接等
7. 编写配置系统测试

**验证命令：**
```bash
./gradlew :titlesystem-common:test --tests "*Config*"
./gradlew :titlesystem-server:test --tests "*EventBusFactory*"
```

### 任务 8/8：集成测试与端到端验证
**目标：** 验证整个系统端到端工作正常
**完成标准：**
- 三模块能协同工作，事件能跨模块传递
- 所有测试通过，包括单元测试、集成测试
- 在不同通信模式下测试功能完整性
- 性能测试验证系统响应时间
- 文档更新完成

**实施步骤：**
1. 创建集成测试场景：
   - 场景1：单服务器模式（LOCAL）
   - 场景2：跨服务器Redis模式
   - 场景3：Velocity代理模式
2. 编写端到端测试用例
3. 性能测试：测量事件延迟、网络开销
4. 压力测试：模拟多玩家同时操作
5. 更新所有文档：README、架构文档、API文档
6. 创建部署指南和配置示例
7. 最终构建验证：`./gradlew build` 成功

**验证命令：**
```bash
# 完整测试套件
./gradlew test
# 集成测试
./gradlew :titlesystem-server:integrationTest
# 构建最终产物
./gradlew build
```

## 依赖关系
- 任务2依赖任务1（需要先创建模块结构）
- 任务3、4、5可并行开发（不同通信层）
- 任务6依赖任务5（需要网络包系统）
- 任务7依赖任务3、4（需要各种事件总线）
- 任务8依赖所有前序任务

## 风险与缓解
1. **Redis连接问题**：添加连接池和重试机制，提供详细的错误日志
2. **Velocity版本兼容性**：明确支持的Velocity版本，测试多个版本
3. **网络延迟影响**：实现客户端缓存，减少对实时网络的依赖
4. **内存占用**：优化事件序列化，使用对象池
5. **向后兼容性**：保持原有API不变，使用适配器模式

## 成功标准
1. 所有8个任务完成，验证条件满足
2. 测试覆盖率>80%
3. 三模块能独立编译和打包
4. 支持三种通信模式可配置切换
5. 端到端集成测试通过
6. 文档完整，包含部署指南

## 审查检查点
- 每完成2个任务后审查进度
- 任务4完成后审查Velocity集成方案
- 任务7完成后审查配置系统设计
- 最终审查所有集成测试结果