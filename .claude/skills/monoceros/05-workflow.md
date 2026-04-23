# Monoceros 动作工作流参考

## 概述

动作工作流（Action Workflow）是 Monoceros 的节点式执行引擎，通过有序节点链实现复杂逻辑。配置文件位于 `plugins/Monoceros/workflow/action/` 目录下。

## 完整配置示例

```yaml
# 示例：战斗命中反馈动作工作流
resource-version: 1

id: combat.hit-feedback
failure-policy: continue
nodes:
  - id: log-hit
    type: log
    message: "战斗命中反馈触发"
  - id: set-damage
    type: set
    key: damage
    value: 10
  - id: check-crit
    type: branch
    condition: combat.crit-check
    then-workflow: combat.crit-effect
  - id: run-script
    type: script
    script: action.combat.hit-feedback
  - id: play-sound
    type: sound
    sound: ENTITY_PLAYER_HURT
    volume: 1.0
    pitch: 1.0
```

## 字段定义

`ActionWorkflowDefinition` 数据类：

| 字段 | YAML 键 | 类型 | 默认值 | 必填 | 说明 |
|------|---------|------|--------|------|------|
| `id` | `id` | String | -- | 是 | 工作流唯一 ID |
| `nodes` | `nodes` | List<ActionNodeDefinition> | -- | 是 | 节点列表（有序执行） |
| `failurePolicy` | `failure-policy` | ActionFailurePolicy | `STOP` | 否 | 失败策略 |

`ActionNodeDefinition` 数据类：

| 字段 | YAML 键 | 类型 | 说明 |
|------|---------|------|------|
| `id` | `id` | String | 节点唯一 ID（工作流内唯一） |
| `type` | `type` | String | 节点类型（必须是已注册的 ActionNode） |
| `config` | 其余键值对 | Map<String, Any?> | 节点配置参数 |

## 失败策略

| 策略 | 说明 |
|------|------|
| `stop` | 节点执行失败时立即停止整个工作流（默认） |
| `continue` | 节点失败后继续执行下一个节点 |
| `skip-node` | 跳过失败节点，继续执行 |

## 多定义模式

单个 YAML 文件中定义多个工作流：

```yaml
resource-version: 1

combat-hit:
  id: combat.hit-feedback
  failure-policy: continue
  nodes:
    - id: log
      type: log
      message: "命中"

combat-crit:
  id: combat.crit-effect
  failure-policy: stop
  nodes:
    - id: log
      type: log
      message: "暴击"
```

判断规则：顶层含 `nodes` 字段时为单定义，否则每个顶层 key 视为独立定义。

## 15 个内建节点类型

### script -- 调用 Fluxon 脚本

```yaml
- id: run-my-script
  type: script
  script: action.combat.hit-feedback    # 脚本定义 ID
```

### set -- 设置变量

```yaml
- id: set-damage
  type: set
  key: damage                           # 变量名
  value: 10                             # 变量值
```

### log -- 日志输出

```yaml
- id: log-info
  type: log
  message: "当前伤害: ${damage}"        # 日志消息
```

### wait -- 延迟执行

```yaml
- id: delay-1s
  type: wait
  ticks: 20                             # 延迟 tick 数（异步延续）
```

### branch -- 条件分支

```yaml
- id: check-condition
  type: branch
  condition: my.condition.script        # 条件脚本 ID（返回 true/false）
  then-workflow: my.then.workflow        # 条件为 true 时执行的工作流 ID（可选）
  else-workflow: my.else.workflow        # 条件为 false 时执行的工作流 ID（可选）
```

### loop -- 循环遍历

```yaml
- id: loop-items
  type: loop
  source: itemList                      # 变量名（指向可迭代对象）
  item-key: currentItem                 # 当前元素变量名（默认 loopItem）
  index-key: currentIndex               # 当前索引变量名（默认 loopIndex）
  workflow: process.single.item         # 每次迭代执行的工作流 ID
  # 或使用 script 替代 workflow:
  # script: process.single.item.script
```

### sound -- 播放音效

```yaml
- id: play-hit-sound
  type: sound
  sound: ENTITY_PLAYER_HURT             # Bukkit Sound 枚举名
  volume: 1.0                           # 音量（0.0~1.0）
  pitch: 1.0                            # 音调（0.5~2.0）
```

### tellraw -- 发送消息

