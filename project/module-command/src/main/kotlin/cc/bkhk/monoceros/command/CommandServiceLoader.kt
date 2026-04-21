package cc.bkhk.monoceros.command

import cc.bkhk.monoceros.api.command.CommandHandler
import cc.bkhk.monoceros.api.command.CommandService
import cc.bkhk.monoceros.api.command.SuggestionProvider
import cc.bkhk.monoceros.impl.registry.ConcurrentRegistry
import cc.bkhk.monoceros.impl.util.DiagnosticLogger
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.PlatformFactory

/**
 * 命令系统生命周期注册器
 */
object CommandServiceLoader {

    private const val MODULE = "Command"

    private val handlerRegistry = ConcurrentRegistry<CommandHandler>()
    private val suggestionRegistry = ConcurrentRegistry<SuggestionProvider>()
    private lateinit var service: DefaultCommandService

    @Awake(LifeCycle.LOAD)
    private fun onLoad() {
        service = DefaultCommandService(handlerRegistry, suggestionRegistry)
        PlatformFactory.registerAPI<CommandService>(service)
        DiagnosticLogger.info(MODULE, "命令服务已注册到 PlatformFactory")
    }

    @Awake(LifeCycle.ENABLE)
    private fun onEnable() {
        val count = service.reloadAll()
        DiagnosticLogger.info(MODULE, "命令定义加载完成: $count 个")

        // 启动文件监听
        service.startWatcher(service.createWatcherCallback())
    }

    @Awake(LifeCycle.DISABLE)
    private fun onDisable() {
        service.stopWatcher()
        DiagnosticLogger.info(MODULE, "命令系统已清理")
    }
}
