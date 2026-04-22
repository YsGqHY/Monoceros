package cc.bkhk.monoceros.impl.mechanic.ops

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/** 性能统计追踪器 */
object PerformanceTracker {
    private data class ModuleStats(
        val invocations: AtomicLong = AtomicLong(0),
        val totalNanos: AtomicLong = AtomicLong(0),
        val errors: AtomicLong = AtomicLong(0),
    )
    private val stats = ConcurrentHashMap<String, ModuleStats>()

    fun record(module: String, nanos: Long) {
        val s = stats.computeIfAbsent(module) { ModuleStats() }
        s.invocations.incrementAndGet()
        s.totalNanos.addAndGet(nanos)
    }
    fun recordError(module: String) { stats.computeIfAbsent(module) { ModuleStats() }.errors.incrementAndGet() }

    fun summary(): Map<String, Map<String, Any>> {
        return stats.entries.associate { (module, s) ->
            val invocations = s.invocations.get()
            val avgMs = if (invocations > 0) s.totalNanos.get() / invocations / 1_000_000.0 else 0.0
            module to mapOf(
                "invocations" to invocations,
                "totalMs" to s.totalNanos.get() / 1_000_000.0,
                "avgMs" to "%.3f".format(avgMs),
                "errors" to s.errors.get(),
            )
        }
    }
    fun reset() { stats.clear() }
}
