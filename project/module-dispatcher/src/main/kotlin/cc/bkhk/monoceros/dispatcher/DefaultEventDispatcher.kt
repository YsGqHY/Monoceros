package cc.bkhk.monoceros.dispatcher

import cc.bkhk.monoceros.Monoceros
import cc.bkhk.monoceros.api.dispatcher.DispatcherContext
import cc.bkhk.monoceros.api.dispatcher.DispatcherDefinition
import cc.bkhk.monoceros.api.dispatcher.DispatcherHandler
import cc.bkhk.monoceros.api.dispatcher.DispatcherRoute
import cc.bkhk.monoceros.api.dispatcher.EventDispatcher
import cc.bkhk.monoceros.api.dispatcher.pipeline.Pipeline
import cc.bkhk.monoceros.api.dispatcher.pipeline.PipelineContext
import cc.bkhk.monoceros.api.registry.Registry
import cc.bkhk.monoceros.dispatcher.pipeline.DefaultPipelineRegistry
import cc.bkhk.monoceros.dispatcher.pipeline.ListPipeline
import cc.bkhk.monoceros.dispatcher.pipeline.ReflexPlayerPipeline
import cc.bkhk.monoceros.impl.util.DiagnosticLogger
import org.bukkit.event.Cancellable
import org.bukkit.event.Event

/**
 * 事件分发器默认实现
 *
 * 执行流程：Pipeline 初始化主体 -> Pipeline 过滤 -> 初始化变量 -> 规则判定 -> 前置脚本 -> 主路由 -> 后置脚本 -> Pipeline 后置 -> 写回状态
 */
class DefaultEventDispatcher(
    override val definition: DispatcherDefinition,
    private val handlerRegistry: Registry<DispatcherHandler>,
) : EventDispatcher {

    private companion object {
        const val MODULE = "Dispatcher"
        /** 兜底 Pipeline：通过反射提取 Player */
        val REFLEX_FALLBACK = ReflexPlayerPipeline(playerRequired = false)
    }

    override fun accept(event: Event) {
        // 忽略已取消事件
        if (definition.ignoreCancelled && event is Cancellable && event.isCancelled) return

        // 创建 Pipeline 上下文
        val pipelineCtx = PipelineContext(event)

        // 获取事件关联的 Pipeline 链
        val pipeline = resolvePipeline(definition.eventKey)

        // 1. 初始化主体
        pipeline.initPrincipal(pipelineCtx)
        // 兜底：如果 Pipeline 未设置 principal，尝试反射提取
        if (pipelineCtx.principal == null) {
            REFLEX_FALLBACK.initPrincipal(pipelineCtx)
        }

        // 2. Pipeline 过滤
        pipeline.filter(pipelineCtx)
        if (pipelineCtx.isCancelled) {
            if (event is Cancellable) event.isCancelled = true
            return
        }
        if (pipelineCtx.isFiltered) return

        // 3. 初始化变量
        pipeline.initVariables(pipelineCtx)

        // 构造 DispatcherContext
        val sender = pipelineCtx.player
        val context = DispatcherContext(
            definitionId = definition.id,
            event = event,
            sender = sender,
        )

        // 注入默认变量
        context.variables["event"] = event
        context.variables["eventName"] = event.eventName
        context.variables["dispatcherId"] = definition.id
        context.variables["dispatcherContext"] = context
        context.variables["timestamp"] = System.currentTimeMillis()
        pipelineCtx.player?.let { context.variables["player"] = it }

        // 合并 Pipeline 注入的变量
        context.variables.putAll(pipelineCtx.variables)

        // 注入定义级变量
        context.variables.putAll(definition.variables)

        // 4. 执行规则判定
        for (rule in definition.rules) {
            try {
                val decision = rule.test(context)
                if (decision.cancelEvent) {
                    if (event is Cancellable) event.isCancelled = true
                    context.cancelled = true
                    return
                }
                if (decision.filtered) {
                    context.filtered = true
                    return
                }
                context.variables.putAll(decision.extraVariables)
            } catch (e: Exception) {
                DiagnosticLogger.warn(MODULE, "规则执行异常: ${definition.id}", e)
            }
        }

        // 5. Pipeline afterFilter（更新冷却等）
        pipeline.afterFilter(pipelineCtx)

        val scriptHandler = Monoceros.api().scripts()

        // 6. 前置脚本
        definition.beforeScript?.let { scriptId ->
            try {
                scriptHandler.invoke(scriptId, sender, context.variables)
            } catch (e: Exception) {
                DiagnosticLogger.warn(MODULE, "前置脚本执行异常: $scriptId", e)
            }
        }

        // 7. 主路由
        try {
            context.routeResult = executeRoute(definition.executeRoute, context)
            pipelineCtx.result = context.routeResult
        } catch (e: Exception) {
            DiagnosticLogger.warn(MODULE, "主路由执行异常: ${definition.id}", e)
        }

        // 8. 后置脚本
        definition.afterScript?.let { scriptId ->
            try {
                scriptHandler.invoke(scriptId, sender, context.variables)
            } catch (e: Exception) {
                DiagnosticLogger.warn(MODULE, "后置脚本执行异常: $scriptId", e)
            }
        }

        // 9. Pipeline 后置处理
        pipeline.postprocess(pipelineCtx)

        // 10. 写回取消状态
        if (context.cancelled && event is Cancellable) {
            event.isCancelled = true
        }
    }

    /** 解析事件关联的 Pipeline */
    private fun resolvePipeline(eventKey: String): Pipeline {
        val pipelines = DefaultPipelineRegistry.getPipelines(eventKey)
        return if (pipelines.isEmpty()) {
            // 无注册 Pipeline，使用空管道
            object : Pipeline {}
        } else if (pipelines.size == 1) {
            pipelines[0]
        } else {
            ListPipeline(pipelines)
        }
    }

    /** 执行主路由 */
    private fun executeRoute(route: DispatcherRoute, context: DispatcherContext): Any? {
        return when (route) {
            is DispatcherRoute.Script -> {
                Monoceros.api().scripts().invoke(route.scriptId, context.sender, context.variables)
            }
            is DispatcherRoute.ActionWorkflow -> {
                Monoceros.api().actionWorkflow().execute(
                    route.workflowId,
                    context.sender,
                    context.variables,
                )
            }
            is DispatcherRoute.Handler -> {
                val handler = handlerRegistry.get(route.handlerId)
                if (handler == null) {
                    DiagnosticLogger.warn(MODULE, "Handler 未注册: ${route.handlerId}")
                    null
                } else {
                    handler.handle(context)
                }
            }
        }
    }
}
