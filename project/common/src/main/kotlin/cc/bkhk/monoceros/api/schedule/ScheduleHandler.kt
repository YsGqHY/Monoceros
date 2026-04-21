package cc.bkhk.monoceros.api.schedule

/**
 * 调度强类型处理器
 *
 * 适用于高频运维任务、复杂状态机等需要强类型逻辑的场景。
 * 通过 [ScheduleRoute.Handler] 路由到此接口。
 */
interface ScheduleHandler {
    val id: String
    fun execute(context: ScheduleContext): Any?
}

/**
 * 调度执行上下文
 */
data class ScheduleContext(
    val definitionId: String,
    val runtimeId: String,
    val runCount: Int,
    val startedAt: Long,
    val variables: Map<String, Any?>,
)
