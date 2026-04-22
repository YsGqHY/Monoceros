package cc.bkhk.monoceros.impl.mechanic.region

import cc.bkhk.monoceros.api.mechanic.region.RegionService
import cc.bkhk.monoceros.impl.util.DiagnosticLogger
import org.bukkit.event.player.PlayerQuitEvent
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.PlatformFactory
import taboolib.common.platform.event.SubscribeEvent

object RegionServiceLoader {
    private lateinit var service: DefaultRegionService

    @Awake(LifeCycle.LOAD)
    fun onLoad() {
        service = DefaultRegionService()
        PlatformFactory.registerAPI<RegionService>(service)
        DiagnosticLogger.info("Region", "区域机制服务已注册")
    }

    @Awake(LifeCycle.ENABLE)
    fun onEnable() { service.startTicking() }

    @Awake(LifeCycle.DISABLE)
    fun onDisable() { service.stopTicking() }

    @SubscribeEvent
    fun onQuit(event: PlayerQuitEvent) { service.clearPlayer(event.player.uniqueId) }
}
