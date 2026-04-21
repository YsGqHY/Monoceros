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

/** 对目标实体显示发光效果 */
class IllusionGlowNode : ActionNode {
    override val type = "illusion.glow"
    override fun execute(context: ActionContext, definition: ActionNodeDefinition): Any? {
        val viewer = context.variables["player"] as? Player ?: return null
        val targets = resolveEntities(context)
        val value = definition.config["value"] as? Boolean ?: true
        for (entity in targets) {
            Monoceros.api().volatility().metadata().setFlag(viewer, entity, EntityFlag.GLOWING, value)
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
            // ActionBar 通过 Title API 的空 title 模拟，或由脚本层处理
            // 此处使用 sendMessage 作为降级
            player.sendMessage(actionbar)
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
        val material = org.bukkit.Material.matchMaterial(materialName) ?: return null
        val data = material.createBlockData()
        Monoceros.api().volatility().blocks().sendBlockChange(player, location, data)
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
    }
}
