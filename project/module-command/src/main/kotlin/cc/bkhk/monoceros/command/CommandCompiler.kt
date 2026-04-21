package cc.bkhk.monoceros.command

import cc.bkhk.monoceros.Monoceros
import cc.bkhk.monoceros.api.command.ArgumentNode
import cc.bkhk.monoceros.api.command.CommandContext
import cc.bkhk.monoceros.api.command.CommandDefinition
import cc.bkhk.monoceros.api.command.CommandHandler
import cc.bkhk.monoceros.api.command.CommandNode
import cc.bkhk.monoceros.api.command.CommandRoute
import cc.bkhk.monoceros.api.command.LiteralNode
import cc.bkhk.monoceros.api.command.SuggestionProvider
import cc.bkhk.monoceros.api.registry.Registry
import cc.bkhk.monoceros.impl.util.DiagnosticLogger
import org.bukkit.command.CommandSender
import taboolib.common.platform.command.command
import taboolib.common.platform.command.component.CommandComponent
import taboolib.common.platform.function.adaptCommandSender
import taboolib.common.platform.function.unregisterCommand
import java.util.concurrent.ConcurrentHashMap

/**
 * 命令编译器
 *
 * 将 CommandDefinition 编译为 TabooLib 命令并注册到服务器。
 */
