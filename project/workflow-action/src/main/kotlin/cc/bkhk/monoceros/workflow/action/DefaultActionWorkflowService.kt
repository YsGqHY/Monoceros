package cc.bkhk.monoceros.workflow.action

import cc.bkhk.monoceros.api.workflow.ActionContext
import cc.bkhk.monoceros.api.workflow.ActionFailurePolicy
import cc.bkhk.monoceros.api.workflow.ActionNode
import cc.bkhk.monoceros.api.workflow.ActionNodeDefinition
import cc.bkhk.monoceros.api.workflow.ActionResult
import cc.bkhk.monoceros.api.workflow.ActionWorkflowDefinition
import cc.bkhk.monoceros.api.workflow.ActionWorkflowService
import cc.bkhk.monoceros.impl.config.ConfigFileHash
import cc.bkhk.monoceros.impl.config.ConfigService
import cc.bkhk.monoceros.impl.config.ConfigServiceCallback
import cc.bkhk.monoceros.impl.registry.ConcurrentRegistry
import cc.bkhk.monoceros.impl.util.DiagnosticLogger
import org.bukkit.command.CommandSender
import taboolib.common.platform.function.submit
import taboolib.library.configuration.ConfigurationSection
import taboolib.module.configuration.Configuration
import java.util.concurrent.ConcurrentHashMap

/**
 * 动作工作流服务默认实现
 *
 * 管理节点注册表和工作流定义，支持 YAML 驱动加载与热重载。
 */
class DefaultActionWorkflowService : ConfigService("workflow/action"), ActionWorkflowService {

    private companion object {
        const val MODULE = "ActionWorkflow"
    }

    /** 节点类型注册表 */
    private val nodeRegistry = ConcurrentRegistry<ActionNode>()

    /** 工作流定义注册表 */
    private val definitions = ConcurrentHashMap<String, ActionWorkflowDefinition>()

    /** 文件 ID -> 该文件产出的工作流定义 ID 集合（用于增量卸载） */
    private val fileToDefinitionIds = ConcurrentHashMap<String, MutableSet<String>>()

    override fun registerNode(node: ActionNode): ActionNode {
        nodeRegistry.register(node.type, node)
        DiagnosticLogger.debug(MODULE, "注册动作节点: ${node.type}")
        return node
    }

    override fun unregisterNode(type: String): ActionNode? {
        return nodeRegistry.unregister(type)
    }

    override fun execute(id: String, sender: CommandSender?, variables: Map<String, Any?>): Any? {
        val definition = definitions[id]
            ?: error("动作工作流定义不存在: $id")

        val context = ActionContext(
            workflowId = id,
            sender = sender,
        )
        context.variables.putAll(variables)
        context.variables["workflowId"] = id

        return executeNodes(definition, context, definition.nodes, 0)
    }

    /**
     * 执行节点链（从指定索引开始）
     *
     * 支持 [ActionResult.Delay] 异步延续和 [ActionResult.Branch] 条件跳转。
     */
    private fun executeNodes(
        definition: ActionWorkflowDefinition,
        context: ActionContext,
        nodes: List<ActionNodeDefinition>,
        startIndex: Int,
    ): Any? {
        var lastResult: Any? = null

        var i = startIndex
        while (i < nodes.size) {
            val nodeDef = nodes[i]
            val node = nodeRegistry.get(nodeDef.type)
            if (node == null) {
                DiagnosticLogger.warn(MODULE, "节点类型未注册: ${nodeDef.type} (workflow=${definition.id}, node=${nodeDef.id})")
                when (definition.failurePolicy) {
                    ActionFailurePolicy.STOP -> return lastResult
                    ActionFailurePolicy.SKIP_NODE, ActionFailurePolicy.CONTINUE -> { i++; continue }
                }
            }

            context.variables["nodeId"] = nodeDef.id
            context.trace.add(nodeDef.id)

            try {
                val result = node.execute(context, nodeDef)

                when (result) {
                    is ActionResult.Delay -> {
                        // 异步延续：延迟指定 tick 后执行剩余节点
                        val remainingNodes = nodes
                        val nextIndex = i + 1
                        val capturedLastResult = lastResult
                        submit(delay = result.ticks) {
                            context.variables["lastResult"] = capturedLastResult
                            executeNodes(definition, context, remainingNodes, nextIndex)
                        }
                        return lastResult
                    }
                    is ActionResult.Branch -> {
                        context.variables["lastResult"] = result.accepted
                        if (result.accepted) {
                            // 条件成立：执行 then-workflow（如果有），然后继续后续节点
                            result.thenWorkflow?.let { wfId ->
                                execute(wfId, context.sender, context.variables)
                            }
                        } else {
                            // 条件不成立：执行 else-workflow（如果有），然后中断
                            result.elseWorkflow?.let { wfId ->
                                execute(wfId, context.sender, context.variables)
                            }
                            return lastResult
                        }
                    }
                    is ActionResult.Break -> {
                        return lastResult
                    }
                    is ActionResult.Continue -> {
                        lastResult = result.value
                        context.variables["lastResult"] = lastResult
                    }
                    else -> {
                        // 普通返回值
                        lastResult = result
                        context.variables["lastResult"] = lastResult
                    }
                }
            } catch (e: Exception) {
                DiagnosticLogger.warn(MODULE, "节点执行失败: ${nodeDef.id} (workflow=${definition.id})", e)
                when (definition.failurePolicy) {
                    ActionFailurePolicy.STOP -> return lastResult
                    ActionFailurePolicy.SKIP_NODE, ActionFailurePolicy.CONTINUE -> {}
                }
            }

            i++
        }

        return lastResult
    }

