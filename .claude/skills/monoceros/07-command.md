# Monoceros 命令系统参考

## 概述

命令系统（Command）支持通过配置定义命令树，包括子命令、参数、补全和权限控制。命令定义通过代码或配置注册。

## 完整配置示例

```yaml
resource-version: 1

id: my-custom-command
aliases:
  - mycmd
  - mc
permission: monoceros.command.mycmd
permission-message: "&c你没有权限执行此命令"
root:
  name: mycmd
  children:
    - name: reload
      type: literal
      route:
        type: script
        value: command.mycmd.reload
    - name: run
      type: literal
      children:
        - name: scriptId
          type: argument
          argument:
            type: script_id
            required: true
          route:
            type: script
            value: command.mycmd.run
    - name: give
      type: literal
      children:
        - name: player
          type: argument
          argument:
            type: player
            required: true
          children:
            - name: amount
              type: argument
              argument:
                type: int
                required: false
                restrict:
                  mode: range
                  value: "1-64"
              route:
                type: script
                value: command.mycmd.give
```

## 字段定义

`CommandDefinition` 数据类：

| 字段 | YAML 键 | 类型 | 默认值 | 必填 | 说明 |
|------|---------|------|--------|------|------|
| `id` | `id` | String | -- | 是 | 命令定义唯一 ID |
| `aliases` | `aliases` | List<String> | -- | 是 | 命令别名列表（第一个为主命令名） |
| `permission` | `permission` | String? | `null` | 否 | 权限节点 |
| `permissionMessage` | `permission-message` | String? | `null` | 否 | 无权限提示消息 |
| `root` | `root` | CommandNode | -- | 是 | 命令根节点 |

## 命令节点

命令树由两种节点类型组成：

### LiteralNode -- 字面量节点

```yaml
- name: reload                          # 子命令名称
  type: literal
  route:                                # 执行路由（可选）
    type: script
    value: command.reload
  children: []                          # 子节点列表（可选）
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `name` | String | 子命令名称 |
| `route` | CommandRoute? | 执行路由 |
| `children` | List<CommandNode> | 子节点 |

### ArgumentNode -- 参数节点

```yaml
- name: player                          # 参数名称
  type: argument
  argument:
    type: player                        # 参数类型
    required: true                      # 是否必填
    suggest: my-suggestion-provider     # 补全提供器 ID（可选）
    restrict:                           # 限制规则（可选）
      mode: range
      value: "1-100"
  route:
    type: script
    value: command.process
  children: []
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `name` | String | 参数名称 |
| `argument` | ArgumentSpec | 参数规格 |
| `route` | CommandRoute? | 执行路由 |
| `children` | List<CommandNode> | 子节点 |

## 参数类型

`ArgumentSpec.type` 支持的参数类型：

| 类型 | 说明 | 示例值 |
|------|------|--------|
| `string` | 字符串 | `hello` |
| `int` | 整数 | `42` |
| `double` | 双精度浮点 | `3.14` |
| `boolean` | 布尔值 | `true` / `false` |
| `player` | 在线玩家名 | `Steve`（自动补全） |
| `offline_player` | 离线玩家名 | `Steve` |
| `world` | 世界名 | `world_nether`（自动补全） |
| `material` | 材质名 | `DIAMOND_SWORD`（自动补全） |
| `script_id` | 脚本 ID | `dispatcher.player-join`（自动补全） |

## 参数规格

`ArgumentSpec` 数据类：

| 字段 | YAML 键 | 类型 | 默认值 | 说明 |
|------|---------|------|--------|------|
| `type` | `type` | ArgumentType | -- | 参数类型 |
| `required` | `required` | Boolean | `true` | 是否必填 |
| `suggest` | `suggest` | String? | `null` | 自定义补全提供器 ID |
| `restrict` | `restrict` | RestrictionSpec? | `null` | 限制规则 |

## 限制规则

`RestrictionSpec` 数据类：

```yaml
restrict:
  mode: range                           # 限制模式
  value: "1-64"                         # 限制值
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `mode` | String | 限制模式标识 |
| `value` | String | 限制值 |

## 命令路由

```yaml
# 路由到脚本
route:
  type: script
  value: command.mycmd.reload

# 路由到动作工作流
route:
  type: action-workflow
  value: command.process.workflow

# 路由到代码处理器
route:
  type: handler
  value: my-command-handler
```

## 命令上下文

`CommandContext` 在路由执行时传递：

| 字段 | 类型 | 说明 |
|------|------|------|
| `definitionId` | String | 命令定义 ID |
| `sender` | ProxyCommandSender | 命令发送者 |
| `rawArgs` | List<String> | 原始参数列表 |
| `parsedArgs` | Map<String, Any?> | 已解析的参数映射（参数名 -> 值） |
| `path` | List<String> | 命令路径（从根到当前节点的名称链） |

脚本中可通过 `&?commandId`、`&?rawArgs`、`&?parsedArgs` 等访问。

## 服务 API

`CommandService` 接口方法：

| 方法 | 说明 |
|------|------|
| `register(definition)` | 注册命令定义 |
| `unregister(id)` | 注销命令 |
| `reloadAll()` | 重载所有命令 |

`SuggestionProvider` 接口：

| 方法/属性 | 说明 |
|-----------|------|
| `id` | 提供器 ID |
| `suggest(sender, args)` | 返回补全建议列表 |
