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

/**
 * MonocerosAPI 默认实现
 *
 * 纯组合，不做额外逻辑。各子服务由对应功能模块在 LOAD 阶段注册。
 */
class DefaultMonocerosAPI(
    private val scriptHandler: MonocerosScriptHandler,
    private val dispatcherService: DispatcherService,
    private val scheduleService: ScheduleService,
    private val commandService: CommandService,
    private val packetService: PacketService,
    private val volatilityService: VolatilityService,
    private val actionWorkflowService: ActionWorkflowService,
    private val propertyWorkflowService: PropertyWorkflowService,
) : MonocerosAPI {

    override fun scripts(): MonocerosScriptHandler = scriptHandler

    override fun dispatchers(): DispatcherService = dispatcherService

    override fun schedules(): ScheduleService = scheduleService

    override fun commands(): CommandService = commandService

    override fun packets(): PacketService = packetService

    override fun volatility(): VolatilityService = volatilityService

    override fun actionWorkflow(): ActionWorkflowService = actionWorkflowService

    override fun propertyWorkflow(): PropertyWorkflowService = propertyWorkflowService
}
