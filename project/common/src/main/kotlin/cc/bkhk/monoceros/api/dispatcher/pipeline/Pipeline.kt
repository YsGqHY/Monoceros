package cc.bkhk.monoceros.api.dispatcher.pipeline

/**
 * 事件处理管道接口
 *
 * Pipeline 定义了事件分发过程中的 5 个生命周期阶段。
 * 多个 Pipeline 按 [priority] 倒序排列，依次处理同一个 [PipelineContext]。
 */
interface Pipeline {

    /** 优先级，数值越大越先执行 */
    val priority: Int get() = 8

    /** 初始化事件主体（Player/Entity/Block 等） */
    fun initPrincipal(context: PipelineContext) {}

    /** 初始化上下文变量 */
    fun initVariables(context: PipelineContext) {}

    /** 过滤/阻断判定，设置 context.isFiltered / isCancelled / isFilterBaffled */
    fun filter(context: PipelineContext) {}

    /** 过滤后置处理（如更新冷却计数），仅在未被过滤时调用 */
    fun afterFilter(context: PipelineContext) {}

    /** 脚本/工作流执行完毕后的后置处理 */
    fun postprocess(context: PipelineContext) {}
}
