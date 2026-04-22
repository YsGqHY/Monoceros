package cc.bkhk.monoceros.impl.service

import cc.bkhk.monoceros.api.script.MonocerosScriptSource
import cc.bkhk.monoceros.api.service.DiagnosticIssue
import cc.bkhk.monoceros.api.service.DiagnosticLevel
import cc.bkhk.monoceros.api.service.DiagnosticsService
import cc.bkhk.monoceros.api.version.VersionAdapterResolver
import cc.bkhk.monoceros.impl.script.FluxonChecker
import cc.bkhk.monoceros.impl.util.DiagnosticLogger
import taboolib.common.platform.PlatformFactory

/**
 * 默认诊断服务实现
 */
class DefaultDiagnosticsService : DiagnosticsService {

    override fun selfcheck(): List<DiagnosticIssue> {
        val issues = mutableListOf<DiagnosticIssue>()

        if (FluxonChecker.isUnavailable()) {
            issues += DiagnosticIssue(
                level = DiagnosticLevel.ERROR,
                source = "fluxon",
                message = "Fluxon 运行时不可用（source=${FluxonChecker.sourceId()}）",
                suggestion = FluxonChecker.startupFailureMessage() ?: "检查网络、依赖下载或外部 FluxonPlugin。",
            )
        } else if (!FluxonChecker.isReady()) {
            issues += DiagnosticIssue(
                level = DiagnosticLevel.WARN,
                source = "fluxon",
                message = "Fluxon 运行时尚未就绪（source=${FluxonChecker.sourceId()}）",
                suggestion = "确认脚本运行时是否已完成装配。",
            )
        }

        val scriptHandler = PlatformFactory.getAPIOrNull<cc.bkhk.monoceros.api.script.MonocerosScriptHandler>()
        if (scriptHandler == null) {
            issues += DiagnosticIssue(
                level = DiagnosticLevel.ERROR,
                source = "script",
                message = "MonocerosScriptHandler 未注册",
                suggestion = "检查 ScriptServiceLoader 是否在 LOAD 阶段完成注册。",
            )
        } else if (scriptHandler.getScriptType(MonocerosScriptSource.DEFAULT_TYPE) == null) {
            issues += DiagnosticIssue(
                level = DiagnosticLevel.ERROR,
                source = "script",
                message = "默认脚本类型 ${MonocerosScriptSource.DEFAULT_TYPE} 未注册",
                suggestion = "检查 FluxonScriptType 注册流程。",
            )
        }

        if (PlatformFactory.getAPIOrNull<cc.bkhk.monoceros.api.dispatcher.DispatcherService>() == null) {
            issues += DiagnosticIssue(DiagnosticLevel.WARN, "dispatcher", "DispatcherService 未注册")
        }
        if (PlatformFactory.getAPIOrNull<cc.bkhk.monoceros.api.schedule.ScheduleService>() == null) {
            issues += DiagnosticIssue(DiagnosticLevel.WARN, "schedule", "ScheduleService 未注册")
        }
        if (PlatformFactory.getAPIOrNull<cc.bkhk.monoceros.api.command.CommandService>() == null) {
            issues += DiagnosticIssue(DiagnosticLevel.WARN, "command", "CommandService 未注册")
        }
        if (PlatformFactory.getAPIOrNull<cc.bkhk.monoceros.api.workflow.ActionWorkflowService>() == null) {
            issues += DiagnosticIssue(DiagnosticLevel.WARN, "workflow-action", "ActionWorkflowService 未注册")
        }
        if (PlatformFactory.getAPIOrNull<cc.bkhk.monoceros.api.workflow.PropertyWorkflowService>() == null) {
            issues += DiagnosticIssue(DiagnosticLevel.WARN, "workflow-property", "PropertyWorkflowService 未注册")
        }

        val packetService = PlatformFactory.getAPIOrNull<cc.bkhk.monoceros.api.wireshark.PacketService>()
        if (packetService == null) {
            issues += DiagnosticIssue(DiagnosticLevel.WARN, "wireshark", "PacketService 未注册")
        } else {
            val allowIntercept = reflectField(packetService, "allowIntercept") as? Boolean ?: false
            val allowRewrite = reflectField(packetService, "allowRewrite") as? Boolean ?: false
            if (allowIntercept || allowRewrite) {
                issues += DiagnosticIssue(
                    level = DiagnosticLevel.WARN,
                    source = "wireshark",
                    message = "高风险 packet 能力已开启（intercept=$allowIntercept, rewrite=$allowRewrite）",
                    suggestion = "生产环境建议只在必要时开启，并结合 trace/selfcheck 使用。",
                )
            }
        }

        return issues
    }

    override fun dumpRuntime(): Map<String, Any?> {
        val versionResolver = PlatformFactory.getAPIOrNull<VersionAdapterResolver>()
        val api = cc.bkhk.monoceros.Monoceros.apiOrNull()
        val packetService = PlatformFactory.getAPIOrNull<cc.bkhk.monoceros.api.wireshark.PacketService>()
        val taps = (reflectField(packetService, "taps") as? Map<*, *>)?.size ?: 0
        val sessions = (reflectField(packetService, "sessions") as? Map<*, *>)?.size ?: 0
        val allowIntercept = reflectField(packetService, "allowIntercept") as? Boolean ?: false
        val allowRewrite = reflectField(packetService, "allowRewrite") as? Boolean ?: false

        return linkedMapOf(
            "apiRegistered" to (api != null),
            "debugEnabled" to DiagnosticLogger.isDebug(),
            "reloadServices" to ReloadService.serviceIds(),
            "fluxonSource" to FluxonChecker.sourceId(),
            "fluxonReady" to FluxonChecker.isReady(),
            "scriptHandlerRegistered" to (PlatformFactory.getAPIOrNull<cc.bkhk.monoceros.api.script.MonocerosScriptHandler>() != null),
            "dispatcherRegistered" to (PlatformFactory.getAPIOrNull<cc.bkhk.monoceros.api.dispatcher.DispatcherService>() != null),
            "scheduleRegistered" to (PlatformFactory.getAPIOrNull<cc.bkhk.monoceros.api.schedule.ScheduleService>() != null),
            "commandRegistered" to (PlatformFactory.getAPIOrNull<cc.bkhk.monoceros.api.command.CommandService>() != null),
            "packetTapCount" to taps,
            "packetSessionCount" to sessions,
            "allowIntercept" to allowIntercept,
            "allowRewrite" to allowRewrite,
            "versionProfile" to versionResolver?.currentProfile(),
            "featureFlags" to versionResolver?.featureFlags(),
        )
    }

    override fun cacheStats(): Map<String, Any?> {
        val scriptHandler = PlatformFactory.getAPIOrNull<cc.bkhk.monoceros.api.script.MonocerosScriptHandler>()
        val scriptStats = scriptHandler?.cacheStats()
        return linkedMapOf(
            "script" to linkedMapOf(
                "cacheSize" to (scriptStats?.cacheSize ?: 0),
                "invokeHits" to (scriptStats?.invokeHits ?: 0),
                "invokeMisses" to (scriptStats?.invokeMisses ?: 0),
                "totalCompilations" to (scriptStats?.totalCompilations ?: 0),
                "totalCompilationNanos" to (scriptStats?.totalCompilationNanos ?: 0L),
            )
        )
    }

    private fun reflectField(instance: Any?, name: String): Any? {
        if (instance == null) return null
        return runCatching {
            val field = instance.javaClass.getDeclaredField(name)
            field.isAccessible = true
            field.get(instance)
        }.getOrNull()
    }
}
