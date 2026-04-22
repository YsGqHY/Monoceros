package cc.bkhk.monoceros.impl.config.livedata

import cc.bkhk.monoceros.impl.applicative.DefaultApplicativeRegistry

/**
 * LiveData 类型转换扩展函数集
 *
 * 提供链式 API 将 LiveData 值转换为目标类型。
 */

// region 异常处理

/** 异常兜底 */
fun <T> LiveData<T>.exceptionally(handler: (Exception) -> T): LiveData<T> =
    ExceptionalLiveData(this, handler)

// endregion

// region 通用转换

/** 通用值转换 */
fun <T, R> LiveData<T>.convert(transformer: (T) -> R): LiveData<R> =
    LiveDataTransformer(this, transformer)

/** 空值兜底 */
@Suppress("UNCHECKED_CAST")
fun <T> LiveData<T?>.default(defaultValue: T & Any): LiveData<T & Any> =
    LiveDataTransformer(this) { it ?: defaultValue } as LiveData<T & Any>

// endregion

// region 基本类型转换

fun <T> LiveData<T>.boolean(): LiveData<Boolean> = convert { value ->
    when (value) {
        is Boolean -> value
        is String -> value.lowercase().let { it == "true" || it == "yes" || it == "1" }
        is Number -> value.toInt() != 0
        null -> throw NullPointerException("配置值为 null: $id")
        else -> throw IllegalArgumentException("无法将 ${value.javaClass.simpleName} 转换为 Boolean")
    }
}

fun <T> LiveData<T>.booleanOrNull(): LiveData<Boolean?> = convert { value ->
    when (value) {
        null -> null
        is Boolean -> value
        is String -> value.lowercase().let { it == "true" || it == "yes" || it == "1" }
        is Number -> value.toInt() != 0
        else -> null
    }
}

fun <T> LiveData<T>.boolean(defaultValue: Boolean): LiveData<Boolean> =
    booleanOrNull().convert { it ?: defaultValue }

fun <T> LiveData<T>.int(): LiveData<Int> = convert { value ->
    when (value) {
        is Int -> value
        is Number -> value.toInt()
        is String -> value.toIntOrNull() ?: throw IllegalArgumentException("无法将 '$value' 转换为 Int")
        null -> throw NullPointerException("配置值为 null: $id")
        else -> throw IllegalArgumentException("无法将 ${value.javaClass.simpleName} 转换为 Int")
    }
}

fun <T> LiveData<T>.intOrNull(): LiveData<Int?> = convert { value ->
    when (value) {
        null -> null
        is Int -> value
        is Number -> value.toInt()
        is String -> value.toIntOrNull()
        else -> null
    }
}

fun <T> LiveData<T>.int(defaultValue: Int): LiveData<Int> =
    intOrNull().convert { it ?: defaultValue }

fun <T> LiveData<T>.long(): LiveData<Long> = convert { value ->
    when (value) {
        is Long -> value
        is Number -> value.toLong()
        is String -> value.toLongOrNull() ?: throw IllegalArgumentException("无法将 '$value' 转换为 Long")
        null -> throw NullPointerException("配置值为 null: $id")
        else -> throw IllegalArgumentException("无法将 ${value.javaClass.simpleName} 转换为 Long")
    }
}

fun <T> LiveData<T>.longOrNull(): LiveData<Long?> = convert { value ->
    when (value) {
        null -> null
        is Long -> value
        is Number -> value.toLong()
        is String -> value.toLongOrNull()
        else -> null
    }
}

fun <T> LiveData<T>.long(defaultValue: Long): LiveData<Long> =
    longOrNull().convert { it ?: defaultValue }

fun <T> LiveData<T>.float(): LiveData<Float> = convert { value ->
    when (value) {
        is Float -> value
        is Number -> value.toFloat()
        is String -> value.toFloatOrNull() ?: throw IllegalArgumentException("无法将 '$value' 转换为 Float")
        null -> throw NullPointerException("配置值为 null: $id")
        else -> throw IllegalArgumentException("无法将 ${value.javaClass.simpleName} 转换为 Float")
    }
}

fun <T> LiveData<T>.floatOrNull(): LiveData<Float?> = convert { value ->
    when (value) {
        null -> null
        is Float -> value
        is Number -> value.toFloat()
        is String -> value.toFloatOrNull()
        else -> null
    }
}

fun <T> LiveData<T>.float(defaultValue: Float): LiveData<Float> =
    floatOrNull().convert { it ?: defaultValue }

fun <T> LiveData<T>.double(): LiveData<Double> = convert { value ->
    when (value) {
        is Double -> value
        is Number -> value.toDouble()
        is String -> value.toDoubleOrNull() ?: throw IllegalArgumentException("无法将 '$value' 转换为 Double")
        null -> throw NullPointerException("配置值为 null: $id")
        else -> throw IllegalArgumentException("无法将 ${value.javaClass.simpleName} 转换为 Double")
    }
}

