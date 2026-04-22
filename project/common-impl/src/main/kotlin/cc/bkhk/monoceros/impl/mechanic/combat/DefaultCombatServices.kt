package cc.bkhk.monoceros.impl.mechanic.combat

import cc.bkhk.monoceros.Monoceros
import cc.bkhk.monoceros.api.mechanic.combat.*
import cc.bkhk.monoceros.impl.util.DiagnosticLogger
import taboolib.common.platform.function.submit
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class DefaultCooldownManager : CooldownManager {
    // playerId -> (key -> expiresAt)
    private val cooldowns = ConcurrentHashMap<UUID, ConcurrentHashMap<String, Long>>()

    override fun setCooldown(playerId: UUID, key: String, durationMs: Long) {
        cooldowns.computeIfAbsent(playerId) { ConcurrentHashMap() }[key] = System.currentTimeMillis() + durationMs
    }
    override fun getCooldownRemaining(playerId: UUID, key: String): Long {
        val expiresAt = cooldowns[playerId]?.get(key) ?: return 0
        return (expiresAt - System.currentTimeMillis()).coerceAtLeast(0)
    }
    override fun hasCooldown(playerId: UUID, key: String): Boolean = getCooldownRemaining(playerId, key) > 0
    override fun clearCooldown(playerId: UUID, key: String) { cooldowns[playerId]?.remove(key) }
    override fun clearAll(playerId: UUID) { cooldowns.remove(playerId) }
}

class DefaultComboTracker : ComboTracker {
    private data class ComboState(var count: Int = 0, var lastHitTime: Long = 0, var targetId: UUID? = null)
    private val combos = ConcurrentHashMap<UUID, ComboState>()
    @Volatile private var timeoutMs: Long = 2000

    override fun recordHit(playerId: UUID, targetId: UUID): Int {
        val state = combos.computeIfAbsent(playerId) { ComboState() }
        val now = System.currentTimeMillis()
        if (now - state.lastHitTime > timeoutMs || state.targetId != targetId) {
            state.count = 0
            state.targetId = targetId
        }
        state.count++
        state.lastHitTime = now
        return state.count
    }
    override fun getComboCount(playerId: UUID): Int = combos[playerId]?.count ?: 0
    override fun resetCombo(playerId: UUID) { combos.remove(playerId) }
    override fun setComboTimeout(timeoutMs: Long) { this.timeoutMs = timeoutMs }
}

class DefaultStatusManager : StatusManager {
    private val statuses = ConcurrentHashMap<UUID, ConcurrentHashMap<String, StatusEffect>>()

    override fun apply(playerId: UUID, effect: StatusEffect): StatusEffect {
        val map = statuses.computeIfAbsent(playerId) { ConcurrentHashMap() }
        val existing = map[effect.id]
        val applied = if (existing != null && existing.stacks < effect.maxStacks) {
            effect.copy(stacks = (existing.stacks + effect.stacks).coerceAtMost(effect.maxStacks))
        } else effect
        map[effect.id] = applied
        return applied
    }
    override fun remove(playerId: UUID, effectId: String): StatusEffect? = statuses[playerId]?.remove(effectId)
    override fun get(playerId: UUID, effectId: String): StatusEffect? = statuses[playerId]?.get(effectId)
    override fun getAll(playerId: UUID): Collection<StatusEffect> = statuses[playerId]?.values?.toList() ?: emptyList()
    override fun clearAll(playerId: UUID) { statuses.remove(playerId) }
    override fun clearExpired(playerId: UUID): Int {
        val map = statuses[playerId] ?: return 0
        val expired = map.values.filter { it.isExpired() }
        expired.forEach { map.remove(it.id) }
        return expired.size
    }
}

class DefaultSkillExecutor(
    private val cooldownManager: CooldownManager,
) : SkillExecutor {
    private data class ActiveSkill(val skillId: String, @Volatile var phase: SkillPhase, @Volatile var cancelled: Boolean = false)
    private val activeSkills = ConcurrentHashMap<UUID, ConcurrentHashMap<String, ActiveSkill>>()

    override fun execute(playerId: UUID, skill: SkillDefinition, variables: Map<String, Any?>) {
        if (cooldownManager.hasCooldown(playerId, "skill:${skill.id}")) return
        if (isExecuting(playerId, skill.id)) return

        val active = ActiveSkill(skill.id, SkillPhase.CONDITION)
        activeSkills.computeIfAbsent(playerId) { ConcurrentHashMap() }[skill.id] = active

        // 条件检查
        val condScript = skill.conditionScript
        if (condScript != null) {
            val result = Monoceros.api().scripts().invoke(condScript, null, variables + ("playerId" to playerId.toString()))
            if (result != true && result != "true") { cleanup(playerId, skill.id); return }
        }

        // 前摇 -> 执行 -> 后摇 -> 冷却
        active.phase = SkillPhase.WINDUP
        val windupDelay = skill.windupTicks.coerceAtLeast(0)
        submit(delay = windupDelay) {
            if (active.cancelled) { cleanup(playerId, skill.id); return@submit }
            active.phase = SkillPhase.EXECUTE
            try {
                Monoceros.api().scripts().invoke(skill.executeScript, null, variables + ("playerId" to playerId.toString()))
            } catch (e: Exception) {
                DiagnosticLogger.warn("SkillExecutor", "技能执行异常: ${skill.id}", e)
            }
            active.phase = SkillPhase.RECOVERY
            submit(delay = skill.recoveryTicks.coerceAtLeast(0)) {
                active.phase = SkillPhase.COOLDOWN
                if (skill.cooldownMs > 0) cooldownManager.setCooldown(playerId, "skill:${skill.id}", skill.cooldownMs)
                cleanup(playerId, skill.id)
            }
        }
    }

    override fun cancel(playerId: UUID, skillId: String) {
        activeSkills[playerId]?.get(skillId)?.cancelled = true
    }
    override fun isExecuting(playerId: UUID, skillId: String): Boolean = activeSkills[playerId]?.containsKey(skillId) == true
    override fun getCurrentPhase(playerId: UUID, skillId: String): SkillPhase? = activeSkills[playerId]?.get(skillId)?.phase

    private fun cleanup(playerId: UUID, skillId: String) {
        activeSkills[playerId]?.remove(skillId)
        if (activeSkills[playerId]?.isEmpty() == true) activeSkills.remove(playerId)
    }
}
