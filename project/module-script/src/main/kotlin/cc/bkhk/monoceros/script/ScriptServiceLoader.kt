package cc.bkhk.monoceros.script

import cc.bkhk.monoceros.api.script.MonocerosScriptHandler
import cc.bkhk.monoceros.api.script.ScriptTaskTracker
import cc.bkhk.monoceros.api.service.ReloadableService
import cc.bkhk.monoceros.impl.config.ConfigFileHash
import cc.bkhk.monoceros.impl.config.ConfigServiceCallback
import cc.bkhk.monoceros.impl.script.DefaultScriptDefinitionRegistry
import cc.bkhk.monoceros.impl.script.DefaultScriptHandler
import cc.bkhk.monoceros.impl.script.DefaultScriptTaskTracker
import cc.bkhk.monoceros.impl.script.FluxonScriptType
import cc.bkhk.monoceros.impl.service.ReloadService
import cc.bkhk.monoceros.impl.util.DiagnosticLogger
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.PlatformFactory
import java.io.File

/**
 * 脚本系统生命周期注册器
 *
 * 在各生命周期阶段完成脚本处理器注册、资源加载、预热与清理。
 */
object ScriptServiceLoader {

    private const val MODULE = "Script"

    private val definitionRegistry = DefaultScriptDefinitionRegistry()
    private val taskTracker = DefaultScriptTaskTracker()
    private lateinit var scriptHandler: DefaultScriptHandler
    private lateinit var scriptService: ScriptService

    @Awake(LifeCycle.LOAD)
    private fun onLoad() {
        // 构建脚本处理器并注册内置 Fluxon 类型
        scriptHandler = DefaultScriptHandler(definitionRegistry)
        scriptHandler.registerScriptType(FluxonScriptType)

        // 注册到 PlatformFactory
        PlatformFactory.registerAPI<MonocerosScriptHandler>(scriptHandler)
        PlatformFactory.registerAPI<ScriptTaskTracker>(taskTracker)
        PlatformFactory.registerAPI<cc.bkhk.monoceros.api.script.ScriptDefinitionRegistry>(definitionRegistry)
        DiagnosticLogger.info(MODULE, "脚本处理器已注册到 PlatformFactory")

        // 初始化资源加载服务
        scriptService = ScriptService(definitionRegistry)

        // 注册到统一重载服务
        ReloadService.register(object : ReloadableService {
            override val serviceId = "script"
            override val priority = 100
            override fun reload(): Int {
                reloadAll()
                return definitionRegistry.keys().size
            }
        })
    }

    @Awake(LifeCycle.ENABLE)
    private fun onEnable() {
        // 全量加载脚本定义
        val result = scriptService.loadAll()
        DiagnosticLogger.info(MODULE, "脚本加载完成: ${result.loaded} 成功, ${result.failed} 失败, 耗时 ${result.costMs}ms")

        // 启动文件监听（增量回调）
        scriptService.startWatcher(object : ConfigServiceCallback {
            override fun onCreated(fileId: String, hash: ConfigFileHash) {
                DiagnosticLogger.info(MODULE, "检测到新脚本文件: $fileId")
                reloadFile(fileId)
            }

            override fun onModified(fileId: String, hash: ConfigFileHash) {
                DiagnosticLogger.info(MODULE, "检测到脚本文件变更: $fileId")
                reloadFile(fileId)
            }

            override fun onDeleted(fileId: String) {
                DiagnosticLogger.info(MODULE, "检测到脚本文件删除: $fileId")
                removeFile(fileId)
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

        // 清理 Fluxon 运行时
        DefaultScriptHandler.resolveFluxonHandler().cleanup()

        DiagnosticLogger.info(MODULE, "脚本系统已清理")
    }

    /** 增量重载单个文件 */
    private fun reloadFile(fileId: String) {
        val dir = scriptService.directory()
        val file = resolveFile(dir, fileId) ?: return
        val result = scriptService.reload(file.toPath())
        DiagnosticLogger.info(MODULE, "脚本增量重载: $fileId -> ${result.loaded} 成功, ${result.failed} 失败")
        // 对重载的脚本重新预热
        definitionRegistry.values()
            .filter { it.preheat && it.enabled && it.file?.toFile()?.let { f -> f.relativeTo(dir).path.replace('\\', '/') } == fileId.replace('.', '/') + "." + (it.file?.toFile()?.extension ?: "") }
            .forEach { def ->
                try {
                    scriptHandler.preheat(def.source, def.id)
                } catch (e: Exception) {
                    DiagnosticLogger.warn(MODULE, "增量预热失败: ${def.id}", e)
                }
            }
    }

    /** 增量移除单个文件 */
    private fun removeFile(fileId: String) {
        val dir = scriptService.directory()
        // 文件已删除，构造路径用于 remove
        val possibleExtensions = listOf("fs", "yml", "yaml")
        val relativePath = fileId.replace('.', '/')
        for (ext in possibleExtensions) {
            val file = File(dir, "$relativePath.$ext")
            // 先 invalidate 该文件关联的脚本缓存
            val path = file.toPath()
            scriptService.remove(path)
        }
        DiagnosticLogger.info(MODULE, "脚本文件已移除: $fileId")
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

    /** 根据 fileId 解析实际文件 */
    private fun resolveFile(dir: File, fileId: String): File? {
        val relativePath = fileId.replace('.', '/')
        for (ext in listOf("fs", "yml", "yaml")) {
            val file = File(dir, "$relativePath.$ext")
            if (file.exists()) return file
        }
        // 尝试 walkTopDown 查找
        return dir.walkTopDown().find { f ->
            f.isFile && !f.name.startsWith("#") && f.extension in setOf("fs", "yml", "yaml") &&
                f.relativeTo(dir).path.replace('\\', '/').substringBeforeLast('.').replace('/', '.') == fileId
        }
    }
}
