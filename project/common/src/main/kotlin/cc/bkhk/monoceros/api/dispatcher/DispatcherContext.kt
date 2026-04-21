package cc.bkhk.monoceros.api.dispatcher

import org.bukkit.event.Event
import taboolib.common.platform.ProxyCommandSender

/**
 * 分发器运行时上下文
 */
data class DispatcherContext(
    val definitionId: String,
    val event: Event,
    val sender: ProxyCommandSender?,
    val variables: MutableMap<String, Any?> = LinkedHashMap(),
    var filtered: Boolean = false,
    var cancelled: Boolean = false,
    var routeResult: Any? = null,
)
