package cc.bkhk.monoceros.api.script

/**
 * 脚本任务跟踪器
 *
 * 管理所有活跃的脚本运行时任务，与脚本定义注册表分离。
 */
interface ScriptTaskTracker {

    /** 注册一个运行中的任务 */
    fun track(task: ScriptRuntimeTask)

    /** 按任务 ID 查询 */
    fun get(taskId: Long): ScriptRuntimeTask?

    /** 按脚本定义 ID 查询所有关联任务 */
    fun getByDefinition(definitionId: String): Collection<ScriptRuntimeTask>

    /** 停止指定脚本定义的所有任务，返回停止数量 */
    fun stopByDefinition(definitionId: String): Int

    /** 停止指定任务 */
    fun stop(taskId: Long): Boolean

    /** 获取所有活跃任务数量 */
    fun activeCount(): Int

    /** 清除所有已完成的任务记录 */
    fun purgeCompleted(): Int
}
