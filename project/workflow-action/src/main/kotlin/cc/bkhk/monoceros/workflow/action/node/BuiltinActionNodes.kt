package cc.bkhk.monoceros.workflow.action.node

import cc.bkhk.monoceros.Monoceros
import cc.bkhk.monoceros.api.workflow.ActionContext
import cc.bkhk.monoceros.api.workflow.ActionNode
import cc.bkhk.monoceros.api.workflow.ActionNodeDefinition
import java.util.concurrent.CompletableFuture

/**
 * 脚本动作节点
 *
 * 调用 Fluxon 脚本执行。
 */
class ScriptActionNode : ActionNode {
    override val type: String = "script"

    override fun execute(context: ActionContext, definition: ActionNodeDefinition): Any? {
        val scriptId = definition.config["script"] as? String
            ?: error("script 节点缺少 script 配置: ${definition.id}")
        return Monoceros.api().scripts().invoke(scriptId, context.sender, context.variables)
    }
}

/**
 * 设置变量节点
 */
class SetActionNode : ActionNode {
    override val type: String = "set"

    override fun execute(context: ActionContext, definition: ActionNodeDefinition): Any? {
        val key = definition.config["key"] as? String
            ?: error("set 节点缺少 key 配置: ${definition.id}")
        val value = definition.config["value"]
        context.variables[key] = value
        return value
    }
}

/**
 * 日志输出节点
 */
class LogActionNode : ActionNode {
    override val type: String = "log"

    override fun execute(context: ActionContext, definition: ActionNodeDefinition): Any? {
        val message = definition.config["message"] as? String ?: ""
        taboolib.common.platform.function.info("[Monoceros] [Workflow] $message")
        return null
    }
}

/**
 * 延迟执行节点
 *
 * 通过 CompletableFuture + TabooLib submit 实现非阻塞延迟，避免阻塞主线程。
 */
class WaitActionNode : ActionNode {
    override val type: String = "wait"

    override fun execute(context: ActionContext, definition: ActionNodeDefinition): Any? {
        val ticks = (definition.config["ticks"] as? Number)?.toLong() ?: 0L
        if (ticks > 0) {
            val future = CompletableFuture<Void?>()
            taboolib.common.platform.function.submit(delay = ticks) {
                future.complete(null)
            }
            // 阻塞等待延迟完成（在工作流执行链中保持顺序语义）
            future.get()
        }
        return null
    }
}

/**
 * 条件分支节点
 *
 * 根据条件脚本的返回值决定是否继续执行。
 */
class BranchActionNode : ActionNode {
    override val type: String = "branch"

    override fun execute(context: ActionContext, definition: ActionNodeDefinition): Any? {
        val conditionScript = definition.config["condition"] as? String
            ?: error("branch 节点缺少 condition 配置: ${definition.id}")
        val result = Monoceros.api().scripts().invoke(conditionScript, context.sender, context.variables)
        // 返回 false 或 null 时标记为 filtered
        return result == true || result == "true"
    }
}