```yaml
- id: send-message
  type: tellraw
  message: "&a命中! 伤害: ${damage}"    # 消息内容（支持颜色代码）
```

### regex -- 正则匹配

```yaml
- id: match-pattern
  type: regex
  pattern: "^(\\d+)-(\\w+)$"           # 正则表达式
  input: myVariable                     # 输入变量名（可选，默认 lastResult）
```

匹配结果注入变量：`regexMatch`（完整匹配）、`regexGroups`（捕获组列表）。

### try-catch -- 异常捕获

```yaml
- id: safe-execute
  type: try-catch
  try: risky.script                     # try 块脚本 ID
  catch: error.handler.script           # catch 块脚本 ID（可选）
  finally: cleanup.script               # finally 块脚本 ID（可选）
```

异常信息注入变量：`exception`（异常消息）、`exceptionType`（异常类型名）。

### input -- 等待玩家输入

```yaml
- id: wait-input
  type: input
  timeout: 200                          # 超时 tick 数
```

### if-else -- 条件执行

```yaml
- id: check-health
  type: if-else
  condition: health.check.script        # 条件脚本 ID（也可用 if 键）
  then: health.low.script               # 条件为 true 时执行的脚本 ID（可选）
  else: health.ok.script                # 条件为 false 时执行的脚本 ID（可选）
```

### math -- 数学运算

```yaml
- id: calc-damage
  type: math
  op: pow                               # 运算类型
  a: baseDamage                         # 操作数 A（变量名或字面值）
  b: 2                                  # 操作数 B（部分运算需要）
```

支持的运算：

| op | 说明 | 参数 |
|----|------|------|
| `abs` | 绝对值 | a |
| `ceil` | 向上取整 | a |
| `floor` | 向下取整 | a |
| `round` | 四舍五入 | a |
| `sqrt` | 平方根 | a |
| `pow` | 幂运算 | a, b |
| `min` | 最小值 | a, b |
| `max` | 最大值 | a, b |
| `random` | 随机数 | a(min), b(max) |
| `sin` | 正弦 | a |
| `cos` | 余弦 | a |
| `tan` | 正切 | a |
| `log` | 自然对数 | a |
| `log10` | 常用对数 | a |

### coerce -- 数值约束

```yaml
- id: clamp-damage
  type: coerce
  op: in                                # 约束类型
  value: damage                         # 变量名
  min: 0                                # 最小值
  max: 100                              # 最大值
```

支持的约束：

| op | 说明 | 参数 |
|----|------|------|
| `in` | 范围约束 | value, min, max |
| `at-least` | 最小值约束 | value, min |
| `at-most` | 最大值约束 | value, max |
| `format` | 格式化小数 | value, decimals |

### dispatch -- 分发到其他目标

```yaml
- id: chain-workflow
  type: dispatch
  workflow: another.workflow.id          # 分发到工作流
  # 或:
  # script: another.script.id           # 分发到脚本
  # dispatcher: another.dispatcher.id   # 分发到分发器
```

## 上下文变量注入

工作流执行过程中自动注入以下变量：

| 变量名 | 说明 |
|--------|------|
| `workflowId` | 当前工作流 ID |
| `nodeId` | 当前节点 ID |
| `lastResult` | 上一个节点的返回值 |
| `branchResult` | branch 节点的判定结果（Boolean） |
| `regexMatch` | regex 节点的完整匹配结果 |
| `regexGroups` | regex 节点的捕获组列表 |
| `exception` | try-catch 节点捕获的异常消息 |
| `exceptionType` | try-catch 节点捕获的异常类型名 |
| `loopItem` | loop 节点的当前元素 |
| `loopIndex` | loop 节点的当前索引 |

## 执行结果类型

`ActionResult` sealed class：

| 类型 | 说明 |
|------|------|
| `Continue(value)` | 继续执行，携带返回值 |
| `Delay(ticks)` | 延迟指定 tick 后继续 |
| `Branch(accepted, thenWorkflow?, elseWorkflow?)` | 条件分支 |
| `Break` | 中断工作流 |

## 服务 API

`ActionWorkflowService` 接口方法：

| 方法 | 说明 |
|------|------|
| `registerNode(node)` | 注册自定义动作节点 |
| `unregisterNode(type)` | 注销动作节点 |
| `execute(id, sender?, variables?)` | 执行工作流 |
| `reloadAll()` | 重载所有工作流定义 |
