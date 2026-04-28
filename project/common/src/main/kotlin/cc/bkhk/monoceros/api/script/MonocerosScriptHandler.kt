package cc.bkhk.monoceros.api.script

import cc.bkhk.monoceros.api.util.SenderAdapter
import org.bukkit.command.CommandSender

/**
 * 脚本处理器统一入口
 *
 * 所有脚本调用通过此接口分发到具体的 [MonocerosScriptType] 执行。
 */
interface MonocerosScriptHandler {

    fun invoke(request: ScriptInvokeRequest): Any?

    /**
     * 按脚本定义 ID 调用脚本
     *
     * 自动从 [ScriptDefinitionRegistry] 查找脚本定义并构造请求。
     * 若定义不存在则返回 null 并输出警告。
     */
    fun invoke(
        definitionId: String,
        sender: CommandSender? = null,
        variables: Map<String, Any?> = emptyMap(),
    ): Any?

    /**
     * 兼容旧版 API：接受任意 sender 类型（含 relocated ProxyCommandSender）
     *
     * 内部通过 [SenderAdapter] 将 sender 转换为 [CommandSender] 后委托给主方法。
     */
    fun invokeCompat(
        definitionId: String,
        sender: Any?,
        variables: Map<String, Any?> = emptyMap(),
    ): Any? {
        return invoke(definitionId, SenderAdapter.adapt(sender), variables)
    }

    fun preheat(definitionId: String)

    fun preheat(source: MonocerosScriptSource, definitionId: String)

    fun registerScriptType(scriptType: MonocerosScriptType): MonocerosScriptType

    fun unregisterScriptType(typeId: String): MonocerosScriptType?

    fun getScriptType(typeId: String): MonocerosScriptType?

    fun invalidate(definitionId: String)

    fun invalidateByPrefix(prefix: String)

    fun cacheStats(): ScriptCacheStats
}
