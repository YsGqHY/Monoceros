package cc.bkhk.monoceros.workflow.action.node

import cc.bkhk.monoceros.Monoceros
import cc.bkhk.monoceros.api.workflow.ActionContext
import cc.bkhk.monoceros.api.workflow.ActionNode
import cc.bkhk.monoceros.api.workflow.ActionNodeDefinition
import cc.bkhk.monoceros.api.workflow.ActionResult
import org.bukkit.event.Event
import kotlin.math.pow

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
 * 返回 [ActionResult.Delay]，由工作流引擎异步延续后续节点，不阻塞当前线程。
 */
class WaitActionNode : ActionNode {
    override val type: String = "wait"

    override fun execute(context: ActionContext, definition: ActionNodeDefinition): Any? {
        val ticks = (definition.config["ticks"] as? Number)?.toLong() ?: 0L
        if (ticks > 0) {
            return ActionResult.Delay(ticks)
        }
        return null
    }
}

/**
 * 条件分支节点
 *
 * 返回 [ActionResult.Branch]，由工作流引擎根据条件结果决定执行 then/else 分支或中断。
 */
class BranchActionNode : ActionNode {
    override val type: String = "branch"

    override fun execute(context: ActionContext, definition: ActionNodeDefinition): Any? {
        val conditionScript = definition.config["condition"] as? String
            ?: error("branch 节点缺少 condition 配置: ${definition.id}")
        val result = Monoceros.api().scripts().invoke(conditionScript, context.sender, context.variables)
        val accepted = result == true || result == "true"
        context.variables["branchResult"] = accepted

        val thenWorkflow = definition.config["then-workflow"] as? String
        val elseWorkflow = definition.config["else-workflow"] as? String
        return ActionResult.Branch(accepted, thenWorkflow, elseWorkflow)
    }
}

class LoopActionNode : ActionNode {
    override val type: String = "loop"

    override fun execute(context: ActionContext, definition: ActionNodeDefinition): Any? {
        val sourceKey = definition.config["source"] as? String ?: "lastResult"
        val iterable = when (val source = context.variables[sourceKey]) {
            is Iterable<*> -> source.toList()
            is Array<*> -> source.toList()
            else -> emptyList<Any?>()
        }
        if (iterable.isEmpty()) return emptyList<Any?>()

        val itemKey = definition.config["item-key"] as? String ?: "loopItem"
        val indexKey = definition.config["index-key"] as? String ?: "loopIndex"
        val workflowId = definition.config["workflow"] as? String
        val scriptId = definition.config["script"] as? String
        val results = mutableListOf<Any?>()

        iterable.forEachIndexed { index, item ->
            context.variables[itemKey] = item
            context.variables[indexKey] = index
            val result = when {
                workflowId != null -> Monoceros.api().actionWorkflow().execute(workflowId, context.sender, context.variables)
                scriptId != null -> Monoceros.api().scripts().invoke(scriptId, context.sender, context.variables)
                else -> item
            }
            results += result
        }
        return results
    }
}

/** 播放音效 */
class SoundActionNode : ActionNode {
    override val type: String = "sound"
    override fun execute(context: ActionContext, definition: ActionNodeDefinition): Any? {
        val player = context.variables["player"] as? org.bukkit.entity.Player ?: return null
        val soundName = definition.config["sound"] as? String ?: return null
        val volume = (definition.config["volume"] as? Number)?.toFloat() ?: 1.0f
        val pitch = (definition.config["pitch"] as? Number)?.toFloat() ?: 1.0f
        val location = context.variables["location"] as? org.bukkit.Location ?: player.location
        player.playSound(location, soundName, volume, pitch)
        return true
    }
}

/** 发送 JSON 富文本消息 */
class TellrawActionNode : ActionNode {
    override val type: String = "tellraw"
    override fun execute(context: ActionContext, definition: ActionNodeDefinition): Any? {
        val message = definition.config["message"] as? String ?: return null
        val colored = message.replace(Regex("&([0-9a-fk-orA-FK-OR])"), "\u00a7$1")
        val player = context.variables["player"] as? org.bukkit.entity.Player
        if (player != null) {
            player.sendMessage(colored)
        } else {
            context.sender?.sendMessage(colored)
        }
        return colored
    }
}

/** 正则匹配与捕获组提取 */
class RegexActionNode : ActionNode {
    override val type: String = "regex"
    override fun execute(context: ActionContext, definition: ActionNodeDefinition): Any? {
        val pattern = definition.config["pattern"] as? String ?: return null
        val input = definition.config["input"] as? String
            ?: context.variables["lastResult"]?.toString()
            ?: return null
        val regex = Regex(pattern)
        val match = regex.find(input) ?: return null
        context.variables["regexMatch"] = match.value
        context.variables["regexGroups"] = match.groupValues
        return match.groupValues
    }
}

