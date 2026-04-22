package cc.bkhk.monoceros.api.schedule

/**
 * 调度类型
 */
enum class ScheduleType {
    DELAY,
    PERIODIC,
    CRON,
    CONDITIONAL,
}

/**
 * 调度运行状态
 */
enum class ScheduleState {
    WAITING,
    RUNNING,
    PAUSED,
    TERMINATED,
}

/**
 * 调度发送者选择器类型
 */
enum class SenderSelectorType {
    CONSOLE,
    ONLINE_PLAYER,
    PLAYER,
    WORLD,
    RANGE,
    AREA,
}

/**
 * 调度发送者选择器定义
 */
data class SenderSelectorDefinition(
    val type: SenderSelectorType,
    val value: String? = null,
    val world: String? = null,
    val origin: String? = null,
    val radius: Double? = null,
    val minX: Double? = null,
    val minY: Double? = null,
    val minZ: Double? = null,
    val maxX: Double? = null,
    val maxY: Double? = null,
    val maxZ: Double? = null,
)

/**
 * 调度定义
 */
data class ScheduleDefinition(
    val id: String,
    val type: ScheduleType,
    val delayTicks: Long = 0,
    val periodTicks: Long = -1,
    val cron: String? = null,
    val async: Boolean = false,
    val autoStart: Boolean = false,
    val prototype: Boolean = false,
    val maxRuns: Int = -1,
    val maxDurationMs: Long = -1,
    val senderSelectors: List<SenderSelectorDefinition> = emptyList(),
    val route: ScheduleRoute,
    val onStartScript: String? = null,
    val onStopScript: String? = null,
    val onPauseScript: String? = null,
    val onResumeScript: String? = null,
    val variables: Map<String, Any?> = emptyMap(),
)
