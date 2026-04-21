package cc.bkhk.monoceros.api.wireshark

/**
 * 数据包匹配器
 */
interface PacketMatcher {
    fun matches(packet: Any): Boolean
}
