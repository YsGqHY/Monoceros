package cc.bkhk.monoceros.impl.script

import cc.bkhk.monoceros.api.script.MonocerosScriptHandler
import cc.bkhk.monoceros.api.script.MonocerosScriptSource
import cc.bkhk.monoceros.api.script.MonocerosScriptType
import cc.bkhk.monoceros.api.script.ScriptCacheStats
import cc.bkhk.monoceros.api.script.ScriptDefinition
import cc.bkhk.monoceros.api.script.ScriptDefinitionRegistry
import cc.bkhk.monoceros.api.script.ScriptInvokeRequest
import cc.bkhk.monoceros.impl.applicative.DefaultApplicativeRegistry
import cc.bkhk.monoceros.impl.exception.ScriptTypeMissingException
import cc.bkhk.monoceros.impl.registry.ConcurrentRegistry
import cc.bkhk.monoceros.impl.script.handler.FluxonHandler
import cc.bkhk.monoceros.impl.util.DiagnosticLogger
import org.bukkit.command.CommandSender
import taboolib.common.platform.function.getDataFolder
import java.io.File
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

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
            "cc.bkhk.monoceros.*",
            "cc.bkhk.monoceros.api.*",
            "org.bukkit.*",
            "org.bukkit.entity.*",
            "org.bukkit.inventory.*",
        )

        /**
         * Fluxon 处理器实例，由 [cc.bkhk.monoceros.impl.script.relocate.FluxonRelocateLoader] 设置。
         * 当 Fluxon 不可用时使用 [UnavailableFluxonHandler] 降级。
         */
        lateinit var fluxonHandler: FluxonHandler

        /** Fluxon 不可用时的降级实现 */
        private object UnavailableFluxonHandler : FluxonHandler {
            override fun invoke(source: String, id: String, sender: CommandSender?, variables: Map<String, Any?>): Any? = null
            override fun preheat(source: String, id: String) {}
            override fun invalidate(id: String) {}
            override fun invalidateByPrefix(prefix: String) {}
            override fun cacheStats(): ScriptCacheStats = ScriptCacheStats()
            override fun cleanup() {}
        }

        fun resolveFluxonHandler(): FluxonHandler {
            return if (::fluxonHandler.isInitialized) {
                fluxonHandler
            } else if (FluxonChecker.isReady()) {
                // Fluxon 已就绪但 handler 尚未被 RelocateLoader 设置，
                // 此时不应直接引用 Fluxon object 以避免类加载问题
                UnavailableFluxonHandler
            } else {
                UnavailableFluxonHandler
            }
        }
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
        sender: CommandSender?,
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

        // 高级封装：参数类型转换
        val convertedVars = applyParameterConversion(definition, variables)

        // 高级封装：前置条件检查
        if (!checkCondition(definition, sender, convertedVars)) return null

        // 执行脚本（带超时和异常处理）
        var result = executeWithAdvancedFeatures(definition, sender, convertedVars)

        // 高级封装：返回值类型转换
        result = applyReturnConversion(definition, result)

        return result
    }

    /** 应用参数类型转换 */
    private fun applyParameterConversion(definition: ScriptDefinition, variables: Map<String, Any?>): Map<String, Any?> {
        if (definition.parameters.isEmpty()) return variables
        val result = LinkedHashMap(variables)
        for ((paramName, typeName) in definition.parameters) {
            val rawValue = result[paramName] ?: continue
            val applicative = DefaultApplicativeRegistry.get<Any>(typeName)
            if (applicative != null) {
                result[paramName] = applicative.convertOrNull(rawValue) ?: rawValue
            }
        }
        return result
    }

    /** 检查前置条件 */
    private fun checkCondition(definition: ScriptDefinition, sender: CommandSender?, variables: Map<String, Any?>): Boolean {
        val conditionId = definition.condition ?: return true
        return try {
            val condResult = invoke(conditionId, sender, variables)
            val accepted = condResult == true || condResult == "true"
            if (!accepted) {
                definition.deny?.let { denyId -> invoke(denyId, sender, variables) }
            }
            accepted
        } catch (e: Exception) {
            DiagnosticLogger.warn("ScriptHandler", "条件脚本执行失败: $conditionId", e)
            true // 条件脚本异常时默认放行
        }
    }

    /** 带超时和异常处理的脚本执行 */
    private fun executeWithAdvancedFeatures(definition: ScriptDefinition, sender: CommandSender?, variables: Map<String, Any?>): Any? {
        val request = ScriptInvokeRequest(
            definitionId = definition.id,
            source = definition.source,
            sender = sender,
            variables = variables,
            asyncAllowed = definition.asyncAllowed,
        )

        return try {
            if (definition.timeoutMs > 0) {
                // 带超时执行
                val future = CompletableFuture.supplyAsync { invoke(request) }
                try {
                    future.get(definition.timeoutMs, TimeUnit.MILLISECONDS)
                } catch (_: TimeoutException) {
                    future.cancel(true)
                    DiagnosticLogger.warn("ScriptHandler", "脚本执行超时: ${definition.id} (${definition.timeoutMs}ms)")
                    definition.onTimeout?.let { invoke(it, sender, variables) }
                    null
                }
            } else {
                invoke(request)
            }
        } catch (e: Exception) {
            DiagnosticLogger.warn("ScriptHandler", "脚本执行异常: ${definition.id}", e)
            definition.onException?.let { invoke(it, sender, variables + ("exception" to e.message)) }
            null
        }
    }

    /** 应用返回值类型转换 */
    private fun applyReturnConversion(definition: ScriptDefinition, result: Any?): Any? {
        val conversionType = definition.returnConversion ?: return result
        if (result == null) return null
        val applicative = DefaultApplicativeRegistry.get<Any>(conversionType)
        return applicative?.convertOrNull(result) ?: result
    }

    /** 将脚本源码写入调试目录 */
    fun writeDebugScript(definitionId: String, source: String) {
        try {
            val debugDir = File(getDataFolder(), "debug/script")
            if (!debugDir.exists()) debugDir.mkdirs()
            val safeId = definitionId.replace('.', '/').replace(':', '_')
            val file = File(debugDir, "$safeId.fs")
            file.parentFile?.mkdirs()
            file.writeText(source, Charsets.UTF_8)
        } catch (e: Exception) {
            DiagnosticLogger.warn("ScriptHandler", "调试脚本写入失败: $definitionId", e)
        }
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
