package cc.bkhk.monoceros.extension.property.common

import cc.bkhk.monoceros.api.workflow.PropertyAccessor
import org.bukkit.Location
import org.bukkit.event.*
import org.bukkit.event.entity.*
import org.bukkit.event.player.*
import taboolib.module.nms.PacketReceiveEvent
import taboolib.module.nms.PacketSendEvent
import kotlin.reflect.KClass

/** Event 基础属性访问器 */
object EventPropertyAccessor : PropertyAccessor<Event> {
    override val targetType: KClass<Event> = Event::class

    override fun read(target: Event, key: String, context: Map<String, Any?>): Any? = when (key) {
        "eventName" -> target.eventName
        "isAsync" -> target.isAsynchronous
        else -> null
    }

    override fun write(target: Event, key: String, value: Any?, context: Map<String, Any?>) {
        when (key) {
            "cancelled" -> {
                if (target is Cancellable) {
                    (target as Cancellable).isCancelled = value as? Boolean
                        ?: error("Event.cancelled 需要 Boolean 类型")
                } else {
                    error("事件 ${target.eventName} 不可取消")
                }
            }
            else -> error("Event 属性 '$key' 不可写")
        }
    }
}

/** PlayerMoveEvent 属性访问器 */
object PlayerMoveEventPropertyAccessor : PropertyAccessor<PlayerMoveEvent> {
    override val targetType: KClass<PlayerMoveEvent> = PlayerMoveEvent::class

    override fun read(target: PlayerMoveEvent, key: String, context: Map<String, Any?>): Any? = when (key) {
        "from" -> target.from
        "to" -> target.to
        "player" -> target.player
        else -> null
    }

    override fun write(target: PlayerMoveEvent, key: String, value: Any?, context: Map<String, Any?>) {
        when (key) {
            "to" -> {
                val loc = value as? Location ?: error("PlayerMoveEvent.to 需要 Location 类型")
                try { target.setTo(loc) } catch (_: Exception) { /* 部分版本不支持 setTo */ }
            }
            "cancelled" -> target.isCancelled = value as? Boolean ?: error("PlayerMoveEvent.cancelled 需要 Boolean 类型")
            else -> error("PlayerMoveEvent 属性 '$key' 不可写")
        }
    }
}

/** PlayerJoinEvent 属性访问器 */
object PlayerJoinEventPropertyAccessor : PropertyAccessor<PlayerJoinEvent> {
    override val targetType: KClass<PlayerJoinEvent> = PlayerJoinEvent::class

    @Suppress("DEPRECATION")
    override fun read(target: PlayerJoinEvent, key: String, context: Map<String, Any?>): Any? = when (key) {
        "player" -> target.player
        "joinMessage" -> target.joinMessage
        else -> null
    }

    @Suppress("DEPRECATION")
    override fun write(target: PlayerJoinEvent, key: String, value: Any?, context: Map<String, Any?>) {
        when (key) {
            "joinMessage" -> target.joinMessage = value as? String
            else -> error("PlayerJoinEvent 属性 '$key' 不可写")
        }
    }
}

/** PlayerQuitEvent 属性访问器 */
object PlayerQuitEventPropertyAccessor : PropertyAccessor<PlayerQuitEvent> {
    override val targetType: KClass<PlayerQuitEvent> = PlayerQuitEvent::class

    @Suppress("DEPRECATION")
    override fun read(target: PlayerQuitEvent, key: String, context: Map<String, Any?>): Any? = when (key) {
        "player" -> target.player
        "quitMessage" -> target.quitMessage
        else -> null
    }

    @Suppress("DEPRECATION")
    override fun write(target: PlayerQuitEvent, key: String, value: Any?, context: Map<String, Any?>) {
        when (key) {
            "quitMessage" -> target.quitMessage = value as? String
            else -> error("PlayerQuitEvent 属性 '$key' 不可写")
        }
    }
}

/** PlayerInteractEvent 属性访问器 */
object PlayerInteractEventPropertyAccessor : PropertyAccessor<PlayerInteractEvent> {
    override val targetType: KClass<PlayerInteractEvent> = PlayerInteractEvent::class

    override fun read(target: PlayerInteractEvent, key: String, context: Map<String, Any?>): Any? = when (key) {
        "player" -> target.player
        "action" -> target.action.name
        "item" -> target.item
        "clickedBlock" -> target.clickedBlock
        "blockFace" -> target.blockFace.name
        "hand" -> try {
            target.hand?.name
        } catch (_: NoSuchMethodError) {
            // 1.12.2 以下版本可能不存在 getHand()
            null
        }
        else -> null
    }
}

