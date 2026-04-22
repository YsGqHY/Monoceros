package cc.bkhk.monoceros.command

import cc.bkhk.monoceros.Monoceros
import cc.bkhk.monoceros.api.schedule.ScheduleState
import cc.bkhk.monoceros.api.script.ScriptDefinitionRegistry
import cc.bkhk.monoceros.api.script.ScriptTaskTracker
import cc.bkhk.monoceros.api.service.DiagnosticLevel
import cc.bkhk.monoceros.api.service.DiagnosticsService
import cc.bkhk.monoceros.api.version.VersionAdapterResolver
import cc.bkhk.monoceros.impl.diagram.TableDiagram
import cc.bkhk.monoceros.impl.service.ReloadService
import cc.bkhk.monoceros.impl.util.DiagnosticLogger
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.PlatformFactory
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.command.PermissionDefault
import taboolib.common.platform.command.command
import taboolib.common.platform.function.adaptCommandSender
import taboolib.common.platform.function.adaptPlayer
import taboolib.common.platform.function.pluginVersion
import taboolib.expansion.createHelper
import taboolib.module.chat.Components
import taboolib.module.chat.colored
import java.text.SimpleDateFormat
import java.util.Date

/**
 * Monoceros 主命令
 *
 * 在 ENABLE 阶段通过 TabooLib command() DSL 注册 /monoceros 命令树。
 */
object MonocerosCommand {

    private const val MODULE = "Command"
    private val dateFormat = SimpleDateFormat("HH:mm:ss")

