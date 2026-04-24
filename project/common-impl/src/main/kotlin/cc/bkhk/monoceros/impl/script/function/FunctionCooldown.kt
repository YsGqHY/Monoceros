package cc.bkhk.monoceros.impl.script.function

import cc.bkhk.monoceros.impl.script.FluxonChecker
import cc.bkhk.monoceros.impl.script.relocate.FluxonRelocate
import org.tabooproject.fluxon.runtime.FluxonRuntime
import org.tabooproject.fluxon.runtime.FunctionSignature
import org.tabooproject.fluxon.runtime.Type
import org.tabooproject.fluxon.runtime.java.Export
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.function.submitAsync
import java.util.concurrent.ConcurrentHashMap

/**
 * 冷却/频率限制函数，基于 ConcurrentHashMap 实现。
 *
 * ```fluxon
 * // 设置 3 秒冷却，返回 true 表示成功（不在冷却中）
 * if cooldown("Steve:skill:fireball", 3000) {
 *     print("技能释放!")
 * } else {
 *     remain = getCooldown("Steve:skill:fireball")
 *     print("冷却中，剩余 ${&remain} ms")
 * }
 *
 * // 检查是否在冷却中
 * if hasCooldown("Steve:skill:fireball") {
 *     print("还在冷却")
 * }
 *
 * // 移除冷却
 * removeCooldown("Steve:skill:fireball")
 * ```
 */
@FluxonRelocate
object FunctionCooldown {

    /** key -> 冷却到期时间戳（毫秒） */
    private val cooldowns = ConcurrentHashMap<String, Long>()

    /** 过期条目清理间隔（tick），约 5 分钟 */
    private const val PURGE_INTERVAL = 6000L

    @Awake(LifeCycle.ENABLE)
    private fun init() {
        if (!FluxonChecker.isReady()) return
        with(FluxonRuntime.getInstance()) {
            exportRegistry.registerClass(CooldownApi::class.java)

            // cooldown(key, durationMs) -> boolean
            registerFunction("cooldown", FunctionSignature.returnsObject().params(Type.STRING, Type.OBJECT)) { ctx ->
                val key = ctx.getString(0)
                val duration = (ctx.getRef(1) as? Number)?.toLong() ?: 0L
                ctx.setReturnRef(CooldownApi.set(key, duration))
            }
            exportRegisteredFunction("cooldown")

            // hasCooldown(key) -> boolean
            registerFunction("hasCooldown", FunctionSignature.returnsObject().params(Type.STRING)) { ctx ->
                ctx.setReturnRef(CooldownApi.has(ctx.getString(0)))
            }
            exportRegisteredFunction("hasCooldown")

            // getCooldown(key) -> long (剩余毫秒)
            registerFunction("getCooldown", FunctionSignature.returnsObject().params(Type.STRING)) { ctx ->
                ctx.setReturnRef(CooldownApi.remaining(ctx.getString(0)))
            }
            exportRegisteredFunction("getCooldown")

            // removeCooldown(key)
            registerFunction("removeCooldown", FunctionSignature.returnsVoid().params(Type.STRING)) { ctx ->
                CooldownApi.remove(ctx.getString(0))
            }
            exportRegisteredFunction("removeCooldown")

            // clearCooldowns()
            registerFunction("clearCooldowns", FunctionSignature.returnsVoid().noParams()) { _ ->
                CooldownApi.clear()
            }
            exportRegisteredFunction("clearCooldowns")
        }
    }

    /** 启动定期清理任务 */
    @Awake(LifeCycle.ACTIVE)
    private fun startPurgeTask() {
        submitAsync(delay = PURGE_INTERVAL, period = PURGE_INTERVAL) {
            val now = System.currentTimeMillis()
            cooldowns.entries.removeIf { it.value <= now }
        }
    }

    /** 插件卸载时清空 */
    @Awake(LifeCycle.DISABLE)
    private fun cleanup() {
        cooldowns.clear()
    }

    object CooldownApi {

        /**
         * 设置冷却。
         * @return true 表示成功设置（之前不在冷却中），false 表示仍在冷却中
         */
        @Export(shared = true)
        fun set(key: String, durationMs: Long): Boolean {
            val now = System.currentTimeMillis()
            val existing = cooldowns[key]
            if (existing != null && existing > now) {
                return false
            }
            cooldowns[key] = now + durationMs
            return true
        }

        /** 检查是否在冷却中 */
        @Export(shared = true)
        fun has(key: String): Boolean {
            val expiry = cooldowns[key] ?: return false
            if (expiry <= System.currentTimeMillis()) {
                cooldowns.remove(key)
                return false
            }
            return true
        }

        /** 获取剩余冷却毫秒数，不在冷却中返回 0 */
        @Export(shared = true)
        fun remaining(key: String): Long {
            val expiry = cooldowns[key] ?: return 0L
            val remain = expiry - System.currentTimeMillis()
            if (remain <= 0) {
                cooldowns.remove(key)
                return 0L
            }
            return remain
        }

        /** 移除冷却 */
        @Export(shared = true)
        fun remove(key: String) {
            cooldowns.remove(key)
        }

        /** 清空所有冷却 */
        @Export(shared = true)
        fun clear() {
            cooldowns.clear()
        }
    }
}
