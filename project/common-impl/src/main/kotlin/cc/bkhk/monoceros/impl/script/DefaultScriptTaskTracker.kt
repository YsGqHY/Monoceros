package cc.bkhk.monoceros.impl.script

import cc.bkhk.monoceros.api.script.ScriptRuntimeTask
import cc.bkhk.monoceros.api.script.ScriptTaskTracker
import java.util.concurrent.ConcurrentHashMap

/**
 * 脚本任务跟踪器默认实现
 *
 * 管理所有活跃的脚本运行时任务，支持按 ID 和定义 ID 查询与停止。
 */
class DefaultScriptTaskTracker : ScriptTaskTracker {

    private val tasks = ConcurrentHashMap<Long, ScriptRuntimeTask>()

    override fun track(task: ScriptRuntimeTask) {
        tasks[task.taskId] = task
    }

    override fun get(taskId: Long): ScriptRuntimeTask? {
        return tasks[taskId]
    }

    override fun getByDefinition(definitionId: String): Collection<ScriptRuntimeTask> {
        return tasks.values.filter { it.definitionId == definitionId }
    }

    override fun stopByDefinition(definitionId: String): Int {
        var count = 0
        tasks.values.filter { it.definitionId == definitionId && !it.isDone() }.forEach {
            it.stop()
            count++
        }
        return count
    }

    override fun stop(taskId: Long): Boolean {
        val task = tasks[taskId] ?: return false
        if (task.isDone()) return false
        task.stop()
        return true
    }

    override fun activeCount(): Int {
        return tasks.values.count { !it.isDone() }
    }

    override fun purgeCompleted(): Int {
        val completed = tasks.entries.filter { it.value.isDone() }
        completed.forEach { tasks.remove(it.key) }
        return completed.size
    }
}
