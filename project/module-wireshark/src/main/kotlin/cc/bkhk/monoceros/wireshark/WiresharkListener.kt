package cc.bkhk.monoceros.wireshark

import cc.bkhk.monoceros.Monoceros
import cc.bkhk.monoceros.api.wireshark.PacketContext
import cc.bkhk.monoceros.api.wireshark.PacketDirection
import cc.bkhk.monoceros.api.wireshark.PacketRewriteSpec
import cc.bkhk.monoceros.api.wireshark.PacketRoute
import cc.bkhk.monoceros.api.wireshark.PacketTapDefinition
import cc.bkhk.monoceros.api.wireshark.PacketTraceRecord
import cc.bkhk.monoceros.impl.util.DiagnosticLogger
import org.bukkit.entity.Player
import taboolib.common.event.InternalEventBus
import taboolib.common.event.InternalListener
import taboolib.common.platform.function.submit
import taboolib.module.nms.PacketReceiveEvent
import taboolib.module.nms.PacketSendEvent
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Packet 事件监听器
 *
 * 动态注册 TabooLib 的 PacketReceiveEvent / PacketSendEvent 监听，
 * 仅在有 tap 定义需要处理时才注册。
 *
 * 监听器本身不会取消任何数据包事件，只有当 tap 配置了 intercept: true
 * 且全局 allowIntercept 开关开启时，才会将事件标记为已取消。
 */
object WiresharkListener {

    private const val MODULE = "Wireshark"

    /** 是否已注册监听器 */
    private val registered = AtomicBoolean(false)

    /** 动态注册的监听器引用，用于注销 */
    private var receiveListener: InternalListener? = null
    private var sendListener: InternalListener? = null

    /** 延迟获取 PacketService 实例 */
    private val service: DefaultPacketService?
        get() = try {
            Monoceros.api().packets() as? DefaultPacketService
        } catch (_: Exception) {
            null
        }

    /**
     * 动态注册 packet 事件监听器
     *
     * 仅在首次调用时注册，后续调用无效。
     * 由 [WiresharkServiceLoader] 在确认有 tap 需要处理时调用。
     */
    fun register() {
        if (!registered.compareAndSet(false, true)) return

        receiveListener = InternalEventBus.listen(PacketReceiveEvent::class.java, 0, true) { event ->
            try {
                val svc = service ?: return@listen
                if (svc.taps.isEmpty()) return@listen
                if (processPacket(svc, PacketDirection.RECEIVE, event.player, event.packet, event.packet.name)) {
                    event.isCancelled = true
                }
            } catch (e: Throwable) {
                DiagnosticLogger.warn(MODULE, "PacketReceiveEvent 处理异常", e)
            }
        }

        sendListener = InternalEventBus.listen(PacketSendEvent::class.java, 0, true) { event ->
            try {
                val svc = service ?: return@listen
                if (svc.taps.isEmpty()) return@listen
                if (processPacket(svc, PacketDirection.SEND, event.player, event.packet, event.packet.name)) {
                    event.isCancelled = true
                }
            } catch (e: Throwable) {
                DiagnosticLogger.warn(MODULE, "PacketSendEvent 处理异常", e)
            }
        }

        DiagnosticLogger.info(MODULE, "Packet 事件监听器已动态注册")
    }

    /**
     * 注销 packet 事件监听器
     *
     * 由 [WiresharkServiceLoader] 在 DISABLE 阶段调用。
     */
    fun unregister() {
        if (!registered.compareAndSet(true, false)) return

        receiveListener?.cancel()
        receiveListener = null
        sendListener?.cancel()
        sendListener = null

        DiagnosticLogger.info(MODULE, "Packet 事件监听器已注销")
    }

    /**
     * 统一处理流程
     *
     * @return true 表示需要取消该数据包（仅当 tap 配置了 intercept 且全局 allowIntercept 开启时）
     */
    private fun processPacket(
        svc: DefaultPacketService,
        direction: PacketDirection,
        player: Player,
        packet: Any,
        packetName: String,
    ): Boolean {
        val session = svc.sessions[player.uniqueId]
        var shouldCancel = false

        for ((tapId, tap) in svc.taps) {
            try {
                if (processTap(svc, tap, session, direction, player, packet, packetName)) {
                    shouldCancel = true
                }
            } catch (e: Exception) {
                DiagnosticLogger.warn(MODULE, "tap 处理异常: $tapId", e)
            }
        }

        return shouldCancel
    }

    /**
     * 处理单个 tap
     *
     * @return true 表示该 tap 要求取消数据包
     */
    private fun processTap(
        svc: DefaultPacketService,
        tap: PacketTapDefinition,
        session: DefaultPacketSession?,
        direction: PacketDirection,
        player: Player,
        packet: Any,
        packetName: String,
    ): Boolean {
        // 1. 方向检查
        if (direction !in tap.direction) return false

        // 2. 会话 tap 启用检查（若有会话则检查，无会话则跳过该 tap）
        if (session == null || tap.id !in session.enabledTapIds) return false

        // 3. 匹配器检查
        tap.matcher?.let { spec ->
            if (!matchPacket(spec.type, spec.value, packetName, packet)) return false
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
        context.variables["traceId"] = "pkt-${tap.id}-${UUID.randomUUID()}"

        // 4. 过滤器
        for (filterSpec in tap.filters) {
            if (filterPacket(filterSpec.type, filterSpec.value, context)) return false
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
            context.variables["packetPackage"] = packet.javaClass.`package`?.name
            context.variables["packetToString"] = packet.toString()
        }

        // 7. rewrite（直接操作 NMS packet 对象，不涉及 Bukkit API，保留在当前线程）
        tap.rewrite?.let { rewrite ->
            if (svc.allowRewrite) {
                applyRewrite(rewrite, packet, context)
            }
        }

        // 8. 路由（涉及脚本/工作流执行，可能修改 Bukkit 状态，切回主线程）
        tap.route?.let { route ->
            submit(async = false) {
                try {
                    executeRoute(svc, route, context, player)
                } catch (e: Exception) {
                    DiagnosticLogger.warn(MODULE, "tap 路由执行异常: ${tap.id}", e)
                }
            }
        }

        // 9. 仅当 tap 明确配置了 intercept 且全局开关允许时才取消数据包
        return tap.intercept && svc.allowIntercept
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

    private fun applyRewrite(rewrite: PacketRewriteSpec, packet: Any, context: PacketContext) {
        when (rewrite.type.lowercase()) {
            "field-set", "set-field" -> {
                val fieldName = rewrite.config["field"]?.toString() ?: return
                val value = rewrite.config["value"]
                val field = runCatching {
                    packet.javaClass.getDeclaredField(fieldName).apply { isAccessible = true }
                }.getOrNull() ?: return
                runCatching { field.set(packet, value) }
                    .onSuccess {
                        context.variables["rewriteField"] = fieldName
                        context.variables["rewriteValue"] = value
                    }
                    .onFailure { ex ->
                        DiagnosticLogger.warn(MODULE, "packet 字段覆写失败: $fieldName", ex)
                    }
            }
        }
    }
}
