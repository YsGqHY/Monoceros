package cc.bkhk.monoceros.dispatcher.pipeline

import cc.bkhk.monoceros.api.dispatcher.pipeline.Pipeline
import cc.bkhk.monoceros.api.dispatcher.pipeline.PipelineContext

/**
 * 组合管道
 *
 * 聚合多个 [Pipeline] 实例，按 [Pipeline.priority] 倒序依次执行。
 * 在 filter 阶段遇到 [PipelineContext.isFilterBaffled] 时中断。
 */
class ListPipeline(
    pipelines: List<Pipeline>,
) : Pipeline {

    private val sorted = pipelines.sortedByDescending { it.priority }

    override val priority: Int = sorted.firstOrNull()?.priority ?: 8

    override fun initPrincipal(context: PipelineContext) {
        for (p in sorted) {
            p.initPrincipal(context)
            if (context.principal != null) break
        }
    }

    override fun initVariables(context: PipelineContext) {
        for (p in sorted) {
            p.initVariables(context)
        }
    }

    override fun filter(context: PipelineContext) {
        for (p in sorted) {
            p.filter(context)
            if (context.isFilterBaffled || context.isCancelled) break
        }
    }

    override fun afterFilter(context: PipelineContext) {
        for (p in sorted) {
            p.afterFilter(context)
        }
    }

    override fun postprocess(context: PipelineContext) {
        for (p in sorted) {
            p.postprocess(context)
        }
    }
}
