package cc.bkhk.monoceros.api.dispatcher

/**
 * 分发器过滤规则
 */
interface DispatcherRule {
    fun test(context: DispatcherContext): DispatcherDecision
}
