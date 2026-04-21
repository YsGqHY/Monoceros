package cc.bkhk.monoceros.schedule

import cc.bkhk.monoceros.api.schedule.ScheduleDefinition
import cc.bkhk.monoceros.api.schedule.ScheduleHandle
import cc.bkhk.monoceros.api.schedule.ScheduleState
import cc.bkhk.monoceros.api.schedule.ScheduleType
import cc.bkhk.monoceros.impl.util.DiagnosticLogger
import taboolib.common.platform.function.submit

/**
 * 调度 Runner 工厂
 *
 * 按调度类型选择对应的 runner 启动任务。
 */
class ScheduleRunnerFactory(
    private val executor: ScheduleExecutor,
) {

    private companion object {
        const val MODULE = "ScheduleRunner"
    }

    /**
     * 启动一个调度任务
     */
    fun start(definition: ScheduleDefinition, variables: Map<String, Any?>): ScheduleHandle {
        return when (definition.type) {
            ScheduleType.DELAY -> startDelay(definition, variables)
            ScheduleType.PERIODIC -> startPeriodic(definition, variables)
            ScheduleType.CRON -> startCron(definition, variables)
            ScheduleType.CONDITIONAL -> startConditional(definition, variables)
        }
    }

    /** 延迟任务 */
    private fun startDelay(definition: ScheduleDefinition, variables: Map<String, Any?>): ScheduleHandle {
        lateinit var handle: DefaultScheduleHandle
        val task = submit(async = definition.async, delay = definition.delayTicks) {
            if (handle.state == ScheduleState.TERMINATED) return@submit
            handle.markRunning()
            handle.incrementRunCount()
            executor.executeLifecycleScript(definition.onStartScript, handle)
            executor.execute(definition, handle, variables)
            handle.stop()
        }
        handle = DefaultScheduleHandle(definition.id) { task.cancel() }
        handle.markRunning()
        return handle
    }

    /** 周期任务 */
    private fun startPeriodic(definition: ScheduleDefinition, variables: Map<String, Any?>): ScheduleHandle {
        lateinit var handle: DefaultScheduleHandle
        val task = submit(async = definition.async, delay = definition.delayTicks, period = definition.periodTicks) {
            when (handle.state) {
                ScheduleState.TERMINATED -> {
                    cancel()
                    return@submit
                }
                ScheduleState.PAUSED -> return@submit
                else -> {}
            }

            handle.markRunning()
            val count = handle.incrementRunCount()

            // 检查最大运行次数
            if (definition.maxRuns > 0 && count > definition.maxRuns) {
                executor.executeLifecycleScript(definition.onStopScript, handle)
                handle.stop()
                cancel()
                return@submit
            }

            // 检查最大运行时长
            if (definition.maxDurationMs > 0 && handle.elapsedMs() > definition.maxDurationMs) {
                executor.executeLifecycleScript(definition.onStopScript, handle)
                handle.stop()
                cancel()
                return@submit
            }

            try {
                executor.execute(definition, handle, variables)
            } catch (e: Exception) {
                DiagnosticLogger.warn(MODULE, "周期任务执行异常: ${definition.id}", e)
            }
        }
        handle = DefaultScheduleHandle(definition.id) { task.cancel() }
        handle.markRunning()
        executor.executeLifecycleScript(definition.onStartScript, handle)
        return handle
    }

    /** Cron 任务（基于周期轮询模拟） */
    private fun startCron(definition: ScheduleDefinition, variables: Map<String, Any?>): ScheduleHandle {
        val cronExpr = definition.cron ?: error("Cron 表达式不能为空: ${definition.id}")
        val cronMatcher = SimpleCronMatcher(cronExpr)

        lateinit var handle: DefaultScheduleHandle
        // 每秒检查一次 cron 是否匹配
        val task = submit(async = definition.async, delay = 20L, period = 20L) {
            when (handle.state) {
                ScheduleState.TERMINATED -> {
                    cancel()
                    return@submit
                }
                ScheduleState.PAUSED -> return@submit
                else -> {}
            }

            if (!cronMatcher.matches()) return@submit

            handle.markRunning()
            val count = handle.incrementRunCount()

            if (definition.maxRuns > 0 && count > definition.maxRuns) {
                executor.executeLifecycleScript(definition.onStopScript, handle)
                handle.stop()
                cancel()
                return@submit
            }

            try {
                executor.execute(definition, handle, variables)
            } catch (e: Exception) {
                DiagnosticLogger.warn(MODULE, "Cron 任务执行异常: ${definition.id}", e)
            }
        }
        handle = DefaultScheduleHandle(definition.id) { task.cancel() }
        handle.markRunning()
        executor.executeLifecycleScript(definition.onStartScript, handle)
        return handle
    }

    /** 条件任务（基于周期轮询检查条件） */
    private fun startConditional(definition: ScheduleDefinition, variables: Map<String, Any?>): ScheduleHandle {
        lateinit var handle: DefaultScheduleHandle
        val checkPeriod = if (definition.periodTicks > 0) definition.periodTicks else 20L
        val task = submit(async = definition.async, delay = definition.delayTicks, period = checkPeriod) {
            when (handle.state) {
                ScheduleState.TERMINATED -> {
                    cancel()
                    return@submit
                }
                ScheduleState.PAUSED -> return@submit
                else -> {}
            }

            // 条件任务每次轮询都执行，由脚本/handler 内部决定是否真正触发
            handle.markRunning()
            handle.incrementRunCount()

            try {
                executor.execute(definition, handle, variables)
            } catch (e: Exception) {
                DiagnosticLogger.warn(MODULE, "条件任务执行异常: ${definition.id}", e)
            }
        }
        handle = DefaultScheduleHandle(definition.id) { task.cancel() }
        handle.markRunning()
        executor.executeLifecycleScript(definition.onStartScript, handle)
        return handle
    }
}
