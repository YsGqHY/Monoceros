package cc.bkhk.monoceros.extension.action.illusion

import cc.bkhk.monoceros.Monoceros
import cc.bkhk.monoceros.api.extension.NativeExtension
import cc.bkhk.monoceros.api.volatility.EntityFlag
import cc.bkhk.monoceros.api.volatility.IllusionKey
import cc.bkhk.monoceros.api.volatility.WorldBorderState
import cc.bkhk.monoceros.api.workflow.ActionContext
import cc.bkhk.monoceros.api.workflow.ActionNode
import cc.bkhk.monoceros.api.workflow.ActionNodeDefinition
import org.bukkit.entity.Entity
import org.bukkit.entity.Player

/** 对目标实体显示发光效果（通过 IllusionSessionService 记录，支持回滚） */
class IllusionGlowNode : ActionNode {
    override val type = "illusion.glow"
    override fun execute(context: ActionContext, definition: ActionNodeDefinition): Any? {
        val viewer = context.variables["player"] as? Player ?: return null
        val targets = resolveEntities(context)
        val value = definition.config["value"] as? Boolean ?: true
        val namespace = definition.config["namespace"] as? String ?: "glow"
        val illusionService = Monoceros.api().volatility().illusions()
        for (entity in targets) {
            val targetId = definition.config["target-id"] as? String ?: "entity-${entity.entityId}"
            val key = IllusionKey(viewer.uniqueId, namespace, targetId)
            illusionService.setEntityFlag(key, entity, EntityFlag.GLOWING, value)
        }
        return targets.size
    }
}

/** 发送视觉警告效果 */
class IllusionWarningNode : ActionNode {
    override val type = "illusion.warning"
    override fun execute(context: ActionContext, definition: ActionNodeDefinition): Any? {
        val player = context.variables["player"] as? Player ?: return null
        val title = (definition.config["title"] as? String)?.replace(Regex("&([0-9a-fk-orA-FK-OR])"), "\u00a7$1") ?: ""
        val subtitle = (definition.config["subtitle"] as? String)?.replace(Regex("&([0-9a-fk-orA-FK-OR])"), "\u00a7$1") ?: ""
        val actionbar = (definition.config["actionbar"] as? String)?.replace(Regex("&([0-9a-fk-orA-FK-OR])"), "\u00a7$1")
        if (title.isNotEmpty() || subtitle.isNotEmpty()) {
            player.sendTitle(title, subtitle, 10, 40, 10)
        }
        if (actionbar != null) {
            player.sendMessage(actionbar)
        }

        val borderSize = (definition.config["border-size"] as? Number)?.toDouble()
        val warningDistance = (definition.config["warning-distance"] as? Number)?.toInt() ?: 1
        val warningTime = (definition.config["warning-time"] as? Number)?.toInt() ?: 0
        if (borderSize != null) {
            val namespace = definition.config["namespace"] as? String ?: "warning"
            val targetId = definition.config["target-id"] as? String ?: "warning-border"
            val state = WorldBorderState(
                world = player.world,
                size = borderSize,
                center = player.location,
                warningTime = warningTime,
                warningDistance = warningDistance,
            )
            Monoceros.api().volatility().illusions().applyWorldBorder(IllusionKey(player.uniqueId, namespace, targetId), state)
        }
        return null
    }
}

/** 构造玩家专属世界边界幻象 */
class IllusionWorldBorderNode : ActionNode {
    override val type = "illusion.world-border"
    override fun execute(context: ActionContext, definition: ActionNodeDefinition): Any? {
        val player = context.variables["player"] as? Player ?: return null
        val namespace = definition.config["namespace"] as? String ?: "default"
        val targetId = definition.config["target-id"] as? String ?: "border"
        val size = (definition.config["size"] as? Number)?.toDouble()
        val warningDistance = (definition.config["warning-distance"] as? Number)?.toInt()
        val warningTime = (definition.config["warning-time"] as? Number)?.toInt()

        val state = WorldBorderState(
            world = player.world,
            size = size,
            center = player.location,
            warningTime = warningTime,
            warningDistance = warningDistance,
        )
        // 通过 IllusionSessionService 注册，便于后续按 key 撤销
        val key = IllusionKey(player.uniqueId, namespace, targetId)
        Monoceros.api().volatility().illusions().applyWorldBorder(key, state)
        return null
    }
}

