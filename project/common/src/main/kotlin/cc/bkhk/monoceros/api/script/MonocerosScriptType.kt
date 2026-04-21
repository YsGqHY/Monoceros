package cc.bkhk.monoceros.api.script

/**
 * 脚本类型执行策略接口
 *
 * 每种脚本类型（如 Fluxon）实现此接口，由 [MonocerosScriptHandler] 统一分发。
 */
interface MonocerosScriptType {

    val id: String

    fun invoke(request: ScriptInvokeRequest): Any?

    fun preheat(definitionId: String, source: MonocerosScriptSource) {}

    fun invalidate(definitionId: String) {}

    fun invalidateByPrefix(prefix: String) {}

    fun cacheStats(): ScriptCacheStats = ScriptCacheStats()
}
