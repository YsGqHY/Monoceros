package cc.bkhk.monoceros.impl.script

import cc.bkhk.monoceros.api.script.MonocerosScriptSource
import cc.bkhk.monoceros.api.script.MonocerosScriptType
import cc.bkhk.monoceros.api.script.ScriptCacheStats
import cc.bkhk.monoceros.api.script.ScriptInvokeRequest
import cc.bkhk.monoceros.impl.exception.ScriptExecuteException
import cc.bkhk.monoceros.impl.util.DiagnosticLogger
import org.bukkit.entity.Player
import org.tabooproject.fluxon.runtime.FluxonRuntime
import org.tabooproject.fluxon.runtime.error.FluxonRuntimeError
import org.tabooproject.fluxon.util.exceptFluxonCompletableFutureError
import org.tabooproject.fluxon.util.printError

/**
 * Fluxon 脚本类型实现
 *
 * 作为默认脚本类型注册到 [DefaultScriptHandler]，负责 Fluxon 脚本的编译与执行。
 */
class FluxonScriptType(
    private val cache: FluxonCompileCache,
    private val autoImports: Set<String>,
) : MonocerosScriptType {

    override val id: String = MonocerosScriptSource.DEFAULT_TYPE

    /** 系统保留变量名，业务变量不允许覆盖 */
    private val reservedVariables = setOf("sender", "player", "source", "scriptId", "now", "thread")

    override fun invoke(request: ScriptInvokeRequest): Any? {
        val scriptBase = cache.getOrCompile(request.definitionId, request.source.content, autoImports)
        // 每次执行创建新的 Environment，隔离变量（参考 Baikiruto）
        val env = FluxonRuntime.getInstance().newEnvironment()

        // 注入业务变量（先注入，后续系统变量覆盖冲突项）
        request.variables.forEach { (key, value) ->
            if (key in reservedVariables) {
                DiagnosticLogger.warn("Fluxon", "业务变量 '$key' 与系统保留变量冲突，已忽略")
                return@forEach
            }
            env.defineRootVariable(key, value)
        }

        // 注入系统变量
        env.defineRootVariable("sender", request.sender)
        env.defineRootVariable("source", request.source)
        env.defineRootVariable("scriptId", request.definitionId)
        env.defineRootVariable("now", System.currentTimeMillis())
        env.defineRootVariable("thread", Thread.currentThread().name)

        // 如果 sender 底层是 Player，额外注入 player 变量
        val platformSender = try {
            request.sender?.origin
        } catch (_: Exception) {
            null
        }
        if (platformSender is Player) {
            env.defineRootVariable("player", platformSender)
        }

        return try {
            scriptBase.eval(env)?.also { it.exceptFluxonCompletableFutureError() }
        } catch (e: FluxonRuntimeError) {
            DiagnosticLogger.warn("Fluxon", "脚本运行时错误: ${request.definitionId}")
            e.printError()
            throw ScriptExecuteException(request.definitionId, e)
        } catch (e: Exception) {
            DiagnosticLogger.warn("Fluxon", "脚本执行失败: ${request.definitionId}", e)
            throw ScriptExecuteException(request.definitionId, e)
        }
    }

    override fun preheat(definitionId: String, source: MonocerosScriptSource) {
        cache.compile(definitionId, source.content, autoImports)
        DiagnosticLogger.info("Fluxon", "脚本预热完成: $definitionId")
    }

    override fun invalidate(definitionId: String) {
        cache.invalidate(definitionId)
    }

    override fun invalidateByPrefix(prefix: String) {
        cache.invalidateByPrefix(prefix)
    }

    override fun cacheStats(): ScriptCacheStats = cache.stats()
}
