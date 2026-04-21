package cc.bkhk.monoceros.api.wireshark

import org.bukkit.entity.Player
import java.util.UUID

/**
 * 数据包服务
 */
interface PacketService {
    fun register(definition: PacketTapDefinition)
    fun unregister(id: String)
    fun openSession(player: Player): PacketSession
    fun closeSession(playerId: UUID)
    fun getSession(playerId: UUID): PacketSession?
}

/**
 * 玩家级数据包会话
 */
interface PacketSession {
    val playerId: UUID
    fun enableTap(id: String)
    fun disableTap(id: String)
    fun trace(limit: Int = 100): List<PacketTraceRecord>
}
