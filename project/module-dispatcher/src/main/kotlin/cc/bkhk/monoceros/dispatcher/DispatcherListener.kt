package cc.bkhk.monoceros.dispatcher

import cc.bkhk.monoceros.api.dispatcher.EventDispatcher
import cc.bkhk.monoceros.impl.util.DiagnosticLogger
import org.bukkit.event.Event
import org.bukkit.event.EventPriority
import taboolib.common.platform.event.ProxyListener
import taboolib.common.platform.function.registerBukkitListener
import taboolib.common.platform.function.unregisterListener
import java.util.EnumMap
import java.util.concurrent.ConcurrentHashMap
import taboolib.common.platform.event.EventPriority as TaboolibPriority

/**
 * 事件监听桥接中心
 *
 * 按 "事件类 -> 优先级 -> dispatcher 列表" 组织运行时表。
 * 每个事件类+优先级最多只注册一个 Bukkit ProxyListener，避免重复注册。
 * dispatcher 列表按 weight 倒序排列，权重高者优先执行。
 */
object DispatcherListener {

    private const val MODULE = "DispatcherListener"

    /**
     * 运行时分发表
     * eventClass -> (priority -> dispatcher 列表)
     */
    private val runtime = ConcurrentHashMap<Class<out Event>, EnumMap<EventPriority, MutableList<EventDispatcher>>>()

    /**
     * Bukkit ProxyListener 缓存
     * eventClass -> (priority -> ProxyListener)
     */
    private val listeners = ConcurrentHashMap<Class<out Event>, EnumMap<EventPriority, ProxyListener>>()

    /**
     * 注册一个 dispatcher 到运行时表
     */
    fun register(eventClass: Class<out Event>, dispatcher: EventDispatcher) {
        val priority = dispatcher.definition.priority
        val priorityMap = runtime.computeIfAbsent(eventClass) { EnumMap(EventPriority::class.java) }

        synchronized(priorityMap) {
            val dispatchers = priorityMap.computeIfAbsent(priority) { mutableListOf() }
            dispatchers.add(dispatcher)
            // 按 weight 倒序排列
            dispatchers.sortByDescending { it.definition.weight }
        }

        // 懒注册：同一事件类+优先级只注册一次 Bukkit 监听器
        val listenerMap = listeners.computeIfAbsent(eventClass) { EnumMap(EventPriority::class.java) }
        listenerMap.computeIfAbsent(priority) {
            registerBukkitListener(eventClass, mapPriority(priority), false) { event ->
                accept(eventClass, priority, event as Event)
            }
        }

        DiagnosticLogger.info(MODULE, "注册 dispatcher: ${dispatcher.definition.id} -> ${eventClass.simpleName}@$priority")
    }

    /**
     * 注销一个 dispatcher
     */
    fun unregister(eventClass: Class<out Event>, dispatcherId: String) {
        val priorityMap = runtime[eventClass] ?: return

        synchronized(priorityMap) {
            for ((priority, dispatchers) in priorityMap) {
                val removed = dispatchers.removeAll { it.definition.id == dispatcherId }
                if (removed) {
                    DiagnosticLogger.info(MODULE, "注销 dispatcher: $dispatcherId from ${eventClass.simpleName}@$priority")
                    // 若该优先级下已无 dispatcher，移除 ProxyListener
                    if (dispatchers.isEmpty()) {
                        listeners[eventClass]?.remove(priority)?.let { proxy ->
                            unregisterListener(proxy)
                        }
                        priorityMap.remove(priority)
                    }
                }
            }
            // 若该事件类下已无任何优先级，清理
            if (priorityMap.isEmpty()) {
                runtime.remove(eventClass)
                listeners.remove(eventClass)
            }
        }
    }

    /**
     * 注销所有 dispatcher 并清理全部 Bukkit 监听器
     */
    fun unregisterAll() {
        listeners.values.forEach { priorityMap ->
            priorityMap.values.forEach { proxy -> unregisterListener(proxy) }
        }
        runtime.clear()
        listeners.clear()
        DiagnosticLogger.info(MODULE, "已注销全部 dispatcher 与 Bukkit 监听器")
    }

    /**
     * 虚拟事件分发入口
     */
    fun dispatchVirtual(event: Event) {
        val eventClass = event.javaClass
        EventPriority.entries.forEach { priority ->
            accept(eventClass, priority, event)
        }
    }

    /**
     * 事件到达时的分发入口
     */
    private fun accept(eventClass: Class<out Event>, priority: EventPriority, event: Event) {
        val priorityMap = runtime[eventClass] ?: return
        val dispatchers = synchronized(priorityMap) {
            priorityMap[priority]?.toList()
        } ?: return

        for (dispatcher in dispatchers) {
            try {
                dispatcher.accept(event)
            } catch (e: Exception) {
                DiagnosticLogger.warn(MODULE, "dispatcher 执行异常: ${dispatcher.definition.id}", e)
            }
        }
    }

    /** Bukkit EventPriority -> TabooLib EventPriority */
    private fun mapPriority(priority: EventPriority): TaboolibPriority {
        return when (priority) {
            EventPriority.LOWEST -> TaboolibPriority.LOWEST
            EventPriority.LOW -> TaboolibPriority.LOW
            EventPriority.NORMAL -> TaboolibPriority.NORMAL
            EventPriority.HIGH -> TaboolibPriority.HIGH
            EventPriority.HIGHEST -> TaboolibPriority.HIGHEST
            EventPriority.MONITOR -> TaboolibPriority.MONITOR
        }
    }
}
