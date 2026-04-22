package cc.bkhk.monoceros.api.mechanic.interact

import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import java.util.UUID

/** 交互定义 */
data class InteractDefinition(
    val id: String,
    val type: InteractType,
    val materialFilter: String? = null,
    val script: String,
    val cooldownMs: Long = 0,
    val variables: Map<String, Any?> = emptyMap(),
)

enum class InteractType {
    RIGHT_CLICK,
    LEFT_CLICK,
    SNEAK_RIGHT_CLICK,
    SNEAK_LEFT_CLICK,
    SNEAK_TOGGLE,
}

/** 视线锁定结果 */
data class LookAtResult(
    val target: Entity?,
    val distance: Double,
)

/** 交互服务 */
interface InteractService {
    fun register(definition: InteractDefinition)
    fun unregister(id: String): InteractDefinition?
    fun get(id: String): InteractDefinition?
    fun all(): Collection<InteractDefinition>
    fun getLookAtTarget(player: Player, maxDistance: Double = 50.0): LookAtResult?
}
