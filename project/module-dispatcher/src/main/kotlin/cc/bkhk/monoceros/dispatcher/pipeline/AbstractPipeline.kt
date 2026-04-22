package cc.bkhk.monoceros.dispatcher.pipeline

import cc.bkhk.monoceros.api.dispatcher.pipeline.Pipeline
import cc.bkhk.monoceros.api.dispatcher.pipeline.PipelineContext
import org.bukkit.event.Event

/**
 * Pipeline 抽象基类
 *
 * 提供事件类型关联和默认空实现。
 */
abstract class AbstractPipeline<T : Event>(
    /** 关联的事件类 */
    val eventClass: Class<T>,
) : Pipeline {

    override val priority: Int = 8

    /** 类型安全的事件访问 */
    @Suppress("UNCHECKED_CAST")
    protected fun castEvent(context: PipelineContext): T? {
        return if (eventClass.isInstance(context.event)) context.event as T else null
    }
}
