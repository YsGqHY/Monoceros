package cc.bkhk.monoceros.api.script

/**
 * 脚本缓存统计
 */
data class ScriptCacheStats(
    val cacheSize: Int = 0,
    val invokeHits: Long = 0,
    val invokeMisses: Long = 0,
    val totalCompilations: Long = 0,
    val totalCompilationNanos: Long = 0,
)