/** PlayerItemHeldEvent 属性访问器 */
object PlayerItemHeldEventPropertyAccessor : PropertyAccessor<PlayerItemHeldEvent> {
    override val targetType: KClass<PlayerItemHeldEvent> = PlayerItemHeldEvent::class

    override fun read(target: PlayerItemHeldEvent, key: String, context: Map<String, Any?>): Any? = when (key) {
        "player" -> target.player
        "previousSlot" -> target.previousSlot
        "newSlot" -> target.newSlot
        else -> null
    }
}

/** PlayerRespawnEvent 属性访问器 */
object PlayerRespawnEventPropertyAccessor : PropertyAccessor<PlayerRespawnEvent> {
    override val targetType: KClass<PlayerRespawnEvent> = PlayerRespawnEvent::class

    override fun read(target: PlayerRespawnEvent, key: String, context: Map<String, Any?>): Any? = when (key) {
        "player" -> target.player
        "respawnLocation" -> target.respawnLocation
        "isBedSpawn" -> try {
            target.isBedSpawn
        } catch (_: NoSuchMethodError) {
            // 部分旧版本可能不存在该方法
            null
        }
        else -> null
    }

    override fun write(target: PlayerRespawnEvent, key: String, value: Any?, context: Map<String, Any?>) {
        when (key) {
            "respawnLocation" -> target.respawnLocation = value as? Location
                ?: error("PlayerRespawnEvent.respawnLocation 需要 Location 类型")
            else -> error("PlayerRespawnEvent 属性 '$key' 不可写")
        }
    }
}

/** PlayerCommandPreprocessEvent 属性访问器 */
object PlayerCommandPreprocessEventPropertyAccessor : PropertyAccessor<PlayerCommandPreprocessEvent> {
    override val targetType: KClass<PlayerCommandPreprocessEvent> = PlayerCommandPreprocessEvent::class

    override fun read(target: PlayerCommandPreprocessEvent, key: String, context: Map<String, Any?>): Any? = when (key) {
        "player" -> target.player
        "message" -> target.message
        else -> null
    }

    override fun write(target: PlayerCommandPreprocessEvent, key: String, value: Any?, context: Map<String, Any?>) {
        when (key) {
            "message" -> target.message = value as? String ?: error("PlayerCommandPreprocessEvent.message 需要 String 类型")
            "cancelled" -> target.isCancelled = value as? Boolean ?: error("PlayerCommandPreprocessEvent.cancelled 需要 Boolean 类型")
            else -> error("PlayerCommandPreprocessEvent 属性 '$key' 不可写")
        }
    }
}

/** AsyncPlayerChatEvent 属性访问器 */
@Suppress("DEPRECATION")
object AsyncPlayerChatEventPropertyAccessor : PropertyAccessor<AsyncPlayerChatEvent> {
    override val targetType: KClass<AsyncPlayerChatEvent> = AsyncPlayerChatEvent::class

    override fun read(target: AsyncPlayerChatEvent, key: String, context: Map<String, Any?>): Any? = when (key) {
        "player" -> target.player
        "message" -> target.message
        "format" -> target.format
        "recipients" -> target.recipients
        else -> null
    }

    override fun write(target: AsyncPlayerChatEvent, key: String, value: Any?, context: Map<String, Any?>) {
        when (key) {
            "message" -> target.message = value as? String ?: error("AsyncPlayerChatEvent.message 需要 String 类型")
            "format" -> target.format = value as? String ?: error("AsyncPlayerChatEvent.format 需要 String 类型")
            "cancelled" -> target.isCancelled = value as? Boolean ?: error("AsyncPlayerChatEvent.cancelled 需要 Boolean 类型")
            else -> error("AsyncPlayerChatEvent 属性 '$key' 不可写")
        }
    }
}

/** EntityDamageEvent 属性访问器 */
object EntityDamageEventPropertyAccessor : PropertyAccessor<EntityDamageEvent> {
    override val targetType: KClass<EntityDamageEvent> = EntityDamageEvent::class

    override fun read(target: EntityDamageEvent, key: String, context: Map<String, Any?>): Any? = when (key) {
        "entity" -> target.entity
        "damage" -> target.damage
        "finalDamage" -> target.finalDamage
        "cause" -> target.cause.name
        else -> null
    }

