package cc.bkhk.monoceros.api.schedule

/**
 * 调度运行时句柄
 */
interface ScheduleHandle {
    val definitionId: String
    val runtimeId: String
    val state: ScheduleState
    val startedAt: Long
    val runCount: Int
    fun pause()
    fun resume()
    fun stop()
}
