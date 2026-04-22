package cc.bkhk.monoceros.impl.applicative.converter

import cc.bkhk.monoceros.impl.applicative.AbstractApplicative

/** List 转换器 */
object ListApplicative : AbstractApplicative<List<*>>(List::class.java) {
    override fun convertOrThrow(instance: Any): List<*> = when (instance) {
        is List<*> -> instance
        is Collection<*> -> instance.toList()
        is Array<*> -> instance.toList()
        is String -> instance.split(",").map { it.trim() }
        else -> listOf(instance)
    }
}

/** Map 转换器 */
object MapApplicative : AbstractApplicative<Map<*, *>>(Map::class.java) {
    override fun convertOrThrow(instance: Any): Map<*, *> = when (instance) {
        is Map<*, *> -> instance
        else -> throw IllegalArgumentException("无法将 ${instance.javaClass.simpleName} 转换为 Map")
    }
}

/**
 * Enum 转换器工厂
 *
 * 为指定枚举类型创建转换器实例。
 */
class EnumApplicative<T : Enum<T>>(private val enumClass: Class<T>) : AbstractApplicative<T>(enumClass) {
    override val name: String = enumClass.simpleName.lowercase()
    override fun convertOrThrow(instance: Any): T = when (instance) {
        is Enum<*> -> {
            @Suppress("UNCHECKED_CAST")
            if (enumClass.isInstance(instance)) instance as T
            else throw IllegalArgumentException("枚举类型不匹配: 期望 ${enumClass.simpleName}, 实际 ${instance.javaClass.simpleName}")
        }
        is String -> {
            val upper = instance.uppercase().replace('-', '_').replace(' ', '_')
            enumClass.enumConstants?.firstOrNull { it.name == upper }
                ?: throw IllegalArgumentException("无效的枚举值: $instance (${enumClass.simpleName})")
        }
        else -> throw IllegalArgumentException("无法将 ${instance.javaClass.simpleName} 转换为 ${enumClass.simpleName}")
    }
}
