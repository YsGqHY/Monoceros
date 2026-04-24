package cc.bkhk.monoceros.wireshark

import cc.bkhk.monoceros.impl.util.DiagnosticLogger
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.platform.bukkit.Exchanges

/**
 * MeteorInjector 注入抑制器
 *
 * 根因：
 * TabooLib 的 ProtocolHandler 在 ENABLE 阶段无条件注入 MeteorInjector 到 Netty pipeline。
 * 当 Monoceros 成为宿主后，MeteorInjector.handlePacket() 会通过 OpenContainer 机制
 * 将 packet 事件分发给其他 TabooLib 插件。但如果那些插件没有安装 BukkitNMS，
 * 它们的 OpenAPI.call("packet_send/v1", ...) 找不到 ProtocolHandler 来处理，
 * 返回 OpenResult.failed()。handlePacket() 将 failed 视为"事件被取消"，返回 null，
 * 导致 MeteorInjector 吞掉所有数据包（包括 status/login 阶段），客户端无法连接。
 *
 * 修复：
 * 在 LOAD 阶段（早于 ENABLE）预设 Exchanges 中的 packet_listener 标记，
 * 使 ProtocolHandler.onEnable() 认为已有其他插件注入了 MeteorInjector，跳过注入。
 * Monoceros 不再成为 MeteorInjector 宿主，避免触发上述 packet 吞包问题。
 */
object MeteorInjectorSuppressor {

    private const val MODULE = "Wireshark"
    private const val PACKET_LISTENER_KEY = "packet_listener/v1"
    private const val SUPPRESSOR_MARKER = "monoceros-suppressed"

    @Awake(LifeCycle.LOAD)
    fun suppress() {
        if (Exchanges.contains(PACKET_LISTENER_KEY)) {
            return
        }
        Exchanges[PACKET_LISTENER_KEY] = SUPPRESSOR_MARKER
        DiagnosticLogger.info(MODULE, "已预设 MeteorInjector 抑制标记，阻止 Monoceros 成为 packet 宿主")
    }

    /**
     * ACTIVE 阶段清理：如果标记仍是抑制器设置的值（没有被其他插件覆盖），
     * 说明没有任何 TabooLib 插件真正注入了 MeteorInjector，清除占位标记。
     */
    @Awake(LifeCycle.ACTIVE)
    fun cleanup() {
        try {
            val current = Exchanges.get<Any?>(PACKET_LISTENER_KEY)
            if (current == SUPPRESSOR_MARKER) {
                Exchanges[PACKET_LISTENER_KEY] = null
                DiagnosticLogger.info(MODULE, "无其他 TabooLib 插件注入 MeteorInjector，已清除抑制标记")
            }
        } catch (_: Exception) {
            // 静默忽略
        }
    }
}
