package cc.bkhk.monoceros.api.wireshark

/**
 * 数据包方向
 */
enum class PacketDirection {
    RECEIVE,
    SEND,
}

/**
 * 数据包匹配规格
 */
data class PacketMatcherSpec(
    val type: String,
    val value: String,
)

/**
 * 数据包过滤规格
 */
data class PacketFilterSpec(
    val type: String,
    val value: String,
)

/**
 * 数据包覆写规格
 */
data class PacketRewriteSpec(
    val type: String,
    val config: Map<String, Any?> = emptyMap(),
)

/**
 * 数据包追踪记录
 */
data class PacketTraceRecord(
    val tapId: String,
    val direction: PacketDirection,
    val packetName: String,
    val timestamp: Long,
    val variables: Map<String, Any?> = emptyMap(),
)

/**
 * 数据包监听定义
 */
data class PacketTapDefinition(
    val id: String,
    val direction: Set<PacketDirection>,
    val matcher: PacketMatcherSpec? = null,
    val filters: List<PacketFilterSpec> = emptyList(),
    val tracking: Boolean = false,
    val parse: Boolean = false,
    val intercept: Boolean = false,
    val rewrite: PacketRewriteSpec? = null,
    val route: PacketRoute? = null,
)
