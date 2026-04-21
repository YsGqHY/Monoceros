package cc.bkhk.monoceros.impl.script

import cc.bkhk.monoceros.api.script.ScriptCacheStats
import cc.bkhk.monoceros.impl.exception.ScriptCompileException
import cc.bkhk.monoceros.impl.util.DiagnosticLogger
import org.tabooproject.fluxon.Fluxon
import org.tabooproject.fluxon.FluxonPlugin
import org.tabooproject.fluxon.compiler.CompilationContext
import org.tabooproject.fluxon.interpreter.bytecode.FluxonClassLoader
import org.tabooproject.fluxon.runtime.FluxonRuntime
import org.tabooproject.fluxon.runtime.RuntimeScriptBase
import taboolib.platform.BukkitPlugin
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Fluxon 编译缓存
 *
 * 缓存已编译的脚本实例，避免重复编译。
 * 缓存键为脚本定义 ID，内容变更时由外部调用 invalidate 失效旧缓存后重新编译。
 * 统计命中率与编译耗时，供诊断使用。
 */
class FluxonCompileCache {

    private val compiled = ConcurrentHashMap<String, RuntimeScriptBase>()
    private val classLoader = FluxonClassLoader(BukkitPlugin::class.java.classLoader)
    private val environment = FluxonRuntime.getInstance().newEnvironment()

    private val invokeHits = AtomicLong(0)
    private val invokeMisses = AtomicLong(0)
    private val totalCompilations = AtomicLong(0)
    private val totalCompilationNanos = AtomicLong(0)

    init {
        // 允许在非脚本环境执行任务（参考 Baikiruto）
        FluxonPlugin.DEFAULT_ALLOW_EXECUTE_TASK_ON_NON_SCRIPT_ENV = true
        // 设置共享身份标识，用于跨插件函数共享
        FluxonRuntime.getInstance().sharingIdentity = "Monoceros"
    }

    /**
     * 获取已编译脚本，未命中时自动编译并缓存
     *
     * @param id 脚本定义 ID
     * @param source 脚本源码
     * @param autoImports 自动导入包集合
     * @return 编译后的脚本实例
     */
    fun getOrCompile(id: String, source: String, autoImports: Set<String>): RuntimeScriptBase {
        val cached = compiled[id]
        if (cached != null) {
            invokeHits.incrementAndGet()
            return cached
        }
        invokeMisses.incrementAndGet()
        return compile(id, source, autoImports)
    }

    /**
     * 预编译脚本并缓存
     */
    fun compile(id: String, source: String, autoImports: Set<String>): RuntimeScriptBase {
        val startNanos = System.nanoTime()
        try {
            val context = CompilationContext(source).apply {
                packageAutoImport += autoImports
            }
            // 使用脚本 ID + 时间戳作为唯一类名，避免类名冲突
            val className = id.replace('.', '_') + "_" + System.currentTimeMillis()
            val result = Fluxon.compile(environment, context, className, classLoader)
            val instance = result.createInstance(classLoader) as RuntimeScriptBase
            compiled[id] = instance
            totalCompilations.incrementAndGet()
            totalCompilationNanos.addAndGet(System.nanoTime() - startNanos)
            return instance
        } catch (e: Exception) {
            DiagnosticLogger.warn("FluxonCache", "脚本编译失败: $id", e)
            throw ScriptCompileException(id, e)
        }
    }

    /** 按 ID 失效缓存 */
    fun invalidate(id: String) {
        compiled.remove(id)
    }

    /** 按前缀批量失效缓存 */
    fun invalidateByPrefix(prefix: String) {
        compiled.keys.filter { it.startsWith(prefix) }.forEach { compiled.remove(it) }
    }

    /** 清空全部缓存 */
    fun clear() {
        compiled.clear()
    }

    /**
     * 清理 Fluxon 运行时资源
     *
     * 在 DISABLE 阶段调用，释放共享函数导出。
     */
    fun cleanup() {
        FluxonRuntime.getInstance().unexportAll()
    }

    /** 当前缓存大小 */
    fun size(): Int = compiled.size

    /** 导出缓存统计 */
    fun stats(): ScriptCacheStats = ScriptCacheStats(
        cacheSize = compiled.size,
        invokeHits = invokeHits.get(),
        invokeMisses = invokeMisses.get(),
        totalCompilations = totalCompilations.get(),
        totalCompilationNanos = totalCompilationNanos.get(),
    )
}
