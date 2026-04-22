package cc.bkhk.monoceros.schedule

import cc.bkhk.monoceros.Monoceros
import cc.bkhk.monoceros.api.registry.Registry
import cc.bkhk.monoceros.api.schedule.ScheduleContext
import cc.bkhk.monoceros.api.schedule.ScheduleDefinition
import cc.bkhk.monoceros.api.schedule.ScheduleHandler
import cc.bkhk.monoceros.api.schedule.ScheduleRoute
import cc.bkhk.monoceros.api.schedule.SenderSelectorDefinition
import cc.bkhk.monoceros.api.schedule.SenderSelectorType
import cc.bkhk.monoceros.impl.util.DiagnosticLogger
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.function.adaptCommandSender
import taboolib.common.platform.function.adaptPlayer

/**
 * 调度执行器
 *
 * 负责将调度任务路由到脚本/工作流/handler 执行。
 */
class ScheduleExecutor(
    private val handlerRegistry: Registry<ScheduleHandler>,
) {

    private data class ResolvedScheduleSender(
        val sender: ProxyCommandSender?,
        val player: Player? = null,
    )

    private companion object {
        const val MODULE = "ScheduleExecutor"
    }

    /**
     * 执行一次调度任务
     */
    fun execute(definition: ScheduleDefinition, handle: DefaultScheduleHandle, runtimeVariables: Map<String, Any?>) {
        val baseVariables = LinkedHashMap<String, Any?>()
        baseVariables.putAll(definition.variables)
        baseVariables.putAll(runtimeVariables)
        baseVariables["scheduleId"] = definition.id
        baseVariables["runtimeId"] = handle.runtimeId
        baseVariables["runCount"] = handle.runCount
        baseVariables["startedAt"] = handle.startedAt
        baseVariables["elapsedMs"] = handle.elapsedMs()
        baseVariables["triggerType"] = definition.type.name

        val senders = resolveSenders(definition.senderSelectors, baseVariables)
        for (resolved in senders) {
            val variables = LinkedHashMap(baseVariables)
            // sender/player 同时注入到 variables 中供 ActionWorkflow/Handler 路由使用，
            // Fluxon 脚本路由中这两个变量由运行时自动注入，variables 中的同名项会被静默跳过
            variables["sender"] = resolved.sender
            resolved.player?.let { variables["player"] = it }

            when (val route = definition.route) {
                is ScheduleRoute.Script -> {
                    try {
                        Monoceros.api().scripts().invoke(route.scriptId, resolved.sender, variables)
                    } catch (e: Exception) {
                        DiagnosticLogger.warn(MODULE, "调度脚本执行失败: ${route.scriptId}", e)
                    }
                }
                is ScheduleRoute.ActionWorkflow -> {
                    try {
                        Monoceros.api().actionWorkflow().execute(route.workflowId, resolved.sender, variables)
                    } catch (e: Exception) {
                        DiagnosticLogger.warn(MODULE, "调度工作流执行失败: ${route.workflowId}", e)
                    }
                }
                is ScheduleRoute.Handler -> {
                    val handler = handlerRegistry.get(route.handlerId)
                    if (handler == null) {
                        DiagnosticLogger.warn(MODULE, "ScheduleHandler 未注册: ${route.handlerId}")
                    } else {
                        try {
                            val context = ScheduleContext(
                                definitionId = definition.id,
                                runtimeId = handle.runtimeId,
                                runCount = handle.runCount,
                                startedAt = handle.startedAt,
                                sender = resolved.sender,
                                variables = variables,
                            )
                            handler.execute(context)
                        } catch (e: Exception) {
                            DiagnosticLogger.warn(MODULE, "ScheduleHandler 执行失败: ${route.handlerId}", e)
                        }
                    }
                }
            }
        }
    }

    /**
     * 执行生命周期脚本（onStart/onStop/onPause/onResume）
     */
    fun executeLifecycleScript(scriptId: String?, handle: DefaultScheduleHandle) {
        if (scriptId == null) return
        try {
            Monoceros.api().scripts().invoke(
                scriptId,
                null,
                mapOf(
                    "scheduleId" to handle.definitionId,
                    "runtimeId" to handle.runtimeId,
                ),
            )
        } catch (e: Exception) {
            DiagnosticLogger.warn(MODULE, "生命周期脚本执行失败: $scriptId", e)
        }
    }

    private fun resolveSenders(
        selectors: List<SenderSelectorDefinition>,
        variables: Map<String, Any?>,
    ): List<ResolvedScheduleSender> {
        if (selectors.isEmpty()) {
            return listOf(ResolvedScheduleSender(null, null))
        }
        val resolved = LinkedHashMap<String, ResolvedScheduleSender>()
        selectors.forEach { selector ->
            resolveSelector(selector, variables).forEach { target ->
                val key = target.player?.uniqueId?.toString() ?: target.sender?.name ?: "system"
                resolved[key] = target
            }
        }
        return if (resolved.isEmpty()) listOf(ResolvedScheduleSender(null, null)) else resolved.values.toList()
    }

    private fun resolveSelector(
        selector: SenderSelectorDefinition,
        variables: Map<String, Any?>,
    ): List<ResolvedScheduleSender> {
        return when (selector.type) {
            SenderSelectorType.CONSOLE -> listOf(ResolvedScheduleSender(adaptCommandSender(Bukkit.getConsoleSender()), null))
            SenderSelectorType.ONLINE_PLAYER -> Bukkit.getOnlinePlayers().map { ResolvedScheduleSender(adaptPlayer(it), it) }
            SenderSelectorType.PLAYER -> resolvePlayer(selector.value, variables)?.let { listOf(ResolvedScheduleSender(adaptPlayer(it), it)) } ?: emptyList()
            SenderSelectorType.WORLD -> resolveWorldPlayers(selector, variables).map { ResolvedScheduleSender(adaptPlayer(it), it) }
            SenderSelectorType.RANGE -> resolveRangePlayers(selector, variables).map { ResolvedScheduleSender(adaptPlayer(it), it) }
            SenderSelectorType.AREA -> resolveAreaPlayers(selector, variables).map { ResolvedScheduleSender(adaptPlayer(it), it) }
        }
    }

    private fun resolvePlayer(value: String?, variables: Map<String, Any?>): Player? {
        val candidate = value?.let { variables[it] ?: it } ?: variables["player"]
        return when (candidate) {
            is Player -> candidate
            is String -> Bukkit.getPlayerExact(candidate)
            else -> null
        }
    }

    private fun resolveWorldPlayers(selector: SenderSelectorDefinition, variables: Map<String, Any?>): List<Player> {
        val worldName = selector.world ?: selector.value?.let { variables[it] ?: it }?.toString()
        val world = worldName?.let(Bukkit::getWorld) ?: (variables["player"] as? Player)?.world ?: (variables["location"] as? Location)?.world
        return world?.players ?: emptyList()
    }

    private fun resolveRangePlayers(selector: SenderSelectorDefinition, variables: Map<String, Any?>): List<Player> {
        val center = resolveOrigin(selector, variables) ?: return emptyList()
        val radius = selector.radius ?: return emptyList()
        val square = radius * radius
        return center.world?.players?.filter { it.location.distanceSquared(center) <= square } ?: emptyList()
    }

    private fun resolveAreaPlayers(selector: SenderSelectorDefinition, variables: Map<String, Any?>): List<Player> {
        val world = (selector.world?.let(Bukkit::getWorld)
            ?: (variables["player"] as? Player)?.world
            ?: (variables["location"] as? Location)?.world)
            ?: return emptyList()
        val minX = selector.minX ?: return emptyList()
        val minY = selector.minY ?: Double.NEGATIVE_INFINITY
        val minZ = selector.minZ ?: return emptyList()
        val maxX = selector.maxX ?: return emptyList()
        val maxY = selector.maxY ?: Double.POSITIVE_INFINITY
        val maxZ = selector.maxZ ?: return emptyList()
        return world.players.filter { player ->
            val location = player.location
            location.x in minX..maxX && location.y in minY..maxY && location.z in minZ..maxZ
        }
    }

    private fun resolveOrigin(selector: SenderSelectorDefinition, variables: Map<String, Any?>): Location? {
        val origin = selector.origin?.let { variables[it] } ?: variables["location"] ?: (variables["player"] as? Player)?.location
        return when (origin) {
            is Location -> origin
            is Player -> origin.location
            else -> null
        }
    }
}
