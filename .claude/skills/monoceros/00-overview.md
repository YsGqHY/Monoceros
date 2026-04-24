# Monoceros 配置总览与通用约定

## 运行时目录结构

Monoceros 的所有配置文件位于服务器 `plugins/Monoceros/` 目录下：

```
plugins/Monoceros/
├── config.yml                    # 主配置
├── lang/                         # 语言文件
│   ├── zh_CN.yml
│   └── en_US.yml
├── script/                       # 脚本文件（.fs / .yml）
│   ├── dispatcher/               # 分发器关联脚本
│   ├── schedule/                 # 调度关联脚本
│   ├── command/                  # 命令关联脚本
│   ├── action/                   # 动作工作流关联脚本
│   ├── shared/                   # 共享/公用脚本
│   └── debug/                    # 调试脚本
├── dispatcher/                   # 事件分发器定义（.yml）
├── schedule/                     # 调度定义（.yml）
├── wireshark/                    # 数据包监听定义（.yml）
└── workflow/
    └── action/                   # 动作工作流定义（.yml）
```

## 文件 ID 推导规则

所有配置文件的 ID 由文件相对路径自动推导：

1. 取文件相对于所属目录的路径
2. 去掉文件扩展名（`.yml`、`.yaml`、`.fs`）
3. 将路径分隔符 `/` 替换为 `.`

| 文件路径 | 推导 ID |
|----------|---------|
| `dispatcher/player-join.yml` | `player-join` |
| `script/dispatcher/player-join.fs` | `dispatcher.player-join` |
| `script/shared/audit-before.fs` | `shared.audit-before` |
| `schedule/broadcast.yml` | `broadcast` |
| `workflow/action/combat-hit.yml` | `combat-hit` |

特殊规则：
- 以 `#` 开头的文件名会被忽略，不参与加载
- YAML 文件中显式声明 `id` 字段时，以声明值为准，覆盖路径推导

## 资源版本标记

所有 YAML 配置文件应包含 `resource-version` 字段：

```yaml
resource-version: 1
```

此字段用于未来配置格式升级时的版本迁移。当前版本固定为 `1`。

## 路由模式

Monoceros 的四个核心系统（事件分发器、调度、数据包监听、命令）共享统一的路由模式，用于指定事件触发后的执行目标：

```yaml
execute:
  route: <路由类型>
  value: <目标 ID>
```

| 路由类型 | 说明 | value 含义 |
|----------|------|------------|
| `script` | 路由到 Fluxon 脚本 | 脚本定义 ID |
| `action-workflow` | 路由到动作工作流 | 工作流定义 ID |
| `handler` | 路由到代码注册的处理器 | 处理器 ID |

示例：

```yaml
# 路由到脚本
execute:
  route: script
  value: dispatcher.player-join

# 路由到动作工作流
execute:
  route: action-workflow
  value: combat.hit-feedback

# 路由到代码处理器
execute:
  route: handler
  value: my-custom-handler
```

注意：`handler` 类型需要在代码中通过对应服务的 API 注册处理器实例，纯配置无法使用。

## 时间单位格式

所有接受时间值的字段支持以下单位后缀：

| 后缀 | 含义 | 示例 |
|------|------|------|
| `t` | tick（1 tick = 50ms） | `20t` = 20 tick = 1 秒 |
| `ms` | 毫秒 | `500ms` = 500 毫秒 |
| `s` 或 `sec` | 秒 | `5s` = 5 秒 |
| `m` 或 `min` | 分钟 | `2m` = 2 分钟 |
| `h` | 小时 | `1h` = 1 小时 |

无单位时的默认行为：
- `parseTicks()` 上下文：默认为 tick
- `parseMs()` 上下文：默认为毫秒

示例：

```yaml
delay: 20t          # 20 tick
period: 5s          # 5 秒（转换为 100 tick）
max-duration-ms: 1h # 1 小时（转换为 3600000 毫秒）
```

## 热重载机制

