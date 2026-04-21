package cc.bkhk.monoceros.extension.action.target

import cc.bkhk.monoceros.Monoceros
import cc.bkhk.monoceros.api.workflow.ActionContext
import cc.bkhk.monoceros.api.workflow.ActionNode
import cc.bkhk.monoceros.api.workflow.ActionNodeDefinition
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player

/** 把当前 player 或 entity 写入 targets */
class TargetSelfNode : ActionNode {
    override val type = "target.self"
    override fun execute(context: ActionContext, definition: ActionNodeDefinition): Any? {
        val self = context.variables["player"] ?: context.variables["entity"] ?: return emptyList<Any>()
        val targets = listOf(self)
        context.variables["targets"] = targets
        return targets
    }
}

/** 选取当前世界内目标 */
class TargetWorldNode : ActionNode {
    override val type = "target.world"
    override fun execute(context: ActionContext, definition: ActionNodeDefinition): Any? {
        val player = context.variables["player"] as? Player ?: return emptyList<Any>()
        val playersOnly = definition.config["players-only"] as? Boolean ?: false
        val targets: List<Entity> = if (playersOnly) {
            player.world.players.toList()
        } else {
            player.world.entities.toList()
        }
        context.variables["targets"] = targets
        return targets
    }
}

/** 选取全服在线玩家 */
class TargetServerNode : ActionNode {
    override val type = "target.server"
    override fun execute(context: ActionContext, definition: ActionNodeDefinition): Any? {
        val targets = Bukkit.getOnlinePlayers().toList()
        context.variables["targets"] = targets
        return targets
    }
}

/** 以半径选取实体/玩家 */
class TargetRadiusNode : ActionNode {
    override val type = "target.radius"
    override fun execute(context: ActionContext, definition: ActionNodeDefinition): Any? {
        val radius = (definition.config["radius"] as? Number)?.toDouble() ?: 5.0
        val playersOnly = definition.config["players-only"] as? Boolean ?: false
        val center = resolveCenter(context)
            ?: return emptyList<Any>()
        val entities = center.world?.getNearbyEntities(center, radius, radius, radius) ?: emptyList()
        val targets = if (playersOnly) entities.filterIsInstance<Player>() else entities.toList()
        context.variables["targets"] = targets
        return targets
    }

    private fun resolveCenter(context: ActionContext): Location? {
        (context.variables["location"] as? Location)?.let { return it }
        (context.variables["player"] as? Player)?.let { return it.location }
        (context.variables["entity"] as? Entity)?.let { return it.location }
        return null
    }
}

/** 以立方体范围选取目标 */
class TargetBoxNode : ActionNode {
    override val type = "target.box"
    override fun execute(context: ActionContext, definition: ActionNodeDefinition): Any? {
        val width = (definition.config["width"] as? Number)?.toDouble() ?: 5.0
        val height = (definition.config["height"] as? Number)?.toDouble() ?: 5.0
        val depth = (definition.config["depth"] as? Number)?.toDouble() ?: 5.0
        val center = (context.variables["location"] as? Location)
            ?: (context.variables["player"] as? Player)?.location
            ?: return emptyList<Any>()
        val targets = center.world?.getNearbyEntities(center, width / 2, height / 2, depth / 2)?.toList()
            ?: emptyList()
        context.variables["targets"] = targets
        return targets
    }
}

/** 对当前 targets 做筛选 */
class TargetFilterNode : ActionNode {
    override val type = "target.filter"
    override fun execute(context: ActionContext, definition: ActionNodeDefinition): Any? {
        @Suppress("UNCHECKED_CAST")
        val targets = context.variables["targets"] as? List<Any> ?: return emptyList<Any>()
        val entityType = definition.config["entity-type"] as? String
        val script = definition.config["script"] as? String

        var filtered = targets

        // 按实体类型过滤
        if (entityType != null) {
            filtered = filtered.filter { e ->
                e is Entity && e.type.name.equals(entityType, ignoreCase = true)
            }
        }

        // 按脚本条件过滤
        if (script != null) {
            filtered = filtered.filter { target ->
                val vars = HashMap(context.variables)
                vars["target"] = target
                val result = Monoceros.api().scripts().invoke(script, context.sender, vars)
                result == true || result == "true"
            }
        }

        context.variables["targets"] = filtered
        return filtered
    }
}

/** 根据视线选取目标 */
class TargetLineOfSightNode : ActionNode {
    override val type = "target.line-of-sight"
    override fun execute(context: ActionContext, definition: ActionNodeDefinition): Any? {
        val maxDistance = (definition.config["max-distance"] as? Number)?.toInt() ?: 50
        val player = context.variables["player"] as? LivingEntity ?: return emptyList<Any>()
        val targets = player.getLineOfSight(null, maxDistance)
            .mapNotNull { block -> block.location.world?.getNearbyEntities(block.location, 0.5, 0.5, 0.5) }
            .flatten()
            .filter { it != player }
            .distinct()
        context.variables["targets"] = targets
        return targets
    }
}
