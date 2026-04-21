package cc.bkhk.monoceros.wireshark

import cc.bkhk.monoceros.api.wireshark.PacketHandler
import cc.bkhk.monoceros.api.wireshark.PacketService
import cc.bkhk.monoceros.impl.registry.ConcurrentRegistry
import cc.bkhk.monoceros.impl.util.DiagnosticLogger
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.PlatformFactory

/**
 * Wireshark 数据包系统生命周期注册器
 */
object WiresharkServiceLoader {

    private const val MODULE = "Wireshark"

    private val handlerRegistry = ConcurrentRegistry<PacketHandler>()
    private lateinit var service: DefaultPacketService

    @Awake(LifeCycle.LOAD)
    private fun onLoad() {
        service = DefaultPacketService(handlerRegistry)
        PlatformFactory.registerAPI<PacketService>(service)
        DiagnosticLogger.info(MODULE, "数据包服务已注册到 PlatformFactory")
    }

    @Awake(LifeCycle.DISABLE)
    private fun onDisable() {
        service.closeAllSessions()
        DiagnosticLogger.info(MODULE, "数据包系统已清理")
    }
}
