package cc.bkhk.monoceros.dispatcher.pipeline.event

import cc.bkhk.monoceros.api.dispatcher.pipeline.PipelineContext
import cc.bkhk.monoceros.dispatcher.pipeline.AbstractPipeline
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityShootBowEvent
import org.bukkit.event.player.*

// region Player 事件 Pipeline

/** PlayerEvent 基础管道：从 PlayerEvent 提取 Player 作为主体 */
open class PlayerEventPipeline : AbstractPipeline<PlayerEvent>(PlayerEvent::class.java) {
    override fun initPrincipal(context: PipelineContext) {
        val event = castEvent(context) ?: return
        context.principal = event.player
        context.principalId = "PLAYER@${event.player.uniqueId}"
        context.player = event.player
    }
}

/** 玩家聊天事件 */
class PlayerChatEventPipeline : AbstractPipeline<AsyncPlayerChatEvent>(AsyncPlayerChatEvent::class.java) {
    override fun initPrincipal(context: PipelineContext) {
        val event = castEvent(context) ?: return
        context.principal = event.player
        context.principalId = "PLAYER@${event.player.uniqueId}"
        context.player = event.player
    }
    override fun initVariables(context: PipelineContext) {
        val event = castEvent(context) ?: return
        context.variables["message"] = event.message
        context.variables["format"] = event.format
        context.variables["recipients"] = event.recipients.toList()
    }
}

/** 玩家命令预处理事件 */
class PlayerCommandPreprocessEventPipeline : AbstractPipeline<PlayerCommandPreprocessEvent>(PlayerCommandPreprocessEvent::class.java) {
    override fun initPrincipal(context: PipelineContext) {
        val event = castEvent(context) ?: return
        context.principal = event.player
        context.principalId = "PLAYER@${event.player.uniqueId}"
        context.player = event.player
    }
    override fun initVariables(context: PipelineContext) {
        val event = castEvent(context) ?: return
        context.variables["command"] = event.message
    }
}

/** 玩家移动事件 */
class PlayerMoveEventPipeline : AbstractPipeline<PlayerMoveEvent>(PlayerMoveEvent::class.java) {
    override fun initPrincipal(context: PipelineContext) {
        val event = castEvent(context) ?: return
        context.principal = event.player
        context.principalId = "PLAYER@${event.player.uniqueId}"
        context.player = event.player
    }
    override fun initVariables(context: PipelineContext) {
        val event = castEvent(context) ?: return
        context.variables["from"] = event.from
        context.variables["to"] = event.to
    }
}

/** 玩家加入事件 */
class PlayerJoinEventPipeline : AbstractPipeline<PlayerJoinEvent>(PlayerJoinEvent::class.java) {
    override fun initPrincipal(context: PipelineContext) {
        val event = castEvent(context) ?: return
        context.principal = event.player
        context.principalId = "PLAYER@${event.player.uniqueId}"
        context.player = event.player
    }
    override fun initVariables(context: PipelineContext) {
        val event = castEvent(context) ?: return
        context.variables["joinMessage"] = event.joinMessage
    }
}

/** 玩家退出事件 */
class PlayerQuitEventPipeline : AbstractPipeline<PlayerQuitEvent>(PlayerQuitEvent::class.java) {
    override fun initPrincipal(context: PipelineContext) {
        val event = castEvent(context) ?: return
        context.principal = event.player
        context.principalId = "PLAYER@${event.player.uniqueId}"
        context.player = event.player
    }
    override fun initVariables(context: PipelineContext) {
        val event = castEvent(context) ?: return
        context.variables["quitMessage"] = event.quitMessage
    }
}

/** 玩家传送事件 */
class PlayerTeleportEventPipeline : AbstractPipeline<PlayerTeleportEvent>(PlayerTeleportEvent::class.java) {
    override fun initPrincipal(context: PipelineContext) {
        val event = castEvent(context) ?: return
        context.principal = event.player
        context.principalId = "PLAYER@${event.player.uniqueId}"
        context.player = event.player
    }
    override fun initVariables(context: PipelineContext) {
        val event = castEvent(context) ?: return
        context.variables["from"] = event.from
        context.variables["to"] = event.to
        context.variables["cause"] = event.cause.name
    }
}

// endregion

// region 虚拟玩家事件 Pipeline（从非 PlayerEvent 中提取 Player）

/** 玩家受伤事件（虚拟事件，从 EntityDamageEvent 中提取 Player） */
class PlayerDamageEventPipeline : AbstractPipeline<EntityDamageEvent>(EntityDamageEvent::class.java) {
    override fun initPrincipal(context: PipelineContext) {
        val event = castEvent(context) ?: return
        val player = event.entity as? Player ?: return
        context.principal = player
        context.principalId = "PLAYER@${player.uniqueId}"
        context.player = player
    }
    override fun filter(context: PipelineContext) {
        if (context.player == null) context.isFiltered = true
    }
    override fun initVariables(context: PipelineContext) {
        val event = castEvent(context) ?: return
        context.variables["damage"] = event.damage
        context.variables["finalDamage"] = event.finalDamage
        context.variables["cause"] = event.cause.name
    }
}

