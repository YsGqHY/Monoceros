package cc.bkhk.monoceros.api.command

/**
 * 命令路由目标
 */
sealed interface CommandRoute {
    data class Script(val scriptId: String) : CommandRoute
    data class ActionWorkflow(val workflowId: String) : CommandRoute
    data class Handler(val handlerId: String) : CommandRoute
}