    override fun write(target: EntityDamageEvent, key: String, value: Any?, context: Map<String, Any?>) {
        when (key) {
            "damage" -> target.damage = (value as? Number)?.toDouble() ?: error("EntityDamageEvent.damage 需要 Number 类型")
            "cancelled" -> target.isCancelled = value as? Boolean ?: error("EntityDamageEvent.cancelled 需要 Boolean 类型")
            else -> error("EntityDamageEvent 属性 '$key' 不可写")
        }
    }
}

/** EntityDeathEvent 属性访问器 */
object EntityDeathEventPropertyAccessor : PropertyAccessor<EntityDeathEvent> {
    override val targetType: KClass<EntityDeathEvent> = EntityDeathEvent::class

    override fun read(target: EntityDeathEvent, key: String, context: Map<String, Any?>): Any? = when (key) {
        "entity" -> target.entity
        "drops" -> target.drops
        "droppedExp" -> target.droppedExp
        "deathMessage" -> try {
            // deathMessage 仅在 PlayerDeathEvent 上可用
            @Suppress("DEPRECATION")
            (target as? org.bukkit.event.entity.PlayerDeathEvent)?.deathMessage
        } catch (_: NoSuchMethodError) {
            null
        }
        else -> null
    }

    override fun write(target: EntityDeathEvent, key: String, value: Any?, context: Map<String, Any?>) {
        when (key) {
            "droppedExp" -> target.droppedExp = (value as? Number)?.toInt() ?: error("EntityDeathEvent.droppedExp 需要 Number 类型")
            "deathMessage" -> try {
                @Suppress("DEPRECATION")
                (target as? org.bukkit.event.entity.PlayerDeathEvent)?.deathMessage = value as? String
            } catch (_: NoSuchMethodError) {
                error("当前版本不支持设置 deathMessage")
            }
            else -> error("EntityDeathEvent 属性 '$key' 不可写")
        }
    }
}

/** EntityShootBowEvent 属性访问器 */
object EntityShootBowEventPropertyAccessor : PropertyAccessor<EntityShootBowEvent> {
    override val targetType: KClass<EntityShootBowEvent> = EntityShootBowEvent::class

    override fun read(target: EntityShootBowEvent, key: String, context: Map<String, Any?>): Any? = when (key) {
        "entity" -> target.entity
        "bow" -> target.bow
        "projectile" -> target.projectile
        "force" -> target.force
        else -> null
    }

    override fun write(target: EntityShootBowEvent, key: String, value: Any?, context: Map<String, Any?>) {
        when (key) {
            "projectile" -> target.projectile = value as? org.bukkit.entity.Entity
                ?: error("EntityShootBowEvent.projectile 需要 Entity 类型")
            "cancelled" -> target.isCancelled = value as? Boolean ?: error("EntityShootBowEvent.cancelled 需要 Boolean 类型")
            else -> error("EntityShootBowEvent 属性 '$key' 不可写")
        }
    }
}

/** PacketSendEvent 属性访问器 */
object PacketSendEventPropertyAccessor : PropertyAccessor<PacketSendEvent> {
    override val targetType: KClass<PacketSendEvent> = PacketSendEvent::class

    override fun read(target: PacketSendEvent, key: String, context: Map<String, Any?>): Any? = when (key) {
        "player" -> target.player
        "packet" -> target.packet
        "packetName" -> target.packet.name
        else -> null
    }

    override fun write(target: PacketSendEvent, key: String, value: Any?, context: Map<String, Any?>) {
        when (key) {
            "cancelled" -> target.isCancelled = value as? Boolean ?: error("PacketSendEvent.cancelled 需要 Boolean 类型")
            else -> error("PacketSendEvent 属性 '$key' 不可写")
        }
    }
}

/** PacketReceiveEvent 属性访问器 */
object PacketReceiveEventPropertyAccessor : PropertyAccessor<PacketReceiveEvent> {
    override val targetType: KClass<PacketReceiveEvent> = PacketReceiveEvent::class

    override fun read(target: PacketReceiveEvent, key: String, context: Map<String, Any?>): Any? = when (key) {
        "player" -> target.player
        "packet" -> target.packet
        "packetName" -> target.packet.name
        else -> null
    }

    override fun write(target: PacketReceiveEvent, key: String, value: Any?, context: Map<String, Any?>) {
        when (key) {
            "cancelled" -> target.isCancelled = value as? Boolean ?: error("PacketReceiveEvent.cancelled 需要 Boolean 类型")
            else -> error("PacketReceiveEvent 属性 '$key' 不可写")
        }
    }
}
