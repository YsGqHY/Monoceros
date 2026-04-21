package cc.bkhk.monoceros.api.wireshark

/**
 * 数据包强类型处理器
 *
 * 适用于高频包快速过滤、需要强类型 packet 解析、复杂 rewrite 逻辑。
 * 通过 [PacketRoute.Handler] 路由到此接口。
 */
interface PacketHandler {
    val id: String
    fun handle(context: PacketContext): Any?
}
