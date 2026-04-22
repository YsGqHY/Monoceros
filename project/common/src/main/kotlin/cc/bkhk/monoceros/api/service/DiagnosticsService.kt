package cc.bkhk.monoceros.api.service

/**
 * 模块重载报告
 */
data class ReloadReport(
    val target: String,
    val loaded: Int,
    val updated: Int = 0,
    val failed: Int = 0,
    val costMs: Long = 0,
)

/**
 * 诊断级别
 */
enum class DiagnosticLevel {
    INFO,
    WARN,
    ERROR,
}

/**
 * 诊断问题
 */
data class DiagnosticIssue(
    val level: DiagnosticLevel,
    val source: String,
    val message: String,
    val suggestion: String? = null,
)

/**
 * 统一诊断服务
 */
interface DiagnosticsService {

    /** 运行自检，返回问题列表 */
    fun selfcheck(): List<DiagnosticIssue>

    /** 导出当前运行态信息 */
    fun dumpRuntime(): Map<String, Any?>

    /** 导出缓存与统计信息 */
    fun cacheStats(): Map<String, Any?>
}
