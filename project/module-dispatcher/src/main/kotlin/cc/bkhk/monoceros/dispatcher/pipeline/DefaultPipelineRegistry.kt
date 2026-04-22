package cc.bkhk.monoceros.dispatcher.pipeline

import cc.bkhk.monoceros.api.dispatcher.pipeline.Pipeline
import cc.bkhk.monoceros.api.dispatcher.pipeline.PipelineRegistry
import cc.bkhk.monoceros.impl.util.DiagnosticLogger
import java.util.concurrent.ConcurrentHashMap

/**
 * Pipeline 注册中心默认实现
 */
object DefaultPipelineRegistry : PipelineRegistry {

    private const val MODULE = "PipelineRegistry"

    /** 事件名 -> Pipeline 列表 */
    private val registry = ConcurrentHashMap<String, MutableList<Pipeline>>()

    override fun register(eventName: String, pipeline: Pipeline) {
        val key = eventName.lowercase()
        registry.computeIfAbsent(key) { mutableListOf() }.add(pipeline)
        // 按 priority 倒序排列
        registry[key]?.sortByDescending { it.priority }
        DiagnosticLogger.debug(MODULE, "注册 Pipeline: ${pipeline.javaClass.simpleName} -> $eventName (priority=${pipeline.priority})")
    }

    override fun getPipelines(eventName: String): List<Pipeline> {
        return registry[eventName.lowercase()]?.toList() ?: emptyList()
    }

    override fun registeredEvents(): Set<String> {
        return registry.keys.toSet()
    }

    /** 清空所有注册 */
    fun clear() {
        registry.clear()
    }
}