/** 玩家被实体攻击事件 */
class PlayerDamageByEntityEventPipeline : AbstractPipeline<EntityDamageByEntityEvent>(EntityDamageByEntityEvent::class.java) {
    override fun initPrincipal(context: PipelineContext) {
        val event = castEvent(context) ?: return
        val player = event.entity as? Player ?: return
        context.principal = player
        context.principalId = "PLAYER@${player.uniqueId}"
        context.player = player
    }
    override fun filter(context: PipelineContext) {
        if (context.player == null) context.isFiltered = true
    }
    override fun initVariables(context: PipelineContext) {
        val event = castEvent(context) ?: return
        context.variables["damage"] = event.damage
        context.variables["finalDamage"] = event.finalDamage
        context.variables["cause"] = event.cause.name
        context.variables["damager"] = event.damager
        // 尝试提取真实攻击者（穿透投射物）
        val realDamager = if (event.damager is Projectile) {
            (event.damager as Projectile).shooter as? Entity
        } else {
            event.damager
        }
        context.variables["realDamager"] = realDamager
    }
}

/** 玩家被玩家攻击事件 */
class PlayerDamageByPlayerEventPipeline : AbstractPipeline<EntityDamageByEntityEvent>(EntityDamageByEntityEvent::class.java) {
    override val priority: Int = 12
    override fun initPrincipal(context: PipelineContext) {
        val event = castEvent(context) ?: return
        val player = event.entity as? Player ?: return
        context.principal = player
        context.principalId = "PLAYER@${player.uniqueId}"
        context.player = player
    }
    override fun filter(context: PipelineContext) {
        val event = castEvent(context) ?: return
        // 必须是玩家被玩家攻击
        if (context.player == null) { context.isFiltered = true; return }
        val damager = if (event.damager is Projectile) {
            (event.damager as Projectile).shooter as? Entity
        } else {
            event.damager
        }
        if (damager !is Player) { context.isFiltered = true; return }
        context.variables["attacker"] = damager
    }
}

/** 玩家射箭事件 */
class PlayerShootBowEventPipeline : AbstractPipeline<EntityShootBowEvent>(EntityShootBowEvent::class.java) {
    override fun initPrincipal(context: PipelineContext) {
        val event = castEvent(context) ?: return
        val player = event.entity as? Player ?: return
        context.principal = player
        context.principalId = "PLAYER@${player.uniqueId}"
        context.player = player
    }
    override fun filter(context: PipelineContext) {
        if (context.player == null) context.isFiltered = true
    }
    override fun initVariables(context: PipelineContext) {
        val event = castEvent(context) ?: return
        context.variables["bow"] = event.bow
        context.variables["projectile"] = event.projectile
        context.variables["force"] = event.force
    }
}

// endregion

// region 实体事件 Pipeline

/** 实体受伤事件 */
class EntityDamageEventPipeline : AbstractPipeline<EntityDamageEvent>(EntityDamageEvent::class.java) {
    override fun initPrincipal(context: PipelineContext) {
        val event = castEvent(context) ?: return
        val entity = event.entity
        context.principal = entity
        context.principalId = "ENTITY@${entity.uniqueId}"
        if (entity is Player) context.player = entity
    }
    override fun initVariables(context: PipelineContext) {
        val event = castEvent(context) ?: return
        context.variables["entity"] = event.entity
        context.variables["damage"] = event.damage
        context.variables["finalDamage"] = event.finalDamage
        context.variables["cause"] = event.cause.name
    }
}

/** 实体被实体攻击事件 */
class EntityDamageByEntityEventPipeline : AbstractPipeline<EntityDamageByEntityEvent>(EntityDamageByEntityEvent::class.java) {
    override fun initPrincipal(context: PipelineContext) {
        val event = castEvent(context) ?: return
        val entity = event.entity
        context.principal = entity
        context.principalId = "ENTITY@${entity.uniqueId}"
        if (entity is Player) context.player = entity
    }
    override fun initVariables(context: PipelineContext) {
        val event = castEvent(context) ?: return
        context.variables["entity"] = event.entity
        context.variables["damager"] = event.damager
        context.variables["damage"] = event.damage
        context.variables["finalDamage"] = event.finalDamage
        context.variables["cause"] = event.cause.name
    }
}

/** 实体射箭事件 */
class EntityShootBowEventPipeline : AbstractPipeline<EntityShootBowEvent>(EntityShootBowEvent::class.java) {
    override fun initPrincipal(context: PipelineContext) {
        val event = castEvent(context) ?: return
        val entity = event.entity
        context.principal = entity
        context.principalId = if (entity is Player) "PLAYER@${entity.uniqueId}" else "ENTITY@${entity.uniqueId}"
        if (entity is Player) context.player = entity
    }
    override fun initVariables(context: PipelineContext) {
        val event = castEvent(context) ?: return
        context.variables["entity"] = event.entity
        context.variables["bow"] = event.bow
        context.variables["projectile"] = event.projectile
        context.variables["force"] = event.force
    }
}

// endregion
