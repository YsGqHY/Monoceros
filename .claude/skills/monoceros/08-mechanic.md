# Monoceros 机制系统参考

## 概述

机制系统（Mechanic）提供面向 Minecraft 服务器玩法的高级能力，包含五个子域：战斗、区域、交互、会话、视觉。

---

## 1. 战斗机制（Combat）

### 冷却管理器（CooldownManager）

管理玩家技能/动作冷却：

| 方法 | 参数 | 说明 |
|------|------|------|
| `setCooldown` | playerId, key, durationMs | 设置冷却 |
| `getCooldownRemaining` | playerId, key | 获取剩余冷却时间（毫秒） |
| `hasCooldown` | playerId, key | 是否在冷却中 |
| `clearCooldown` | playerId, key | 清除指定冷却 |
| `clearAll` | playerId | 清除玩家所有冷却 |

### 连击追踪器（ComboTracker）

追踪玩家连击计数：

| 方法 | 参数 | 说明 |
|------|------|------|
| `recordHit` | playerId, targetId | 记录一次命中，返回当前连击数 |
| `getComboCount` | playerId | 获取当前连击数 |
| `resetCombo` | playerId | 重置连击 |
| `setComboTimeout` | timeoutMs | 设置连击超时时间 |

### 状态管理器（StatusManager）

管理玩家状态效果（Buff/Debuff）：

| 方法 | 参数 | 说明 |
|------|------|------|
| `apply` | playerId, effect | 施加状态效果 |
| `remove` | playerId, effectId | 移除状态效果 |
| `get` | playerId, effectId | 获取状态效果 |
| `getAll` | playerId | 获取所有状态效果 |
| `clearAll` | playerId | 清除所有状态 |
| `clearExpired` | playerId | 清除已过期状态，返回清除数 |

`StatusEffect` 数据类：

| 字段 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `id` | String | -- | 效果唯一 ID |
| `name` | String | -- | 效果显示名 |
| `stacks` | Int | `1` | 当前层数 |
| `maxStacks` | Int | `1` | 最大层数 |
| `durationMs` | Long | `-1` | 持续时间（毫秒），-1 永久 |
| `appliedAt` | Long | 当前时间 | 施加时间戳 |
| `metadata` | Map<String, Any?> | 空映射 | 附加元数据 |

方法：
- `isExpired()` -- 是否已过期
- `remainingMs()` -- 剩余时间（毫秒），-1 表示永久

### 技能执行器（SkillExecutor）

管理技能执行生命周期：

| 方法 | 参数 | 说明 |
|------|------|------|
| `execute` | playerId, skill, variables? | 执行技能 |
| `cancel` | playerId, skillId | 取消技能 |
| `isExecuting` | playerId, skillId | 是否正在执行 |
| `getCurrentPhase` | playerId, skillId | 获取当前阶段 |

`SkillDefinition` 数据类：

| 字段 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `id` | String | -- | 技能唯一 ID |
| `conditionScript` | String? | `null` | 前置条件脚本 ID |
| `windupTicks` | Long | `0` | 蓄力时间（tick） |
| `executeScript` | String | -- | 执行脚本 ID |
| `recoveryTicks` | Long | `0` | 恢复时间（tick） |
| `cooldownMs` | Long | `0` | 冷却时间（毫秒） |

技能阶段枚举 `SkillPhase`：

| 阶段 | 说明 |
|------|------|
| `CONDITION` | 条件检查 |
| `WINDUP` | 蓄力 |
| `EXECUTE` | 执行 |
| `RECOVERY` | 恢复 |
| `COOLDOWN` | 冷却 |

---

## 2. 区域机制（Region）

### 区域服务（RegionService）

| 方法 | 参数 | 说明 |
|------|------|------|
| `register` | definition | 注册区域 |
| `unregister` | id | 注销区域 |
| `get` | id | 获取区域定义 |
| `all` | -- | 获取所有区域 |
| `getRegionsAt` | location | 获取指定位置的所有区域 |
| `isInRegion` | playerId, regionId | 玩家是否在区域内 |
| `getPlayersInRegion` | regionId | 获取区域内所有玩家 |

`RegionDefinition` 数据类：

| 字段 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `id` | String | -- | 区域唯一 ID |
| `shape` | RegionShape | -- | 区域形状 |
| `onEnterScript` | String? | `null` | 进入区域脚本 ID |
| `onLeaveScript` | String? | `null` | 离开区域脚本 ID |
| `onStayScript` | String? | `null` | 驻留区域脚本 ID |
| `stayIntervalTicks` | Long | `20` | 驻留脚本执行间隔（tick） |
| `effects` | List<RegionEffect> | 空列表 | 区域效果列表 |
| `variables` | Map<String, Any?> | 空映射 | 注入变量 |

### 区域形状

```kotlin
sealed class RegionShape {
    // 长方体
    data class Cuboid(
        val world: String,
        val minX: Double, val minY: Double, val minZ: Double,
        val maxX: Double, val maxY: Double, val maxZ: Double
    )
    // 球形
    data class Sphere(
        val world: String,
        val centerX: Double, val centerY: Double, val centerZ: Double,
        val radius: Double
    )
}
```

### 区域效果

`RegionEffect` 数据类：

| 字段 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `type` | String | -- | 药水效果类型名 |
| `amplifier` | Int | `0` | 效果等级（0 = I级） |
| `ambient` | Boolean | `true` | 是否为环境效果（粒子更少） |

