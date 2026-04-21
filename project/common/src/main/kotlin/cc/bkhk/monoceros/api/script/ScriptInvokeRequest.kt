package cc.bkhk.monoceros.api.script

import taboolib.common.platform.ProxyCommandSender

/**
 * 脚本调用请求
 */
data class ScriptInvokeRequest(
    val definitionId: String,
    val source: MonocerosScriptSource,
    val sender: ProxyCommandSender?,
    val variables: Map<String, Any?> = emptyMap(),
    val asyncAllowed: Boolean = true,
)
