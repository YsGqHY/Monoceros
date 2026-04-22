package cc.bkhk.monoceros.dispatcher

import cc.bkhk.monoceros.api.dispatcher.DispatcherDefinition
import cc.bkhk.monoceros.api.dispatcher.DispatcherHandler
import cc.bkhk.monoceros.api.dispatcher.DispatcherRoute
import cc.bkhk.monoceros.api.dispatcher.DispatcherService
import cc.bkhk.monoceros.api.dispatcher.EventDispatcher
import cc.bkhk.monoceros.api.registry.Registry
import cc.bkhk.monoceros.impl.config.ConfigFileHash
import cc.bkhk.monoceros.impl.config.ConfigService
import cc.bkhk.monoceros.impl.config.ConfigServiceCallback
import cc.bkhk.monoceros.impl.util.DiagnosticLogger
import org.bukkit.event.Event
import org.bukkit.event.EventPriority
import taboolib.common.platform.function.submit
import taboolib.library.configuration.ConfigurationSection
import taboolib.module.configuration.Configuration
import java.util.concurrent.ConcurrentHashMap

/**
 * 事件分发服务默认实现
 *
 * 基于 ConfigService 扫描 dispatcher/ 目录，从 YAML 解析 DispatcherDefinition。
 * 支持动态注册、注销、配置重载。
 */
