package cc.bkhk.monoceros.extension.action.event

import cc.bkhk.monoceros.Monoceros
import cc.bkhk.monoceros.api.dispatcher.DispatcherContext
import cc.bkhk.monoceros.api.extension.NativeExtension
import cc.bkhk.monoceros.api.workflow.ActionContext
import cc.bkhk.monoceros.api.workflow.ActionNode
import cc.bkhk.monoceros.api.workflow.ActionNodeDefinition
import org.bukkit.entity.Player

/** 取消当前可取消事件 */
class EventCancelNode : ActionNode {
    override val type = "event.cancel"
    override fun execute(context: ActionContext, definition: ActionNodeDefinition): Any? {
        val dc = context.variables["dispatcherContext"] as? DispatcherContext ?: return false
        dc.cancelled = true
        return true
    }
}

/** 忽略后续执行链 */
class EventIgnoreNode : ActionNode {
    override val type = "event.ignore"
    override fun execute(context: ActionContext, definition: ActionNodeDefinition): Any? {
        val dc = context.variables["dispatcherContext"] as? DispatcherContext ?: return false
        dc.filtered = true
        return true
    }
}

/** 对事件对象执行属性写入 */
class EventWriteNode : ActionNode {
    override val type = "event.write"
    override fun execute(context: ActionContext, definition: ActionNodeDefinition): Any? {
        val event = context.variables["event"] ?: return null
        val path = definition.config["path"] as? String ?: return null
        val value = definition.config["value"]
        Monoceros.api().propertyWorkflow().write(
            cc.bkhk.monoceros.api.workflow.PropertyRequest(
                target = event,
                path = path.split("."),
                value = value,
                context = context.variables,
            )
        )
        return value
    }
}

/** 给事件相关发送者回复信息 */
class EventReplyNode : ActionNode {
    override val type = "event.reply"
    override fun execute(context: ActionContext, definition: ActionNodeDefinition): Any? {
        val message = definition.config["message"] as? String ?: return null
        val colored = message.replace(Regex("&([0-9a-fk-orA-FK-OR])"), "\u00a7$1")
        val player = context.variables["player"] as? Player
        if (player != null) {
            player.sendMessage(colored)
        } else {
            context.sender?.sendMessage(colored)
        }
        return null
    }
}

/** 读取事件取消状态 */
class EventCancelledNode : ActionNode {
    override val type = "event.cancelled"
    override fun execute(context: ActionContext, definition: ActionNodeDefinition): Any? {
        val event = context.variables["event"] as? org.bukkit.event.Cancellable ?: return false
        return event.isCancelled
    }
}

/** 获取事件名称 */
class EventNameNode : ActionNode {
    override val type = "event.name"
    override fun execute(context: ActionContext, definition: ActionNodeDefinition): Any? {
        val event = context.variables["event"] as? org.bukkit.event.Event ?: return null
        return event.eventName
    }
}

/** 异步等待指定事件触发（带超时） */
class EventWaitNode : ActionNode {
    override val type = "event.wait"
    override fun execute(context: ActionContext, definition: ActionNodeDefinition): Any? {
        val eventName = definition.config["event"] as? String ?: return null
        val timeoutTicks = (definition.config["timeout"] as? Number)?.toLong() ?: 200L
        // 返回 ActionResult.Delay 让引擎异步等待
        return cc.bkhk.monoceros.api.workflow.ActionResult.Delay(timeoutTicks)
    }
}

class EventActionExtension : NativeExtension() {
    override val id = "action-event"
    override val name = "事件域动作扩展"
    override val version = "1.0.0"

    override fun onEnable() {
        val service = Monoceros.api().actionWorkflow()
        service.registerNode(EventCancelNode())
        service.registerNode(EventIgnoreNode())
        service.registerNode(EventWriteNode())
        service.registerNode(EventReplyNode())
        service.registerNode(EventCancelledNode())
        service.registerNode(EventNameNode())
        service.registerNode(EventWaitNode())
    }
}
