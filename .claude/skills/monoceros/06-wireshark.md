# Monoceros 数据包监听参考

## 概述

数据包监听系统（Wireshark）提供 Minecraft 网络数据包的抓取、过滤、匹配、追踪、拦截和覆写能力。配置文件位于 `plugins/Monoceros/wireshark/` 目录下。

需要 `module-wireshark` 模块（已安装 BukkitNMS + BukkitNMSUtil）。

## 完整配置示例

```yaml
id: example.packet.trace

direction:
  - send
  - receive

matcher:
  type: packet-name
  value: PacketPlayOutChat

filters:
  - type: player-permission
    value: monoceros.debug
  - type: world
    value: world

tracking: true
parse: true
intercept: false

rewrite:
  type: field-set
  field: b
  value: monoceros

route:
  type: script
  value: debug.packet.trace
```

## 字段定义

`PacketTapDefinition` 数据类：

| 字段 | YAML 键 | 类型 | 默认值 | 必填 | 说明 |
|------|---------|------|--------|------|------|
| `id` | `id` | String | -- | 是 | 监听点唯一 ID |
| `direction` | `direction` | Set<PacketDirection> | -- | 是 | 监听方向 |
| `matcher` | `matcher` | PacketMatcherSpec? | `null` | 否 | 数据包匹配器 |
| `filters` | `filters` | List<PacketFilterSpec> | 空列表 | 否 | 过滤器列表 |
| `tracking` | `tracking` | Boolean | `false` | 否 | 是否追踪记录 |
| `parse` | `parse` | Boolean | `false` | 否 | 是否解析字段 |
| `intercept` | `intercept` | Boolean | `false` | 否 | 是否拦截（需主配置开启） |
| `rewrite` | `rewrite` | PacketRewriteSpec? | `null` | 否 | 覆写规格（需主配置开启） |
| `route` | `route` | PacketRoute? | `null` | 否 | 执行路由 |

## 数据包方向

```yaml
direction:
  - send       # 服务端发送给客户端的数据包
  - receive    # 客户端发送给服务端的数据包
```

| 值 | 说明 |
|----|------|
| `send` | 出站数据包（PacketSendEvent） |
| `receive` | 入站数据包（PacketReceiveEvent） |

可同时监听两个方向。

## 匹配器

`matcher` 定义数据包匹配条件：

```yaml
matcher:
  type: packet-name                     # 匹配器类型
  value: PacketPlayOutChat              # 匹配值
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `type` | String | 匹配器类型标识 |
| `value` | String | 匹配值 |

## 过滤器

`filters` 定义额外过滤条件，所有过滤器必须全部通过才会触发路由：

```yaml
filters:
  - type: player-permission
    value: monoceros.debug
  - type: world
    value: world
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `type` | String | 过滤器类型标识 |
| `value` | String | 过滤值 |

## 追踪与解析

| 字段 | 说明 |
|------|------|
| `tracking: true` | 启用追踪，记录匹配到的数据包到 `PacketTraceRecord` |
| `parse: true` | 启用字段解析，将数据包字段注入到上下文变量 |

`PacketTraceRecord` 数据类：

| 字段 | 类型 | 说明 |
|------|------|------|
| `tapId` | String | 监听点 ID |
| `direction` | PacketDirection | 方向 |
| `packetName` | String | 数据包名称 |
| `timestamp` | Long | 时间戳 |
| `variables` | Map<String, Any?> | 解析出的变量 |

## 拦截与覆写

拦截和覆写是高风险操作，需要在主配置 `config.yml` 中显式开启：

```yaml
# config.yml
wireshark:
  allow-intercept: true    # 允许拦截
  allow-rewrite: true      # 允许覆写
```

### 拦截

```yaml
intercept: true            # 取消原始数据包，阻止其继续传递
```

### 覆写

```yaml
rewrite:
  type: field-set          # 覆写类型
  field: b                 # 目标字段名
  value: monoceros         # 新值
```

`PacketRewriteSpec` 数据类：

| 字段 | 类型 | 说明 |
|------|------|------|
| `type` | String | 覆写类型标识 |
| `config` | Map<String, Any?> | 覆写配置（YAML 中直接展开为同级字段） |

## 路由

数据包监听的路由使用 `route` 字段（注意不是 `execute`）：

```yaml
# 路由到脚本
route:
  type: script
  value: debug.packet.trace

# 路由到动作工作流
route:
  type: action-workflow
  value: packet.process.workflow

# 路由到代码处理器
route:
  type: handler
  value: my-packet-handler
```

## 数据包上下文

`PacketContext` 在路由执行时传递：

| 字段 | 类型 | 说明 |
|------|------|------|
| `tapId` | String | 监听点 ID |
| `direction` | PacketDirection | 方向 |
| `player` | Player | 关联玩家 |
| `packet` | Any | 原始数据包对象 |
| `timestamp` | Long | 时间戳 |
| `variables` | MutableMap<String, Any?> | 上下文变量 |
| `cancelled` | Boolean | 是否取消 |
| `rewrittenPacket` | Any? | 覆写后的数据包 |

脚本中可通过 `&?packetName`、`&?packetClass`、`&?direction` 等访问。

## 会话管理

`PacketService` 支持按玩家管理数据包监听会话：

| 方法 | 说明 |
|------|------|
| `register(definition)` | 注册监听点定义 |
| `unregister(id)` | 注销监听点 |
| `openSession(playerId)` | 打开玩家会话 |
| `closeSession(playerId)` | 关闭玩家会话 |
| `getSession(playerId)` | 获取玩家会话 |

`PacketSession` 接口：

| 方法/属性 | 说明 |
|-----------|------|
| `playerId` | 玩家 UUID |
| `enableTap(tapId)` | 启用指定监听点 |
| `disableTap(tapId)` | 禁用指定监听点 |
| `trace()` | 获取追踪记录列表 |

玩家退出时自动清理会话。
