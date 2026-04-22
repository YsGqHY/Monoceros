package cc.bkhk.monoceros.dispatcher

import cc.bkhk.monoceros.impl.util.DiagnosticLogger
import org.bukkit.event.Event
import java.util.concurrent.ConcurrentHashMap

/**
 * 事件类名到 Class 的映射工具
 *
 * 支持三种格式：
 * - 全限定类名：org.bukkit.event.player.PlayerJoinEvent
 * - Bukkit 短名称：PlayerJoinEvent（自动遍历 Bukkit 事件包匹配）
 * - 缓存已解析的映射，避免重复反射
 */
object EventClassMapping {

    private const val MODULE = "EventMapping"

    /** Bukkit 事件包列表 */
    private val BUKKIT_EVENT_PACKAGES = listOf(
        "org.bukkit.event.block",
        "org.bukkit.event.enchantment",
        "org.bukkit.event.entity",
        "org.bukkit.event.hanging",
        "org.bukkit.event.inventory",
        "org.bukkit.event.player",
        "org.bukkit.event.raid",
        "org.bukkit.event.server",
        "org.bukkit.event.vehicle",
        "org.bukkit.event.weather",
        "org.bukkit.event.world",
        "cc.bkhk.monoceros.dispatcher.event",
    )

    /** 解析缓存 */
    private val cache = ConcurrentHashMap<String, Class<out Event>>()

    /**
     * 将事件名称解析为事件类
     *
     * @param name 事件名称（全限定名或短名称）
     * @return 事件类，解析失败返回 null
     */
    @Suppress("UNCHECKED_CAST")
    fun resolve(name: String): Class<out Event>? {
        cache[name]?.let { return it }

        val clazz = if (name.contains('.')) {
            // 全限定类名
            try {
                Class.forName(name) as? Class<out Event>
            } catch (e: ClassNotFoundException) {
                DiagnosticLogger.warn(MODULE, "事件类未找到: $name")
                null
            }
        } else {
            // 短名称，遍历 Bukkit 事件包匹配
            resolveShortName(name)
        }

        if (clazz != null) {
            cache[name] = clazz
        }
        return clazz
    }

    /** 遍历 Bukkit 事件包尝试匹配短名称 */
    @Suppress("UNCHECKED_CAST")
    private fun resolveShortName(name: String): Class<out Event>? {
        for (pkg in BUKKIT_EVENT_PACKAGES) {
            try {
                val clazz = Class.forName("$pkg.$name")
                if (Event::class.java.isAssignableFrom(clazz)) {
                    return clazz as Class<out Event>
                }
            } catch (_: ClassNotFoundException) {
                continue
            }
        }
        DiagnosticLogger.warn(MODULE, "无法解析事件短名称: $name")
        return null
    }

    /** 清空缓存 */
    fun clearCache() {
        cache.clear()
    }
}
