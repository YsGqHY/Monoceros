package cc.bkhk.monoceros.schedule

import cc.bkhk.monoceros.api.schedule.ScheduleHandler
import cc.bkhk.monoceros.api.schedule.ScheduleService
import cc.bkhk.monoceros.api.service.ReloadableService
import cc.bkhk.monoceros.impl.registry.ConcurrentRegistry
import cc.bkhk.monoceros.impl.service.ReloadService
import cc.bkhk.monoceros.impl.util.DiagnosticLogger
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.PlatformFactory

/**
 * 调度系统生命周期注册器
 */
object ScheduleServiceLoader {

    private const val MODULE = "Schedule"

    private val handlerRegistry = ConcurrentRegistry<ScheduleHandler>()
    private lateinit var service: DefaultScheduleService

    @Awake(LifeCycle.LOAD)
    fun onLoad() {
        service = DefaultScheduleService(handlerRegistry)
        PlatformFactory.registerAPI<ScheduleService>(service)
        DiagnosticLogger.info(MODULE, "调度服务已注册到 PlatformFactory")

        // 注册到统一重载服务
        ReloadService.register(object : ReloadableService {
            override val serviceId = "schedule"
            override val priority = 50
            override fun reload(): Int = service.reloadAll()
        })
    }

    @Awake(LifeCycle.ENABLE)
    fun onEnable() {
        val count = service.reloadAll()
        DiagnosticLogger.info(MODULE, "调度定义加载完成: $count 个")

        // 启动文件监听
        service.startWatcher(service.createWatcherCallback())
    }

    @Awake(LifeCycle.DISABLE)
    fun onDisable() {
        service.stopWatcher()
        service.stopAll()
        DiagnosticLogger.info(MODULE, "调度系统已清理")
    }
}
