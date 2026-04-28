package cc.bkhk.monoceros.api.script

import cc.bkhk.monoceros.api.util.SenderAdapter
import org.bukkit.command.CommandSender

/**
 * 脚本调用请求
 */
data class ScriptInvokeRequest(
    val definitionId: String,
    val source: MonocerosScriptSource,
    val sender: CommandSender?,
    val variables: Map<String, Any?> = emptyMap(),
    val asyncAllowed: Boolean = true,
) {
    companion object {
        /** 兼容旧版 API：接受任意 sender 类型（含 relocated ProxyCommandSender） */
        @JvmStatic
        fun fromAnySender(
            definitionId: String,
            source: MonocerosScriptSource,
            sender: Any?,
            variables: Map<String, Any?> = emptyMap(),
            asyncAllowed: Boolean = true,
        ): ScriptInvokeRequest = ScriptInvokeRequest(definitionId, source, SenderAdapter.adapt(sender), variables, asyncAllowed)
    }
}
