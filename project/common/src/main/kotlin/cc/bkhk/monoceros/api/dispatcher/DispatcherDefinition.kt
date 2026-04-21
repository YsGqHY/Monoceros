package cc.bkhk.monoceros.api.dispatcher

import org.bukkit.event.EventPriority

/**
 * 分发器定义
 */
data class DispatcherDefinition(
    val id: String,
    val eventKey: String,
    val priority: EventPriority = EventPriority.NORMAL,
    val weight: Int = 0,
    val ignoreCancelled: Boolean = false,
    val beforeScript: String? = null,
    val executeRoute: DispatcherRoute,
    val afterScript: String? = null,
    val rules: List<DispatcherRule> = emptyList(),
    val variables: Map<String, Any?> = emptyMap(),
)
