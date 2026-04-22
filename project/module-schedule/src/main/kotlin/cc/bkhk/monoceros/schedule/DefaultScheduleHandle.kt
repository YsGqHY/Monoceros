package cc.bkhk.monoceros.schedule

import cc.bkhk.monoceros.api.schedule.ScheduleHandle
import cc.bkhk.monoceros.api.schedule.ScheduleState
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/**
 * 调度运行时句柄默认实现
 *
 * 每个句柄代表一个调度定义的一次运行实例。
 */
class DefaultScheduleHandle(
    override val definitionId: String,
) : ScheduleHandle {

    override val runtimeId: String = UUID.randomUUID().toString().substring(0, 8)
    override val startedAt: Long = System.currentTimeMillis()

    private val _state = AtomicReference(ScheduleState.WAITING)
    private val _runCount = AtomicInteger(0)

    /** 停止回调，由 runner 通过 [setCancelCallback] 设置 */
    @Volatile
    private var cancelCallback: (() -> Unit)? = null

    override val state: ScheduleState get() = _state.get()
    override val runCount: Int get() = _runCount.get()

    override fun pause() {
        _state.compareAndSet(ScheduleState.RUNNING, ScheduleState.PAUSED)
    }

    override fun resume() {
        _state.compareAndSet(ScheduleState.PAUSED, ScheduleState.RUNNING)
    }

    override fun stop() {
        if (_state.getAndSet(ScheduleState.TERMINATED) != ScheduleState.TERMINATED) {
            cancelCallback?.invoke()
        }
    }

    /** 设置取消回调（由 ScheduleRunnerFactory 在 task 创建后调用） */
    fun setCancelCallback(callback: () -> Unit) {
        cancelCallback = callback
        // 如果在设置回调之前已经被 stop，立即执行取消
        if (_state.get() == ScheduleState.TERMINATED) {
            callback()
        }
    }

    /** 标记为运行中 */
    fun markRunning() {
        _state.set(ScheduleState.RUNNING)
    }

    /** 递增运行计数 */
    fun incrementRunCount(): Int = _runCount.incrementAndGet()

    /** 已运行时长（毫秒） */
    fun elapsedMs(): Long = System.currentTimeMillis() - startedAt
}
