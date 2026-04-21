package cc.bkhk.monoceros.impl.script

import cc.bkhk.monoceros.api.script.ScriptRuntimeTask
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicLong

/**
 * 脚本运行时任务默认实现
 *
 * 通过 CompletableFuture 跟踪执行状态，支持超时控制。
 */
class DefaultScriptTask(
    override val definitionId: String,
    override val async: Boolean,
    override val variables: Map<String, Any?>,
) : ScriptRuntimeTask {

    companion object {
        private val idGenerator = AtomicLong(0)
    }

    override val taskId: Long = idGenerator.incrementAndGet()
    override val startedAt: Long = System.currentTimeMillis()

    /** 任务执行 Future，外部可通过此对象获取结果或取消 */
    val future: CompletableFuture<Any?> = CompletableFuture()

    override fun isDone(): Boolean = future.isDone

    override fun stop() {
        future.cancel(true)
    }

    /** 标记任务完成 */
    fun complete(result: Any?) {
        future.complete(result)
    }

    /** 标记任务异常完成 */
    fun completeExceptionally(throwable: Throwable) {
        future.completeExceptionally(throwable)
    }
}
