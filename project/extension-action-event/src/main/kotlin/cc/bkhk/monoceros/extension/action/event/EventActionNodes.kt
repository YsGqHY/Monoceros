package cc.bkhk.monoceros.extension.action.event

import cc.bkhk.monoceros.Monoceros
import cc.bkhk.monoceros.api.dispatcher.DispatcherContext
import cc.bkhk.monoceros.api.extension.NativeExtension
import cc.bkhk.monoceros.api.workflow.ActionContext
import cc.bkhk.monoceros.api.workflow.ActionNode
import cc.bkhk.monoceros.api.workflow.ActionNodeDefinition
import org.bukkit.entity.Player
import taboolib.common.platform.function.adaptPlayer

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
    }
}
