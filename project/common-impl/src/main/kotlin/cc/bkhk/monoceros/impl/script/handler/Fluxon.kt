package cc.bkhk.monoceros.impl.script.handler

import cc.bkhk.monoceros.api.script.ScriptCacheStats
import cc.bkhk.monoceros.impl.script.DefaultScriptHandler
import cc.bkhk.monoceros.impl.script.relocate.FluxonRelocate
import cc.bkhk.monoceros.impl.util.DiagnosticLogger
import org.bukkit.entity.Player
import org.tabooproject.fluxon.Fluxon
import org.tabooproject.fluxon.FluxonPlugin
import org.tabooproject.fluxon.compiler.CompilationContext
import org.tabooproject.fluxon.interpreter.bytecode.FluxonClassLoader
import org.tabooproject.fluxon.runtime.FluxonRuntime
import org.tabooproject.fluxon.runtime.RuntimeScriptBase
import org.tabooproject.fluxon.runtime.error.FluxonRuntimeError
import org.tabooproject.fluxon.util.exceptFluxonCompletableFutureError
import org.tabooproject.fluxon.util.printError
import taboolib.common.LifeCycle
import taboolib.common.Requires
import taboolib.common.platform.Awake
import taboolib.common.platform.ProxyCommandSender
import taboolib.platform.BukkitPlugin
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Fluxon 处理器实现
 *
 * 直接引用 Fluxon 类，标记 @FluxonRelocate 以支持外部 FluxonPlugin 存在时的 ASM 转译。
 * 标记 @Requires 确保只在 Fluxon 运行时可用时才被加载。
 */
@Requires(missingClasses = ["!org.tabooproject.fluxon.ParseScript"])
@FluxonRelocate
object Fluxon : FluxonHandler {

    private const val MODULE = "Fluxon"

    private val compiledScripts = ConcurrentHashMap<String, RuntimeScriptBase>()
    private val classLoader = FluxonClassLoader(BukkitPlugin::class.java.classLoader)
    private val environment = FluxonRuntime.getInstance().newEnvironment()

    private val invokeHits = AtomicLong(0)
    private val invokeMisses = AtomicLong(0)
    private val totalCompilations = AtomicLong(0)
    private val totalCompilationNanos = AtomicLong(0)

    /** 系统保留变量名，业务变量不允许覆盖 */
    private val reservedVariables = setOf("sender", "player", "source", "scriptId", "now", "thread")

    init {
        // 允许在非脚本环境执行任务（参考 Baikiruto）
        FluxonPlugin.DEFAULT_ALLOW_EXECUTE_TASK_ON_NON_SCRIPT_ENV = true
        // 设置共享身份标识，用于跨插件函数共享
        FluxonRuntime.getInstance().sharingIdentity = "Monoceros"
    }

    @Awake(LifeCycle.DISABLE)
    private fun onDisable() {
        cleanup()
    }

    override fun invoke(
        source: String,
        id: String,
        sender: ProxyCommandSender?,
        variables: Map<String, Any?>
    ): Any? {
        if (!compiledScripts.containsKey(id)) {
            invokeMisses.incrementAndGet()
            preheat(source, id)
        } else {
            invokeHits.incrementAndGet()
        }

        val scriptBase = compiledScripts[id] ?: return null

        // 每次执行创建新的 Environment，隔离变量
        val env = FluxonRuntime.getInstance().newEnvironment()

        // 注入业务变量（先注入，后续系统变量覆盖冲突项）
        variables.forEach { (key, value) ->
            if (key in reservedVariables) {
                DiagnosticLogger.warn(MODULE, "业务变量 '$key' 与系统保留变量冲突，已忽略")
                return@forEach
            }
            env.defineRootVariable(key, value)
        }

        // 注入系统变量
        env.defineRootVariable("sender", sender)
        env.defineRootVariable("source", source)
        env.defineRootVariable("scriptId", id)
        env.defineRootVariable("now", System.currentTimeMillis())
        env.defineRootVariable("thread", Thread.currentThread().name)

        // 如果 sender 底层是 Player，额外注入 player 变量
        val platformSender = try {
            sender?.origin
        } catch (_: Exception) {
            null
        }
        if (platformSender is Player) {
            env.defineRootVariable("player", platformSender)
        }

        return try {
            scriptBase.eval(env)?.also { it.exceptFluxonCompletableFutureError() }
        } catch (e: FluxonRuntimeError) {
            DiagnosticLogger.warn(MODULE, "脚本运行时错误: $id")
            e.printError()
            null
        } catch (e: Throwable) {
            DiagnosticLogger.warn(MODULE, "脚本执行失败: $id", e)
            null
        }
    }

    override fun preheat(source: String, id: String) {
        val startNanos = System.nanoTime()
        try {
            val context = CompilationContext(source).apply {
                packageAutoImport += DefaultScriptHandler.DEFAULT_PACKAGE_AUTO_IMPORT
            }
            // 使用脚本 ID + 时间戳作为唯一类名，避免类名冲突
            val className = id.replace('.', '_') + "_" + System.currentTimeMillis()
            val result = Fluxon.compile(environment, context, className, classLoader)
            compiledScripts[id] = result.createInstance(classLoader) as RuntimeScriptBase
            totalCompilations.incrementAndGet()
            totalCompilationNanos.addAndGet(System.nanoTime() - startNanos)
        } catch (e: Throwable) {
            DiagnosticLogger.warn(MODULE, "脚本编译失败: $id", e)
        }
    }

    override fun invalidate(id: String) {
        compiledScripts.remove(id)
    }

    override fun invalidateByPrefix(prefix: String) {
        compiledScripts.keys
            .filter { it.startsWith(prefix) }
            .forEach { compiledScripts.remove(it) }
    }

    override fun cacheStats(): ScriptCacheStats {
        return ScriptCacheStats(
            cacheSize = compiledScripts.size,
            invokeHits = invokeHits.get(),
            invokeMisses = invokeMisses.get(),
            totalCompilations = totalCompilations.get(),
            totalCompilationNanos = totalCompilationNanos.get()
        )
    }

    override fun cleanup() {
        compiledScripts.clear()
        try {
            FluxonRuntime.getInstance().unexportAll()
        } catch (_: Throwable) {
            // 静默忽略
        }
    }
}
