package cc.bkhk.monoceros.api.workflow

import kotlin.reflect.KClass

/**
 * 属性请求
 */
data class PropertyRequest(
    val target: Any,
    val path: List<String>,
    val value: Any? = null,
    val context: Map<String, Any?> = emptyMap(),
)

/**
 * 属性访问器
 */
interface PropertyAccessor<T : Any> {
    val targetType: KClass<T>
    fun read(target: T, key: String, context: Map<String, Any?> = emptyMap()): Any?
    fun write(target: T, key: String, value: Any?, context: Map<String, Any?> = emptyMap()) {
        error("Property '$key' is read-only for ${targetType.qualifiedName}")
    }
}

/**
 * 属性工作流服务
 */
interface PropertyWorkflowService {
    fun register(accessor: PropertyAccessor<*>): PropertyAccessor<*>
    fun unregister(type: KClass<*>): PropertyAccessor<*>?
    fun read(request: PropertyRequest): Any?
    fun write(request: PropertyRequest)
}