/** 异常捕获与降级处理 */
class TryCatchActionNode : ActionNode {
    override val type: String = "try-catch"
    override fun execute(context: ActionContext, definition: ActionNodeDefinition): Any? {
        val tryScript = definition.config["try"] as? String ?: return null
        val catchScript = definition.config["catch"] as? String
        val finallyScript = definition.config["finally"] as? String
        return try {
            val result = Monoceros.api().scripts().invoke(tryScript, context.sender, context.variables)
            finallyScript?.let { Monoceros.api().scripts().invoke(it, context.sender, context.variables) }
            result
        } catch (e: Exception) {
            context.variables["exception"] = e.message
            context.variables["exceptionType"] = e.javaClass.simpleName
            val catchResult = catchScript?.let { Monoceros.api().scripts().invoke(it, context.sender, context.variables) }
            finallyScript?.let { Monoceros.api().scripts().invoke(it, context.sender, context.variables) }
            catchResult
        }
    }
}

/** 等待玩家输入（聊天消息捕获 + 超时） */
class InputActionNode : ActionNode {
    override val type: String = "input"
    override fun execute(context: ActionContext, definition: ActionNodeDefinition): Any? {
        val timeoutTicks = (definition.config["timeout"] as? Number)?.toLong() ?: 200L
        // 返回 Delay 让引擎异步等待，实际输入捕获需要配合事件监听
        return ActionResult.Delay(timeoutTicks)
    }
}

/** 条件分支（脚本级 if-else） */
class IfElseActionNode : ActionNode {
    override val type: String = "if-else"
    override fun execute(context: ActionContext, definition: ActionNodeDefinition): Any? {
        val conditionScript = definition.config["condition"] as? String
            ?: definition.config["if"] as? String
            ?: return null
        val thenScript = definition.config["then"] as? String
        val elseScript = definition.config["else"] as? String
        val result = Monoceros.api().scripts().invoke(conditionScript, context.sender, context.variables)
        val accepted = result == true || result == "true"
        return if (accepted) {
            thenScript?.let { Monoceros.api().scripts().invoke(it, context.sender, context.variables) }
        } else {
            elseScript?.let { Monoceros.api().scripts().invoke(it, context.sender, context.variables) }
        }
    }
}

/** 数学运算节点 */
class MathActionNode : ActionNode {
    override val type: String = "math"
    override fun execute(context: ActionContext, definition: ActionNodeDefinition): Any? {
        val op = definition.config["op"] as? String ?: return null
        val a = (definition.config["a"] as? Number ?: context.variables["lastResult"] as? Number)?.toDouble() ?: return null
        val b = (definition.config["b"] as? Number)?.toDouble()
        return when (op.lowercase()) {
            "abs" -> kotlin.math.abs(a)
            "ceil" -> kotlin.math.ceil(a)
            "floor" -> kotlin.math.floor(a)
            "round" -> kotlin.math.round(a)
            "sqrt" -> kotlin.math.sqrt(a)
            "pow" -> a.pow(b ?: 2.0)
            "min" -> kotlin.math.min(a, b ?: a)
            "max" -> kotlin.math.max(a, b ?: a)
            "random" -> Math.random() * a
            "sin" -> kotlin.math.sin(a)
            "cos" -> kotlin.math.cos(a)
            "tan" -> kotlin.math.tan(a)
            "log" -> kotlin.math.ln(a)
            "log10" -> kotlin.math.log10(a)
            else -> null
        }
    }
}

/** 数值范围约束节点 */
class CoerceActionNode : ActionNode {
    override val type: String = "coerce"
    override fun execute(context: ActionContext, definition: ActionNodeDefinition): Any? {
        val value = (definition.config["value"] as? Number ?: context.variables["lastResult"] as? Number)?.toDouble() ?: return null
        val op = definition.config["op"] as? String ?: "in"
        val min = (definition.config["min"] as? Number)?.toDouble()
        val max = (definition.config["max"] as? Number)?.toDouble()
        val decimals = (definition.config["decimals"] as? Number)?.toInt()
        val result = when (op.lowercase()) {
            "in" -> if (min != null && max != null) value.coerceIn(min, max) else value
            "at-least", "atleast" -> if (min != null) value.coerceAtLeast(min) else value
            "at-most", "atmost" -> if (max != null) value.coerceAtMost(max) else value
            "format" -> if (decimals != null) "%.${decimals}f".format(value).toDouble() else value
            else -> value
        }
        return result
    }
}

class DispatchActionNode : ActionNode {
    override val type: String = "dispatch"

    override fun execute(context: ActionContext, definition: ActionNodeDefinition): Any? {
        val workflowId = definition.config["workflow"] as? String
        val scriptId = definition.config["script"] as? String
        val dispatcherId = definition.config["dispatcher"] as? String
        return when {
            workflowId != null -> Monoceros.api().actionWorkflow().execute(workflowId, context.sender, context.variables)
            scriptId != null -> Monoceros.api().scripts().invoke(scriptId, context.sender, context.variables)
            dispatcherId != null -> {
                val event = context.variables["event"] as? Event ?: return null
                Monoceros.api().dispatchers().get(dispatcherId)?.accept(event)
                event
            }
            else -> null
        }
    }
}