---

## 3. 交互机制（Interact）

### 交互服务（InteractService）

| 方法 | 参数 | 说明 |
|------|------|------|
| `register` | definition | 注册交互定义 |
| `unregister` | id | 注销交互 |
| `get` | id | 获取交互定义 |
| `all` | -- | 获取所有交互 |
| `getLookAtTarget` | player, maxDistance? | 获取玩家视线目标 |

`InteractDefinition` 数据类：

| 字段 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `id` | String | -- | 交互唯一 ID |
| `type` | InteractType | -- | 交互类型 |
| `materialFilter` | String? | `null` | 材质过滤（仅匹配指定材质） |
| `script` | String | -- | 执行脚本 ID |
| `cooldownMs` | Long | `0` | 冷却时间（毫秒） |
| `variables` | Map<String, Any?> | 空映射 | 注入变量 |

交互类型枚举 `InteractType`：

| 类型 | 说明 |
|------|------|
| `RIGHT_CLICK` | 右键点击 |
| `LEFT_CLICK` | 左键点击 |
| `SNEAK_RIGHT_CLICK` | 潜行+右键 |
| `SNEAK_LEFT_CLICK` | 潜行+左键 |
| `SNEAK_TOGGLE` | 潜行切换 |

`LookAtResult` 数据类：

| 字段 | 类型 | 说明 |
|------|------|------|
| `target` | Entity? | 视线目标实体（无目标时为 null） |
| `distance` | Double | 距离 |

---

## 4. 会话机制（Session）

### 会话服务（SessionService）

通过 `Monoceros.api().sessions()` 获取。在 Fluxon 脚本中：

```fluxon
api = static cc.bkhk.monoceros.Monoceros.api()
session = &api.sessions().getOrCreate(&p.getUniqueId())
```

| 方法 | 参数 | 说明 |
|------|------|------|
| `getOrCreate` | playerId | 获取或创建玩家会话 |
| `get` | playerId | 获取玩家会话（不存在返回 null） |
| `destroy` | playerId | 销毁玩家会话 |
| `snapshot` | playerId | 获取会话快照 |
| `restore` | playerId, snapshot | 恢复会话快照 |
| `getPlayersInMechanic` | mechanicId | 获取参与指定机制的玩家 |
| `activeCount` | -- | 活跃会话数 |

### 玩家会话（PlayerSession）

| 方法/属性 | 类型 | 说明 |
|-----------|------|------|
| `playerId` | UUID | 玩家 UUID |
| `createdAt` | Long | 创建时间戳 |
| `get(key)` | Any? | 获取会话变量 |
| `set(key, value)` | -- | 设置会话变量 |
| `remove(key)` | Any? | 移除会话变量 |
| `has(key)` | Boolean | 是否存在变量 |
| `snapshot()` | Map<String, Any?> | 导出快照 |
| `restore(snapshot)` | -- | 恢复快照 |
| `clear()` | -- | 清空所有变量 |
| `getActiveMechanics()` | Set<String> | 获取参与的机制 ID 集合 |
| `joinMechanic(mechanicId)` | -- | 加入机制 |
| `leaveMechanic(mechanicId)` | -- | 离开机制 |

---

## 5. 视觉机制（Visual）

### 视觉服务（VisualService）

#### BossBar 管理

| 方法 | 参数 | 说明 |
|------|------|------|
| `createBossBar` | id, title, color?, style? | 创建 BossBar |
| `getBossBar` | id | 获取 BossBar |
| `removeBossBar` | id | 移除 BossBar |
| `showBossBar` | id, playerId | 向玩家显示 |
| `hideBossBar` | id, playerId | 对玩家隐藏 |
| `updateBossBar` | id, title?, progress?, color? | 更新 BossBar |

`ManagedBossBar` 数据类：

| 字段 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `id` | String | -- | BossBar ID |
| `title` | String | -- | 标题文本 |
| `progress` | Double | `1.0` | 进度（0.0~1.0） |
| `color` | BarColor | `WHITE` | 颜色 |
| `style` | BarStyle | `SOLID` | 样式 |

BarColor 枚举值：`PINK`、`BLUE`、`RED`、`GREEN`、`YELLOW`、`PURPLE`、`WHITE`
BarStyle 枚举值：`SOLID`、`SEGMENTED_6`、`SEGMENTED_10`、`SEGMENTED_12`、`SEGMENTED_20`

#### 消息发送

| 方法 | 参数 | 说明 |
|------|------|------|
| `sendActionBar` | playerId, message, durationTicks? | 发送 ActionBar 消息 |
| `sendTitle` | playerId, title, subtitle?, fadeIn?, stay?, fadeOut? | 发送 Title |
| `queueMessage` | playerId, message | 排队消息 |

`sendTitle` 默认参数：`fadeIn=10`、`stay=40`、`fadeOut=10`（单位 tick）

`QueuedMessage` 数据类：

| 字段 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `content` | String | -- | 消息内容 |
| `priority` | Int | `0` | 优先级（越大越先显示） |
| `durationTicks` | Long | `40` | 显示时长（tick） |
| `type` | MessageType | `ACTION_BAR` | 消息类型 |

消息类型枚举 `MessageType`：

| 类型 | 说明 |
|------|------|
| `ACTION_BAR` | ActionBar 消息 |
| `TITLE` | 主标题 |
| `SUBTITLE` | 副标题 |
