package cc.bkhk.monoceros.command

import cc.bkhk.monoceros.api.command.CommandHandler
import cc.bkhk.monoceros.api.command.CommandService
import cc.bkhk.monoceros.api.command.SuggestionProvider
import cc.bkhk.monoceros.api.service.ReloadableService
import cc.bkhk.monoceros.impl.registry.ConcurrentRegistry
import cc.bkhk.monoceros.impl.service.ReloadService
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
    fun onLoad() {
        suggestionRegistry.register(OnlinePlayerSuggestionProvider.id, OnlinePlayerSuggestionProvider)
        suggestionRegistry.register(OfflinePlayerSuggestionProvider.id, OfflinePlayerSuggestionProvider)
        suggestionRegistry.register(WorldSuggestionProvider.id, WorldSuggestionProvider)
        suggestionRegistry.register(MaterialSuggestionProvider.id, MaterialSuggestionProvider)
        suggestionRegistry.register(ScriptIdSuggestionProvider.id, ScriptIdSuggestionProvider)
        suggestionRegistry.register(DispatcherIdSuggestionProvider.id, DispatcherIdSuggestionProvider)
        suggestionRegistry.register(ScheduleIdSuggestionProvider.id, ScheduleIdSuggestionProvider)
        suggestionRegistry.register(WorkflowIdSuggestionProvider.id, WorkflowIdSuggestionProvider)

        service = DefaultCommandService(handlerRegistry, suggestionRegistry)
        PlatformFactory.registerAPI<CommandService>(service)
        DiagnosticLogger.info(MODULE, "命令服务已注册到 PlatformFactory")

        // 注册到统一重载服务
        ReloadService.register(object : ReloadableService {
            override val serviceId = "command"
            override val priority = 50
            override fun reload(): Int = service.reloadAll()
        })
    }

    @Awake(LifeCycle.ENABLE)
    fun onEnable() {
        val count = service.reloadAll()
        DiagnosticLogger.info(MODULE, "命令定义加载完成: $count 个")

        // 启动文件监听
        service.startWatcher(service.createWatcherCallback())
    }

    @Awake(LifeCycle.DISABLE)
    fun onDisable() {
        service.stopWatcher()
        DiagnosticLogger.info(MODULE, "命令系统已清理")
    }
}
