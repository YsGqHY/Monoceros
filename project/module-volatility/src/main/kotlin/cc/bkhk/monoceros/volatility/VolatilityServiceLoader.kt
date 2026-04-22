package cc.bkhk.monoceros.volatility

import cc.bkhk.monoceros.api.volatility.VolatilityService
import cc.bkhk.monoceros.impl.util.DiagnosticLogger
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.PlatformFactory

/**
 * 挥发能力系统生命周期注册器
 */
object VolatilityServiceLoader {

    private const val MODULE = "Volatility"

    private val illusionService = DefaultIllusionSessionService()
    private lateinit var service: DefaultVolatilityService

    @Awake(LifeCycle.LOAD)
    fun onLoad() {
        service = DefaultVolatilityService(illusionService)
        PlatformFactory.registerAPI<VolatilityService>(service)
        DiagnosticLogger.info(MODULE, "挥发能力服务已注册到 PlatformFactory")
    }

    @Awake(LifeCycle.DISABLE)
    fun onDisable() {
        illusionService.clearAll()
        DiagnosticLogger.info(MODULE, "挥发能力系统已清理")
    }
}
