# Monoceros 事件分发器参考

## 概述

事件分发器（Dispatcher）是 Monoceros 的核心监听机制，以"事件类型 -> 事件优先级 -> Dispatcher 列表"的方式组织监听器。配置文件位于 `plugins/Monoceros/dispatcher/` 目录下。

## 完整配置示例

```yaml
# 示例：玩家加入事件分发器
resource-version: 1

id: player.join.welcome
listen-event: PlayerJoinEvent
listen-priority: NORMAL
weight: 20
ignore-cancelled: false
before-script: shared.audit-before
execute:
  route: script
  value: dispatcher.player-join
after-script: shared.audit-after
rules:
  - type: permission
    value: monoceros.join.welcome
  - type: world
    value: world
variables:
  triggerSource: join
  welcomeType: default
```

## 字段定义

`DispatcherDefinition` 数据类：

| 字段 | YAML 键 | 类型 | 默认值 | 必填 | 说明 |
|------|---------|------|--------|------|------|
| `id` | `id` | String | -- | 是 | 分发器唯一 ID |
| `eventKey` | `listen-event` | String | -- | 是 | Bukkit 事件类简名 |
| `priority` | `listen-priority` | EventPriority | `NORMAL` | 否 | 事件监听优先级 |
| `weight` | `weight` | Int | `0` | 否 | 同优先级下的权重（越大越先执行） |
| `ignoreCancelled` | `ignore-cancelled` | Boolean | `false` | 否 | 是否忽略已取消的事件 |
| `beforeScript` | `before-script` | String? | `null` | 否 | 前置脚本 ID |
| `executeRoute` | `execute.route` + `execute.value` | DispatcherRoute | -- | 是 | 执行路由 |
| `afterScript` | `after-script` | String? | `null` | 否 | 后置脚本 ID |
| `rules` | `rules` | List | 空列表 | 否 | 过滤规则列表 |
| `variables` | `variables` | Map<String, Any?> | 空映射 | 否 | 注入变量 |

## 事件优先级

`listen-priority` 支持 Bukkit 标准 EventPriority 枚举值：

| 值 | 执行顺序 | 说明 |
|----|----------|------|
| `LOWEST` | 最先 | 最早执行，适合预处理 |
| `LOW` | 较早 | |
| `NORMAL` | 默认 | 大多数分发器使用 |
| `HIGH` | 较晚 | |
| `HIGHEST` | 最晚 | 最后执行，适合最终决策 |
| `MONITOR` | 监控 | 仅用于观察，不应修改事件 |

同一优先级下，`weight` 值越大的分发器越先执行。

## 路由类型

```yaml
# 路由到 Fluxon 脚本
execute:
  route: script
  value: dispatcher.player-join

# 路由到动作工作流
execute:
  route: action-workflow
  value: combat.hit-feedback

# 路由到代码注册的处理器
execute:
  route: handler
  value: my-handler-id
```

## 分发器规则

`rules` 字段定义过滤规则，每条规则包含 `type` 和 `value`：

```yaml
rules:
  - type: permission
    value: monoceros.admin
  - type: world
    value: world_nether
  - type: script
    value: my.condition.script
```

规则通过 `DispatcherRule.test(context)` 返回 `DispatcherDecision`：

| 字段 | 类型 | 说明 |
|------|------|------|
| `filtered` | Boolean | 是否过滤掉此事件（不执行路由） |
| `cancelEvent` | Boolean | 是否取消原始 Bukkit 事件 |
| `extraVariables` | Map<String, Any?> | 追加到上下文的额外变量 |

## 分发器上下文

`DispatcherContext` 在执行过程中传递：

| 字段 | 类型 | 说明 |
|------|------|------|
| `definitionId` | String | 分发器定义 ID |
| `event` | Event | 原始 Bukkit 事件对象 |
| `sender` | ProxyCommandSender? | TabooLib 抽象发送者 |
| `variables` | MutableMap<String, Any?> | 上下文变量（可读写） |
| `filtered` | Boolean | 是否被过滤 |
| `cancelled` | Boolean | 是否被取消 |
| `routeResult` | Any? | 路由执行结果 |

## 内建 Pipeline 事件

Monoceros 为以下 13 个 Bukkit 事件内建了 Pipeline 支持，提供 5 阶段生命周期（initPrincipal、initVariables、filter、afterFilter、postprocess）：

| 事件类 | 说明 |
|--------|------|
| `AsyncPlayerChatEvent` | 异步玩家聊天 |
| `PlayerCommandPreprocessEvent` | 玩家命令预处理 |
| `PlayerMoveEvent` | 玩家移动 |
| `PlayerJoinEvent` | 玩家加入 |
| `PlayerQuitEvent` | 玩家退出 |
| `PlayerTeleportEvent` | 玩家传送 |
| `PlayerDamageEvent` | 玩家受伤 |
| `PlayerDamageByEntityEvent` | 玩家被实体伤害 |
| `PlayerDamageByPlayerEvent` | 玩家被玩家伤害 |
| `PlayerShootBowEvent` | 玩家射箭 |
| `EntityDamageEvent` | 实体受伤 |
| `EntityDamageByEntityEvent` | 实体被实体伤害 |
| `EntityShootBowEvent` | 实体射箭 |

注意：`listen-event` 字段使用事件类的简名（不含包名），如 `PlayerJoinEvent` 而非 `org.bukkit.event.player.PlayerJoinEvent`。

## Pipeline 生命周期

每个内建 Pipeline 事件经过 5 个阶段：

| 阶段 | 方法 | 说明 |
|------|------|------|
| 1 | `initPrincipal` | 初始化主体（通常是玩家或实体） |
| 2 | `initVariables` | 初始化上下文变量 |
| 3 | `filter` | 执行过滤规则 |
| 4 | `afterFilter` | 过滤后处理 |
| 5 | `postprocess` | 后处理（路由执行后） |

`PipelineContext` 字段：

| 字段 | 类型 | 说明 |
|------|------|------|
| `event` | Event | 原始事件 |
| `principal` | Any? | 主体对象 |
| `principalId` | UUID? | 主体 UUID |
| `player` | Player? | 玩家对象 |
| `isCancelled` | Boolean | 是否取消 |
| `isFiltered` | Boolean | 是否过滤 |
| `isFilterBaffled` | Boolean | 是否被规则阻挡 |
| `result` | Any? | 执行结果 |
| `variables` | MutableMap<String, Any?> | 上下文变量 |

## 服务 API

`DispatcherService` 接口方法：

| 方法 | 说明 |
|------|------|
| `register(definition)` | 注册分发器定义 |
| `unregister(id)` | 注销分发器 |
| `get(id)` | 获取分发器定义 |
| `reloadAll()` | 重载所有分发器 |

`PipelineRegistry` 接口方法：

| 方法 | 说明 |
|------|------|
| `register(eventName, pipeline)` | 注册 Pipeline |
| `getPipelines(eventName)` | 获取指定事件的 Pipeline 列表 |
| `registeredEvents()` | 获取所有已注册事件名 |
