package cc.bkhk.monoceros.api.schedule

/**
 * 调度路由目标
 */
sealed interface ScheduleRoute {
    data class Script(val scriptId: String) : ScheduleRoute
    data class ActionWorkflow(val workflowId: String) : ScheduleRoute
    data class Handler(val handlerId: String) : ScheduleRoute
}
