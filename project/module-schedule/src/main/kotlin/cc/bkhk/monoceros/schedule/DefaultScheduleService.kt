package cc.bkhk.monoceros.schedule

import cc.bkhk.monoceros.api.registry.Registry
import cc.bkhk.monoceros.api.schedule.ScheduleDefinition
import cc.bkhk.monoceros.api.schedule.ScheduleHandle
import cc.bkhk.monoceros.api.schedule.ScheduleHandler
import cc.bkhk.monoceros.api.schedule.ScheduleRoute
import cc.bkhk.monoceros.api.schedule.ScheduleService
import cc.bkhk.monoceros.api.schedule.ScheduleState
import cc.bkhk.monoceros.api.schedule.ScheduleType
import cc.bkhk.monoceros.api.schedule.SenderSelectorDefinition
import cc.bkhk.monoceros.api.schedule.SenderSelectorType
import cc.bkhk.monoceros.impl.config.ConfigFileHash
import cc.bkhk.monoceros.impl.config.ConfigService
import cc.bkhk.monoceros.impl.config.ConfigServiceCallback
import cc.bkhk.monoceros.impl.registry.ConcurrentRegistry
import cc.bkhk.monoceros.impl.util.DiagnosticLogger
import cc.bkhk.monoceros.impl.util.TimeUtil
import taboolib.common.platform.function.submit
import taboolib.library.configuration.ConfigurationSection
import taboolib.module.configuration.Configuration
import java.util.concurrent.ConcurrentHashMap

/**
 * 调度服务默认实现
 *
 * 基于 ConfigService 扫描 schedule/ 目录，从 YAML 解析 ScheduleDefinition。
 * 管理定义注册表和活跃句柄。
 */
