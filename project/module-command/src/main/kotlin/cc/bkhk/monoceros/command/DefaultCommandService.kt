package cc.bkhk.monoceros.command

import cc.bkhk.monoceros.api.command.ArgumentNode
import cc.bkhk.monoceros.api.command.ArgumentSpec
import cc.bkhk.monoceros.api.command.ArgumentType
import cc.bkhk.monoceros.api.command.CommandDefinition
import cc.bkhk.monoceros.api.command.CommandHandler
import cc.bkhk.monoceros.api.command.CommandNode
import cc.bkhk.monoceros.api.command.CommandRoute
import cc.bkhk.monoceros.api.command.CommandService
import cc.bkhk.monoceros.api.command.LiteralNode
import cc.bkhk.monoceros.api.command.RestrictionSpec
import cc.bkhk.monoceros.api.command.SuggestionProvider
import cc.bkhk.monoceros.api.registry.Registry
import cc.bkhk.monoceros.impl.config.ConfigFileHash
import cc.bkhk.monoceros.impl.config.ConfigService
import cc.bkhk.monoceros.impl.config.ConfigServiceCallback
import cc.bkhk.monoceros.impl.util.DiagnosticLogger
import org.bukkit.Bukkit
import taboolib.common.platform.function.submit
import taboolib.library.configuration.ConfigurationSection
import taboolib.module.configuration.Configuration
import java.util.concurrent.ConcurrentHashMap

/**
 * 命令服务默认实现
 *
 * 基于 ConfigService 扫描 command/ 目录，从 YAML 解析 CommandDefinition。
 * 编译失败时保留旧命令可用。
 */
