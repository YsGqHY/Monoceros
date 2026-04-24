# Monoceros 脚本系统参考

## 概述

Monoceros 使用 Fluxon 作为唯一主脚本运行时。脚本文件位于 `plugins/Monoceros/script/` 目录下，支持两种格式：

- `.fs` 文件：纯 Fluxon 脚本，ID 由路径推导
- `.yml` 文件：带元数据的脚本定义，支持高级封装

## 脚本 ID 推导

脚本 ID 由文件相对于 `script/` 目录的路径推导：

| 文件路径 | 脚本 ID |
|----------|---------|
| `script/dispatcher/player-join.fs` | `dispatcher.player-join` |
| `script/schedule/world.tick.broadcast.fs` | `schedule.world.tick.broadcast` |
| `script/shared/audit-before.fs` | `shared.audit-before` |
| `script/debug/packet.trace.fs` | `debug.packet.trace` |

## .fs 文件格式

纯 Fluxon 脚本，直接编写脚本内容：

```fluxon
// 脚本 ID: dispatcher.player-join
// 被 dispatcher/player-join.yml 引用

print("玩家 ${&?player} 加入了服务器")
```

```fluxon
// 脚本 ID: schedule.world.tick.broadcast
// 被 schedule/broadcast.yml 引用

print("[Monoceros] 周期广播 #${&?runCount} (来源: ${&?triggerSource})")
```

```fluxon
// 脚本 ID: action.combat.hit-feedback
// 被 workflow/action/combat-hit.yml 引用

dmg = &?damage ?: 0
print("[战斗] 命中反馈: 伤害=${&dmg}")
```

```fluxon
// 脚本 ID: debug.packet.trace
// 被 wireshark/example.yml 引用

name = &?packetName ?: "unknown"
cls = &?packetClass ?: ""
print("[Wireshark] 追踪: ${&name} (${&cls})")
```

## .yml 脚本定义格式

### 单定义模式

```yaml
id: my.script.id                       # 可选，默认由路径推导
type: fluxon                           # 脚本类型，默认 fluxon
script: |                              # 脚本内容（必填）
  print("hello")
enabled: true                          # 是否启用，默认 true
preheat: false                         # 是否预热（ACTIVE 阶段预编译），默认 false
async: true                            # 是否允许异步执行，默认 true
tags:                                  # 标签集合
  - combat
  - debug
metadata:                              # 附加元数据
  author: test
  version: "1.0"
```

### 高级封装字段

```yaml
id: advanced.script.example
type: fluxon
script: |
  // 参数已通过 Applicative 转换后注入上下文
  print("伤害: ${&?damage}, 目标: ${&?target}")

# 参数声明：参数名 -> Applicative 类型名
# 调用时传入的变量会通过 ApplicativeRegistry 自动转换
parameters:
  damage: double
  target: player

# 前置条件脚本 ID
# 执行前先运行此脚本，返回 false/null 时拒绝执行
condition: my.condition.script

# 条件不满足时执行的脚本 ID
deny: my.deny.script

# 自定义函数映射：函数名 -> 脚本 ID
# 在主脚本中可通过上下文调用这些函数
functions:
  onHit: my.onhit.script
  onDeath: my.ondeath.script

# 执行超时（毫秒），0 表示不限制
timeout: 5000

# 超时回调脚本 ID
on-timeout: my.timeout.handler

# 异常回调脚本 ID
on-exception: my.error.handler

# 返回值类型转换（Applicative 名称）
return-conversion: int
```

### 多定义模式

单个 YAML 文件中定义多个脚本：

```yaml
resource-version: 1

combat-check:
  script: |
    &?player != null
  tags: [combat]

combat-deny:
  script: |
    print("条件不满足")
  tags: [combat]
```

## 完整字段定义

`ScriptDefinition` 数据类：

| 字段 | YAML 键 | 类型 | 默认值 | 说明 |
|------|---------|------|--------|------|
| `id` | `id` | String | 路径推导 | 脚本唯一 ID |
| `source` | `script` + `type` | MonocerosScriptSource | -- | 脚本源码与类型 |
| `file` | -- | Path | -- | 源文件路径（自动填充） |
| `enabled` | `enabled` | Boolean | `true` | 是否启用 |
| `preheat` | `preheat` | Boolean | `false` | 是否预热 |
| `asyncAllowed` | `async` | Boolean | `true` | 是否允许异步 |
| `tags` | `tags` | Set<String> | 空集合 | 标签 |
| `metadata` | `metadata` | Map<String, Any?> | 空映射 | 附加元数据 |
| `parameters` | `parameters` | Map<String, String> | 空映射 | 参数名到类型名映射 |
| `condition` | `condition` | String? | `null` | 前置条件脚本 ID |
| `deny` | `deny` | String? | `null` | 条件拒绝脚本 ID |
| `functions` | `functions` | Map<String, String> | 空映射 | 函数名到脚本 ID 映射 |
| `timeoutMs` | `timeout` | Long | `0` | 超时毫秒数，0 不限制 |
| `onTimeout` | `on-timeout` | String? | `null` | 超时回调脚本 ID |
| `onException` | `on-exception` | String? | `null` | 异常回调脚本 ID |
| `returnConversion` | `return-conversion` | String? | `null` | 返回值类型转换 |

`MonocerosScriptSource` 数据类：

| 字段 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `type` | String | `"fluxon"` | 脚本类型标识 |
| `content` | String | -- | 脚本源码内容 |
| `origin` | String? | `null` | 来源标识 |

## 系统保留变量

以下变量在脚本执行时自动注入上下文：

