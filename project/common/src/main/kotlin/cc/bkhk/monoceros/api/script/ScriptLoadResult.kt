package cc.bkhk.monoceros.api.script

/**
 * 脚本加载结果
 */
data class ScriptLoadResult(
    /** 成功加载数量 */
    val loaded: Int,
    /** 加载失败数量 */
    val failed: Int,
    /** 移除数量 */
    val removed: Int,
    /** 耗时（毫秒） */
    val costMs: Long,
)
