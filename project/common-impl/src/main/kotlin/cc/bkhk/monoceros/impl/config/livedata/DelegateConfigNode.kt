package cc.bkhk.monoceros.impl.config.livedata

import taboolib.library.configuration.ConfigurationSection
import taboolib.module.configuration.Configuration
import kotlin.reflect.KProperty

/**
 * 配置节点委托
 *
 * 从 [ConfigurationSection] 中按 keys 顺序查找第一个存在的键读取值。
 * 如果底层是 [Configuration]，自动注册 onReload 监听，配置重载时更新值并触发回调。
 */
class DelegateConfigNode(
    private val config: ConfigurationSection,
    private val keys: Array<out String>,
) : LiveData<Any?> {

    override val id: String = keys.firstOrNull() ?: ""

    @Volatile
    private var cachedValue: Any? = readValue()
    private val callbacks = mutableListOf<(Any?) -> Unit>()

    init {
        // 如果是 Configuration，注册重载监听
        if (config is Configuration) {
            config.onReload { update() }
        }
    }

    override fun getValue(): Any? = cachedValue

    override fun onUpdate(callback: (Any?) -> Unit) {
        callbacks.add(callback)
    }

    override fun getValue(thisRef: Any?, property: KProperty<*>): Any? = cachedValue

    /** 手动触发更新 */
    fun update() {
        val newValue = readValue()
        cachedValue = newValue
        callbacks.forEach { it(newValue) }
    }

    private fun readValue(): Any? {
        for (key in keys) {
            if (config.contains(key)) {
                return config.get(key)
            }
        }
        return null
    }
}

/**
 * 从 ConfigurationSection 创建 LiveData
 */
fun ConfigurationSection.read(vararg keys: String): LiveData<Any?> {
    return DelegateConfigNode(this, keys)
}
