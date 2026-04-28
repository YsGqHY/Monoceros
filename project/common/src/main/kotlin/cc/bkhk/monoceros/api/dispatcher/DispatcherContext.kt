package cc.bkhk.monoceros.api.dispatcher

import cc.bkhk.monoceros.api.util.SenderAdapter
import org.bukkit.command.CommandSender
import org.bukkit.event.Event

/**
 * 分发器运行时上下文
 */
data class DispatcherContext(
    val definitionId: String,
    val event: Event,
    val sender: CommandSender?,
    val variables: MutableMap<String, Any?> = LinkedHashMap(),
    var filtered: Boolean = false,
    var cancelled: Boolean = false,
    var routeResult: Any? = null,
) {
    companion object {
        /** 兼容旧版 API：接受任意 sender 类型（含 relocated ProxyCommandSender） */
        @JvmStatic
        fun fromAnySender(
            definitionId: String,
            event: Event,
            sender: Any?,
            variables: MutableMap<String, Any?> = LinkedHashMap(),
            filtered: Boolean = false,
            cancelled: Boolean = false,
            routeResult: Any? = null,
        ): DispatcherContext = DispatcherContext(
            definitionId, event, SenderAdapter.adapt(sender), variables, filtered, cancelled, routeResult,
        )
    }
}
