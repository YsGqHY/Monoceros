package cc.bkhk.monoceros.wireshark

import cc.bkhk.monoceros.Monoceros
import cc.bkhk.monoceros.api.wireshark.PacketContext
import cc.bkhk.monoceros.api.wireshark.PacketDirection
import cc.bkhk.monoceros.api.wireshark.PacketRoute
import cc.bkhk.monoceros.api.wireshark.PacketTapDefinition
import cc.bkhk.monoceros.api.wireshark.PacketTraceRecord
import cc.bkhk.monoceros.impl.util.DiagnosticLogger
import org.bukkit.entity.Player
import taboolib.common.platform.event.SubscribeEvent
import taboolib.module.nms.PacketReceiveEvent
import taboolib.module.nms.PacketSendEvent

/**
 * Packet 事件监听器
 *
 * 监听 TabooLib 的 PacketReceiveEvent / PacketSendEvent，
 * 按 tap 定义执行过滤、匹配、追踪、路由、拦截、覆写流程。
 */
object WiresharkListener {

    private const val MODULE = "Wireshark"

    /** 延迟获取 PacketService 实例 */
    private val service: DefaultPacketService?
        get() = try {
            Monoceros.api().packets() as? DefaultPacketService
        } catch (_: Exception) {
            null
        }

    @SubscribeEvent
    fun onPacketReceive(event: PacketReceiveEvent) {
        val svc = service ?: return
        val player = event.player
        processPacket(svc, PacketDirection.RECEIVE, player, event.packet, event.packet.name) {
            event.isCancelled = true
        }
    }

    @SubscribeEvent
    fun onPacketSend(event: PacketSendEvent) {
        val svc = service ?: return
        val player = event.player
        processPacket(svc, PacketDirection.SEND, player, event.packet, event.packet.name) {
            event.isCancelled = true
        }
    }

    /**
     * 统一处理流程
     */
    private fun processPacket(
        svc: DefaultPacketService,
        direction: PacketDirection,
        player: Player,
        packet: Any,
        packetName: String,
        cancelAction: () -> Unit,
    ) {
        val session = svc.sessions[player.uniqueId]

        for ((tapId, tap) in svc.taps) {
            try {
                processTap(svc, tap, session, direction, player, packet, packetName, cancelAction)
            } catch (e: Exception) {
                DiagnosticLogger.warn(MODULE, "tap 处理异常: $tapId", e)
            }
        }
    }

    /**
     * 处理单个 tap
     */
    private fun processTap(
        svc: DefaultPacketService,
        tap: PacketTapDefinition,
        session: DefaultPacketSession?,
        direction: PacketDirection,
        player: Player,
        packet: Any,
        packetName: String,
        cancelAction: () -> Unit,
    ) {
        // 1. 方向检查
        if (direction !in tap.direction) return

        // 2. 会话 tap 启用检查（若有会话则检查，无会话则跳过该 tap）
        if (session == null || tap.id !in session.enabledTapIds) return

        // 3. 匹配器检查
        tap.matcher?.let { spec ->
            if (!matchPacket(spec.type, spec.value, packetName, packet)) return
        }

        // 构造上下文
        val context = PacketContext(
            tapId = tap.id,
            direction = direction,
            player = player,
            packet = packet,
            timestamp = System.currentTimeMillis(),
        )
        context.variables["packetName"] = packetName
        context.variables["player"] = player

        // 4. 过滤器
        for (filterSpec in tap.filters) {
            if (filterPacket(filterSpec.type, filterSpec.value, context)) return
        }

        // 5. 追踪
        if (tap.tracking) {
            session.addTrace(
                PacketTraceRecord(
                    tapId = tap.id,
                    direction = direction,
                    packetName = packetName,
                    timestamp = context.timestamp,
                    variables = context.variables.toMap(),
                )
            )
        }

        // 6. 解析（补充可读变量）
        if (tap.parse) {
            context.variables["packetClass"] = packet.javaClass.simpleName
        }

        // 7. 路由
        tap.route?.let { route ->
            try {
                executeRoute(svc, route, context, player)
            } catch (e: Exception) {
                DiagnosticLogger.warn(MODULE, "tap 路由执行异常: ${tap.id}", e)
            }
        }

        // 8. 拦截
        if (tap.intercept && svc.allowIntercept) {
            context.cancelled = true
        }

        // 9. 应用结果
        if (context.cancelled) {
            cancelAction()
        }
    }

    /** 执行路由 */
    private fun executeRoute(
        svc: DefaultPacketService,
        route: PacketRoute,
        context: PacketContext,
        player: Player,
    ) {
        when (route) {
            is PacketRoute.Script -> {
                Monoceros.api().scripts().invoke(
                    route.scriptId,
                    taboolib.common.platform.function.adaptPlayer(player),
                    context.variables,
                )
            }
            is PacketRoute.ActionWorkflow -> {
                Monoceros.api().actionWorkflow().execute(
                    route.workflowId,
                    taboolib.common.platform.function.adaptPlayer(player),
                    context.variables,
                )
            }
            is PacketRoute.Handler -> {
                val handler = svc.handlerRegistry.get(route.handlerId)
                if (handler == null) {
                    DiagnosticLogger.warn(MODULE, "PacketHandler 未注册: ${route.handlerId}")
                } else {
                    handler.handle(context)
                }
            }
        }
    }

    /** 匹配数据包 */
    private fun matchPacket(type: String, value: String, packetName: String, packet: Any): Boolean {
        return when (type.lowercase()) {
            "packet-class", "class" -> packetName == value || packet.javaClass.simpleName == value
            "packet-name", "name" -> packetName == value
            "regex" -> packetName.matches(Regex(value))
            else -> false
        }
    }

    /** 过滤数据包，返回 true 表示应过滤掉 */
    private fun filterPacket(type: String, value: String, context: PacketContext): Boolean {
        return when (type.lowercase()) {
            "exclude-name" -> context.variables["packetName"] == value
            "exclude-regex" -> (context.variables["packetName"] as? String)?.matches(Regex(value)) == true
            else -> false
        }
    }
}
