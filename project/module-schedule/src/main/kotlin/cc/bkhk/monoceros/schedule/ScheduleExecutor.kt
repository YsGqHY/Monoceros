package cc.bkhk.monoceros.schedule

import cc.bkhk.monoceros.Monoceros
import cc.bkhk.monoceros.api.registry.Registry
import cc.bkhk.monoceros.api.schedule.ScheduleContext
import cc.bkhk.monoceros.api.schedule.ScheduleDefinition
import cc.bkhk.monoceros.api.schedule.ScheduleHandler
import cc.bkhk.monoceros.api.schedule.ScheduleRoute
import cc.bkhk.monoceros.impl.util.DiagnosticLogger

/**
 * 调度执行器
 *
 * 负责将调度任务路由到脚本/工作流/handler 执行。
 */
class ScheduleExecutor(
    private val handlerRegistry: Registry<ScheduleHandler>,
) {

    private companion object {
        const val MODULE = "ScheduleExecutor"
    }

    /**
     * 执行一次调度任务
     */
    fun execute(definition: ScheduleDefinition, handle: DefaultScheduleHandle, runtimeVariables: Map<String, Any?>) {
        // 合并变量：system > runtime > definition
        val variables = LinkedHashMap<String, Any?>()
        variables.putAll(definition.variables)
        variables.putAll(runtimeVariables)
        variables["scheduleId"] = definition.id
        variables["runtimeId"] = handle.runtimeId
        variables["runCount"] = handle.runCount
        variables["startedAt"] = handle.startedAt
        variables["elapsedMs"] = handle.elapsedMs()
        variables["triggerType"] = definition.type.name

        when (val route = definition.route) {
            is ScheduleRoute.Script -> {
                try {
                    Monoceros.api().scripts().invoke(route.scriptId, null, variables)
                } catch (e: Exception) {
                    DiagnosticLogger.warn(MODULE, "调度脚本执行失败: ${route.scriptId}", e)
                }
            }
            is ScheduleRoute.ActionWorkflow -> {
                try {
                    Monoceros.api().actionWorkflow().execute(route.workflowId, null, variables)
                } catch (e: Exception) {
                    DiagnosticLogger.warn(MODULE, "调度工作流执行失败: ${route.workflowId}", e)
                }
            }
            is ScheduleRoute.Handler -> {
                val handler = handlerRegistry.get(route.handlerId)
                if (handler == null) {
                    DiagnosticLogger.warn(MODULE, "ScheduleHandler 未注册: ${route.handlerId}")
                } else {
                    try {
                        val context = ScheduleContext(
                            definitionId = definition.id,
                            runtimeId = handle.runtimeId,
                            runCount = handle.runCount,
                            startedAt = handle.startedAt,
                            variables = variables,
                        )
                        handler.execute(context)
                    } catch (e: Exception) {
                        DiagnosticLogger.warn(MODULE, "ScheduleHandler 执行失败: ${route.handlerId}", e)
                    }
                }
            }
        }
    }

    /**
     * 执行生命周期脚本（onStart/onStop/onPause/onResume）
     */
    fun executeLifecycleScript(scriptId: String?, handle: DefaultScheduleHandle) {
        if (scriptId == null) return
        try {
            Monoceros.api().scripts().invoke(
                scriptId,
                null,
                mapOf(
                    "scheduleId" to handle.definitionId,
                    "runtimeId" to handle.runtimeId,
                ),
            )
        } catch (e: Exception) {
            DiagnosticLogger.warn(MODULE, "生命周期脚本执行失败: $scriptId", e)
        }
    }
}