/** 发送假方块 */
class IllusionFakeBlockNode : ActionNode {
    override val type = "illusion.fake-block"
    override fun execute(context: ActionContext, definition: ActionNodeDefinition): Any? {
        val player = context.variables["player"] as? Player ?: return null
        val location = context.variables["location"] as? org.bukkit.Location ?: return null
        val materialName = definition.config["material"] as? String ?: return null
        val namespace = definition.config["namespace"] as? String ?: "fake-block"
        val targetId = definition.config["target-id"] as? String ?: "${location.blockX},${location.blockY},${location.blockZ}"
        val material = org.bukkit.Material.matchMaterial(materialName) ?: return null
        val data = material.createBlockData()
        val key = IllusionKey(player.uniqueId, namespace, targetId)
        Monoceros.api().volatility().illusions().putBlock(key, location, data)
        return null
    }
}

/** 按 namespace 或 targetId 清理幻象 */
class IllusionClearNode : ActionNode {
    override val type = "illusion.clear"
    override fun execute(context: ActionContext, definition: ActionNodeDefinition): Any? {
        val player = context.variables["player"] as? Player ?: return null
        val namespace = definition.config["namespace"] as? String
        val targetId = definition.config["target-id"] as? String

        val illusionService = Monoceros.api().volatility().illusions()
        if (namespace != null && targetId != null) {
            val key = IllusionKey(player.uniqueId, namespace, targetId)
            illusionService.clear(key)
        } else {
            // 未指定具体 key 时，清除该玩家的所有幻象
            illusionService.clearViewer(player.uniqueId)
        }
        return null
    }
}

/** 从上下文解析实体列表 */
private fun resolveEntities(context: ActionContext): List<Entity> {
    @Suppress("UNCHECKED_CAST")
    (context.variables["targets"] as? List<*>)?.filterIsInstance<Entity>()?.let { return it }
    (context.variables["target"] as? Entity)?.let { return listOf(it) }
    (context.variables["player"] as? Player)?.let { return listOf(it) }
    return emptyList()
}

/** 警告效果接口 */
interface WarningEffect {
    val level: Double
    fun apply(player: Player)
    fun stop()
    fun isActive(): Boolean
}

/** 淡入式警告效果：从当前 level 线性渐变到目标 level */
class FadeInWarningEffect(
    private val player: Player,
    private val targetLevel: Double,
    private val durationTicks: Long,
    private val namespace: String = "warning",
) : WarningEffect {
    override var level: Double = 0.0; private set
    @Volatile private var active = true
    private var taskId: Long = -1

    override fun apply(player: Player) {
        val startLevel = level
        val steps = (durationTicks / 2).coerceAtLeast(1)
        val increment = (targetLevel - startLevel) / steps
        var step = 0L
        val task = taboolib.common.platform.function.submit(period = 2L) {
            if (!active || step >= steps) { stop(); cancel(); return@submit }
            level = startLevel + increment * step
            sendWarningBorder(player, level, namespace)
            step++
        }
    }

    override fun stop() { active = false }
    override fun isActive(): Boolean = active
}

/** 呼吸式警告效果：正弦波 level 变化 */
class BreathingWarningEffect(
    private val player: Player,
    private val maxLevel: Double,
    private val periodTicks: Long,
    private val namespace: String = "warning",
) : WarningEffect {
    override var level: Double = 0.0; private set
    @Volatile private var active = true
    private var tick = 0L

    override fun apply(player: Player) {
        val task = taboolib.common.platform.function.submit(period = 2L) {
            if (!active) { cancel(); return@submit }
            level = maxLevel * (0.5 + 0.5 * kotlin.math.sin(2.0 * Math.PI * tick / periodTicks))
            sendWarningBorder(player, level, namespace)
            tick += 2
        }
    }

    override fun stop() { active = false }
    override fun isActive(): Boolean = active
}

/** 按玩家 UUID 管理活跃警告效果 */
object ActionIllusionWarning {
    private val activeEffects = java.util.concurrent.ConcurrentHashMap<java.util.UUID, WarningEffect>()

    fun set(player: Player, effect: WarningEffect) {
        activeEffects[player.uniqueId]?.stop()
        activeEffects[player.uniqueId] = effect
        effect.apply(player)
    }

    fun clear(player: Player) {
        activeEffects.remove(player.uniqueId)?.stop()
        // 恢复世界边界
        val world = player.world
        val wb = world.worldBorder
        val realState = WorldBorderState(world = world, size = wb.size, center = wb.center, warningTime = wb.warningTime, warningDistance = wb.warningDistance)
        Monoceros.api().volatility().worldBorder().sendWorldBorder(player, realState)
    }

    fun get(player: Player): WarningEffect? = activeEffects[player.uniqueId]
}

private fun sendWarningBorder(player: Player, level: Double, namespace: String) {
    val size = (player.world.worldBorder.size * (1.0 - level.coerceIn(0.0, 1.0))).coerceAtLeast(1.0)
    val state = WorldBorderState(world = player.world, size = size, center = player.location, warningDistance = 0, warningTime = 0)
    Monoceros.api().volatility().worldBorder().sendWorldBorder(player, state)
}

