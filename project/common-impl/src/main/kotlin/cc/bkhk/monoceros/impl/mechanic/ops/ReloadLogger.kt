package cc.bkhk.monoceros.impl.mechanic.ops

import cc.bkhk.monoceros.impl.util.DiagnosticLogger
import cc.bkhk.monoceros.impl.util.TimingUtil
import java.text.SimpleDateFormat
import java.util.Date
import java.util.concurrent.ConcurrentLinkedQueue

/** 重载日志记录器 */
object ReloadLogger {
    data class ReloadEntry(
        val timestamp: Long = System.currentTimeMillis(),
        val serviceId: String,
        val loaded: Int,
        val costMs: Double,
        val success: Boolean,
        val error: String? = null,
    )

    private val history = ConcurrentLinkedQueue<ReloadEntry>()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    private const val MAX_HISTORY = 100

    fun record(serviceId: String, loaded: Int, costMs: Double, success: Boolean, error: String? = null) {
        val entry = ReloadEntry(serviceId = serviceId, loaded = loaded, costMs = costMs, success = success, error = error)
        history.add(entry)
        while (history.size > MAX_HISTORY) history.poll()
        if (success) {
            DiagnosticLogger.info("Reload", "[$serviceId] 重载完成: $loaded 个, 耗时 ${TimingUtil.formatMs(costMs)}")
        } else {
            DiagnosticLogger.warn("Reload", "[$serviceId] 重载失败: ${error ?: "未知错误"}")
        }
    }

    fun getHistory(): List<ReloadEntry> = history.toList()

    fun formatHistory(): List<String> = history.map { entry ->
        val time = dateFormat.format(Date(entry.timestamp))
        val status = if (entry.success) "OK" else "FAIL"
        "[$time] ${entry.serviceId}: $status, ${entry.loaded} loaded, ${TimingUtil.formatMs(entry.costMs)}${if (entry.error != null) " - ${entry.error}" else ""}"
    }

    fun clear() { history.clear() }
}
