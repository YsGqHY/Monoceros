package cc.bkhk.monoceros.api.dispatcher.pipeline

/**
 * Pipeline 注册中心接口
 */
interface PipelineRegistry {

    /** 注册 Pipeline 类，关联到事件名 */
    fun register(eventName: String, pipeline: Pipeline)

    /** 获取指定事件名关联的所有 Pipeline（按 priority 倒序） */
    fun getPipelines(eventName: String): List<Pipeline>

    /** 获取所有已注册的事件名 */
    fun registeredEvents(): Set<String>
}
