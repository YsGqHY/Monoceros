package cc.bkhk.monoceros.api.script

/**
 * 脚本来源数据
 */
data class MonocerosScriptSource(
    val type: String = DEFAULT_TYPE,
    val content: String,
    val origin: String? = null,
) {
    companion object {
        const val DEFAULT_TYPE: String = "fluxon"
    }
}
