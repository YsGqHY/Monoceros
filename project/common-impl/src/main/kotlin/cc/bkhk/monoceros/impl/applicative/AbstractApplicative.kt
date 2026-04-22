package cc.bkhk.monoceros.impl.applicative

import cc.bkhk.monoceros.api.applicative.Applicative

/**
 * Applicative 抽象基类
 *
 * 子类只需实现 [convertOrThrow] 即可。
 */
abstract class AbstractApplicative<T>(val clazz: Class<T>) : Applicative<T> {

    override val name: String = clazz.simpleName.lowercase()

    /**
     * 核心转换逻辑，子类实现
     *
     * @throws IllegalArgumentException 当类型不匹配或值不合法时
     */
    abstract fun convertOrThrow(instance: Any): T

    override fun convert(instance: Any?): T {
        if (instance == null) throw NullPointerException("无法将 null 转换为 ${clazz.simpleName}")
        return convertOrThrow(instance)
    }

    override fun convertOrNull(instance: Any?): T? {
        if (instance == null) return null
        return try {
            convertOrThrow(instance)
        } catch (_: Exception) {
            null
        }
    }
}
