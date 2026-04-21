package cc.bkhk.monoceros.impl.noop

import cc.bkhk.monoceros.api.command.CommandDefinition
import cc.bkhk.monoceros.api.command.CommandService
import cc.bkhk.monoceros.api.dispatcher.DispatcherDefinition
import cc.bkhk.monoceros.api.dispatcher.DispatcherService
import cc.bkhk.monoceros.api.dispatcher.EventDispatcher
import cc.bkhk.monoceros.api.schedule.ScheduleDefinition
import cc.bkhk.monoceros.api.schedule.ScheduleHandle
import cc.bkhk.monoceros.api.schedule.ScheduleService
import cc.bkhk.monoceros.api.schedule.ScheduleState
import cc.bkhk.monoceros.api.script.MonocerosScriptHandler
import cc.bkhk.monoceros.api.script.MonocerosScriptSource
import cc.bkhk.monoceros.api.script.MonocerosScriptType
import cc.bkhk.monoceros.api.script.ScriptCacheStats
import cc.bkhk.monoceros.api.script.ScriptInvokeRequest
import cc.bkhk.monoceros.api.volatility.DynamicWorldBorderState
import cc.bkhk.monoceros.api.volatility.EntityFlag
import cc.bkhk.monoceros.api.volatility.IllusionKey
import cc.bkhk.monoceros.api.volatility.IllusionSessionService
import cc.bkhk.monoceros.api.volatility.VolatileBlockService
import cc.bkhk.monoceros.api.volatility.VolatileEntityMetadataService
import cc.bkhk.monoceros.api.volatility.VolatileWorldBorderService
import cc.bkhk.monoceros.api.volatility.VolatilityService
import cc.bkhk.monoceros.api.volatility.WorldBorderState
import cc.bkhk.monoceros.api.wireshark.PacketService
import cc.bkhk.monoceros.api.wireshark.PacketSession
import cc.bkhk.monoceros.api.wireshark.PacketTapDefinition
import cc.bkhk.monoceros.api.wireshark.PacketTraceRecord
import cc.bkhk.monoceros.api.workflow.ActionNode
import cc.bkhk.monoceros.api.workflow.ActionWorkflowService
import cc.bkhk.monoceros.api.workflow.PropertyAccessor
import cc.bkhk.monoceros.api.workflow.PropertyRequest
import cc.bkhk.monoceros.api.workflow.PropertyWorkflowService
import org.bukkit.Location
import org.bukkit.block.data.BlockData
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import taboolib.common.platform.ProxyCommandSender
import java.util.UUID
import kotlin.reflect.KClass

/**
 * P1 阶段占位实现
 *
 * 各子服务在对应功能模块实现后，会替换这些占位实例。
 * 占位实现不做任何实际操作，仅保证 API 可正常获取而不抛异常。
 */

// region Script

class NoopScriptHandler : MonocerosScriptHandler {
    override fun invoke(request: ScriptInvokeRequest): Any? = null
    override fun invoke(definitionId: String, sender: ProxyCommandSender?, variables: Map<String, Any?>): Any? = null
    override fun preheat(definitionId: String) {}
    override fun preheat(source: MonocerosScriptSource, definitionId: String) {}
    override fun registerScriptType(scriptType: MonocerosScriptType): MonocerosScriptType = scriptType
    override fun unregisterScriptType(typeId: String): MonocerosScriptType? = null
    override fun getScriptType(typeId: String): MonocerosScriptType? = null
    override fun invalidate(definitionId: String) {}
    override fun invalidateByPrefix(prefix: String) {}
    override fun cacheStats(): ScriptCacheStats = ScriptCacheStats()
}

// endregion

// region Dispatcher

class NoopDispatcherService : DispatcherService {
    override fun register(definition: DispatcherDefinition): EventDispatcher =
        error("DispatcherService 尚未初始化，无法注册分发器: ${definition.id}")
    override fun unregister(id: String): EventDispatcher? = null
    override fun get(id: String): EventDispatcher? = null
    override fun reloadAll(): Int = 0
}

// endregion

// region Schedule

class NoopScheduleService : ScheduleService {
    override fun register(definition: ScheduleDefinition): ScheduleDefinition = definition
    override fun unregister(id: String): ScheduleDefinition? = null
    override fun start(id: String, variables: Map<String, Any?>): ScheduleHandle =
        error("ScheduleService 尚未初始化，无法启动调度: $id")
    override fun pause(id: String, runtimeId: String): Int = 0
    override fun resume(id: String, runtimeId: String): Int = 0
    override fun stop(id: String, runtimeId: String): Int = 0
    override fun getHandles(id: String): Collection<ScheduleHandle> = emptyList()
}

// endregion

// region Command

class NoopCommandService : CommandService {
    override fun register(definition: CommandDefinition) {}
    override fun unregister(id: String) {}
    override fun reloadAll(): Int = 0
}

// endregion

// region Packet

class NoopPacketService : PacketService {
    override fun register(definition: PacketTapDefinition) {}
    override fun unregister(id: String) {}
    override fun openSession(player: Player): PacketSession =
        error("PacketService 尚未初始化，无法打开会话")
    override fun closeSession(playerId: UUID) {}
    override fun getSession(playerId: UUID): PacketSession? = null
}

// endregion

// region Volatility

class NoopVolatilityService : VolatilityService {
    private val blockService = object : VolatileBlockService {
        override fun sendBlockChange(viewer: Player, location: Location, data: BlockData) {}
        override fun sendBlockChanges(viewer: Player, changes: List<Pair<Location, BlockData>>) {}
    }
    private val worldBorderService = object : VolatileWorldBorderService {
        override fun sendWorldBorder(viewer: Player, state: WorldBorderState) {}
        override fun sendDynamicWorldBorder(viewer: Player, state: DynamicWorldBorderState) {}
    }
    private val metadataService = object : VolatileEntityMetadataService {
        override fun setFlag(viewer: Player, entity: Entity, flag: EntityFlag, value: Boolean) {}
        override fun setPose(viewer: Player, entity: Entity, pose: org.bukkit.entity.Pose) {}
        override fun updateHealth(viewer: Player, entity: Entity, health: Float) {}
        override fun mount(viewer: Player, entity: Entity) {}
    }
    private val illusionService = object : IllusionSessionService {
        override fun putBlock(key: IllusionKey, location: Location, data: BlockData) {}
        override fun removeBlock(key: IllusionKey, location: Location) {}
        override fun applyWorldBorder(key: IllusionKey, state: WorldBorderState) {}
        override fun clear(key: IllusionKey) {}
        override fun clearViewer(viewerId: java.util.UUID) {}
    }

    override fun blocks(): VolatileBlockService = blockService
    override fun worldBorder(): VolatileWorldBorderService = worldBorderService
    override fun metadata(): VolatileEntityMetadataService = metadataService
    override fun illusions(): IllusionSessionService = illusionService
}

// endregion

// region Workflow

class NoopActionWorkflowService : ActionWorkflowService {
    override fun registerNode(node: ActionNode): ActionNode = node
    override fun unregisterNode(type: String): ActionNode? = null
    override fun execute(id: String, sender: ProxyCommandSender?, variables: Map<String, Any?>): Any? = null
    override fun reloadAll(): Int = 0
}

class NoopPropertyWorkflowService : PropertyWorkflowService {
    override fun register(accessor: PropertyAccessor<*>): PropertyAccessor<*> = accessor
    override fun unregister(type: KClass<*>): PropertyAccessor<*>? = null
    override fun read(request: PropertyRequest): Any? = null
    override fun write(request: PropertyRequest) {}
}

// endregion
