package cc.bkhk.monoceros.impl.util

import taboolib.common.platform.function.info
import taboolib.common.platform.function.warning

/**
 * 诊断日志工具
 *
 * 统一诊断输出格式，便于运维排查。
 */
object DiagnosticLogger {

    @Volatile
    private var debug = false

    /** 获取调试模式状态 */
    fun isDebug(): Boolean = debug

    /** 设置调试模式 */
    fun setDebug(enabled: Boolean) {
        debug = enabled
    }

    /** 输出诊断信息 */
    fun info(module: String, message: String) {
        info("[$module] $message")
    }

    /** 仅在调试模式下输出的详细信息 */
    fun debug(module: String, message: String) {
        if (debug) info("[$module] $message")
    }

    /** 输出诊断警告 */
    fun warn(module: String, message: String) {
        warning("[$module] $message")
    }

    /** 输出诊断警告（附带异常） */
    fun warn(module: String, message: String, throwable: Throwable) {
        warning("[$module] $message")
        throwable.printStackTrace()
    }

    /** 输出模块加载摘要 */
    fun summary(module: String, loaded: Int, failed: Int = 0) {
        if (failed > 0) {
            warning("[$module] 加载完成: $loaded 成功, $failed 失败")
        } else {
            info("[$module] 加载完成: $loaded 项")
        }
    }
}
