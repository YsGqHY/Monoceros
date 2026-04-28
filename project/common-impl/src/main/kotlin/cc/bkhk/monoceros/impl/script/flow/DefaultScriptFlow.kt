package cc.bkhk.monoceros.impl.script.flow

import cc.bkhk.monoceros.Monoceros
import cc.bkhk.monoceros.api.script.ScriptFlow
import cc.bkhk.monoceros.impl.util.DiagnosticLogger
import org.bukkit.command.CommandSender
import java.util.concurrent.atomic.AtomicBoolean

/**
 * ScriptFlow 默认实现
 *
 * 有序执行脚本链，支持前置/后置处理、共享变量流转、失败处理和主动终止。
 */
class DefaultScriptFlow(
    private val sender: CommandSender?,
    initialVariables: Map<String, Any?> = emptyMap(),
) : ScriptFlow {

    private companion object {
        const val MODULE = "ScriptFlow"
    }

    /** 脚本执行条目 */
    private data class ScriptEntry(
        val scriptId: String,
        val extraVariables: Map<String, Any?> = emptyMap(),
        var preprocess: ((MutableMap<String, Any?>) -> Unit)? = null,
        var postprocess: ((Any?, MutableMap<String, Any?>) -> Unit)? = null,
    )

    private val entries = mutableListOf<ScriptEntry>()
    private val sharedVariables = LinkedHashMap<String, Any?>()
    private val terminated = AtomicBoolean(false)
    private var failureHandler: ((Exception) -> Unit)? = null

    init {
        sharedVariables.putAll(initialVariables)
    }

    override fun add(scriptId: String): ScriptFlow {
        entries.add(ScriptEntry(scriptId))
        return this
    }

    override fun add(scriptId: String, extraVariables: Map<String, Any?>): ScriptFlow {
        entries.add(ScriptEntry(scriptId, extraVariables))
        return this
    }

    override fun preprocess(handler: (MutableMap<String, Any?>) -> Unit): ScriptFlow {
        entries.lastOrNull()?.preprocess = handler
        return this
    }

    override fun postprocess(handler: (Any?, MutableMap<String, Any?>) -> Unit): ScriptFlow {
        entries.lastOrNull()?.postprocess = handler
        return this
    }

    override fun onFailure(handler: (Exception) -> Unit): ScriptFlow {
        failureHandler = handler
        return this
    }

    override fun terminate() {
        terminated.set(true)
    }

    override fun isTerminated(): Boolean = terminated.get()

    override fun variables(): MutableMap<String, Any?> = sharedVariables

    override fun execute(): Any? {
        var lastResult: Any? = null
        val scriptHandler = Monoceros.api().scripts()

        for (entry in entries) {
            if (terminated.get()) break

            try {
                // 合并变量
                val variables = LinkedHashMap(sharedVariables)
                variables.putAll(entry.extraVariables)

                // 前置处理
                entry.preprocess?.invoke(variables)
                if (terminated.get()) break

                // 执行脚本
                lastResult = scriptHandler.invoke(entry.scriptId, sender, variables)

                // 后置处理
                entry.postprocess?.invoke(lastResult, sharedVariables)
                if (terminated.get()) break

                // 将结果写入共享变量
                sharedVariables["lastResult"] = lastResult
            } catch (e: Exception) {
                DiagnosticLogger.warn(MODULE, "脚本执行失败: ${entry.scriptId}", e)
                failureHandler?.invoke(e)
                terminate()
                break
            }
        }

        return lastResult
    }
}
