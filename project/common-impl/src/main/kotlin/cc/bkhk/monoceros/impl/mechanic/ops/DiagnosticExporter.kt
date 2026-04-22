package cc.bkhk.monoceros.impl.mechanic.ops

import cc.bkhk.monoceros.Monoceros
import cc.bkhk.monoceros.api.mechanic.region.RegionService
import cc.bkhk.monoceros.api.mechanic.session.SessionService
import cc.bkhk.monoceros.impl.service.ReloadService
import taboolib.common.platform.PlatformFactory
import taboolib.common.platform.function.getDataFolder
import taboolib.common.platform.function.pluginVersion
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

/** 诊断导出增强 */
object DiagnosticExporter {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss")

    /** 导出完整运行态快照到文件 */
    fun exportSnapshot(): File {
        val timestamp = dateFormat.format(Date())
        val dir = File(getDataFolder(), "debug/snapshots")
        dir.mkdirs()
        val file = File(dir, "snapshot-$timestamp.txt")

        val sb = StringBuilder()
        sb.appendLine("=== Monoceros Runtime Snapshot ===")
        sb.appendLine("Version: $pluginVersion")
        sb.appendLine("Time: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date())}")
        sb.appendLine()

        // 服务状态
        sb.appendLine("--- Services ---")
        ReloadService.serviceIds().forEach { sb.appendLine("  $it: registered") }
        sb.appendLine()

        // 脚本缓存
        sb.appendLine("--- Script Cache ---")
        try {
            val stats = Monoceros.api().scripts().cacheStats()
            sb.appendLine("  cacheSize: ${stats.cacheSize}")
            sb.appendLine("  compilations: ${stats.totalCompilations}")
            sb.appendLine("  hits: ${stats.invokeHits}")
            sb.appendLine("  misses: ${stats.invokeMisses}")
        } catch (_: Exception) { sb.appendLine("  unavailable") }
        sb.appendLine()

        // 性能统计
        sb.appendLine("--- Performance ---")
        PerformanceTracker.summary().forEach { (module, stats) ->
            sb.appendLine("  $module: $stats")
        }
        sb.appendLine()

        // 区域
        sb.appendLine("--- Regions ---")
        try {
            val regionService = PlatformFactory.getAPIOrNull<RegionService>()
            sb.appendLine("  registered: ${regionService?.all()?.size ?: 0}")
        } catch (_: Exception) { sb.appendLine("  unavailable") }
        sb.appendLine()

        // 会话
        sb.appendLine("--- Sessions ---")
        try {
            val sessionService = PlatformFactory.getAPIOrNull<SessionService>()
            sb.appendLine("  active: ${sessionService?.activeCount() ?: 0}")
        } catch (_: Exception) { sb.appendLine("  unavailable") }
        sb.appendLine()

        // 兼容性
        sb.appendLine("--- Compatibility ---")
        CompatibilityChecker.getIssues().forEach { sb.appendLine("  WARN: $it") }
        if (CompatibilityChecker.getIssues().isEmpty()) sb.appendLine("  OK")
        sb.appendLine()

        // 重载历史
        sb.appendLine("--- Reload History ---")
        ReloadLogger.formatHistory().forEach { sb.appendLine("  $it") }

        file.writeText(sb.toString(), Charsets.UTF_8)
        return file
    }
}
