package cc.bkhk.monoceros.dispatcher

import cc.bkhk.monoceros.Monoceros
import cc.bkhk.monoceros.api.dispatcher.DispatcherContext
import cc.bkhk.monoceros.api.dispatcher.DispatcherDefinition
import cc.bkhk.monoceros.api.dispatcher.DispatcherHandler
import cc.bkhk.monoceros.api.dispatcher.DispatcherRoute
import cc.bkhk.monoceros.api.dispatcher.EventDispatcher
import cc.bkhk.monoceros.api.registry.Registry
import cc.bkhk.monoceros.impl.util.DiagnosticLogger
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.player.PlayerEvent
import taboolib.common.platform.function.adaptPlayer

/**
 * 事件分发器默认实现
 *
 * 执行流程：构造 Context -> 规则判定 -> 前置脚本 -> 主路由 -> 后置脚本 -> 写回状态
 */
class DefaultEventDispatcher(
    override val definition: DispatcherDefinition,
    private val handlerRegistry: Registry<DispatcherHandler>,
) : EventDispatcher {

    private companion object {
        const val MODULE = "Dispatcher"
    }

    override fun accept(event: Event) {
        // 忽略已取消事件
        if (definition.ignoreCancelled && event is Cancellable && event.isCancelled) return

        // 构造上下文
        val sender = extractSender(event)
        val context = DispatcherContext(
            definitionId = definition.id,
            event = event,
            sender = sender,
        )

        // 注入默认变量
        context.variables["event"] = event
        context.variables["eventName"] = event.eventName
        context.variables["dispatcherId"] = definition.id
        context.variables["timestamp"] = System.currentTimeMillis()
        extractPlayer(event)?.let { context.variables["player"] = it }

        // 注入定义级变量
        context.variables.putAll(definition.variables)

        // 执行规则判定
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

        val scriptHandler = Monoceros.api().scripts()

        // 前置脚本
        definition.beforeScript?.let { scriptId ->
            try {
                scriptHandler.invoke(scriptId, sender, context.variables)
            } catch (e: Exception) {
                DiagnosticLogger.warn(MODULE, "前置脚本执行异常: $scriptId", e)
            }
        }

        // 主路由
        try {
            context.routeResult = executeRoute(definition.executeRoute, context)
        } catch (e: Exception) {
            DiagnosticLogger.warn(MODULE, "主路由执行异常: ${definition.id}", e)
        }

        // 后置脚本
        definition.afterScript?.let { scriptId ->
            try {
                scriptHandler.invoke(scriptId, sender, context.variables)
            } catch (e: Exception) {
                DiagnosticLogger.warn(MODULE, "后置脚本执行异常: $scriptId", e)
            }
        }

        // 写回取消状态
        if (context.cancelled && event is Cancellable) {
            event.isCancelled = true
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

    /** 从事件中提取 ProxyCommandSender */
    private fun extractSender(event: Event) = try {
        if (event is PlayerEvent) adaptPlayer(event.player) else null
    } catch (_: Exception) {
        null
    }

    /** 从事件中提取 Player */
    private fun extractPlayer(event: Event) = try {
        if (event is PlayerEvent) event.player else null
    } catch (_: Exception) {
        null
    }
}