Monoceros 的所有配置服务均支持热重载：

- 文件变更检测基于 SHA-256 哈希比对，只处理实际变更的文件
- 支持增量回调：新增（onCreated）、修改（onModified）、删除（onDeleted）
- 文件监听通过 NIO WatchService 实现，支持节流控制（默认 500ms）
- 脚本系统采用"最后一次成功版本保留"策略，编译失败不会覆盖已缓存的可用版本
- 可通过 `/monoceros reload` 命令手动触发全量重载

重载优先级（priority 越高越先重载）：

| 服务 | priority | serviceId |
|------|----------|-----------|
| 脚本 | 100 | `script` |
| 事件分发器 | 50 | `dispatcher` |
| 调度 | 50 | `schedule` |
| 数据包监听 | 40 | `wireshark` |
| 动作工作流 | 30 | `workflow-action` |

## 单文件与多定义模式

部分配置系统支持在单个 YAML 文件中定义多个条目：

### 单定义模式

文件顶层直接是定义字段：

```yaml
resource-version: 1
id: my.definition
# ... 其他字段
```

### 多定义模式

文件顶层每个 key 是一个独立定义：

```yaml
resource-version: 1

first-definition:
  id: first.def
  # ... 其他字段

second-definition:
  id: second.def
  # ... 其他字段
```

支持多定义模式的系统：脚本定义（`.yml`）、动作工作流。

## 语言文件格式

语言文件位于 `lang/` 目录，使用扁平 key-value 格式：

```yaml
resource-version: 1

script-load-success: "脚本加载完成: {0} 成功, {1} 失败"
dispatcher-load-success: "事件分发器加载完成: {0} 个"
```

- 占位符使用 `{0}`、`{1}`、`{2}` 等序号格式
- 通过 TabooLib I18n 系统发送：`player.sendLang("key", arg0, arg1)`
- 支持 `zh_CN.yml`（简体中文）和 `en_US.yml`（英文）

## 变量注入

多个配置系统支持 `variables` 字段，用于向执行上下文注入静态变量：

```yaml
variables:
  triggerSource: join
  customKey: customValue
  numericValue: 42
```

这些变量在脚本中通过 `&?variableName` 安全引用访问。

## API 门面

Monoceros 通过 `MonocerosAPI` 门面接口提供 9 个子服务访问器：

| 方法 | 返回类型 | 说明 |
|------|----------|------|
| `scripts()` | `MonocerosScriptHandler` | 脚本处理器 |
| `dispatchers()` | `DispatcherService` | 事件分发服务 |
| `schedules()` | `ScheduleService` | 调度服务 |
| `commands()` | `CommandService` | 命令服务 |
| `packets()` | `PacketService` | 数据包服务 |
| `volatility()` | `VolatilityService` | 挥发能力服务 |
| `actionWorkflow()` | `ActionWorkflowService` | 动作工作流服务 |
| `propertyWorkflow()` | `PropertyWorkflowService` | 属性工作流服务 |
| `sessions()` | `SessionService` | 会话服务 |

全局访问：`Monoceros.api()` 返回 `MonocerosAPI` 实例。`Monoceros.plugin()` 返回 Bukkit `Plugin` 实例。

### Fluxon 脚本中访问 API

`Monoceros` 的公开方法标注了 `@JvmStatic`，在 Fluxon 脚本中可直接通过 `static` 调用：

```fluxon
// 获取 API
api = static cc.bkhk.monoceros.Monoceros.api()

// 获取插件实例（用于 Bukkit Scheduler 等）
plugin = static cc.bkhk.monoceros.Monoceros.plugin()

// 获取会话
session = &api.sessions().getOrCreate(&p.getUniqueId())

// 获取脚本处理器
scripts = &api.scripts()
```

注意：不要写 `static cc.bkhk.monoceros.api.Monoceros`，`Monoceros` 类在 `cc.bkhk.monoceros` 包下，不在 `api` 子包下。