fun <T> LiveData<T>.doubleOrNull(): LiveData<Double?> = convert { value ->
    when (value) {
        null -> null
        is Double -> value
        is Number -> value.toDouble()
        is String -> value.toDoubleOrNull()
        else -> null
    }
}

fun <T> LiveData<T>.double(defaultValue: Double): LiveData<Double> =
    doubleOrNull().convert { it ?: defaultValue }

fun <T> LiveData<T>.string(): LiveData<String> = convert { value ->
    value?.toString() ?: throw NullPointerException("配置值为 null: $id")
}

fun <T> LiveData<T>.stringOrNull(): LiveData<String?> = convert { value ->
    value?.toString()
}

fun <T> LiveData<T>.string(defaultValue: String): LiveData<String> =
    stringOrNull().convert { it ?: defaultValue }

// endregion

// region 集合转换

@Suppress("UNCHECKED_CAST")
fun <T> LiveData<T>.list(): LiveData<List<*>> = convert { value ->
    when (value) {
        is List<*> -> value
        is Collection<*> -> value.toList()
        is Array<*> -> value.toList()
        null -> throw NullPointerException("配置值为 null: $id")
        else -> listOf(value)
    }
}

fun <T> LiveData<T>.listOrNull(): LiveData<List<*>?> = convert { value ->
    when (value) {
        null -> null
        is List<*> -> value
        is Collection<*> -> value.toList()
        is Array<*> -> value.toList()
        else -> listOf(value)
    }
}

@Suppress("UNCHECKED_CAST")
fun <T> LiveData<T>.map(): LiveData<Map<*, *>> = convert { value ->
    when (value) {
        is Map<*, *> -> value
        null -> throw NullPointerException("配置值为 null: $id")
        else -> throw IllegalArgumentException("无法将 ${value.javaClass.simpleName} 转换为 Map")
    }
}

fun <T> LiveData<T>.mapOrNull(): LiveData<Map<*, *>?> = convert { value ->
    when (value) {
        null -> null
        is Map<*, *> -> value
        else -> null
    }
}

// endregion

// region 集合元素映射

/** 将 List 中的元素逐个转换 */
fun <R> LiveData<List<*>>.mapElements(transformer: (Any?) -> R): LiveData<List<R>> =
    convert { list -> list.map { transformer(it) } }

/** 将 Map 的键值对逐个转换 */
fun <K, V> LiveData<Map<*, *>>.mapEntries(transformer: (Map.Entry<*, *>) -> Pair<K, V>): LiveData<Map<K, V>> =
    convert { map -> map.entries.associate { transformer(it) } }

/** 将 Map 的键转为 String，值保持 Any? */
fun LiveData<Map<*, *>>.normalize(): LiveData<Map<String, Any?>> =
    convert { map -> map.entries.associate { it.key.toString() to it.value } }

// endregion

// region 快捷组合

fun <T> LiveData<T>.intList(): LiveData<List<Int>> =
    list().mapElements { value ->
        when (value) {
            is Int -> value
            is Number -> value.toInt()
            is String -> value.toIntOrNull() ?: 0
            else -> 0
        }
    }

fun <T> LiveData<T>.stringList(): LiveData<List<String>> =
    list().mapElements { it?.toString() ?: "" }

@Suppress("UNCHECKED_CAST")
fun <T> LiveData<T>.mapList(): LiveData<List<Map<*, *>>> =
    list().mapElements { value ->
        when (value) {
            is Map<*, *> -> value
            else -> emptyMap<Any, Any>()
        }
    }

fun <T> LiveData<T>.normalizeMap(): LiveData<Map<String, Any?>> =
    map().normalize()

// endregion

// region Applicative 集成

/**
 * 使用 Applicative 注册表进行类型转换
 */
inline fun <T, reified R : Any> LiveData<T>.applicative(): LiveData<R> = convert { value ->
    val applicative = DefaultApplicativeRegistry.get(R::class.java)
        ?: throw IllegalStateException("未注册 ${R::class.java.simpleName} 的 Applicative 转换器")
    applicative.convert(value)
}

inline fun <T, reified R : Any> LiveData<T>.applicativeOrNull(): LiveData<R?> = convert { value ->
    val applicative = DefaultApplicativeRegistry.get(R::class.java) ?: return@convert null
    applicative.convertOrNull(value)
}

inline fun <T, reified R : Any> LiveData<T>.applicative(defaultValue: R): LiveData<R> = convert { value ->
    val applicative = DefaultApplicativeRegistry.get(R::class.java) ?: return@convert defaultValue
    applicative.convertOrNull(value) ?: defaultValue
}

// endregion