    override fun reloadAll(): Int {
        definitions.clear()
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
        return loaded
    }

    /** 从文件加载工作流定义 */
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
            if (config.contains("nodes")) {
                val definition = parseDefinition(config, fileId)
                if (definition != null) {
                    definitions[definition.id] = definition
                    loadedIds.add(definition.id)
                    count++
                }
            } else {
                // 多定义文件
                for (key in config.getKeys(false)) {
                    val section = config.getConfigurationSection(key) ?: continue
                    if (!section.contains("nodes")) continue
                    val id = section.getString("id") ?: "$fileId.$key"
                    val definition = parseDefinition(section, id)
                    if (definition != null) {
                        definitions[definition.id] = definition
                        loadedIds.add(definition.id)
                        count++
                    }
                }
            }
        } catch (e: Exception) {
            DiagnosticLogger.warn(MODULE, "工作流文件解析失败: ${file.path}", e)
        }

        // 记录文件到定义 ID 的映射
        if (loadedIds.isNotEmpty()) {
            fileToDefinitionIds[fileId] = loadedIds
        }
        return count
    }

    /** 按文件 ID 注销该文件关联的所有工作流定义 */
    private fun unregisterByFileId(fileId: String) {
        val ids = fileToDefinitionIds.remove(fileId) ?: return
        ids.forEach { definitions.remove(it) }
    }

    /** 解析工作流定义 */
    private fun parseDefinition(section: ConfigurationSection, defaultId: String): ActionWorkflowDefinition? {
        val id = section.getString("id") ?: defaultId
        val failurePolicy = try {
            ActionFailurePolicy.valueOf(section.getString("failure-policy")?.uppercase()?.replace("-", "_") ?: "STOP")
        } catch (_: Exception) {
            ActionFailurePolicy.STOP
        }

        val nodesList = section.getMapList("nodes")
        val nodes = nodesList.mapNotNull { map ->
            val nodeId = map["id"]?.toString() ?: return@mapNotNull null
            val type = map["type"]?.toString() ?: return@mapNotNull null
            val config = map.filterKeys { it != "id" && it != "type" }.mapKeys { it.key.toString() }
            ActionNodeDefinition(id = nodeId, type = type, config = config)
        }

        if (nodes.isEmpty()) return null

        return ActionWorkflowDefinition(id = id, nodes = nodes, failurePolicy = failurePolicy)
    }

    /** 创建文件监听回调（增量式） */
    fun createWatcherCallback(): ConfigServiceCallback = object : ConfigServiceCallback {
        override fun onCreated(fileId: String, hash: ConfigFileHash) {
            DiagnosticLogger.info(MODULE, "检测到新工作流文件: $fileId")
            loadFile(fileId)
        }
        override fun onModified(fileId: String, hash: ConfigFileHash) {
            DiagnosticLogger.info(MODULE, "检测到工作流文件变更: $fileId")
            // 基于真实映射卸载，再重新加载
            unregisterByFileId(fileId)
            loadFile(fileId)
        }
        override fun onDeleted(fileId: String) {
            DiagnosticLogger.info(MODULE, "检测到工作流文件删除: $fileId")
            unregisterByFileId(fileId)
        }
    }
}
