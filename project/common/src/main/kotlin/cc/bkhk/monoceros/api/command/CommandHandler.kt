package cc.bkhk.monoceros.api.command

import taboolib.common.platform.ProxyCommandSender

/**
 * 命令执行上下文
 */
data class CommandContext(
    val definitionId: String,
    val sender: ProxyCommandSender,
    val rawArgs: List<String>,
    val parsedArgs: Map<String, Any?>,
    val path: List<String>,
)

/**
 * 命令强类型处理器
 *
 * 适用于 reload、selfcheck、diagnostics 等高频运维命令。
 * 通过 [CommandRoute.Handler] 路由到此接口。
 */
interface CommandHandler {
    val id: String
    fun execute(context: CommandContext): Any?
}

/**
 * 参数补全提供器
 */
interface SuggestionProvider {
    val id: String
    fun suggest(context: CommandContext): List<String>
}
