package cc.bkhk.monoceros.api.dispatcher

/**
 * 事件分发服务
 */
interface DispatcherService {
    fun register(definition: DispatcherDefinition): EventDispatcher
    fun unregister(id: String): EventDispatcher?
    fun get(id: String): EventDispatcher?
    fun reloadAll(): Int
}
