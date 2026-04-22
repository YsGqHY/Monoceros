package cc.bkhk.monoceros.impl.script

import cc.bkhk.monoceros.api.script.MonocerosScriptSource
import cc.bkhk.monoceros.api.script.MonocerosScriptType
import cc.bkhk.monoceros.api.script.ScriptCacheStats
import cc.bkhk.monoceros.api.script.ScriptInvokeRequest
import cc.bkhk.monoceros.impl.util.DiagnosticLogger

/**
 * Fluxon 脚本类型实现
 *
 * 作为默认脚本类型注册到 [DefaultScriptHandler]，通过 [DefaultScriptHandler.resolveFluxonHandler]
 * 间接调用 Fluxon 处理器，避免直接引用 Fluxon 类导致类加载失败。
 */
object FluxonScriptType : MonocerosScriptType {

    override val id: String = MonocerosScriptSource.DEFAULT_TYPE

    override fun invoke(request: ScriptInvokeRequest): Any? {
        val handler = DefaultScriptHandler.resolveFluxonHandler()
        return handler.invoke(request.source.content, request.definitionId, request.sender, request.variables)
    }

    override fun preheat(definitionId: String, source: MonocerosScriptSource) {
        val handler = DefaultScriptHandler.resolveFluxonHandler()
        handler.preheat(source.content, definitionId)
        DiagnosticLogger.debug("Fluxon", "脚本预热完成: $definitionId")
    }

    override fun invalidate(definitionId: String) {
        DefaultScriptHandler.resolveFluxonHandler().invalidate(definitionId)
    }

    override fun invalidateByPrefix(prefix: String) {
        DefaultScriptHandler.resolveFluxonHandler().invalidateByPrefix(prefix)
    }

    override fun cacheStats(): ScriptCacheStats {
        return DefaultScriptHandler.resolveFluxonHandler().cacheStats()
    }
}