class CommandCompiler(
    private val handlerRegistry: Registry<CommandHandler>,
    private val suggestionRegistry: Registry<SuggestionProvider>,
) {

    private companion object {
        const val MODULE = "CommandCompiler"
    }

    /** 已注册的命令名称，用于注销 */
    private val registeredCommands = ConcurrentHashMap<String, List<String>>()

    /**
     * 编译并注册命令
     */
    fun compile(definition: CommandDefinition) {
        val aliases = definition.aliases.toMutableList()
        val primaryName = if (aliases.isNotEmpty()) aliases.removeFirst() else definition.id

        try {
            command(
                name = primaryName,
                aliases = aliases,
                permission = definition.permission ?: "",
                permissionMessage = definition.permissionMessage ?: "",
            ) {
                buildNode(this, definition.root, definition.id, listOf(primaryName))
            }
            registeredCommands[definition.id] = listOf(primaryName) + aliases
            DiagnosticLogger.info(MODULE, "注册命令: $primaryName (${definition.id})")
        } catch (e: Exception) {
            DiagnosticLogger.warn(MODULE, "命令注册失败: ${definition.id}", e)
        }
    }

    /**
     * 注销命令
     */
    fun unregister(definitionId: String) {
        val names = registeredCommands.remove(definitionId) ?: return
        names.forEach { name ->
            try {
                unregisterCommand(name)
            } catch (_: Exception) {
            }
        }
        DiagnosticLogger.info(MODULE, "注销命令: $definitionId")
    }

    /** 注销所有命令 */
    fun unregisterAll() {
        registeredCommands.keys.toList().forEach { unregister(it) }
    }

    /**
     * 递归构建命令节点
     */
    private fun buildNode(
        component: CommandComponent,
        node: CommandNode,
        definitionId: String,
        path: List<String>,
    ) {
        when (node) {
            is LiteralNode -> buildLiteralNode(component, node, definitionId, path)
            is ArgumentNode -> buildArgumentNode(component, node, definitionId, path)
        }
    }

    /** 构建字面量节点 */
    private fun buildLiteralNode(
        component: CommandComponent,
        node: LiteralNode,
        definitionId: String,
        path: List<String>,
    ) {
        // 设置执行器
        node.route?.let { route ->
            component.execute<CommandSender> { sender, _, content ->
                val proxyCmd = adaptCommandSender(sender)
                val cmdContext = CommandContext(
                    definitionId = definitionId,
                    sender = proxyCmd,
                    rawArgs = content.split(" ").filter { it.isNotBlank() },
                    parsedArgs = emptyMap(),
                    path = path,
                )
                executeRoute(route, cmdContext)
            }
        }

        // 递归构建子节点
        buildChildren(component, node.children, definitionId, path)
    }

    /** 构建参数节点 */
    private fun buildArgumentNode(
        component: CommandComponent,
        node: ArgumentNode,
        definitionId: String,
        path: List<String>,
    ) {
        // 设置执行器
        node.route?.let { route ->
            component.execute<CommandSender> { sender, _, content ->
                val proxyCmd = adaptCommandSender(sender)
                val parsedArgs = mutableMapOf<String, Any?>()
                parsedArgs[node.name] = content
                val cmdContext = CommandContext(
                    definitionId = definitionId,
                    sender = proxyCmd,
                    rawArgs = content.split(" ").filter { it.isNotBlank() },
                    parsedArgs = parsedArgs,
                    path = path,
                )
                executeRoute(route, cmdContext)
            }
        }

        // 递归构建子节点
        buildChildren(component, node.children, definitionId, path)
    }

    /** 构建子节点列表 */
    private fun buildChildren(
        component: CommandComponent,
        children: List<CommandNode>,
        definitionId: String,
        path: List<String>,
    ) {
        for (child in children) {
            when (child) {
                is LiteralNode -> {
                    component.literal(child.name) {
                        val childPath = path + child.name
                        buildNode(this, child, definitionId, childPath)
                    }
                }
                is ArgumentNode -> {
                    component.dynamic(child.name) {
                        // 补全
                        child.argument.suggest?.let { suggestId ->
                            suggestion<CommandSender> { sender, _ ->
                                getSuggestions(suggestId, definitionId, sender)
                            }
                        }
                        val childPath = path + "<${child.name}>"
                        buildNode(this, child, definitionId, childPath)
                    }
                }
            }
        }
    }

    /** 执行路由 */
    private fun executeRoute(route: CommandRoute, context: CommandContext) {
        val variables = mapOf(
            "rawArgs" to context.rawArgs,
            "args" to context.parsedArgs,
            "commandId" to context.definitionId,
            "path" to context.path,
        )
        when (route) {
            is CommandRoute.Script -> {
                try {
                    Monoceros.api().scripts().invoke(route.scriptId, context.sender, variables)
                } catch (e: Exception) {
                    DiagnosticLogger.warn(MODULE, "命令脚本执行失败: ${route.scriptId}", e)
                }
            }
            is CommandRoute.ActionWorkflow -> {
                try {
                    Monoceros.api().actionWorkflow().execute(
                        route.workflowId,
                        context.sender,
                        variables,
                    )
                } catch (e: Exception) {
                    DiagnosticLogger.warn(MODULE, "命令工作流执行失败: ${route.workflowId}", e)
                }
            }
            is CommandRoute.Handler -> {
                val handler = handlerRegistry.get(route.handlerId)
                if (handler == null) {
                    DiagnosticLogger.warn(MODULE, "CommandHandler 未注册: ${route.handlerId}")
                } else {
                    try {
                        handler.execute(context)
                    } catch (e: Exception) {
                        DiagnosticLogger.warn(MODULE, "CommandHandler 执行失败: ${route.handlerId}", e)
                    }
                }
            }
        }
    }

    /** 获取补全建议 */
    private fun getSuggestions(
        suggestId: String,
        definitionId: String,
        sender: CommandSender,
    ): List<String> {
        val provider = suggestionRegistry.get(suggestId)
        if (provider != null) {
            val cmdContext = CommandContext(
                definitionId = definitionId,
                sender = adaptCommandSender(sender),
                rawArgs = emptyList(),
                parsedArgs = emptyMap(),
                path = emptyList(),
            )
            return provider.suggest(cmdContext)
        }
        // 内建补全
        return when (suggestId) {
            "online-player" -> org.bukkit.Bukkit.getOnlinePlayers().map { it.name }
            "world" -> org.bukkit.Bukkit.getWorlds().map { it.name }
            else -> emptyList()
        }
    }
}
