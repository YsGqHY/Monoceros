package cc.bkhk.monoceros.api.wireshark

/**
 * 数据包过滤器
 */
interface PacketFilter {
    fun test(context: PacketContext): Boolean
}
