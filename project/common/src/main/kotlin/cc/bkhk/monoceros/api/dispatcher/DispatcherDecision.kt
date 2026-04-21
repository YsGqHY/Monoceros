package cc.bkhk.monoceros.api.dispatcher

/**
 * 规则判定结果
 */
data class DispatcherDecision(
    val filtered: Boolean = false,
    val cancelEvent: Boolean = false,
    val extraVariables: Map<String, Any?> = emptyMap(),
)