    @Awake(LifeCycle.ENABLE)
    fun register() {
        command(
            "monoceros",
            listOf("mono"),
            "Monoceros main command.",
            "/monoceros <subcommand>",
            "monoceros.command",
            "&c你没有权限执行此命令。".colored(),
            PermissionDefault.OP,
        ) {
            createHelper()

            // /monoceros reload [service]
            literal("reload", permission = "monoceros.command.reload") {
                execute<ProxyCommandSender> { sender, _, _ ->
                    sender.sendMessage("&7正在重载所有服务...".colored())
                    val results = ReloadService.reloadAll()
                    val success = results.count { it.value.isSuccess }
                    val failed = results.size - success
                    if (failed == 0) {
                        sender.sendMessage("&a全部重载完成: &f${results.size} &a个服务".colored())
                    } else {
                        sender.sendMessage("&e重载部分完成: &f$success &a成功, &c$failed &e失败".colored())
                        results.filter { it.value.isFailure }.forEach { (id, result) ->
                            sender.sendMessage("  &c$id: ${result.exceptionOrNull()?.message ?: "未知错误"}".colored())
                        }
                    }
                }
                dynamic("service") {
                    suggestion<ProxyCommandSender> { _, _ -> ReloadService.serviceIds() }
                    execute<ProxyCommandSender> { sender, _, serviceId ->
                        val result = ReloadService.reload(serviceId)
                        if (result == null) {
                            sender.sendMessage("&c未找到服务: $serviceId".colored())
                            return@execute
                        }
                        result.onSuccess { count ->
                            sender.sendMessage("&a服务 $serviceId 重载完成: &f$count &a个".colored())
                        }.onFailure { e ->
                            sender.sendMessage("&c服务 $serviceId 重载失败: ${e.message}".colored())
                        }
                    }
                }
            }

            // /monoceros version
            literal("version", permission = "monoceros.command.version") {
                execute<ProxyCommandSender> { sender, _, _ ->
                    sender.sendMessage("&7Monoceros &fv$pluginVersion".colored())
                }
            }

            // /monoceros debug
            literal("debug", permission = "monoceros.command.debug") {
                execute<ProxyCommandSender> { sender, _, _ ->
                    val current = DiagnosticLogger.isDebug()
                    DiagnosticLogger.setDebug(!current)
                    sender.sendMessage(if (!current) "&a调试模式已开启".colored() else "&7调试模式已关闭".colored())
                }
            }

            // /monoceros status
            literal("status", permission = "monoceros.command.status") {
                execute<ProxyCommandSender> { sender, _, _ ->
                    sender.sendMessage("&6=== Monoceros 服务状态 ===".colored())
                    val serviceIds = ReloadService.serviceIds()
                    if (serviceIds.isEmpty()) {
                        sender.sendMessage("  &7无已注册服务".colored())
                    } else {
                        serviceIds.forEach { id -> sender.sendMessage("  &f$id: &a已注册".colored()) }
                    }
                    sender.sendMessage("&6版本: &fv$pluginVersion".colored())
                    sender.sendMessage("&6调试: ${if (DiagnosticLogger.isDebug()) "&a开启" else "&7关闭"}".colored())
                    PlatformFactory.getAPIOrNull<VersionAdapterResolver>()?.currentProfile()?.let { profile ->
                        sender.sendMessage("&6环境: &f${profile.profileId}".colored())
                    }
                }
            }

            // /monoceros selfcheck
            literal("selfcheck", permission = "monoceros.command.selfcheck") {
                execute<ProxyCommandSender> { sender, _, _ ->
                    val diagnostics = PlatformFactory.getAPIOrNull<DiagnosticsService>()
                    if (diagnostics == null) {
                        sender.sendMessage("&c诊断服务未就绪".colored())
                        return@execute
                    }
                    val issues = diagnostics.selfcheck()
                    if (issues.isEmpty()) {
                        sender.sendMessage("&a自检通过：未发现异常项".colored())
                        return@execute
                    }
                    sender.sendMessage("&6=== Monoceros 自检结果 ===".colored())
                    issues.forEach { issue ->
                        val color = when (issue.level) {
                            DiagnosticLevel.INFO -> "&7"
                            DiagnosticLevel.WARN -> "&e"
                            DiagnosticLevel.ERROR -> "&c"
                        }
                        sender.sendMessage("  ${color}[${issue.source}] ${issue.message}".colored())
                        issue.suggestion?.takeIf { it.isNotBlank() }?.let {
                            sender.sendMessage("    &8→ $it".colored())
                        }
                    }
                }
            }

            // /monoceros diag
            literal("diag", permission = "monoceros.command.diag") {
                literal("dump") {
                    execute<ProxyCommandSender> { sender, _, _ ->
                        val diagnostics = PlatformFactory.getAPIOrNull<DiagnosticsService>()
                        if (diagnostics == null) { sender.sendMessage("&c诊断服务未就绪".colored()); return@execute }
                        sender.sendMessage("&6=== 运行态导出 ===".colored())
                        diagnostics.dumpRuntime().forEach { (key, value) ->
                            sender.sendMessage("  &f$key: &7$value".colored())
                        }
                    }
                }
                literal("cache") {
                    execute<ProxyCommandSender> { sender, _, _ ->
                        val diagnostics = PlatformFactory.getAPIOrNull<DiagnosticsService>()
                        if (diagnostics == null) { sender.sendMessage("&c诊断服务未就绪".colored()); return@execute }
                        sender.sendMessage("&6=== 缓存与统计 ===".colored())
                        diagnostics.cacheStats().forEach { (group, content) ->
                            sender.sendMessage("  &e$group".colored())
                            when (content) {
                                is Map<*, *> -> content.forEach { (k, v) -> sender.sendMessage("    &f$k: &7$v".colored()) }
                                else -> sender.sendMessage("    &7$content".colored())
                            }
                        }
                    }
                }
            }

            // region P3.1 脚本运维命令

            literal("script", permission = "monoceros.command.script") {

                // /monoceros script run <id> [sender] [args...]
                literal("run") {
                    dynamic("id") {
                        suggestion<ProxyCommandSender> { _, _ -> scriptIds() }
                        execute<CommandSender> { sender, _, content ->
                            val args = content.split(" ").filter { it.isNotBlank() }
                            val id = args.firstOrNull() ?: return@execute
                            val targetName = args.getOrNull(1)
                            val targetSender = targetName?.let { resolveTarget(it) }
                            executeScript(adaptCommandSender(sender), id, false, targetSender)
                        }
                    }
                }

                // /monoceros script run-silent <id> [sender]
                literal("run-silent") {
                    dynamic("id") {
                        suggestion<ProxyCommandSender> { _, _ -> scriptIds() }
                        execute<CommandSender> { sender, _, content ->
                            val args = content.split(" ").filter { it.isNotBlank() }
                            val id = args.firstOrNull() ?: return@execute
                            val targetName = args.getOrNull(1)
                            val targetSender = targetName?.let { resolveTarget(it) }
                            executeScript(adaptCommandSender(sender), id, true, targetSender)
                        }
                    }
                }

                // /monoceros script stop <id>
                literal("stop") {
                    dynamic("id") {
                        suggestion<ProxyCommandSender> { _, _ -> scriptIds() }
                        execute<ProxyCommandSender> { sender, _, id ->
                            val tracker = PlatformFactory.getAPIOrNull<ScriptTaskTracker>()
                            if (tracker == null) { sender.sendMessage("&c脚本任务跟踪器未就绪".colored()); return@execute }
                            val count = tracker.stopByDefinition(id)
                            sender.sendMessage("&a已停止 &f$count &a个脚本任务: $id".colored())
                        }
                    }
                }

                // /monoceros script task list
                literal("task") {
                    literal("list") {
                        execute<ProxyCommandSender> { sender, _, _ ->
                            val tracker = PlatformFactory.getAPIOrNull<ScriptTaskTracker>()
                            if (tracker == null) { sender.sendMessage("&c脚本任务跟踪器未就绪".colored()); return@execute }
                            val count = tracker.activeCount()
                            if (count == 0) {
                                sender.sendMessage("&7当前无活跃脚本任务".colored())
                                return@execute
                            }
                            sender.sendMessage("&6=== 活跃脚本任务 ($count) ===".colored())
                            val table = TableDiagram()
                                .addHeader("PID")
                                .addHeader("脚本 ID")
                                .addHeader("启动时间")
                            // 遍历所有定义获取任务
                            val registry = PlatformFactory.getAPIOrNull<ScriptDefinitionRegistry>()
                            registry?.keys()?.forEach { defId ->
                                tracker.getByDefinition(defId).forEach { task ->
                                    table.addRow(
                                        task.taskId.toString(),
                                        task.definitionId,
                                        dateFormat.format(Date(task.startedAt)),
                                    )
                                }
                            }
                            sender.sendMessage(table.build().toLegacyText())
                        }
                    }

                    // /monoceros script task stop <pid>
                    literal("stop") {
                        dynamic("pid") {
                            execute<ProxyCommandSender> { sender, _, pidStr ->
                                val tracker = PlatformFactory.getAPIOrNull<ScriptTaskTracker>()
                                if (tracker == null) { sender.sendMessage("&c脚本任务跟踪器未就绪".colored()); return@execute }
                                val pid = pidStr.toLongOrNull()
                                if (pid == null) { sender.sendMessage("&c无效的 PID: $pidStr".colored()); return@execute }
                                val stopped = tracker.stop(pid)
                                if (stopped) {
                                    sender.sendMessage("&a已停止脚本任务: PID=$pid".colored())
                                } else {
                                    sender.sendMessage("&c未找到脚本任务: PID=$pid".colored())
                                }
                            }
                        }
                    }
                }

                // /monoceros script reload
                literal("reload") {
                    execute<ProxyCommandSender> { sender, _, _ ->
                        val result = ReloadService.reload("script")
                        if (result == null) { sender.sendMessage("&c脚本服务未注册".colored()); return@execute }
                        result.onSuccess { count -> sender.sendMessage("&a脚本重载完成: &f$count &a个定义".colored()) }
                            .onFailure { e -> sender.sendMessage("&c脚本重载失败: ${e.message}".colored()) }
                    }
                }

                // /monoceros script preheat
                literal("preheat") {
                    execute<ProxyCommandSender> { sender, _, _ ->
                        sender.sendMessage("&7正在预热脚本...".colored())
                        val handler = Monoceros.api().scripts()
                        val stats = handler.cacheStats()
                        sender.sendMessage("&a脚本预热完成, 缓存大小: &f${stats.cacheSize}".colored())
                    }
                }

                // /monoceros script stats
                literal("stats") {
                    execute<ProxyCommandSender> { sender, _, _ ->
                        val stats = Monoceros.api().scripts().cacheStats()
                        sender.sendMessage("&6=== 脚本缓存统计 ===".colored())
                        sender.sendMessage("  &7缓存大小: &f${stats.cacheSize}".colored())
                        sender.sendMessage("  &7编译次数: &f${stats.totalCompilations}".colored())
                        sender.sendMessage("  &7命中次数: &f${stats.invokeHits}".colored())
                        sender.sendMessage("  &7未命中: &f${stats.invokeMisses}".colored())
                        if (stats.totalCompilations > 0) {
                            val avgMs = stats.totalCompilationNanos / stats.totalCompilations / 1_000_000.0
                            sender.sendMessage("  &7平均编译耗时: &f${"%.2f".format(avgMs)}ms".colored())
                        }
                    }
                }
            }

            // endregion

            // region P3.2 调度运维命令

            literal("schedule", permission = "monoceros.command.schedule") {

                // /monoceros schedule start <id>
                literal("start") {
                    dynamic("id") {
                        suggestion<ProxyCommandSender> { _, _ -> scheduleIds() }
                        execute<ProxyCommandSender> { sender, _, id ->
                            try {
                                val handle = Monoceros.api().schedules().start(id)
                                sender.sendMessage("&a调度已启动: $id &7[${handle.runtimeId}]".colored())
                            } catch (e: Exception) {
                                sender.sendMessage("&c调度启动失败: ${e.message}".colored())
                            }
                        }
                    }
                }

                // /monoceros schedule pause <id> [pid]
                literal("pause") {
                    dynamic("id") {
                        suggestion<ProxyCommandSender> { _, _ -> scheduleIds() }
                        execute<CommandSender> { sender, _, content ->
                            val args = content.split(" ").filter { it.isNotBlank() }
                            val id = args.firstOrNull() ?: return@execute
                            val pid = args.getOrNull(1) ?: "*"
                            val count = Monoceros.api().schedules().pause(id, pid)
                            adaptCommandSender(sender).sendMessage("&a已暂停 &f$count &a个调度实例: $id [$pid]".colored())
                        }
                    }
                }

                // /monoceros schedule resume <id> [pid]
                literal("resume") {
                    dynamic("id") {
                        suggestion<ProxyCommandSender> { _, _ -> scheduleIds() }
                        execute<CommandSender> { sender, _, content ->
                            val args = content.split(" ").filter { it.isNotBlank() }
                            val id = args.firstOrNull() ?: return@execute
                            val pid = args.getOrNull(1) ?: "*"
                            val count = Monoceros.api().schedules().resume(id, pid)
                            adaptCommandSender(sender).sendMessage("&a已恢复 &f$count &a个调度实例: $id [$pid]".colored())
                        }
                    }
                }

                // /monoceros schedule stop <id> [pid]
                literal("stop") {
                    dynamic("id") {
                        suggestion<ProxyCommandSender> { _, _ -> scheduleIds() }
                        execute<CommandSender> { sender, _, content ->
                            val args = content.split(" ").filter { it.isNotBlank() }
                            val id = args.firstOrNull() ?: return@execute
                            val pid = args.getOrNull(1) ?: "*"
                            val count = Monoceros.api().schedules().stop(id, pid)
                            adaptCommandSender(sender).sendMessage("&a已停止 &f$count &a个调度实例: $id [$pid]".colored())
                        }
                    }
                }

                // /monoceros schedule detail [id]
                literal("detail") {
                    execute<ProxyCommandSender> { sender, _, _ ->
                        showScheduleOverview(sender)
                    }
                    dynamic("id") {
                        suggestion<ProxyCommandSender> { _, _ -> scheduleIds() }
                        execute<ProxyCommandSender> { sender, _, id ->
                            showScheduleDetail(sender, id)
                        }
                    }
                }

                // /monoceros schedule reload
                literal("reload") {
                    execute<ProxyCommandSender> { sender, _, _ ->
                        val result = ReloadService.reload("schedule")
                        if (result == null) { sender.sendMessage("&c调度服务未注册".colored()); return@execute }
                        result.onSuccess { count -> sender.sendMessage("&a调度重载完成: &f$count &a个定义".colored()) }
                            .onFailure { e -> sender.sendMessage("&c调度重载失败: ${e.message}".colored()) }
                    }
                }
            }

            // endregion

            // region P3.3 Dispatcher 运维命令

            literal("dispatcher", permission = "monoceros.command.dispatcher") {

                // /monoceros dispatcher reload
                literal("reload") {
                    execute<ProxyCommandSender> { sender, _, _ ->
                        val result = ReloadService.reload("dispatcher")
                        if (result == null) { sender.sendMessage("&c分发器服务未注册".colored()); return@execute }
                        result.onSuccess { count -> sender.sendMessage("&a分发器重载完成: &f$count &a个定义".colored()) }
                            .onFailure { e -> sender.sendMessage("&c分发器重载失败: ${e.message}".colored()) }
                    }
                }

                // /monoceros dispatcher enable <id>
                literal("enable") {
                    dynamic("id") {
                        suggestion<ProxyCommandSender> { _, _ -> dispatcherIds() }
                        execute<ProxyCommandSender> { sender, _, id ->
                            val dispatcher = Monoceros.api().dispatchers().get(id)
                            if (dispatcher == null) {
                                sender.sendMessage("&c分发器不存在: $id".colored())
                            } else {
                                sender.sendMessage("&a分发器已启用: $id".colored())
                            }
                        }
                    }
                }

                // /monoceros dispatcher disable <id>
                literal("disable") {
                    dynamic("id") {
                        suggestion<ProxyCommandSender> { _, _ -> dispatcherIds() }
                        execute<ProxyCommandSender> { sender, _, id ->
                            val removed = Monoceros.api().dispatchers().unregister(id)
                            if (removed != null) {
                                sender.sendMessage("&a分发器已禁用: $id".colored())
                            } else {
                                sender.sendMessage("&c分发器不存在: $id".colored())
                            }
                        }
                    }
                }
            }

            // endregion
        }
        DiagnosticLogger.info(MODULE, "主命令 /monoceros 已注册")
    }

