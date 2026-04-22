package cc.bkhk.monoceros.impl.applicative.converter

import cc.bkhk.monoceros.impl.applicative.AbstractApplicative

/** Boolean 转换器 */
object BooleanApplicative : AbstractApplicative<Boolean>(Boolean::class.java) {
    override val aliases = arrayOf("bool")
    override fun convertOrThrow(instance: Any): Boolean = when (instance) {
        is Boolean -> instance
        is String -> when (instance.lowercase()) {
            "true", "yes", "1" -> true
            "false", "no", "0" -> false
            else -> throw IllegalArgumentException("无法将 '$instance' 转换为 Boolean")
        }
        is Number -> instance.toInt() != 0
        else -> throw IllegalArgumentException("无法将 ${instance.javaClass.simpleName} 转换为 Boolean")
    }
}

/** Int 转换器 */
object IntApplicative : AbstractApplicative<Int>(Int::class.java) {
    override val aliases = arrayOf("integer")
    override fun convertOrThrow(instance: Any): Int = when (instance) {
        is Int -> instance
        is Number -> instance.toInt()
        is String -> instance.toIntOrNull() ?: throw IllegalArgumentException("无法将 '$instance' 转换为 Int")
        is Boolean -> if (instance) 1 else 0
        else -> throw IllegalArgumentException("无法将 ${instance.javaClass.simpleName} 转换为 Int")
    }
}

/** Long 转换器 */
object LongApplicative : AbstractApplicative<Long>(Long::class.java) {
    override fun convertOrThrow(instance: Any): Long = when (instance) {
        is Long -> instance
        is Number -> instance.toLong()
        is String -> instance.toLongOrNull() ?: throw IllegalArgumentException("无法将 '$instance' 转换为 Long")
        else -> throw IllegalArgumentException("无法将 ${instance.javaClass.simpleName} 转换为 Long")
    }
}

/** Float 转换器 */
object FloatApplicative : AbstractApplicative<Float>(Float::class.java) {
    override fun convertOrThrow(instance: Any): Float = when (instance) {
        is Float -> instance
        is Number -> instance.toFloat()
        is String -> instance.toFloatOrNull() ?: throw IllegalArgumentException("无法将 '$instance' 转换为 Float")
        else -> throw IllegalArgumentException("无法将 ${instance.javaClass.simpleName} 转换为 Float")
    }
}

/** Double 转换器 */
object DoubleApplicative : AbstractApplicative<Double>(Double::class.java) {
    override fun convertOrThrow(instance: Any): Double = when (instance) {
        is Double -> instance
        is Number -> instance.toDouble()
        is String -> instance.toDoubleOrNull() ?: throw IllegalArgumentException("无法将 '$instance' 转换为 Double")
        else -> throw IllegalArgumentException("无法将 ${instance.javaClass.simpleName} 转换为 Double")
    }
}

/** String 转换器 */
object StringApplicative : AbstractApplicative<String>(String::class.java) {
    override val aliases = arrayOf("str", "text")
    override fun convertOrThrow(instance: Any): String = when (instance) {
        is String -> instance
        is Collection<*> -> instance.joinToString("\n")
        is Array<*> -> instance.joinToString("\n")
        else -> instance.toString()
    }
}