class DefaultScheduleService(
    private val handlerRegistry: Registry<ScheduleHandler>,
) : ConfigService("schedule"), ScheduleService {

    private companion object {
        const val MODULE = "ScheduleService"
    }

    private val definitionRegistry = ConcurrentRegistry<ScheduleDefinition>()
    private val activeHandles = ConcurrentHashMap<String, MutableMap<String, ScheduleHandle>>()
    private val executor = ScheduleExecutor(handlerRegistry)

    /** 文件 ID -> 该文件产出的调度定义 ID 集合（用于增量卸载） */
    private val fileToDefinitionIds = ConcurrentHashMap<String, MutableSet<String>>()

    /** 句柄自然结束时的回收回调 */
    private val onHandleComplete: (String, String) -> Unit = { definitionId, runtimeId ->
        activeHandles[definitionId]?.remove(runtimeId)
        if (activeHandles[definitionId]?.isEmpty() == true) {
            activeHandles.remove(definitionId)
        }
        DiagnosticLogger.info(MODULE, "调度句柄自然结束并回收: $definitionId [$runtimeId]")
    }

    private val runnerFactory = ScheduleRunnerFactory(executor, onHandleComplete)

    override fun register(definition: ScheduleDefinition): ScheduleDefinition {
        definitionRegistry.register(definition.id, definition)
        return definition
    }

    override fun unregister(id: String): ScheduleDefinition? {
        // 停止所有活跃实例
        stop(id)
        return definitionRegistry.unregister(id)
    }

    override fun start(id: String, variables: Map<String, Any?>): ScheduleHandle {
        val definition = definitionRegistry.get(id)
            ?: error("调度定义不存在: $id")

        // 非 prototype 模式下，同一定义只允许一个活跃实例
        if (!definition.prototype) {
            val existing = activeHandles[id]
            if (existing != null && existing.isNotEmpty()) {
                error("调度 $id 已有活跃实例，非 prototype 模式不允许多实例")
            }
        }

        val handle = runnerFactory.start(definition, variables)
        activeHandles.computeIfAbsent(id) { ConcurrentHashMap() }[handle.runtimeId] = handle
        DiagnosticLogger.info(MODULE, "启动调度: $id [${handle.runtimeId}]")
        return handle
    }

    override fun pause(id: String, runtimeId: String): Int {
        val definition = definitionRegistry.get(id)
        val handles = collectHandles(id, runtimeId)
        var count = 0
        for (handle in handles) {
            handle.pause()
            count++
            // 执行 on-pause 生命周期脚本
            if (definition?.onPauseScript != null && handle is DefaultScheduleHandle) {
                executor.executeLifecycleScript(definition.onPauseScript, handle)
            }
        }
        return count
    }

    override fun resume(id: String, runtimeId: String): Int {
        val definition = definitionRegistry.get(id)
        val handles = collectHandles(id, runtimeId)
        var count = 0
        for (handle in handles) {
            handle.resume()
            count++
            // 执行 on-resume 生命周期脚本
            if (definition?.onResumeScript != null && handle is DefaultScheduleHandle) {
                executor.executeLifecycleScript(definition.onResumeScript, handle)
            }
        }
        return count
    }

    override fun stop(id: String, runtimeId: String): Int {
        val count = operateHandles(id, runtimeId) { it.stop() }
        // 清理已终止的句柄
        activeHandles[id]?.entries?.removeIf {
            it.value.state == ScheduleState.TERMINATED
        }
        if (activeHandles[id]?.isEmpty() == true) {
            activeHandles.remove(id)
        }
        return count
    }

    override fun getHandles(id: String): Collection<ScheduleHandle> {
        return activeHandles[id]?.values?.toList() ?: emptyList()
    }

    /** 收集匹配的句柄列表（不执行操作） */
    private fun collectHandles(id: String, runtimeId: String): List<ScheduleHandle> {
        val handles = activeHandles[id] ?: return emptyList()
        return if (runtimeId == "*") {
            handles.values.toList()
        } else {
            listOfNotNull(handles[runtimeId])
        }
    }

    /** 对匹配的句柄执行操作 */
    private fun operateHandles(id: String, runtimeId: String, action: (ScheduleHandle) -> Unit): Int {
        val handles = activeHandles[id] ?: return 0
        var count = 0
        if (runtimeId == "*") {
            handles.values.forEach { action(it); count++ }
        } else {
            handles[runtimeId]?.let { action(it); count++ }
        }
        return count
    }

    /** 停止所有活跃任务 */
    fun stopAll() {
        activeHandles.keys.toList().forEach { stop(it) }
    }

    /**
     * 全量重载
     */
    fun reloadAll(): Int {
        // 停止所有活跃任务
        stopAll()
        definitionRegistry.clear()
        fileToDefinitionIds.clear()

        // 清空哈希快照，确保全量扫描时所有文件都触发 onCreated
        clearHashes()

        var loaded = 0
        scan(object : ConfigServiceCallback {
            override fun onCreated(fileId: String, hash: ConfigFileHash) {
                loaded += loadFile(fileId)
            }
            override fun onModified(fileId: String, hash: ConfigFileHash) {
                loaded += loadFile(fileId)
            }
            override fun onDeleted(fileId: String) {}
        })

        DiagnosticLogger.summary(MODULE, loaded)

        // 自动启动标记为 autoStart 的任务
        var autoStarted = 0
        definitionRegistry.values().filter { it.autoStart }.forEach { def ->
            try {
                start(def.id)
                autoStarted++
            } catch (e: Exception) {
                DiagnosticLogger.warn(MODULE, "自动启动失败: ${def.id}", e)
            }
        }
        if (autoStarted > 0) {
            DiagnosticLogger.info(MODULE, "自动启动调度: $autoStarted 个")
        }

        return loaded
    }

    /** 从文件加载调度定义 */
    private fun loadFile(fileId: String): Int {
        val dir = directory()
        val file = dir.walkTopDown().find { f ->
            f.isFile && !f.name.startsWith("#") && f.extension in setOf("yml", "yaml") &&
                f.relativeTo(dir).path.replace('\\', '/').substringBeforeLast('.').replace('/', '.') == fileId
        } ?: return 0

        val loadedIds = mutableSetOf<String>()
        var count = 0
        try {
            val config = Configuration.loadFromFile(file)

            // 单定义文件
            if (config.contains("type")) {
                val definition = parseDefinition(config, fileId)
                if (definition != null) {
                    register(definition)
                    loadedIds.add(definition.id)
                    count++
                }
            } else {
                // 多定义文件
                for (key in config.getKeys(false)) {
                    val section = config.getConfigurationSection(key) ?: continue
                    if (!section.contains("type")) continue
                    val id = section.getString("id") ?: "$fileId.$key"
                    val definition = parseDefinition(section, id)
                    if (definition != null) {
                        register(definition)
                        loadedIds.add(definition.id)
                        count++
                    }
                }
            }
        } catch (e: Exception) {
            DiagnosticLogger.warn(MODULE, "调度文件解析失败: ${file.path}", e)
        }

        // 记录文件到定义 ID 的映射
        if (loadedIds.isNotEmpty()) {
            fileToDefinitionIds[fileId] = loadedIds
        }
        return count
    }

    /** 按文件 ID 注销该文件关联的所有调度定义 */
    private fun unregisterByFileId(fileId: String) {
        val ids = fileToDefinitionIds.remove(fileId) ?: return
        ids.forEach { unregister(it) }
    }

    /** 从 YAML 配置节解析 ScheduleDefinition */
    private fun parseDefinition(section: ConfigurationSection, defaultId: String): ScheduleDefinition? {
        val id = section.getString("id") ?: defaultId
        val type = try {
            ScheduleType.valueOf(section.getString("type")?.uppercase() ?: "PERIODIC")
        } catch (_: Exception) {
            ScheduleType.PERIODIC
        }
        val delay = TimeUtil.parseTicksOrDefault(section.getString("delay") ?: "0")
        val period = TimeUtil.parseTicksOrDefault(section.getString("period") ?: "-1", -1L)
        val cron = section.getString("cron")
        val async = section.getBoolean("async", false)
        val autoStart = section.getBoolean("auto-start", false)
        val prototype = section.getBoolean("prototype", false)
        val maxRuns = section.getInt("max-runs", -1)
        val maxDurationMs = section.getLong("max-duration-ms", -1)

        val senderSelectors = parseSelectors(section)
        val route = parseRoute(section) ?: return null

        val onStart = section.getString("on-start")
        val onStop = section.getString("on-stop")
        val onPause = section.getString("on-pause")
        val onResume = section.getString("on-resume")

        val variables = mutableMapOf<String, Any?>()
        section.getConfigurationSection("variables")?.let { vars ->
            for (key in vars.getKeys(false)) {
                variables[key] = vars.get(key)
            }
        }

        return ScheduleDefinition(
            id = id,
            type = type,
            delayTicks = delay,
            periodTicks = period,
            cron = cron,
            async = async,
            autoStart = autoStart,
            prototype = prototype,
            maxRuns = maxRuns,
            maxDurationMs = maxDurationMs,
            senderSelectors = senderSelectors,
            route = route,
            onStartScript = onStart,
            onStopScript = onStop,
            onPauseScript = onPause,
            onResumeScript = onResume,
            variables = variables,
        )
    }

    /** 解析发送者选择器 */
    private fun parseSelectors(section: ConfigurationSection): List<SenderSelectorDefinition> {
        val selectors = mutableListOf<SenderSelectorDefinition>()
        section.getConfigurationSection("sender")?.let { sender ->
            parseSelectorSection(sender)?.let { selectors += it }
        }
        section.getMapList("senders").forEach { map ->
            parseSelectorMap(map)?.let { selectors += it }
        }
        return selectors
    }

    private fun parseSelectorSection(section: ConfigurationSection): SenderSelectorDefinition? {
        val type = try {
            SenderSelectorType.valueOf(section.getString("type")?.uppercase()?.replace('-', '_') ?: return null)
        } catch (_: Exception) {
            return null
        }
        return SenderSelectorDefinition(
            type = type,
            value = section.getString("value") ?: section.getString("player") ?: section.getString("name"),
            world = section.getString("world"),
            origin = section.getString("origin"),
            radius = section.getDouble("radius").takeIf { section.contains("radius") },
            minX = section.getDouble("min-x").takeIf { section.contains("min-x") },
            minY = section.getDouble("min-y").takeIf { section.contains("min-y") },
            minZ = section.getDouble("min-z").takeIf { section.contains("min-z") },
            maxX = section.getDouble("max-x").takeIf { section.contains("max-x") },
            maxY = section.getDouble("max-y").takeIf { section.contains("max-y") },
            maxZ = section.getDouble("max-z").takeIf { section.contains("max-z") },
        )
    }

    private fun parseSelectorMap(map: Map<*, *>): SenderSelectorDefinition? {
        val type = try {
            SenderSelectorType.valueOf(map["type"]?.toString()?.uppercase()?.replace('-', '_') ?: return null)
        } catch (_: Exception) {
            return null
        }
        return SenderSelectorDefinition(
            type = type,
            value = map["value"]?.toString() ?: map["player"]?.toString() ?: map["name"]?.toString(),
            world = map["world"]?.toString(),
            origin = map["origin"]?.toString(),
            radius = (map["radius"] as? Number)?.toDouble(),
            minX = (map["min-x"] as? Number)?.toDouble(),
            minY = (map["min-y"] as? Number)?.toDouble(),
            minZ = (map["min-z"] as? Number)?.toDouble(),
            maxX = (map["max-x"] as? Number)?.toDouble(),
            maxY = (map["max-y"] as? Number)?.toDouble(),
            maxZ = (map["max-z"] as? Number)?.toDouble(),
        )
    }

    /** 解析路由配置 */
    private fun parseRoute(section: ConfigurationSection): ScheduleRoute? {
        val executeSection = section.getConfigurationSection("execute")
        if (executeSection != null) {
            val routeType = executeSection.getString("route") ?: "script"
            val value = executeSection.getString("value") ?: return null
            return when (routeType.lowercase()) {
                "script" -> ScheduleRoute.Script(value)
                "action", "workflow" -> ScheduleRoute.ActionWorkflow(value)
                "handler" -> ScheduleRoute.Handler(value)
                else -> null
            }
        }
        val executeValue = section.getString("execute")
        if (executeValue != null) {
            return ScheduleRoute.Script(executeValue)
        }
        return null
    }

    /** 创建文件监听回调 */
    fun createWatcherCallback(): ConfigServiceCallback = object : ConfigServiceCallback {
        override fun onCreated(fileId: String, hash: ConfigFileHash) {
            DiagnosticLogger.info(MODULE, "检测到新调度文件: $fileId")
            // 调度操作涉及 TabooLib submit，在主线程执行
            submit(async = false) { loadFile(fileId) }
        }
        override fun onModified(fileId: String, hash: ConfigFileHash) {
            DiagnosticLogger.info(MODULE, "检测到调度文件变更: $fileId")
            // 基于真实映射卸载，再重新加载，在主线程执行
            submit(async = false) {
                unregisterByFileId(fileId)
                loadFile(fileId)
            }
        }
        override fun onDeleted(fileId: String) {
            DiagnosticLogger.info(MODULE, "检测到调度文件删除: $fileId")
            // 基于真实映射卸载，在主线程执行
            submit(async = false) { unregisterByFileId(fileId) }
        }
    }
}
