package cc.bkhk.monoceros.api.script

/**
 * 脚本运行时任务
 *
 * 表示一次脚本执行的运行态，与脚本定义（静态资源）分离。
 */
interface ScriptRuntimeTask {

    /** 任务 ID（全局唯一递增） */
    val taskId: Long

    /** 关联的脚本定义 ID */
    val definitionId: String

    /** 任务启动时间戳（毫秒） */
    val startedAt: Long

    /** 是否异步执行 */
    val async: Boolean

    /** 运行时变量快照 */
    val variables: Map<String, Any?>

    /** 任务是否已完成 */
    fun isDone(): Boolean

    /** 停止任务 */
    fun stop()
}