class DefaultDispatcherService(
    private val handlerRegistry: Registry<DispatcherHandler>,
) : ConfigService("dispatcher"), DispatcherService {

    private companion object {
        const val MODULE = "DispatcherService"
    }

    /** 已注册的 dispatcher 实例 */
    private val dispatchers = ConcurrentHashMap<String, DispatcherEntry>()

    /** 文件 ID -> 该文件产出的 dispatcher ID 集合（用于增量卸载） */
    private val fileToDispatcherIds = ConcurrentHashMap<String, MutableSet<String>>()

    /** dispatcher 条目，关联事件类用于注销 */
    private data class DispatcherEntry(
        val dispatcher: EventDispatcher,
        val eventClass: Class<out Event>,
    )

    override fun register(definition: DispatcherDefinition): EventDispatcher {
        // 若已存在同 ID，先注销
        unregister(definition.id)

        val eventClass = EventClassMapping.resolve(definition.eventKey)
        if (eventClass == null) {
            DiagnosticLogger.warn(MODULE, "无法解析事件类: ${definition.eventKey}，跳过 dispatcher: ${definition.id}")
            return DefaultEventDispatcher(definition, handlerRegistry)
        }

        val dispatcher = DefaultEventDispatcher(definition, handlerRegistry)
        DispatcherListener.register(eventClass, dispatcher)
        dispatchers[definition.id] = DispatcherEntry(dispatcher, eventClass)
        return dispatcher
    }

    override fun unregister(id: String): EventDispatcher? {
        val entry = dispatchers.remove(id) ?: return null
        DispatcherListener.unregister(entry.eventClass, id)
        return entry.dispatcher
    }

    override fun get(id: String): EventDispatcher? {
        return dispatchers[id]?.dispatcher
    }

    override fun reloadAll(): Int {
        // 注销所有现有 dispatcher
        val oldIds = dispatchers.keys.toList()
        oldIds.forEach { unregister(it) }
        fileToDispatcherIds.clear()

        // 清空哈希快照，确保全量扫描时所有文件都触发 onCreated
        clearHashes()

        // 重新扫描并加载
        var loaded = 0

        scan(object : ConfigServiceCallback {
            override fun onCreated(fileId: String, hash: ConfigFileHash) {
                loaded += loadFile(fileId)
            }

            override fun onModified(fileId: String, hash: ConfigFileHash) {
                loaded += loadFile(fileId)
            }

            override fun onDeleted(fileId: String) {
                // 全量重载模式下不需要处理删除
            }
        })

        DiagnosticLogger.summary(MODULE, loaded)
        return loaded
    }

    /** 从文件加载 dispatcher 定义 */
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

            // 单定义文件（顶层包含 listen-event）
            if (config.contains("listen-event")) {
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
                    if (!section.contains("listen-event")) continue
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
            DiagnosticLogger.warn(MODULE, "dispatcher 文件解析失败: ${file.path}", e)
        }

        // 记录文件到定义 ID 的映射
        if (loadedIds.isNotEmpty()) {
            fileToDispatcherIds[fileId] = loadedIds
        }
        return count
    }

    /** 按文件 ID 注销该文件关联的所有 dispatcher */
    private fun unregisterByFileId(fileId: String) {
        val ids = fileToDispatcherIds.remove(fileId) ?: return
        ids.forEach { unregister(it) }
    }

    /** 从 YAML 配置节解析 DispatcherDefinition */
    private fun parseDefinition(section: ConfigurationSection, defaultId: String): DispatcherDefinition? {
        val id = section.getString("id") ?: defaultId
        val eventKey = section.getString("listen-event") ?: return null
        val priority = try {
            EventPriority.valueOf(section.getString("listen-priority")?.uppercase() ?: "NORMAL")
        } catch (_: Exception) {
            EventPriority.NORMAL
        }
        val weight = section.getInt("weight", 0)
        val ignoreCancelled = section.getBoolean("ignore-cancelled", false)
        val beforeScript = section.getString("before-execute")
        val afterScript = section.getString("after-execute")

        val route = parseRoute(section) ?: return null

        val variables = mutableMapOf<String, Any?>()
        section.getConfigurationSection("variables")?.let { vars ->
            for (key in vars.getKeys(false)) {
                variables[key] = vars.get(key)
            }
        }

        return DispatcherDefinition(
            id = id,
            eventKey = eventKey,
            priority = priority,
            weight = weight,
            ignoreCancelled = ignoreCancelled,
            beforeScript = beforeScript,
            executeRoute = route,
            afterScript = afterScript,
            variables = variables,
        )
    }

    /** 解析路由配置 */
    private fun parseRoute(section: ConfigurationSection): DispatcherRoute? {
        val executeSection = section.getConfigurationSection("execute")
        if (executeSection != null) {
            val routeType = executeSection.getString("route") ?: "script"
            val value = executeSection.getString("value") ?: return null
            return when (routeType.lowercase()) {
                "script" -> DispatcherRoute.Script(value)
                "action", "workflow" -> DispatcherRoute.ActionWorkflow(value)
                "handler" -> DispatcherRoute.Handler(value)
                else -> {
                    DiagnosticLogger.warn(MODULE, "未知路由类型: $routeType")
                    null
                }
            }
        }

        // 简写格式：execute 直接是脚本 ID
        val executeValue = section.getString("execute")
        if (executeValue != null) {
            return DispatcherRoute.Script(executeValue)
        }

        return null
    }

    /** 创建文件监听回调 */
    fun createWatcherCallback(): ConfigServiceCallback = object : ConfigServiceCallback {
        override fun onCreated(fileId: String, hash: ConfigFileHash) {
            DiagnosticLogger.info(MODULE, "检测到新 dispatcher 文件: $fileId")
            // Dispatcher 注册涉及 Bukkit 监听器，必须在主线程执行
            submit(async = false) { loadFile(fileId) }
        }

        override fun onModified(fileId: String, hash: ConfigFileHash) {
            DiagnosticLogger.info(MODULE, "检测到 dispatcher 文件变更: $fileId")
            // 基于真实映射卸载，再重新加载，在主线程执行
            submit(async = false) {
                unregisterByFileId(fileId)
                loadFile(fileId)
            }
        }

        override fun onDeleted(fileId: String) {
            DiagnosticLogger.info(MODULE, "检测到 dispatcher 文件删除: $fileId")
            // 基于真实映射卸载，在主线程执行
            submit(async = false) { unregisterByFileId(fileId) }
        }
    }
}
