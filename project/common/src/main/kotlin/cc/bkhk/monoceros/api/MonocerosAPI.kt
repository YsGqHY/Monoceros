package cc.bkhk.monoceros.api

import cc.bkhk.monoceros.api.command.CommandService
import cc.bkhk.monoceros.api.dispatcher.DispatcherService
import cc.bkhk.monoceros.api.schedule.ScheduleService
import cc.bkhk.monoceros.api.script.MonocerosScriptHandler
import cc.bkhk.monoceros.api.volatility.VolatilityService
import cc.bkhk.monoceros.api.wireshark.PacketService
import cc.bkhk.monoceros.api.workflow.ActionWorkflowService
import cc.bkhk.monoceros.api.workflow.PropertyWorkflowService

/**
 * Monoceros 主门面接口
 *
 * 每个方法返回对应子系统的服务接口，具体实现由 common-impl 或各功能模块在 LOAD 阶段注册。
 */
interface MonocerosAPI {

    fun scripts(): MonocerosScriptHandler

    fun dispatchers(): DispatcherService

    fun schedules(): ScheduleService

    fun commands(): CommandService

    fun packets(): PacketService

    fun volatility(): VolatilityService

    fun actionWorkflow(): ActionWorkflowService

    fun propertyWorkflow(): PropertyWorkflowService
}
