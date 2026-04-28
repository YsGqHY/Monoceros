package cc.bkhk.monoceros.command

import cc.bkhk.monoceros.Monoceros
import cc.bkhk.monoceros.api.command.ArgumentNode
import cc.bkhk.monoceros.api.command.CommandContext
import cc.bkhk.monoceros.api.command.CommandDefinition
import cc.bkhk.monoceros.api.command.CommandHandler
import cc.bkhk.monoceros.api.command.CommandNode
import cc.bkhk.monoceros.api.command.CommandRoute
import cc.bkhk.monoceros.api.command.LiteralNode
import cc.bkhk.monoceros.api.command.RestrictionSpec
import cc.bkhk.monoceros.api.command.SuggestionProvider
import cc.bkhk.monoceros.api.registry.Registry
import cc.bkhk.monoceros.impl.util.DiagnosticLogger
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.command.CommandSender
import taboolib.common.platform.command.command
import taboolib.common.platform.command.component.CommandComponent
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
            DiagnosticLogger.debug(MODULE, "注册命令: $primaryName (${definition.id})")
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
        DiagnosticLogger.debug(MODULE, "注销命令: $definitionId")
    }

    /** 注销所有命令 */
    fun unregisterAll() {
        registeredCommands.keys.toList().forEach { unregister(it) }
    }

    /**
     * 递归构建命令节点
     *
     * @param accumulatedArgs 从父节点累积的已解析参数
     */
    private fun buildNode(
        component: CommandComponent,
        node: CommandNode,
        definitionId: String,
        path: List<String>,
        accumulatedArgs: Map<String, Any?> = emptyMap(),
    ) {
        when (node) {
            is LiteralNode -> buildLiteralNode(component, node, definitionId, path, accumulatedArgs)
            is ArgumentNode -> buildArgumentNode(component, node, definitionId, path, accumulatedArgs)
        }
    }

    /** 构建字面量节点 */
    private fun buildLiteralNode(
        component: CommandComponent,
        node: LiteralNode,
        definitionId: String,
        path: List<String>,
        accumulatedArgs: Map<String, Any?>,
    ) {
        // 设置执行器
        node.route?.let { route ->
            val capturedArgs = HashMap(accumulatedArgs)
            component.execute<CommandSender> { sender, _, content ->
                val cmdContext = CommandContext(
                    definitionId = definitionId,
                    sender = sender,
                    rawArgs = content.split(" ").filter { it.isNotBlank() },
                    parsedArgs = capturedArgs,
                    path = path,
                )
                executeRoute(route, cmdContext)
            }
        }

        // 递归构建子节点
        buildChildren(component, node.children, definitionId, path, accumulatedArgs)
    }

    /** 构建参数节点 */
    private fun buildArgumentNode(
        component: CommandComponent,
        node: ArgumentNode,
        definitionId: String,
        path: List<String>,
        accumulatedArgs: Map<String, Any?>,
    ) {
        // 设置执行器
        node.route?.let { route ->
            val capturedArgs = HashMap(accumulatedArgs)
            component.execute<CommandSender> { sender, _, content ->
                val rawValue = content.trim()
                val parsedValue = try {
                    parseArgumentValue(node.argument, rawValue)
                } catch (ex: IllegalArgumentException) {
                    sender.sendMessage("\u00a7c${ex.message ?: "参数解析失败"}")
                    return@execute
                }
                val restrictionError = validateRestriction(node.argument.restrict, sender, rawValue, parsedValue)
                if (restrictionError != null) {
                    sender.sendMessage("\u00a7c$restrictionError")
                    return@execute
                }

                val parsedArgs = HashMap(capturedArgs)
                parsedArgs[node.name] = parsedValue
                val cmdContext = CommandContext(
                    definitionId = definitionId,
                    sender = sender,
                    rawArgs = content.split(" ").filter { it.isNotBlank() },
                    parsedArgs = parsedArgs,
                    path = path,
                )
                executeRoute(route, cmdContext)
            }
        }

        // 递归构建子节点（将当前参数节点的名称加入累积参数，实际值在运行时填充）
        buildChildren(component, node.children, definitionId, path, accumulatedArgs)
    }

    /** 构建子节点列表 */
    private fun buildChildren(
        component: CommandComponent,
        children: List<CommandNode>,
        definitionId: String,
        path: List<String>,
        accumulatedArgs: Map<String, Any?>,
    ) {
        for (child in children) {
            when (child) {
                is LiteralNode -> {
                    component.literal(child.name) {
                        val childPath = path + child.name
                        buildNode(this, child, definitionId, childPath, accumulatedArgs)
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
                        buildNode(this, child, definitionId, childPath, accumulatedArgs)
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
                sender = sender,
                rawArgs = emptyList(),
                parsedArgs = emptyMap(),
                path = emptyList(),
            )
            return provider.suggest(cmdContext)
        }
        // 内建补全兜底
        return when (suggestId) {
            "online-player" -> Bukkit.getOnlinePlayers().map { it.name }
            "offline-player" -> Bukkit.getOfflinePlayers().mapNotNull { it.name }
            "world" -> Bukkit.getWorlds().map { it.name }
            "material" -> Material.entries.map { it.name.lowercase() }
            else -> emptyList()
        }
    }

    private fun parseArgumentValue(spec: cc.bkhk.monoceros.api.command.ArgumentSpec, raw: String): Any? {
        return when (spec.type) {
            cc.bkhk.monoceros.api.command.ArgumentType.STRING,
            cc.bkhk.monoceros.api.command.ArgumentType.SCRIPT_ID -> raw
            cc.bkhk.monoceros.api.command.ArgumentType.INT -> raw.toIntOrNull()
                ?: throw IllegalArgumentException("参数必须是整数: $raw")
            cc.bkhk.monoceros.api.command.ArgumentType.DOUBLE -> raw.toDoubleOrNull()
                ?: throw IllegalArgumentException("参数必须是数字: $raw")
            cc.bkhk.monoceros.api.command.ArgumentType.BOOLEAN -> raw.toBooleanStrictOrNull()
                ?: throw IllegalArgumentException("参数必须是 true 或 false: $raw")
            cc.bkhk.monoceros.api.command.ArgumentType.PLAYER -> Bukkit.getPlayerExact(raw)
                ?: throw IllegalArgumentException("玩家不存在或不在线: $raw")
            cc.bkhk.monoceros.api.command.ArgumentType.OFFLINE_PLAYER -> Bukkit.getOfflinePlayers().firstOrNull {
                it.name.equals(raw, ignoreCase = true)
            } ?: throw IllegalArgumentException("离线玩家不存在: $raw")
            cc.bkhk.monoceros.api.command.ArgumentType.WORLD -> Bukkit.getWorld(raw)
                ?: throw IllegalArgumentException("世界不存在: $raw")
            cc.bkhk.monoceros.api.command.ArgumentType.MATERIAL -> Material.matchMaterial(raw)
                ?: throw IllegalArgumentException("物品材质不存在: $raw")
        }
    }

    private fun validateRestriction(
        restriction: RestrictionSpec?,
        sender: CommandSender,
        raw: String,
        parsed: Any?,
    ): String? {
        if (restriction == null) return null
        return when (restriction.mode.lowercase()) {
            "range" -> validateRange(parsed, restriction.value)
            "regex" -> if (Regex(restriction.value).matches(raw)) null else "参数不匹配正则限制: ${restriction.value}"
            "enum", "whitelist" -> {
                val allowed = restriction.value.split(',', '|').map { it.trim() }.filter { it.isNotEmpty() }
                if (allowed.any { it.equals(raw, ignoreCase = true) }) null else "参数必须是以下值之一: ${allowed.joinToString(", ")}"
            }
            "permission" -> if (sender.hasPermission(restriction.value)) null else "缺少参数使用权限: ${restriction.value}"
            "sender" -> when (restriction.value.lowercase()) {
                "player" -> if (sender is org.bukkit.entity.Player) null else "该参数仅允许玩家使用"
                "console" -> if (sender !is org.bukkit.entity.Player) null else "该参数仅允许控制台使用"
                else -> null
            }
            else -> null
        }
    }

    private fun validateRange(parsed: Any?, value: String): String? {
        val number = parsed as? Number ?: return "range 限制仅适用于数值参数"
        val match = Regex("^\\s*(-?\\d+(?:\\.\\d+)?)\\s*(?:\\.\\.|-)\\s*(-?\\d+(?:\\.\\d+)?)\\s*$").find(value)
            ?: return null
        val min = match.groupValues[1].toDoubleOrNull() ?: return null
        val max = match.groupValues[2].toDoubleOrNull() ?: return null
        val current = number.toDouble()
        return if (current in min..max) null else "参数必须在 $min 到 $max 之间"
    }
}
