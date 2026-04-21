package cc.bkhk.monoceros.api.schedule

/**
 * 调度服务
 */
interface ScheduleService {
    fun register(definition: ScheduleDefinition): ScheduleDefinition
    fun unregister(id: String): ScheduleDefinition?
    fun start(id: String, variables: Map<String, Any?> = emptyMap()): ScheduleHandle
    fun pause(id: String, runtimeId: String = "*"): Int
    fun resume(id: String, runtimeId: String = "*"): Int
    fun stop(id: String, runtimeId: String = "*"): Int
    fun getHandles(id: String): Collection<ScheduleHandle>
}
