package cc.bkhk.monoceros.api.wireshark

/**
 * 数据包路由目标
 */
sealed interface PacketRoute {
    data class Script(val scriptId: String) : PacketRoute
    data class ActionWorkflow(val workflowId: String) : PacketRoute
    data class Handler(val handlerId: String) : PacketRoute
}
