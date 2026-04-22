package cc.bkhk.monoceros.extension.action.entity

import cc.bkhk.monoceros.api.workflow.ActionContext
import cc.bkhk.monoceros.api.workflow.ActionNode
import cc.bkhk.monoceros.api.workflow.ActionNodeDefinition
import org.bukkit.Location
import org.bukkit.entity.Damageable
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

/** 从上下文解析目标实体列表 */
internal fun resolveTargets(context: ActionContext): List<Entity> {
    @Suppress("UNCHECKED_CAST")
    (context.variables["targets"] as? List<*>)?.filterIsInstance<Entity>()?.let { return it }
    (context.variables["target"] as? Entity)?.let { return listOf(it) }
    (context.variables["player"] as? Player)?.let { return listOf(it) }
    return emptyList()
}

private fun resolveItem(context: ActionContext): ItemStack? {
    return context.variables["item"] as? ItemStack
        ?: context.variables["targetItem"] as? ItemStack
        ?: context.variables["lastResult"] as? ItemStack
}

class EntityDamageNode : ActionNode {
    override val type = "entity.damage"
    override fun execute(context: ActionContext, definition: ActionNodeDefinition): Any? {
        val amount = (definition.config["amount"] as? Number)?.toDouble() ?: 0.0
        val targets = resolveTargets(context)
        for (entity in targets) {
            if (entity is Damageable) entity.damage(amount)
        }
        return targets.size
    }
}

class EntityHealNode : ActionNode {
    override val type = "entity.heal"
    override fun execute(context: ActionContext, definition: ActionNodeDefinition): Any? {
        val amount = (definition.config["amount"] as? Number)?.toDouble() ?: 0.0
        val targets = resolveTargets(context)
        for (entity in targets) {
            if (entity is Damageable) {
                entity.health = (entity.health + amount).coerceAtMost(entity.maxHealth)
            }
        }
        return targets.size
    }
}

class EntityTeleportNode : ActionNode {
    override val type = "entity.teleport"
    override fun execute(context: ActionContext, definition: ActionNodeDefinition): Any? {
        val location = context.variables["location"] as? Location ?: return 0
        val targets = resolveTargets(context)
        for (entity in targets) {
            entity.teleport(location)
        }
        return targets.size
    }
}

class EntityPotionAddNode : ActionNode {
    override val type = "entity.potion.add"
    override fun execute(context: ActionContext, definition: ActionNodeDefinition): Any? {
        val typeName = definition.config["type"] as? String ?: return 0
        val potionType = PotionEffectType.getByName(typeName) ?: return 0
        val duration = (definition.config["duration"] as? Number)?.toInt() ?: 200
        val amplifier = (definition.config["amplifier"] as? Number)?.toInt() ?: 0
        val ambient = definition.config["ambient"] as? Boolean ?: false
        val particles = definition.config["particles"] as? Boolean ?: true
        val effect = PotionEffect(potionType, duration, amplifier, ambient, particles)
        val targets = resolveTargets(context)
        for (entity in targets) {
            if (entity is LivingEntity) entity.addPotionEffect(effect)
        }
        return targets.size
    }
}

class EntityPotionRemoveNode : ActionNode {
    override val type = "entity.potion.remove"
    override fun execute(context: ActionContext, definition: ActionNodeDefinition): Any? {
        val typeName = definition.config["type"] as? String ?: return 0
        val potionType = PotionEffectType.getByName(typeName) ?: return 0
        val targets = resolveTargets(context)
        for (entity in targets) {
            if (entity is LivingEntity) entity.removePotionEffect(potionType)
        }
        return targets.size
    }
}

class EntitySwitchNode : ActionNode {
    override val type = "entity.switch"
    override fun execute(context: ActionContext, definition: ActionNodeDefinition): Any? {
        val property = definition.config["property"] as? String ?: return 0
        val value = definition.config["value"] as? Boolean ?: true
        val targets = resolveTargets(context)
        for (entity in targets) {
            when (property.lowercase()) {
                "glowing" -> entity.isGlowing = value
                "silent" -> entity.isSilent = value
                "invulnerable" -> entity.isInvulnerable = value
                "gravity" -> entity.setGravity(value)
                "ai" -> if (entity is LivingEntity) entity.setAI(value)
                "collidable" -> if (entity is LivingEntity) entity.isCollidable = value
            }
        }
        return targets.size
    }
}

class EntityEquipmentSetNode : ActionNode {
    override val type = "entity.equipment.set"

    override fun execute(context: ActionContext, definition: ActionNodeDefinition): Any? {
        val slot = (definition.config["slot"] as? String)?.lowercase() ?: return 0
        val item = resolveItem(context) ?: return 0
        val targets = resolveTargets(context).filterIsInstance<LivingEntity>()
        for (entity in targets) {
            val equipment = entity.equipment ?: continue
            when (slot) {
                "main-hand", "mainhand", "hand" -> equipment.setItemInMainHand(item.clone())
                "off-hand", "offhand" -> equipment.setItemInOffHand(item.clone())
                "helmet", "head" -> equipment.helmet = item.clone()
                "chestplate", "chest" -> equipment.chestplate = item.clone()
                "leggings", "legs" -> equipment.leggings = item.clone()
                "boots", "feet" -> equipment.boots = item.clone()
            }
        }
        return targets.size
    }
}
