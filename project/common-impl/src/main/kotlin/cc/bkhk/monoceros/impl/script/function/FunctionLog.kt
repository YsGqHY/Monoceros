package cc.bkhk.monoceros.impl.script.function

import cc.bkhk.monoceros.impl.script.FluxonChecker
import cc.bkhk.monoceros.impl.script.relocate.FluxonRelocate
import cc.bkhk.monoceros.impl.util.DiagnosticLogger
import org.tabooproject.fluxon.runtime.FluxonRuntime
import org.tabooproject.fluxon.runtime.FunctionSignature
import org.tabooproject.fluxon.runtime.Type
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake

/**
 * 结构化日志函数，封装 DiagnosticLogger 为 Fluxon 脚本函数。
 *
 * ```fluxon
 * logInfo("玩家加入处理完成")
 * logWarn("配置项缺失，使用默认值")
 * logDebug("变量快照: ${&snapshot}")
 * logError("脚本执行异常")
 * ```
 */
@FluxonRelocate
object FunctionLog {

    private const val MODULE = "Script"

    @Awake(LifeCycle.ENABLE)
    private fun init() {
        if (!FluxonChecker.isReady()) return
        with(FluxonRuntime.getInstance()) {
            // logInfo(msg)
            registerFunction("logInfo", FunctionSignature.returnsVoid().params(Type.STRING)) { ctx ->
                DiagnosticLogger.info(MODULE, ctx.getString(0))
            }
            exportRegisteredFunction("logInfo")

            // logWarn(msg)
            registerFunction("logWarn", FunctionSignature.returnsVoid().params(Type.STRING)) { ctx ->
                DiagnosticLogger.warn(MODULE, ctx.getString(0))
            }
            exportRegisteredFunction("logWarn")

            // logDebug(msg)
            registerFunction("logDebug", FunctionSignature.returnsVoid().params(Type.STRING)) { ctx ->
                DiagnosticLogger.debug(MODULE, ctx.getString(0))
            }
            exportRegisteredFunction("logDebug")

            // logError(msg)
            registerFunction("logError", FunctionSignature.returnsVoid().params(Type.STRING)) { ctx ->
                DiagnosticLogger.warn(MODULE, "[ERROR] " + ctx.getString(0))
            }
            exportRegisteredFunction("logError")
        }
    }
}
