package cc.bkhk.monoceros.impl.script

import cc.bkhk.monoceros.api.script.MonocerosScriptHandler
import cc.bkhk.monoceros.api.script.MonocerosScriptSource
import cc.bkhk.monoceros.api.script.MonocerosScriptType
import cc.bkhk.monoceros.api.script.ScriptCacheStats
import cc.bkhk.monoceros.api.script.ScriptDefinition
import cc.bkhk.monoceros.api.script.ScriptDefinitionRegistry
import cc.bkhk.monoceros.api.script.ScriptInvokeRequest
import cc.bkhk.monoceros.impl.exception.ScriptTypeMissingException
import cc.bkhk.monoceros.impl.registry.ConcurrentRegistry
import cc.bkhk.monoceros.impl.util.DiagnosticLogger
import taboolib.common.platform.ProxyCommandSender

/**
 * 脚本处理器默认实现
 *
 * 统一入口，根据脚本类型分发到对应的 [MonocerosScriptType] 执行。
 * 缓存统计与失效操作遍历所有已注册脚本类型聚合结果。
 */
class DefaultScriptHandler(
    private val definitionRegistry: ScriptDefinitionRegistry,
) : MonocerosScriptHandler {

    companion object {
        /** 自动导入包集合，在 Fluxon 编译前注入 */
        val DEFAULT_PACKAGE_AUTO_IMPORT: MutableSet<String> = mutableSetOf(
            "cc.bkhk.monoceros.api.*",
            "org.bukkit.*",
            "org.bukkit.entity.*",
            "org.bukkit.inventory.*",
        )
    }

    private val scriptTypeRegistry = ConcurrentRegistry<MonocerosScriptType>()

    override fun invoke(request: ScriptInvokeRequest): Any? {
        val typeId = request.source.type
        val scriptType = scriptTypeRegistry.get(typeId)
            ?: throw ScriptTypeMissingException(typeId)
        return scriptType.invoke(request)
    }

    override fun invoke(
        definitionId: String,
        sender: ProxyCommandSender?,
        variables: Map<String, Any?>,
    ): Any? {
        val definition = definitionRegistry.get(definitionId)
        if (definition == null) {
            DiagnosticLogger.warn("ScriptHandler", "脚本定义不存在: $definitionId")
            return null
        }
        if (!definition.enabled) {
            DiagnosticLogger.warn("ScriptHandler", "脚本已禁用: $definitionId")
            return null
        }
        return invoke(
            ScriptInvokeRequest(
                definitionId = definitionId,
                source = definition.source,
                sender = sender,
                variables = variables,
                asyncAllowed = definition.asyncAllowed,
            )
        )
    }

    override fun preheat(definitionId: String) {
        val definition = definitionRegistry.get(definitionId)
        if (definition == null) {
            DiagnosticLogger.warn("ScriptHandler", "预热失败，脚本定义不存在: $definitionId")
            return
        }
        preheat(definition.source, definitionId)
    }

    override fun preheat(source: MonocerosScriptSource, definitionId: String) {
        val typeId = source.type
        val scriptType = scriptTypeRegistry.get(typeId)
        if (scriptType == null) {
            DiagnosticLogger.warn("ScriptHandler", "预热失败，脚本类型未注册: $typeId")
            return
        }
        scriptType.preheat(definitionId, source)
    }

    override fun registerScriptType(scriptType: MonocerosScriptType): MonocerosScriptType {
        scriptTypeRegistry.register(scriptType.id, scriptType)
        DiagnosticLogger.info("ScriptHandler", "注册脚本类型: ${scriptType.id}")
        return scriptType
    }

    override fun unregisterScriptType(typeId: String): MonocerosScriptType? {
        val removed = scriptTypeRegistry.unregister(typeId)
        if (removed != null) {
            DiagnosticLogger.info("ScriptHandler", "注销脚本类型: $typeId")
        }
        return removed
    }

    override fun getScriptType(typeId: String): MonocerosScriptType? {
        return scriptTypeRegistry.get(typeId)
    }

    override fun invalidate(definitionId: String) {
        scriptTypeRegistry.values().forEach { it.invalidate(definitionId) }
    }

    override fun invalidateByPrefix(prefix: String) {
        scriptTypeRegistry.values().forEach { it.invalidateByPrefix(prefix) }
    }

    override fun cacheStats(): ScriptCacheStats {
        val allStats = scriptTypeRegistry.values().map { it.cacheStats() }
        if (allStats.isEmpty()) return ScriptCacheStats()
        return ScriptCacheStats(
            cacheSize = allStats.sumOf { it.cacheSize },
            invokeHits = allStats.sumOf { it.invokeHits },
            invokeMisses = allStats.sumOf { it.invokeMisses },
            totalCompilations = allStats.sumOf { it.totalCompilations },
            totalCompilationNanos = allStats.sumOf { it.totalCompilationNanos },
        )
    }
}
