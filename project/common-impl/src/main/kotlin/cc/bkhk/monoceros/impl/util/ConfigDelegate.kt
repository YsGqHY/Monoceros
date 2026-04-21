package cc.bkhk.monoceros.impl.util

import taboolib.library.configuration.ConfigurationSection

/**
 * 配置解析辅助委托
 *
 * 提供 lazy 方式从 ConfigurationSection 读取字段，避免重复样板代码。
 */
class ConfigDelegate(private val section: ConfigurationSection) {

    fun string(key: String, default: String = ""): Lazy<String> = lazy {
        section.getString(key) ?: default
    }

    fun int(key: String, default: Int = 0): Lazy<Int> = lazy {
        section.getInt(key, default)
    }

    fun long(key: String, default: Long = 0L): Lazy<Long> = lazy {
        section.getLong(key, default)
    }

    fun double(key: String, default: Double = 0.0): Lazy<Double> = lazy {
        section.getDouble(key, default)
    }

    fun boolean(key: String, default: Boolean = false): Lazy<Boolean> = lazy {
        section.getBoolean(key, default)
    }

    fun stringList(key: String): Lazy<List<String>> = lazy {
        section.getStringList(key) ?: emptyList()
    }

    fun section(key: String): Lazy<ConfigurationSection?> = lazy {
        section.getConfigurationSection(key)
    }
}
