package cc.bkhk.monoceros.impl.config.livedata

import kotlin.reflect.KProperty

/**
 * 响应式配置值接口
 *
 * 支持 Kotlin 属性委托，配置重载时自动更新值并触发回调。
 */
interface LiveData<T> {

    /** 配置键名 */
    val id: String

    /** 获取当前值 */
    fun getValue(): T

    /** 注册值变更回调 */
    fun onUpdate(callback: (T) -> Unit)

    /** Kotlin 属性委托支持 */
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T = getValue()
}
