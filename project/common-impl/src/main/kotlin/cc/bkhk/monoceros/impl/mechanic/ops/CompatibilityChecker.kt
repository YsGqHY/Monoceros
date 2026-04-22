package cc.bkhk.monoceros.impl.mechanic.ops

import cc.bkhk.monoceros.impl.util.DiagnosticLogger
import org.bukkit.Bukkit
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake

/** 兼容性检测器 */
object CompatibilityChecker {
    private val knownIncompatible = listOf(
        "ProtocolLib" to "可能与 Wireshark 数据包监听冲突",
        "ViaVersion" to "跨版本协议转换可能影响 NMS 发包",
        "PacketEvents" to "可能与 Wireshark 数据包监听冲突",
    )

    @Awake(LifeCycle.ENABLE)
    fun check() {
        val issues = mutableListOf<String>()
        for ((plugin, reason) in knownIncompatible) {
            if (Bukkit.getPluginManager().getPlugin(plugin) != null) {
                issues.add("检测到 $plugin: $reason")
            }
        }
        if (issues.isNotEmpty()) {
            DiagnosticLogger.warn("Compatibility", "发现 ${issues.size} 个潜在兼容性问题:")
            issues.forEach { DiagnosticLogger.warn("Compatibility", "  - $it") }
        } else {
            DiagnosticLogger.info("Compatibility", "兼容性检测通过")
        }
    }

    fun getIssues(): List<String> {
        val issues = mutableListOf<String>()
        for ((plugin, reason) in knownIncompatible) {
            if (Bukkit.getPluginManager().getPlugin(plugin) != null) {
                issues.add("$plugin: $reason")
            }
        }
        return issues
    }
}
