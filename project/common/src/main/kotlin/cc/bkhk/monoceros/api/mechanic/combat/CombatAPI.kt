package cc.bkhk.monoceros.api.mechanic.combat

import java.util.UUID

/** 冷却管理器 */
interface CooldownManager {
    fun setCooldown(playerId: UUID, key: String, durationMs: Long)
    fun getCooldownRemaining(playerId: UUID, key: String): Long
    fun hasCooldown(playerId: UUID, key: String): Boolean
    fun clearCooldown(playerId: UUID, key: String)
    fun clearAll(playerId: UUID)
}

/** 连击追踪器 */
interface ComboTracker {
    fun recordHit(playerId: UUID, targetId: UUID): Int
    fun getComboCount(playerId: UUID): Int
    fun resetCombo(playerId: UUID)
    fun setComboTimeout(timeoutMs: Long)
}

/** 状态叠层 */
data class StatusEffect(
    val id: String,
    val name: String,
    val stacks: Int = 1,
    val maxStacks: Int = 1,
    val durationMs: Long = -1,
    val appliedAt: Long = System.currentTimeMillis(),
    val metadata: Map<String, Any?> = emptyMap(),
) {
    fun isExpired(): Boolean = durationMs > 0 && System.currentTimeMillis() - appliedAt > durationMs
    fun remainingMs(): Long = if (durationMs <= 0) -1 else (durationMs - (System.currentTimeMillis() - appliedAt)).coerceAtLeast(0)
}

/** 状态管理器 */
interface StatusManager {
    fun apply(playerId: UUID, effect: StatusEffect): StatusEffect
    fun remove(playerId: UUID, effectId: String): StatusEffect?
    fun get(playerId: UUID, effectId: String): StatusEffect?
    fun getAll(playerId: UUID): Collection<StatusEffect>
    fun clearAll(playerId: UUID)
    fun clearExpired(playerId: UUID): Int
}

/** 技能触发链阶段 */
enum class SkillPhase { CONDITION, WINDUP, EXECUTE, RECOVERY, COOLDOWN }

/** 技能定义 */
data class SkillDefinition(
    val id: String,
    val conditionScript: String? = null,
    val windupTicks: Long = 0,
    val executeScript: String,
    val recoveryTicks: Long = 0,
    val cooldownMs: Long = 0,
)

/** 技能执行器 */
interface SkillExecutor {
    fun execute(playerId: UUID, skill: SkillDefinition, variables: Map<String, Any?> = emptyMap())
    fun cancel(playerId: UUID, skillId: String)
    fun isExecuting(playerId: UUID, skillId: String): Boolean
    fun getCurrentPhase(playerId: UUID, skillId: String): SkillPhase?
}