class DefaultCommandService(
    private val handlerRegistry: Registry<CommandHandler>,
    private val suggestionRegistry: Registry<SuggestionProvider>,
) : ConfigService("command"), CommandService {

    private companion object {
        const val MODULE = "CommandService"
    }

    private val definitions = ConcurrentHashMap<String, CommandDefinition>()
    private val compiler = CommandCompiler(handlerRegistry, suggestionRegistry)

    /** 文件 ID -> 该文件产出的命令定义 ID 集合（用于增量卸载） */
    private val fileToDefinitionIds = ConcurrentHashMap<String, MutableSet<String>>()

    override fun register(definition: CommandDefinition) {
        // 先注销旧命令
        unregister(definition.id)
        definitions[definition.id] = definition
        compiler.compile(definition)
    }

    override fun unregister(id: String) {
        definitions.remove(id)
        compiler.unregister(id)
    }

    override fun reloadAll(): Int {
        // 先收集所有待注册的命令定义
        val pendingDefinitions = mutableListOf<CommandDefinition>()
        fileToDefinitionIds.clear()

        // 清空哈希快照，确保全量扫描时所有文件都触发 onCreated
        clearHashes()

        scan(object : ConfigServiceCallback {
            override fun onCreated(fileId: String, hash: ConfigFileHash) {
                pendingDefinitions += collectDefinitions(fileId)
            }
            override fun onModified(fileId: String, hash: ConfigFileHash) {
                pendingDefinitions += collectDefinitions(fileId)
            }
            override fun onDeleted(fileId: String) {}
        })

        // 批量注销所有旧命令
        compiler.unregisterAll()
        definitions.clear()

        // 延迟一个 tick 注册新命令，避免与 Paper 异步命令树构建线程产生 ConcurrentModificationException
        if (pendingDefinitions.isNotEmpty()) {
            submit(async = false, delay = 1) {
                for (definition in pendingDefinitions) {
                    definitions[definition.id] = definition
                    compiler.compile(definition)
                }
                // 注册完成后统一刷新一次命令树
                syncCommandsToOnlinePlayers()
            }
        }

        DiagnosticLogger.summary(MODULE, pendingDefinitions.size)
        return pendingDefinitions.size
    }

    /** 收集文件中的命令定义（不注册） */
    private fun collectDefinitions(fileId: String): List<CommandDefinition> {
        val dir = directory()
        val file = dir.walkTopDown().find { f ->
            f.isFile && !f.name.startsWith("#") && f.extension in setOf("yml", "yaml") &&
                f.relativeTo(dir).path.replace('\\', '/').substringBeforeLast('.').replace('/', '.') == fileId
        } ?: return emptyList()

        val result = mutableListOf<CommandDefinition>()
        val loadedIds = mutableSetOf<String>()
        try {
            val config = Configuration.loadFromFile(file)

            if (config.contains("root")) {
                val definition = parseDefinition(config, fileId)
                if (definition != null) {
                    result.add(definition)
                    loadedIds.add(definition.id)
                }
            } else {
                for (key in config.getKeys(false)) {
                    val section = config.getConfigurationSection(key) ?: continue
                    if (!section.contains("root")) continue
                    val id = section.getString("id") ?: "$fileId.$key"
                    val definition = parseDefinition(section, id)
                    if (definition != null) {
                        result.add(definition)
                        loadedIds.add(definition.id)
                    }
                }
            }
        } catch (e: Exception) {
            DiagnosticLogger.warn(MODULE, "命令文件解析失败: ${file.path}", e)
        }

        if (loadedIds.isNotEmpty()) {
            fileToDefinitionIds[fileId] = loadedIds
        }
        return result
    }

    /** 向所有在线玩家刷新命令树 */
    private fun syncCommandsToOnlinePlayers() {
        try {
            Bukkit.getOnlinePlayers().forEach { it.updateCommands() }
        } catch (_: Throwable) {
            // 静默忽略，部分版本可能不支持
        }
    }

    /** 从文件加载命令定义 */
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

            // 单定义文件（顶层包含 root）
            if (config.contains("root")) {
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
                    if (!section.contains("root")) continue
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
            DiagnosticLogger.warn(MODULE, "命令文件解析失败: ${file.path}", e)
        }

        // 记录文件到定义 ID 的映射
        if (loadedIds.isNotEmpty()) {
            fileToDefinitionIds[fileId] = loadedIds
        }
        return count
    }

    /** 按文件 ID 注销该文件关联的所有命令 */
    private fun unregisterByFileId(fileId: String) {
        val ids = fileToDefinitionIds.remove(fileId) ?: return
        ids.forEach { unregister(it) }
    }

    /** 从 YAML 配置节解析 CommandDefinition */
    private fun parseDefinition(section: ConfigurationSection, defaultId: String): CommandDefinition? {
        val id = section.getString("id") ?: defaultId
        val aliases = section.getStringList("aliases")
        val permission = section.getString("permission")
        val permissionMessage = section.getString("permission-message")

        val rootSection = section.getConfigurationSection("root") ?: return null
        val root = parseNode(rootSection) ?: return null

        return CommandDefinition(
            id = id,
            aliases = aliases,
            permission = permission,
            permissionMessage = permissionMessage,
            root = root,
        )
    }

    /** 递归解析命令节点 */
    private fun parseNode(section: ConfigurationSection): CommandNode? {
        val type = section.getString("type") ?: "literal"
        val name = section.getString("name") ?: return null

        val route = parseRoute(section)
        val children = parseChildren(section)

        return when (type.lowercase()) {
            "literal" -> LiteralNode(name = name, route = route, children = children)
            "argument" -> {
                val argSection = section.getConfigurationSection("argument")
                val argument = if (argSection != null) {
                    parseArgumentSpec(argSection)
                } else {
                    ArgumentSpec(type = ArgumentType.STRING)
                }
                ArgumentNode(name = name, argument = argument, route = route, children = children)
            }
            else -> null
        }
    }

    /** 解析子节点列表 */
    private fun parseChildren(section: ConfigurationSection): List<CommandNode> {
        val childrenList = section.getMapList("children")
        if (childrenList.isEmpty()) return emptyList()

        val result = mutableListOf<CommandNode>()
        for (childMap in childrenList) {
            // 将 Map 转为临时 ConfigurationSection
            val tempConfig = Configuration.empty(taboolib.module.configuration.Type.YAML)
            childMap.forEach { (key, value) -> tempConfig.set(key.toString(), value) }
            val node = parseNode(tempConfig)
            if (node != null) {
                result.add(node)
            }
        }
        return result
    }

    /** 解析参数规格 */
    private fun parseArgumentSpec(section: ConfigurationSection): ArgumentSpec {
        val type = try {
            ArgumentType.valueOf(section.getString("type")?.uppercase() ?: "STRING")
        } catch (_: Exception) {
            ArgumentType.STRING
        }
        val required = section.getBoolean("required", true)
        val suggest = section.getString("suggest")
        val restrictSection = section.getConfigurationSection("restrict")
        val restrict = if (restrictSection != null) {
            RestrictionSpec(
                mode = restrictSection.getString("mode") ?: "",
                value = restrictSection.getString("value") ?: "",
            )
        } else null

        return ArgumentSpec(type = type, required = required, suggest = suggest, restrict = restrict)
    }

    /** 解析路由配置 */
    private fun parseRoute(section: ConfigurationSection): CommandRoute? {
        val routeSection = section.getConfigurationSection("route")
        if (routeSection != null) {
            val routeType = routeSection.getString("type") ?: "script"
            val value = routeSection.getString("value") ?: return null
            return when (routeType.lowercase()) {
                "script" -> CommandRoute.Script(value)
                "action", "workflow" -> CommandRoute.ActionWorkflow(value)
                "handler" -> CommandRoute.Handler(value)
                else -> null
            }
        }
        return null
    }

    /** 创建文件监听回调 */
    fun createWatcherCallback(): ConfigServiceCallback = object : ConfigServiceCallback {
        override fun onCreated(fileId: String, hash: ConfigFileHash) {
            DiagnosticLogger.info(MODULE, "检测到新命令文件: $fileId")
            // 命令注册涉及 Bukkit CommandMap，必须在主线程执行
            submit(async = false) { loadFile(fileId) }
        }
        override fun onModified(fileId: String, hash: ConfigFileHash) {
            DiagnosticLogger.info(MODULE, "检测到命令文件变更: $fileId")
            // 基于真实映射卸载，再重新加载，在主线程执行
            submit(async = false) {
                unregisterByFileId(fileId)
                loadFile(fileId)
            }
        }
        override fun onDeleted(fileId: String) {
            DiagnosticLogger.info(MODULE, "检测到命令文件删除: $fileId")
            // 基于真实映射卸载，在主线程执行
            submit(async = false) { unregisterByFileId(fileId) }
        }
    }
}
