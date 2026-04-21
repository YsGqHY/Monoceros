package cc.bkhk.monoceros.api.dispatcher

import org.bukkit.event.Event

/**
 * 事件分发器
 */
interface EventDispatcher {
    val definition: DispatcherDefinition
    fun accept(event: Event)
}
