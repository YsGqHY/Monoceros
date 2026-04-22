package cc.bkhk.monoceros.dispatcher

import cc.bkhk.monoceros.api.dispatcher.DispatcherHandler
import cc.bkhk.monoceros.api.dispatcher.DispatcherService
import cc.bkhk.monoceros.api.dispatcher.pipeline.PipelineRegistry
import cc.bkhk.monoceros.api.service.ReloadableService
import cc.bkhk.monoceros.dispatcher.pipeline.DefaultPipelineRegistry
import cc.bkhk.monoceros.dispatcher.pipeline.event.*
import cc.bkhk.monoceros.impl.registry.ConcurrentRegistry
import cc.bkhk.monoceros.impl.service.ReloadService
import cc.bkhk.monoceros.impl.util.DiagnosticLogger
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.PlatformFactory

/**
 * 事件分发系统生命周期注册器
 */
object DispatcherServiceLoader {

    private const val MODULE = "Dispatcher"

    private val handlerRegistry = ConcurrentRegistry<DispatcherHandler>()
    private lateinit var service: DefaultDispatcherService

    @Awake(LifeCycle.LOAD)
    fun onLoad() {
        service = DefaultDispatcherService(handlerRegistry)
        PlatformFactory.registerAPI<DispatcherService>(service)
        PlatformFactory.registerAPI<PipelineRegistry>(DefaultPipelineRegistry)
        DiagnosticLogger.info(MODULE, "事件分发服务已注册到 PlatformFactory")

        // 注册内建 Pipeline
        registerBuiltinPipelines()

        // 注册到统一重载服务
        ReloadService.register(object : ReloadableService {
            override val serviceId = "dispatcher"
            override val priority = 50
            override fun reload(): Int = service.reloadAll()
        })
    }

    @Awake(LifeCycle.ENABLE)
    fun onEnable() {
        val count = service.reloadAll()
        DiagnosticLogger.info(MODULE, "事件分发器加载完成: $count 个")

        // 启动文件监听
        service.startWatcher(service.createWatcherCallback())
    }

    @Awake(LifeCycle.DISABLE)
    fun onDisable() {
        service.stopWatcher()
        DispatcherListener.unregisterAll()
        DiagnosticLogger.info(MODULE, "事件分发系统已清理")
    }

    /** 注册内建事件 Pipeline */
    private fun registerBuiltinPipelines() {
        val reg = DefaultPipelineRegistry

        // 玩家事件
        reg.register("AsyncPlayerChatEvent", PlayerChatEventPipeline())
        reg.register("PlayerCommandPreprocessEvent", PlayerCommandPreprocessEventPipeline())
        reg.register("PlayerMoveEvent", PlayerMoveEventPipeline())
        reg.register("PlayerJoinEvent", PlayerJoinEventPipeline())
        reg.register("PlayerQuitEvent", PlayerQuitEventPipeline())
        reg.register("PlayerTeleportEvent", PlayerTeleportEventPipeline())

        // 虚拟玩家事件（从非 PlayerEvent 中提取 Player）
        reg.register("PlayerDamageEvent", PlayerDamageEventPipeline())
        reg.register("PlayerDamageByEntityEvent", PlayerDamageByEntityEventPipeline())
        reg.register("PlayerDamageByPlayerEvent", PlayerDamageByPlayerEventPipeline())
        reg.register("PlayerShootBowEvent", PlayerShootBowEventPipeline())

        // 实体事件
        reg.register("EntityDamageEvent", EntityDamageEventPipeline())
        reg.register("EntityDamageByEntityEvent", EntityDamageByEntityEventPipeline())
        reg.register("EntityShootBowEvent", EntityShootBowEventPipeline())

        DiagnosticLogger.info(MODULE, "内建 Pipeline 注册完成: ${reg.registeredEvents().size} 个事件")
    }
}
