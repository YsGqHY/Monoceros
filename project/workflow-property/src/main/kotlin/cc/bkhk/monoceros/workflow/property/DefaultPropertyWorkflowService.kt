package cc.bkhk.monoceros.workflow.property

import cc.bkhk.monoceros.api.workflow.PropertyAccessor
import cc.bkhk.monoceros.api.workflow.PropertyRequest
import cc.bkhk.monoceros.api.workflow.PropertyWorkflowService
import cc.bkhk.monoceros.impl.util.DiagnosticLogger
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

/**
 * 属性工作流服务默认实现
 *
 * 管理属性访问器注册表，支持精确类型匹配和层级查找。
 * 支持链式路径读取与末段写入。
 */
class DefaultPropertyWorkflowService : PropertyWorkflowService {

    private companion object {
        const val MODULE = "PropertyWorkflow"
    }

    /** 精确类型索引 */
    private val accessorsByExactType = ConcurrentHashMap<KClass<*>, PropertyAccessor<*>>()

    /** 层级查找缓存（仅缓存命中结果，未命中不缓存） */
    private val hierarchyCache = ConcurrentHashMap<KClass<*>, PropertyAccessor<*>>()

    override fun register(accessor: PropertyAccessor<*>): PropertyAccessor<*> {
        accessorsByExactType[accessor.targetType] = accessor
        // 清空层级缓存，因为新注册可能影响查找结果
        hierarchyCache.clear()
        DiagnosticLogger.info(MODULE, "注册属性访问器: ${accessor.targetType.qualifiedName}")
        return accessor
    }

    override fun unregister(type: KClass<*>): PropertyAccessor<*>? {
        hierarchyCache.clear()
        return accessorsByExactType.remove(type)
    }

    override fun read(request: PropertyRequest): Any? {
        if (request.path.isEmpty()) return request.target

        var current: Any = request.target
        for (i in request.path.indices) {
            val key = request.path[i]
            val accessor = findAccessor(current::class)
                ?: error("未找到类型 ${current::class.qualifiedName} 的属性访问器")

            @Suppress("UNCHECKED_CAST")
            current = (accessor as PropertyAccessor<Any>).read(current, key, request.context) ?: return null
        }
        return current
    }

    override fun write(request: PropertyRequest) {
        if (request.path.isEmpty()) error("属性路径不能为空")

        // 定位到倒数第二层对象
        var current: Any = request.target
        for (i in 0 until request.path.size - 1) {
            val key = request.path[i]
            val accessor = findAccessor(current::class)
                ?: error("未找到类型 ${current::class.qualifiedName} 的属性访问器")

            @Suppress("UNCHECKED_CAST")
            current = (accessor as PropertyAccessor<Any>).read(current, key, request.context)
                ?: error("属性路径中间节点为 null: ${request.path.subList(0, i + 1).joinToString(".")}")
        }

        // 写入最后一段
        val lastKey = request.path.last()
        val accessor = findAccessor(current::class)
            ?: error("未找到类型 ${current::class.qualifiedName} 的属性访问器")

        @Suppress("UNCHECKED_CAST")
        (accessor as PropertyAccessor<Any>).write(current, lastKey, request.value, request.context)
    }

    /**
     * 查找属性访问器
     *
     * 优先精确类型匹配，找不到时向父类/接口层级查找。
     */
    private fun findAccessor(type: KClass<*>): PropertyAccessor<*>? {
        // 精确匹配
        accessorsByExactType[type]?.let { return it }

        // 层级缓存
        hierarchyCache[type]?.let { return it }

        // 层级查找
        for ((registeredType, accessor) in accessorsByExactType) {
            try {
                if (registeredType.java.isAssignableFrom(type.java)) {
                    hierarchyCache[type] = accessor
                    return accessor
                }
            } catch (_: Exception) {
                // kotlin-reflect 在某些环境下可能抛异常
            }
        }

        // 未找到，不缓存 null（ConcurrentHashMap 不允许 null 值）
        return null
    }
}