/** 设置淡入式警告 */
class IllusionWarningSetNode : ActionNode {
    override val type = "illusion.warning.set"
    override fun execute(context: ActionContext, definition: ActionNodeDefinition): Any? {
        val player = context.variables["player"] as? Player ?: return null
        val level = (definition.config["level"] as? Number)?.toDouble() ?: 0.5
        val duration = (definition.config["duration"] as? Number)?.toLong() ?: 40L
        val namespace = definition.config["namespace"] as? String ?: "warning"
        val effect = FadeInWarningEffect(player, level, duration, namespace)
        ActionIllusionWarning.set(player, effect)
        return true
    }
}

/** 设置呼吸式警告 */
class IllusionWarningBreathingNode : ActionNode {
    override val type = "illusion.warning.breathing"
    override fun execute(context: ActionContext, definition: ActionNodeDefinition): Any? {
        val player = context.variables["player"] as? Player ?: return null
        val maxLevel = (definition.config["max-level"] as? Number)?.toDouble() ?: 0.5
        val period = (definition.config["period"] as? Number)?.toLong() ?: 60L
        val namespace = definition.config["namespace"] as? String ?: "warning"
        val effect = BreathingWarningEffect(player, maxLevel, period, namespace)
        ActionIllusionWarning.set(player, effect)
        return true
    }
}

/** 清除警告效果 */
class IllusionWarningClearNode : ActionNode {
    override val type = "illusion.warning.clear"
    override fun execute(context: ActionContext, definition: ActionNodeDefinition): Any? {
        val player = context.variables["player"] as? Player ?: return null
        ActionIllusionWarning.clear(player)
        return true
    }
}

/** 伪造实体血量显示 */
class IllusionHealthNode : ActionNode {
    override val type = "illusion.health"
    override fun execute(context: ActionContext, definition: ActionNodeDefinition): Any? {
        val viewer = context.variables["player"] as? Player ?: return null
        val targets = resolveEntities(context)
        val health = (definition.config["health"] as? Number)?.toFloat() ?: return null
        for (entity in targets) {
            Monoceros.api().volatility().metadata().updateHealth(viewer, entity, health)
        }
        return targets.size
    }
}

/** 创建客户端全息文字 */
class IllusionHologramNode : ActionNode {
    override val type = "illusion.hologram"
    override fun execute(context: ActionContext, definition: ActionNodeDefinition): Any? {
        val player = context.variables["player"] as? Player ?: return null
        val text = (definition.config["text"] as? String)?.replace(Regex("&([0-9a-fk-orA-FK-OR])"), "\u00a7$1") ?: return null
        val location = context.variables["location"] as? org.bukkit.Location ?: player.location.clone().add(0.0, 2.0, 0.0)
        // 使用 ArmorStand 实现客户端全息文字
        val world = location.world ?: return null
        try {
            val armorStand = world.spawn(location, org.bukkit.entity.ArmorStand::class.java) { stand ->
                stand.isVisible = false
                stand.isMarker = true
                stand.setGravity(false)
                stand.isCustomNameVisible = true
                stand.customName = text
                stand.isSmall = true
            }
            // 记录到上下文以便后续清理
            context.variables["hologram"] = armorStand
            context.variables["hologramId"] = armorStand.entityId
            return armorStand.entityId
        } catch (e: Exception) {
            return null
        }
    }
}

/** 切换幻象上下文（设置当前操作的 namespace/targetId） */
class IllusionSwitchNode : ActionNode {
    override val type = "illusion.switch"
    override fun execute(context: ActionContext, definition: ActionNodeDefinition): Any? {
        val namespace = definition.config["namespace"] as? String ?: return null
        val targetId = definition.config["target-id"] as? String
        context.variables["illusionNamespace"] = namespace
        if (targetId != null) {
            context.variables["illusionTargetId"] = targetId
        }
        return namespace
    }
}

class IllusionActionExtension : NativeExtension() {
    override val id = "action-illusion"
    override val name = "幻象域动作扩展"
    override val version = "1.0.0"

    override fun onEnable() {
        val service = Monoceros.api().actionWorkflow()
        service.registerNode(IllusionGlowNode())
        service.registerNode(IllusionWarningNode())
        service.registerNode(IllusionWorldBorderNode())
        service.registerNode(IllusionFakeBlockNode())
        service.registerNode(IllusionClearNode())
        service.registerNode(IllusionWarningSetNode())
        service.registerNode(IllusionWarningBreathingNode())
        service.registerNode(IllusionWarningClearNode())
        service.registerNode(IllusionHealthNode())
        service.registerNode(IllusionHologramNode())
        service.registerNode(IllusionSwitchNode())
    }
}
