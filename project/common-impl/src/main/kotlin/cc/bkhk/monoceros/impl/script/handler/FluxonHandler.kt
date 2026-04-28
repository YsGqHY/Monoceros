package cc.bkhk.monoceros.impl.script.handler

import cc.bkhk.monoceros.api.script.ScriptCacheStats
import org.bukkit.command.CommandSender

/**
 * Fluxon 处理器接口
 *
 * 抽象 Fluxon 的编译、执行、缓存操作，使上层代码不直接依赖 Fluxon 类。
 * 当 Fluxon 不可用时，由 [cc.bkhk.monoceros.impl.script.DefaultScriptHandler] 降级为 Noop 实现。
 */
interface FluxonHandler {

    fun invoke(
        source: String,
        id: String,
        sender: CommandSender?,
        variables: Map<String, Any?>
    ): Any?

    fun preheat(source: String, id: String)

    fun invalidate(id: String)

    fun invalidateByPrefix(prefix: String)

    fun cacheStats(): ScriptCacheStats

    /** 清理运行时资源，在 DISABLE 阶段调用 */
    fun cleanup()
}
