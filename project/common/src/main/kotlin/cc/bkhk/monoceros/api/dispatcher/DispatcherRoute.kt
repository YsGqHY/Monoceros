package cc.bkhk.monoceros.api.dispatcher

/**
 * 分发器路由目标
 */
sealed interface DispatcherRoute {
    data class Script(val scriptId: String) : DispatcherRoute
    data class ActionWorkflow(val workflowId: String) : DispatcherRoute
    data class Handler(val handlerId: String) : DispatcherRoute
}
