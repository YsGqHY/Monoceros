package cc.bkhk.monoceros.api.wireshark

import org.bukkit.entity.Player

/**
 * 数据包运行时上下文
 */
data class PacketContext(
    val tapId: String,
    val direction: PacketDirection,
    val player: Player?,
    val packet: Any,
    val timestamp: Long,
    val variables: MutableMap<String, Any?> = LinkedHashMap(),
    var cancelled: Boolean = false,
    var rewrittenPacket: Any? = null,
)
