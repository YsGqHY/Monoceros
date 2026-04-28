package cc.bkhk.monoceros.api.util

import org.bukkit.command.CommandSender
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap

/**
 * 发送者类型适配器
 *
 * 将任意 sender 对象安全转换为 Bukkit [CommandSender]。
 * 支持直接传入 [CommandSender]，也支持传入 TabooLib 的 ProxyCommandSender
 * （含 relocate 后的变体），通过反射提取其 `origin` 属性完成转换。
 *
 * 这是项目中唯一的兼容桥接点：由于 TabooLib 类型在编译后会被 relocate
 * 到项目命名空间，外部插件无法直接引用，因此必须使用原生 Java 反射
 * （不会被 relocate）来安全提取底层 CommandSender。
 */
object SenderAdapter {

    /**
     * getOrigin 方法缓存，按 Class 缓存避免重复反射查找。
     * value 为 null 表示该类不支持 getOrigin。
     */
    private val originMethodCache = ConcurrentHashMap<Class<*>, Method?>()

    /**
     * 将任意 sender 对象转换为 [CommandSender]
     *
     * @param sender 可以是 [CommandSender]、ProxyCommandSender（含 relocated 变体）或 null
     * @return 转换后的 [CommandSender]，无法转换时返回 null
     */
    @JvmStatic
    fun adapt(sender: Any?): CommandSender? {
        if (sender == null) return null
        if (sender is CommandSender) return sender
        val method = originMethodCache.getOrPut(sender.javaClass) {
            try {
                sender.javaClass.getMethod("getOrigin")
            } catch (_: NoSuchMethodException) {
                null
            }
        }
        return try {
            method?.invoke(sender) as? CommandSender
        } catch (_: Exception) {
            null
        }
    }
}
