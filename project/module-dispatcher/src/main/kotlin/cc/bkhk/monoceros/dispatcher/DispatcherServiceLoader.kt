package cc.bkhk.monoceros.dispatcher

import cc.bkhk.monoceros.api.dispatcher.DispatcherHandler
import cc.bkhk.monoceros.api.dispatcher.DispatcherService
import cc.bkhk.monoceros.impl.registry.ConcurrentRegistry
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
    private fun onLoad() {
        service = DefaultDispatcherService(handlerRegistry)
        PlatformFactory.registerAPI<DispatcherService>(service)
        DiagnosticLogger.info(MODULE, "事件分发服务已注册到 PlatformFactory")
    }

    @Awake(LifeCycle.ENABLE)
    private fun onEnable() {
        val count = service.reloadAll()
        DiagnosticLogger.info(MODULE, "事件分发器加载完成: $count 个")

        // 启动文件监听
        service.startWatcher(service.createWatcherCallback())
    }

    @Awake(LifeCycle.DISABLE)
    private fun onDisable() {
        service.stopWatcher()
        DispatcherListener.unregisterAll()
        DiagnosticLogger.info(MODULE, "事件分发系统已清理")
    }
}
