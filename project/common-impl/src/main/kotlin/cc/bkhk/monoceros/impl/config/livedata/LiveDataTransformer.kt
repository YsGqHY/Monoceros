package cc.bkhk.monoceros.impl.config.livedata

import kotlin.reflect.KProperty

/**
 * 值转换管道
 *
 * 订阅源 [LiveData] 的更新，自动将值从 T 转换为 R。
 */
class LiveDataTransformer<T, R>(
    private val source: LiveData<T>,
    private val transform: (T) -> R,
) : LiveData<R> {

    override val id: String = source.id

    @Volatile
    private var cachedValue: R = transform(source.getValue())
    private val callbacks = mutableListOf<(R) -> Unit>()

    init {
        source.onUpdate { newValue ->
            try {
                val transformed = transform(newValue)
                cachedValue = transformed
                callbacks.forEach { it(transformed) }
            } catch (e: Exception) {
                throw ConfigFieldReadException(id, e)
            }
        }
    }

    override fun getValue(): R {
        return try {
            cachedValue
        } catch (e: Exception) {
            throw ConfigFieldReadException(id, e)
        }
    }

    override fun onUpdate(callback: (R) -> Unit) {
        callbacks.add(callback)
    }

    override fun getValue(thisRef: Any?, property: KProperty<*>): R = getValue()
}

/**
 * 异常兜底包装
 *
 * 当源 LiveData 的值更新过程中发生异常时，调用 [fallback] 提供兜底值。
 */
class ExceptionalLiveData<T>(
    private val source: LiveData<T>,
    private val fallback: (Exception) -> T,
) : LiveData<T> {

    override val id: String = source.id

    @Volatile
    private var cachedValue: T = try {
        source.getValue()
    } catch (e: Exception) {
        fallback(e)
    }
    private val callbacks = mutableListOf<(T) -> Unit>()

    init {
        source.onUpdate { newValue ->
            cachedValue = newValue
            callbacks.forEach { it(newValue) }
        }
    }

    override fun getValue(): T = cachedValue

    override fun onUpdate(callback: (T) -> Unit) {
        callbacks.add(callback)
    }

    override fun getValue(thisRef: Any?, property: KProperty<*>): T = getValue()
}

/** 配置字段读取异常 */
class ConfigFieldReadException(val field: String, cause: Throwable? = null) :
    RuntimeException("配置字段读取失败: $field", cause)