| 变量名 | 类型 | 说明 |
|--------|------|------|
| `sender` | ProxyCommandSender? | 命令发送者（TabooLib 抽象） |
| `player` | Player? | Bukkit 玩家对象（如果 sender 是玩家） |
| `source` | String? | 触发来源标识 |
| `scriptId` | String | 当前脚本 ID |
| `now` | Long | 当前时间戳（毫秒） |
| `thread` | String | 当前线程名 |

在脚本中使用安全引用访问：`&?player`、`&?sender`、`&?scriptId`。

## 自动导入包

Fluxon 脚本自动导入以下 Java/Kotlin 包：

- `cc.bkhk.monoceros.*`
- `cc.bkhk.monoceros.api.*`
- `org.bukkit.*`
- `org.bukkit.entity.*`
- `org.bukkit.inventory.*`

无需手动 import 即可直接使用这些包中的类。

注意：`Monoceros` 全局入口类在 `cc.bkhk.monoceros` 包下（不在 `api` 子包下），自动导入 `cc.bkhk.monoceros.*` 已覆盖。`Monoceros` 的公开方法标注了 `@JvmStatic`，Fluxon 脚本中可直接 `static` 调用：

```fluxon
api = static cc.bkhk.monoceros.Monoceros.api()
plugin = static cc.bkhk.monoceros.Monoceros.plugin()
```

## Monoceros 扩展函数

Monoceros 在 ENABLE 阶段向 Fluxon 运行时注册了以下扩展函数，脚本中直接调用，无需 import：

| 分类 | 函数 | 说明 |
|------|------|------|
| JSON | `jsonParse(str)`, `jsonStringify(obj)`, `jsonPretty(obj)`, `jsonObject()`, `jsonArray()` | JSON 解析/序列化/构建 |
| HTTP | `httpGet(url)`, `httpPost(url, body)`, `httpRequest(url, method, headers, body)` | 异步 HTTP 请求，返回 CompletableFuture，用 `await` 消费 |
| UUID | `uuid()`, `uuidFromString(str)`, `uuidFromName(name)` | UUID 生成与解析 |
| 冷却 | `cooldown(key, ms)`, `hasCooldown(key)`, `getCooldown(key)`, `removeCooldown(key)` | 冷却/频率限制 |
| 颜色 | `colored(text)`, `uncolored(text)` | 颜色代码转换（`&a` → `§a`），String 扩展 `:: colored()` |
| 日志 | `logInfo(msg)`, `logWarn(msg)`, `logDebug(msg)`, `logError(msg)` | 结构化日志输出 |
| 正则 | `regex(pattern)`, `regexMatch(text, pattern)`, `regexMatchAll(text, pattern)`, `regexReplace(text, pattern, replacement)` | 正则表达式（含捕获组） |

完整 API 参考见 `16-monoceros-functions.md`。

## 脚本处理器 API

`MonocerosScriptHandler` 接口方法：

| 方法 | 说明 |
|------|------|
| `invoke(request)` | 按 ScriptInvokeRequest 执行脚本 |
| `invoke(definitionId, sender?, variables?)` | 按定义 ID 执行脚本 |
| `preheat(definitionId)` | 预热/预编译指定脚本 |
| `invalidate(scriptId)` | 按脚本 ID 失效缓存 |
| `invalidateByPrefix(prefix)` | 按前缀批量失效缓存 |
| `cacheStats()` | 导出缓存统计 |
| `registerScriptType(type)` | 注册新脚本类型 |
| `unregisterScriptType(typeId)` | 注销脚本类型 |

`ScriptInvokeRequest` 数据类：

| 字段 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `definitionId` | String? | `null` | 脚本定义 ID |
| `source` | MonocerosScriptSource? | `null` | 内联脚本源码 |
| `sender` | ProxyCommandSender? | `null` | 命令发送者 |
| `variables` | Map<String, Any?> | 空映射 | 注入变量 |
| `asyncAllowed` | Boolean | `true` | 是否允许异步 |

`definitionId` 和 `source` 至少提供一个。

## 缓存统计

`ScriptCacheStats` 数据类：

| 字段 | 类型 | 说明 |
|------|------|------|
| `cacheSize` | Int | 当前缓存条目数 |
| `invokeHits` | Long | 缓存命中次数 |
| `invokeMisses` | Long | 缓存未命中次数 |
| `totalCompilations` | Long | 总编译次数 |
| `totalCompilationNanos` | Long | 总编译耗时（纳秒） |

## 脚本加载结果

`ScriptLoadResult` 数据类：

| 字段 | 类型 | 说明 |
|------|------|------|
| `loaded` | Int | 成功加载数 |
| `failed` | Int | 加载失败数 |
| `removed` | Int | 移除数 |
| `costMs` | Long | 耗时（毫秒） |

## 脚本任务跟踪

`ScriptTaskTracker` 接口方法：

| 方法 | 说明 |
|------|------|
| `track(task)` | 跟踪运行中的脚本任务 |
| `get(taskId)` | 按任务 ID 获取 |
| `getByDefinition(definitionId)` | 按脚本定义 ID 获取所有任务 |
| `stopByDefinition(definitionId)` | 停止指定定义的所有任务 |
| `stop(taskId)` | 停止指定任务 |
| `activeCount()` | 活跃任务数 |
| `purgeCompleted()` | 清理已完成任务 |

## 脚本流（ScriptFlow）

有序脚本执行链，支持前处理、后处理和失败回调：

| 方法 | 说明 |
|------|------|
| `add(scriptId)` | 添加脚本到执行链 |
| `preprocess(scriptId)` | 设置前处理脚本 |
| `postprocess(scriptId)` | 设置后处理脚本 |
| `onFailure(scriptId)` | 设置失败回调脚本 |
| `terminate()` | 终止执行链 |
| `execute(sender?, variables?)` | 执行整个链 |
| `variables()` | 获取/设置链级变量 |
