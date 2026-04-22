package cc.bkhk.monoceros.impl

import cc.bkhk.monoceros.api.MonocerosAPI
import cc.bkhk.monoceros.api.command.CommandService
import cc.bkhk.monoceros.api.dispatcher.DispatcherService
import cc.bkhk.monoceros.api.schedule.ScheduleService
import cc.bkhk.monoceros.api.script.MonocerosScriptHandler
import cc.bkhk.monoceros.api.volatility.VolatilityService
import cc.bkhk.monoceros.api.wireshark.PacketService
import cc.bkhk.monoceros.api.workflow.ActionWorkflowService
import cc.bkhk.monoceros.api.workflow.PropertyWorkflowService
import cc.bkhk.monoceros.impl.noop.NoopActionWorkflowService
import cc.bkhk.monoceros.impl.noop.NoopCommandService
import cc.bkhk.monoceros.impl.noop.NoopDispatcherService
import cc.bkhk.monoceros.impl.noop.NoopPacketService
import cc.bkhk.monoceros.impl.noop.NoopPropertyWorkflowService
import cc.bkhk.monoceros.impl.noop.NoopScheduleService
import cc.bkhk.monoceros.impl.noop.NoopScriptHandler
import cc.bkhk.monoceros.impl.noop.NoopVolatilityService
import taboolib.common.platform.PlatformFactory

/**
 * MonocerosAPI 默认实现
 *
 * 每次调用时从 [PlatformFactory] 动态查找真实实现，
 * 查不到则回退到伴生对象中的静态 Noop 实例。
 * 不把 Noop 注册进 PlatformFactory，避免覆盖真实实现。
 */
class DefaultMonocerosAPI : MonocerosAPI {

    companion object {
        private val NOOP_SCRIPT = NoopScriptHandler()
        private val NOOP_DISPATCHER = NoopDispatcherService()
        private val NOOP_SCHEDULE = NoopScheduleService()
        private val NOOP_COMMAND = NoopCommandService()
        private val NOOP_PACKET = NoopPacketService()
        private val NOOP_VOLATILITY = NoopVolatilityService()
        private val NOOP_ACTION_WORKFLOW = NoopActionWorkflowService()
        private val NOOP_PROPERTY_WORKFLOW = NoopPropertyWorkflowService()
    }

    override fun scripts(): MonocerosScriptHandler =
        PlatformFactory.getAPIOrNull<MonocerosScriptHandler>() ?: NOOP_SCRIPT

    override fun dispatchers(): DispatcherService =
        PlatformFactory.getAPIOrNull<DispatcherService>() ?: NOOP_DISPATCHER

    override fun schedules(): ScheduleService =
        PlatformFactory.getAPIOrNull<ScheduleService>() ?: NOOP_SCHEDULE

    override fun commands(): CommandService =
        PlatformFactory.getAPIOrNull<CommandService>() ?: NOOP_COMMAND

    override fun packets(): PacketService =
        PlatformFactory.getAPIOrNull<PacketService>() ?: NOOP_PACKET

    override fun volatility(): VolatilityService =
        PlatformFactory.getAPIOrNull<VolatilityService>() ?: NOOP_VOLATILITY

    override fun actionWorkflow(): ActionWorkflowService =
        PlatformFactory.getAPIOrNull<ActionWorkflowService>() ?: NOOP_ACTION_WORKFLOW

    override fun propertyWorkflow(): PropertyWorkflowService =
        PlatformFactory.getAPIOrNull<PropertyWorkflowService>() ?: NOOP_PROPERTY_WORKFLOW
}
