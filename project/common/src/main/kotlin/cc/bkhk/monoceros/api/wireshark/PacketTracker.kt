package cc.bkhk.monoceros.api.wireshark

/**
 * 数据包追踪器
 */
interface PacketTracker {
    fun track(context: PacketContext)
}
