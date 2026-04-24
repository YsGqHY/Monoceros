# Monoceros 挥发能力参考

## 概述

挥发能力（Volatility）提供客户端侧的视觉欺骗能力，包括伪方块、伪世界边界、实体元数据修改和幻象会话管理。所有操作仅影响指定玩家的客户端显示，不修改服务端实际状态。

需要 `module-volatility` 模块（已安装 BukkitNMS + BukkitNMSUtil）。

## 服务入口

通过 `Monoceros.api().volatility()` 获取 `VolatilityService`（Kotlin），在 Fluxon 脚本中使用 `static cc.bkhk.monoceros.Monoceros.api().volatility()`。包含四个子服务：

| 方法 | 返回类型 | 说明 |
|------|----------|------|
| `blocks()` | `VolatileBlockService` | 伪方块服务 |
| `worldBorder()` | `VolatileWorldBorderService` | 伪世界边界服务 |
| `metadata()` | `VolatileEntityMetadataService` | 实体元数据服务 |
| `illusions()` | `IllusionSessionService` | 幻象会话服务 |

---

## 1. 伪方块服务（VolatileBlockService）

向指定玩家发送虚假的方块变更，仅客户端可见：

| 方法 | 参数 | 说明 |
|------|------|------|
| `sendBlockChange` | viewer, location, data | 发送单个方块变更 |
| `sendBlockChanges` | viewer, changes | 批量发送方块变更 |

参数类型：
- `viewer`: `Player` -- 目标玩家
- `location`: `Location` -- 方块位置
- `data`: `BlockData` -- 方块数据
- `changes`: `List<Pair<Location, BlockData>>` -- 批量变更列表

---

## 2. 伪世界边界服务（VolatileWorldBorderService）

向指定玩家发送虚假的世界边界：

| 方法 | 参数 | 说明 |
|------|------|------|
| `sendWorldBorder` | viewer, state | 发送静态世界边界 |
| `sendDynamicWorldBorder` | viewer, state | 发送动态（渐变）世界边界 |

### 静态世界边界

`WorldBorderState` 数据类：

| 字段 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `world` | World | -- | 世界对象 |
| `size` | Double? | `null` | 边界大小（直径） |
| `center` | Location? | `null` | 中心位置 |
| `warningTime` | Int? | `null` | 警告时间（秒） |
| `warningDistance` | Int? | `null` | 警告距离（方块） |
| `damageBuffer` | Double? | `null` | 伤害缓冲区大小 |
| `damageAmount` | Double? | `null` | 每秒伤害量 |

### 动态世界边界

`DynamicWorldBorderState` 数据类：

| 字段 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `world` | World | -- | 世界对象 |
| `oldSize` | Double | -- | 起始大小 |
| `newSize` | Double | -- | 目标大小 |
| `speedMs` | Long | -- | 过渡时间（毫秒） |
| `center` | Location? | `null` | 中心位置 |
| `warningTime` | Int? | `null` | 警告时间（秒） |
| `warningDistance` | Int? | `null` | 警告距离（方块） |
| `damageBuffer` | Double? | `null` | 伤害缓冲区大小 |
| `damageAmount` | Double? | `null` | 每秒伤害量 |

---

## 3. 实体元数据服务（VolatileEntityMetadataService）

修改指定玩家看到的实体元数据：

| 方法 | 参数 | 说明 |
|------|------|------|
| `setFlag` | viewer, entity, flag, value | 设置实体标志 |
| `setPose` | viewer, entity, pose | 设置实体姿态 |
| `updateHealth` | viewer, entity, health | 更新实体血量显示 |
| `mount` | viewer, entity | 骑乘实体 |

### 实体标志

`EntityFlag` 枚举：

| 标志 | 说明 |
|------|------|
| `ON_FIRE` | 着火效果 |
| `SNEAKING` | 潜行姿态 |
| `SPRINTING` | 疾跑状态 |
| `INVISIBLE` | 隐身效果 |
| `GLOWING` | 发光效果 |

### 实体姿态

使用 Bukkit 的 `org.bukkit.entity.Pose` 枚举，常用值：
- `STANDING` -- 站立
- `FALL_FLYING` -- 鞘翅飞行
- `SLEEPING` -- 睡觉
- `SWIMMING` -- 游泳
- `SPIN_ATTACK` -- 旋转攻击（三叉戟）
- `SNEAKING` -- 潜行
- `DYING` -- 死亡

---

## 4. 幻象会话服务（IllusionSessionService）

管理按玩家、命名空间和目标 ID 组织的幻象效果：

| 方法 | 参数 | 说明 |
|------|------|------|
| `putBlock` | key, location, data | 放置幻象方块 |
| `removeBlock` | key, location | 移除幻象方块 |
| `applyWorldBorder` | key, state | 应用幻象世界边界 |
| `setEntityFlag` | key, entity, flag, value | 设置幻象实体标志 |
| `clear` | key | 清除指定 key 的所有幻象 |
| `clearViewer` | viewerId | 清除指定玩家的所有幻象 |

### 幻象键

`IllusionKey` 数据类：

| 字段 | 类型 | 说明 |
|------|------|------|
| `viewerId` | UUID | 观察者玩家 UUID |
| `namespace` | String | 命名空间（用于分组管理） |
| `targetId` | String | 目标标识（自定义） |

幻象键的三级结构允许精确控制幻象的生命周期：
- 按 `viewerId` 清除某玩家的所有幻象
- 按 `namespace` 分组管理同一系统的幻象
- 按 `targetId` 精确定位单个幻象目标