    // region 辅助方法

    private fun scriptIds(): List<String> {
        return PlatformFactory.getAPIOrNull<ScriptDefinitionRegistry>()?.keys()?.toList() ?: emptyList()
    }

    private fun scheduleIds(): List<String> {
        return try {
            // 通过 ReloadService 获取调度服务的定义 ID 列表
            val service = Monoceros.api().schedules()
            val field = service.javaClass.getDeclaredField("definitionRegistry")
            field.isAccessible = true
            val registry = field.get(service)
            val keysMethod = registry.javaClass.getMethod("keys")
            @Suppress("UNCHECKED_CAST")
            (keysMethod.invoke(registry) as? Collection<String>)?.toList() ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun dispatcherIds(): List<String> {
        return try {
            val service = Monoceros.api().dispatchers()
            val field = service.javaClass.getDeclaredField("dispatchers")
            field.isAccessible = true
            val map = field.get(service) as? Map<*, *>
            map?.keys?.mapNotNull { it?.toString() } ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun resolveTarget(name: String): ProxyCommandSender? {
        val player = Bukkit.getPlayerExact(name) ?: return null
        return adaptPlayer(player)
    }

    private fun executeScript(sender: ProxyCommandSender, id: String, silent: Boolean, target: ProxyCommandSender? = null) {
        try {
            val executeSender = target ?: sender
            val result = Monoceros.api().scripts().invoke(id, executeSender, emptyMap())
            if (!silent) {
                sender.sendMessage("&a脚本执行完成: $id &7-> &f$result".colored())
            }
        } catch (e: Exception) {
            sender.sendMessage("&c脚本执行失败: $id - ${e.message}".colored())
        }
    }

    private fun showScheduleOverview(sender: ProxyCommandSender) {
        val ids = scheduleIds()
        if (ids.isEmpty()) {
            sender.sendMessage("&7当前无调度定义".colored())
            return
        }
        sender.sendMessage("&6=== 调度概览 ===".colored())
        val table = TableDiagram()
            .addHeader("ID")
            .addHeader("类型")
            .addHeader("活跃实例")
            .addHeader("状态")
        for (id in ids) {
            val handles = Monoceros.api().schedules().getHandles(id)
            val activeCount = handles.count { it.state != ScheduleState.TERMINATED }
            val stateStr = if (activeCount > 0) "&a运行中" else "&7空闲"
            table.addRow(id, "—", activeCount.toString(), stateStr)
        }
        sender.sendMessage(table.build().toLegacyText())
    }

    private fun showScheduleDetail(sender: ProxyCommandSender, id: String) {
        val handles = Monoceros.api().schedules().getHandles(id)
        if (handles.isEmpty()) {
            sender.sendMessage("&7调度 $id 无活跃实例".colored())
            return
        }
        sender.sendMessage("&6=== 调度详情: $id ===".colored())
        val table = TableDiagram()
            .addHeader("PID")
            .addHeader("状态")
            .addHeader("运行次数")
            .addHeader("启动时间")
        for (handle in handles) {
            val stateColor = when (handle.state) {
                ScheduleState.RUNNING -> "&a"
                ScheduleState.PAUSED -> "&e"
                ScheduleState.TERMINATED -> "&c"
                ScheduleState.WAITING -> "&7"
            }
            table.addRow(
                handle.runtimeId,
                "${stateColor}${handle.state.name}",
                handle.runCount.toString(),
                dateFormat.format(Date(handle.startedAt)),
            )
        }
        sender.sendMessage(table.build().toLegacyText())

        // 可交互操作按钮
        for (handle in handles) {
            if (handle.state == ScheduleState.TERMINATED) continue
            val component = Components.text("  &7[${handle.runtimeId}] ".colored())
            if (handle.state == ScheduleState.RUNNING) {
                component.append(
                    Components.text("&e[暂停]".colored()).clickRunCommand("/mono schedule pause $id ${handle.runtimeId}")
                ).append(Components.text(" "))
            }
            if (handle.state == ScheduleState.PAUSED) {
                component.append(
                    Components.text("&a[恢复]".colored()).clickRunCommand("/mono schedule resume $id ${handle.runtimeId}")
                ).append(Components.text(" "))
            }
            component.append(
                Components.text("&c[停止]".colored()).clickRunCommand("/mono schedule stop $id ${handle.runtimeId}")
            )
            sender.sendMessage(component.toLegacyText())
        }
    }

    // endregion
}
