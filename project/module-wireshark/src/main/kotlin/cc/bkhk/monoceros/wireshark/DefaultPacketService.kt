package cc.bkhk.monoceros.wireshark

import cc.bkhk.monoceros.api.wireshark.PacketHandler
import cc.bkhk.monoceros.api.wireshark.PacketService
import cc.bkhk.monoceros.api.wireshark.PacketSession
import cc.bkhk.monoceros.api.wireshark.PacketTapDefinition
import cc.bkhk.monoceros.api.registry.Registry
import cc.bkhk.monoceros.impl.util.DiagnosticLogger
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 数据包服务默认实现
 *
 * 管理 tap 注册表和玩家会话。
 */
class DefaultPacketService(
    internal val handlerRegistry: Registry<PacketHandler>,
) : PacketService {

    private companion object {
        const val MODULE = "PacketService"
    }

    /** 已注册的 tap 定义 */
    internal val taps = ConcurrentHashMap<String, PacketTapDefinition>()

    /** 玩家会话 */
    internal val sessions = ConcurrentHashMap<UUID, DefaultPacketSession>()

    /** 全局高风险能力开关 */
    internal var allowIntercept: Boolean = false
    internal var allowRewrite: Boolean = false

    override fun register(definition: PacketTapDefinition) {
        taps[definition.id] = definition
        DiagnosticLogger.info(MODULE, "注册 packet tap: ${definition.id}")
    }

    override fun unregister(id: String) {
        taps.remove(id)
        // 从所有会话中移除该 tap
        sessions.values.forEach { it.enabledTapIds.remove(id) }
        DiagnosticLogger.info(MODULE, "注销 packet tap: $id")
    }

    override fun openSession(player: Player): PacketSession {
        val session = sessions.computeIfAbsent(player.uniqueId) { DefaultPacketSession(it) }
        DiagnosticLogger.info(MODULE, "打开 packet 会话: ${player.name}")
        return session
    }

    override fun closeSession(playerId: UUID) {
        sessions.remove(playerId)
    }

    override fun getSession(playerId: UUID): PacketSession? {
        return sessions[playerId]
    }

    /** 关闭所有会话 */
    fun closeAllSessions() {
        sessions.clear()
    }
}
