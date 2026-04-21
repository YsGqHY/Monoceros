package cc.bkhk.monoceros.api.dispatcher

/**
 * 分发器强类型处理器
 *
 * 适用于高性能内建机制、复杂状态机、需要强类型访问事件对象的流程。
 * 通过 [DispatcherRoute.Handler] 路由到此接口。
 */
interface DispatcherHandler {
    val id: String
    fun handle(context: DispatcherContext): Any?
}
