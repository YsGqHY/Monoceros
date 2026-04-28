package cc.bkhk.monoceros.api.command

import cc.bkhk.monoceros.api.util.SenderAdapter
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender

/**
 * 命令执行上下文
 */
data class CommandContext(
    val definitionId: String,
    val sender: CommandSender,
    val rawArgs: List<String>,
    val parsedArgs: Map<String, Any?>,
    val path: List<String>,
) {
    companion object {
        /** 兼容旧版 API：接受任意 sender 类型（含 relocated ProxyCommandSender） */
        @JvmStatic
        fun fromAnySender(
            definitionId: String,
            sender: Any?,
            rawArgs: List<String>,
            parsedArgs: Map<String, Any?>,
            path: List<String>,
        ): CommandContext = CommandContext(
            definitionId,
            SenderAdapter.adapt(sender) ?: Bukkit.getConsoleSender(),
            rawArgs,
            parsedArgs,
            path,
        )
    }
}

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
