package cc.bkhk.monoceros.impl.util

import taboolib.common.platform.function.info
import taboolib.common.platform.function.warning

/**
 * 诊断日志工具
 *
 * 统一诊断输出格式，便于运维排查。
 */
object DiagnosticLogger {

    private const val PREFIX = "[Monoceros]"

    /** 输出诊断信息 */
    fun info(module: String, message: String) {
        info("$PREFIX [$module] $message")
    }

    /** 输出诊断警告 */
    fun warn(module: String, message: String) {
        warning("$PREFIX [$module] $message")
    }

    /** 输出诊断警告（附带异常） */
    fun warn(module: String, message: String, throwable: Throwable) {
        warning("$PREFIX [$module] $message")
        throwable.printStackTrace()
    }

    /** 输出模块加载摘要 */
    fun summary(module: String, loaded: Int, failed: Int = 0) {
        if (failed > 0) {
            warning("$PREFIX [$module] 加载完成: $loaded 成功, $failed 失败")
        } else {
            info("$PREFIX [$module] 加载完成: $loaded 项")
        }
    }
}
