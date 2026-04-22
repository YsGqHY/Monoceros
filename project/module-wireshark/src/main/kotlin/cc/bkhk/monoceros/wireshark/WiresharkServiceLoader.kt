package cc.bkhk.monoceros.wireshark

import cc.bkhk.monoceros.api.service.ReloadableService
import cc.bkhk.monoceros.api.wireshark.PacketHandler
import cc.bkhk.monoceros.api.wireshark.PacketService
import cc.bkhk.monoceros.impl.registry.ConcurrentRegistry
import cc.bkhk.monoceros.impl.service.ReloadService
import cc.bkhk.monoceros.impl.util.DiagnosticLogger
import org.bukkit.event.player.PlayerQuitEvent
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.PlatformFactory
import taboolib.common.platform.event.SubscribeEvent

/**
 * Wireshark 数据包系统生命周期注册器
 */
object WiresharkServiceLoader {

    private const val MODULE = "Wireshark"

    private val handlerRegistry = ConcurrentRegistry<PacketHandler>()
    private lateinit var service: DefaultPacketService
    private lateinit var configService: PacketTapConfigService

    @Awake(LifeCycle.LOAD)
    fun onLoad() {
        service = DefaultPacketService(handlerRegistry)
        configService = PacketTapConfigService(service)
        PlatformFactory.registerAPI<PacketService>(service)
        DiagnosticLogger.info(MODULE, "数据包服务已注册到 PlatformFactory")

        ReloadService.register(object : ReloadableService {
            override val serviceId: String = "wireshark"
            override val priority: Int = 40
            override fun reload(): Int = configService.reloadAll()
        })
    }

    @Awake(LifeCycle.ENABLE)
    fun onEnable() {
        val count = configService.reloadAll()
        DiagnosticLogger.info(MODULE, "packet tap 加载完成: $count 个")
        configService.startWatcher(configService.createWatcherCallback())
    }

    @Awake(LifeCycle.DISABLE)
    fun onDisable() {
        configService.stopWatcher()
        service.closeAllSessions()
        DiagnosticLogger.info(MODULE, "数据包系统已清理")
    }

    /** 玩家退出时自动清理 packet 会话 */
    @SubscribeEvent
    fun onPlayerQuit(event: PlayerQuitEvent) {
        service.closeSession(event.player.uniqueId)
    }
}
