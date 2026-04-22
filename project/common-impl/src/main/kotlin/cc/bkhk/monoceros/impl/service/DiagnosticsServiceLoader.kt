package cc.bkhk.monoceros.impl.service

import cc.bkhk.monoceros.api.service.DiagnosticsService
import cc.bkhk.monoceros.impl.util.DiagnosticLogger
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.PlatformFactory

/**
 * 诊断服务注册器
 */
object DiagnosticsServiceLoader {

    private const val MODULE = "Diagnostics"

    @Awake(LifeCycle.LOAD)
    fun onLoad() {
        PlatformFactory.registerAPI<DiagnosticsService>(DefaultDiagnosticsService())
        DiagnosticLogger.info(MODULE, "诊断服务已注册到 PlatformFactory")
    }
}
