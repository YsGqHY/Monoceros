# Monoceros 调度系统参考

## 概述

调度系统（Schedule）提供四种调度类型：延迟、周期、Cron、条件触发。配置文件位于 `plugins/Monoceros/schedule/` 目录下。

## 完整配置示例

```yaml
# 示例：周期广播调度
resource-version: 1

id: world.tick.broadcast
type: periodic
delay: 20t
period: 200t
auto-start: true
prototype: false
async: false
max-runs: -1
max-duration-ms: -1
sender-selectors:
  - type: online_player
  - type: console
execute:
  route: script
  value: schedule.world.tick.broadcast
on-start-script: shared.schedule-start-log
on-stop-script: shared.schedule-stop-log
on-pause-script: null
on-resume-script: null
variables:
  triggerSource: periodic
```

## 字段定义

`ScheduleDefinition` 数据类：

| 字段 | YAML 键 | 类型 | 默认值 | 必填 | 说明 |
|------|---------|------|--------|------|------|
| `id` | `id` | String | -- | 是 | 调度唯一 ID |
| `type` | `type` | ScheduleType | -- | 是 | 调度类型 |
| `delayTicks` | `delay` | 时间字符串 | `0` | 否 | 首次执行延迟 |
| `periodTicks` | `period` | 时间字符串 | `-1` | 否 | 执行周期 |
| `cron` | `cron` | String? | `null` | 否 | Cron 表达式（仅 CRON 类型） |
| `async` | `async` | Boolean | `false` | 否 | 是否异步执行 |
| `autoStart` | `auto-start` | Boolean | `false` | 否 | 是否随插件启动自动开始 |
| `prototype` | `prototype` | Boolean | `false` | 否 | 原型模式（每次 start 创建新实例） |
| `maxRuns` | `max-runs` | Int | `-1` | 否 | 最大执行次数，-1 无限 |
| `maxDurationMs` | `max-duration-ms` | 时间字符串 | `-1` | 否 | 最大运行时长，-1 无限 |
| `senderSelectors` | `sender-selectors` | List | 空列表 | 否 | 发送者选择器 |
| `route` | `execute.route` + `execute.value` | ScheduleRoute | -- | 是 | 执行路由 |
| `onStartScript` | `on-start-script` | String? | `null` | 否 | 启动回调脚本 ID |
| `onStopScript` | `on-stop-script` | String? | `null` | 否 | 停止回调脚本 ID |
| `onPauseScript` | `on-pause-script` | String? | `null` | 否 | 暂停回调脚本 ID |
| `onResumeScript` | `on-resume-script` | String? | `null` | 否 | 恢复回调脚本 ID |
| `variables` | `variables` | Map<String, Any?> | 空映射 | 否 | 注入变量 |

## 调度类型

| 类型 | 说明 | 必要字段 |
|------|------|----------|
| `delay` | 延迟执行一次 | `delay` |
| `periodic` | 周期重复执行 | `delay` + `period` |
| `cron` | Cron 表达式驱动 | `cron` |
| `conditional` | 条件触发 | 由外部条件驱动 |

### delay 示例

```yaml
id: one-shot.welcome
type: delay
delay: 5s
execute:
  route: script
  value: schedule.welcome
```

### periodic 示例

```yaml
id: periodic.cleanup
type: periodic
delay: 1m
period: 5m
async: true
auto-start: true
max-runs: 100
execute:
  route: script
  value: schedule.cleanup
```

### cron 示例

```yaml
id: cron.daily-reset
type: cron
cron: "0 0 0 * * ?"
auto-start: true
execute:
  route: script
  value: schedule.daily-reset
```

## 发送者选择器

`sender-selectors` 定义调度执行时的目标发送者：

```yaml
sender-selectors:
  # 控制台
  - type: console

  # 所有在线玩家
  - type: online_player

  # 指定玩家（按名称）
  - type: player
    value: Steve

  # 指定世界中的所有玩家
  - type: world
    world: world_nether

  # 指定坐标范围内的玩家（球形）
  - type: range
    world: world
    origin: "0,64,0"
    radius: 100.0

  # 指定区域内的玩家（长方体）
  - type: area
    world: world
    minX: -50.0
    minY: 0.0
    minZ: -50.0
    maxX: 50.0
    maxY: 256.0
    maxZ: 50.0
```

`SenderSelectorDefinition` 字段：

| 字段 | 类型 | 说明 |
|------|------|------|
| `type` | SenderSelectorType | 选择器类型 |
| `value` | String? | 值（PLAYER 类型时为玩家名） |
| `world` | String? | 世界名 |
| `origin` | String? | 原点坐标（"x,y,z" 格式） |
| `radius` | Double? | 半径（RANGE 类型） |
| `minX/minY/minZ` | Double? | 最小坐标（AREA 类型） |
| `maxX/maxY/maxZ` | Double? | 最大坐标（AREA 类型） |

选择器类型枚举：

| 类型 | 说明 |
|------|------|
| `CONSOLE` | 控制台 |
| `ONLINE_PLAYER` | 所有在线玩家 |
| `PLAYER` | 指定玩家 |
| `WORLD` | 指定世界的玩家 |
| `RANGE` | 球形范围内的玩家 |
| `AREA` | 长方体区域内的玩家 |

## 调度上下文

`ScheduleContext` 在执行时传递：

| 字段 | 类型 | 说明 |
|------|------|------|
| `definitionId` | String | 调度定义 ID |
| `runtimeId` | String | 运行时实例 ID |
| `runCount` | Int | 当前执行次数 |
| `startedAt` | Long | 启动时间戳 |
| `sender` | ProxyCommandSender? | 当前发送者 |
| `variables` | MutableMap<String, Any?> | 上下文变量 |

脚本中可通过 `&?runCount`、`&?definitionId` 等访问。

## 调度状态

| 状态 | 说明 |
|------|------|
| `WAITING` | 等待启动 |
| `RUNNING` | 运行中 |
| `PAUSED` | 已暂停 |
| `TERMINATED` | 已终止 |

## 服务 API

`ScheduleService` 接口方法：

| 方法 | 说明 |
|------|------|
| `register(definition)` | 注册调度定义 |
| `unregister(id)` | 注销调度 |
| `start(id)` | 启动调度 |
| `pause(id)` | 暂停调度 |
| `resume(id)` | 恢复调度 |
| `stop(id)` | 停止调度 |
| `getHandles()` | 获取所有调度句柄 |

`ScheduleHandle` 接口：

| 方法/属性 | 类型 | 说明 |
|-----------|------|------|
| `definitionId` | String | 定义 ID |
| `runtimeId` | String | 运行时 ID |
| `state` | ScheduleState | 当前状态 |
| `startedAt` | Long | 启动时间 |
| `runCount` | Int | 执行次数 |
| `pause()` | -- | 暂停 |
| `resume()` | -- | 恢复 |
| `stop()` | -- | 停止 |
