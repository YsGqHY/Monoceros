package cc.bkhk.monoceros.script

import cc.bkhk.monoceros.api.script.MonocerosScriptHandler
import cc.bkhk.monoceros.api.script.ScriptTaskTracker
import cc.bkhk.monoceros.impl.config.ConfigFileHash
import cc.bkhk.monoceros.impl.config.ConfigServiceCallback
import cc.bkhk.monoceros.impl.script.DefaultScriptDefinitionRegistry
import cc.bkhk.monoceros.impl.script.DefaultScriptHandler
import cc.bkhk.monoceros.impl.script.DefaultScriptTaskTracker
import cc.bkhk.monoceros.impl.script.FluxonCompileCache
import cc.bkhk.monoceros.impl.script.FluxonScriptType
import cc.bkhk.monoceros.impl.util.DiagnosticLogger
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.PlatformFactory

/**
 * 脚本系统生命周期注册器
 *
 * 在各生命周期阶段完成脚本处理器注册、资源加载、预热与清理。
 */
object ScriptServiceLoader {

    private const val MODULE = "Script"

    private val definitionRegistry = DefaultScriptDefinitionRegistry()
    private val compileCache = FluxonCompileCache()
    private val taskTracker = DefaultScriptTaskTracker()
    private lateinit var scriptHandler: DefaultScriptHandler
    private lateinit var scriptService: ScriptService

    @Awake(LifeCycle.LOAD)
    private fun onLoad() {
        // 构建脚本处理器并注册内置 Fluxon 类型
        scriptHandler = DefaultScriptHandler(definitionRegistry)
        val fluxonType = FluxonScriptType(compileCache, DefaultScriptHandler.DEFAULT_PACKAGE_AUTO_IMPORT)
        scriptHandler.registerScriptType(fluxonType)

        // 注册到 PlatformFactory
        PlatformFactory.registerAPI<MonocerosScriptHandler>(scriptHandler)
        PlatformFactory.registerAPI<ScriptTaskTracker>(taskTracker)
        DiagnosticLogger.info(MODULE, "脚本处理器已注册到 PlatformFactory")

        // 初始化资源加载服务
        scriptService = ScriptService(definitionRegistry)
    }

    @Awake(LifeCycle.ENABLE)
    private fun onEnable() {
        // 全量加载脚本定义
        val result = scriptService.loadAll()
        DiagnosticLogger.info(MODULE, "脚本加载完成: ${result.loaded} 成功, ${result.failed} 失败, 耗时 ${result.costMs}ms")

        // 启动文件监听
        scriptService.startWatcher(object : ConfigServiceCallback {
            override fun onCreated(fileId: String, hash: ConfigFileHash) {
                DiagnosticLogger.info(MODULE, "检测到新脚本文件: $fileId")
                reloadAll()
            }

            override fun onModified(fileId: String, hash: ConfigFileHash) {
                DiagnosticLogger.info(MODULE, "检测到脚本文件变更: $fileId")
                reloadAll()
            }

            override fun onDeleted(fileId: String) {
                DiagnosticLogger.info(MODULE, "检测到脚本文件删除: $fileId")
                reloadAll()
            }
        })
    }

    @Awake(LifeCycle.ACTIVE)
    private fun onActive() {
        // 预热标记为 preheat 的脚本
        var preheatCount = 0
        definitionRegistry.values().filter { it.preheat && it.enabled }.forEach { def ->
            try {
                scriptHandler.preheat(def.source, def.id)
                preheatCount++
            } catch (e: Exception) {
                DiagnosticLogger.warn(MODULE, "脚本预热失败: ${def.id}", e)
            }
        }
        if (preheatCount > 0) {
            DiagnosticLogger.info(MODULE, "脚本预热完成: $preheatCount 个")
        }

        // 输出缓存统计基线
        val stats = scriptHandler.cacheStats()
        DiagnosticLogger.info(MODULE, "缓存基线: size=${stats.cacheSize}, compilations=${stats.totalCompilations}")
    }

    @Awake(LifeCycle.DISABLE)
    private fun onDisable() {
        // 停止文件监听
        scriptService.stopWatcher()

        // 停止所有活跃脚本任务
        val activeCount = taskTracker.activeCount()
        if (activeCount > 0) {
            DiagnosticLogger.warn(MODULE, "停止 $activeCount 个活跃脚本任务")
            // 遍历所有定义，停止关联任务
            definitionRegistry.keys().forEach { defId ->
                taskTracker.stopByDefinition(defId)
            }
        }
        taskTracker.purgeCompleted()

        // 清空编译缓存并清理 Fluxon 运行时
        compileCache.clear()
        compileCache.cleanup()

        DiagnosticLogger.info(MODULE, "脚本系统已清理")
    }

    /** 全量重载脚本 */
    private fun reloadAll() {
        // 失效所有缓存
        definitionRegistry.keys().forEach { scriptHandler.invalidate(it) }
        // 停止所有活跃任务
        definitionRegistry.keys().forEach { taskTracker.stopByDefinition(it) }
        // 重新加载
        val result = scriptService.loadAll()
        DiagnosticLogger.info(MODULE, "脚本热重载: ${result.loaded} 成功, ${result.failed} 失败, 耗时 ${result.costMs}ms")
        // 重新预热
        definitionRegistry.values().filter { it.preheat && it.enabled }.forEach { def ->
            try {
                scriptHandler.preheat(def.source, def.id)
            } catch (e: Exception) {
                DiagnosticLogger.warn(MODULE, "热重载预热失败: ${def.id}", e)
            }
        }
    }
}
