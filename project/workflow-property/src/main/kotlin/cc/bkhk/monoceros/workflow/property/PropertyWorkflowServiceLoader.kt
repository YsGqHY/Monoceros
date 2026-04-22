package cc.bkhk.monoceros.workflow.property

import cc.bkhk.monoceros.api.workflow.PropertyWorkflowService
import cc.bkhk.monoceros.impl.util.DiagnosticLogger
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.PlatformFactory

/**
 * 属性工作流系统生命周期注册器
 */
object PropertyWorkflowServiceLoader {

    private const val MODULE = "PropertyWorkflow"

    private lateinit var service: DefaultPropertyWorkflowService

    @Awake(LifeCycle.LOAD)
    fun onLoad() {
        service = DefaultPropertyWorkflowService()
        PlatformFactory.registerAPI<PropertyWorkflowService>(service)
        DiagnosticLogger.info(MODULE, "属性工作流服务已注册到 PlatformFactory")
    }

    @Awake(LifeCycle.DISABLE)
    fun onDisable() {
        DiagnosticLogger.info(MODULE, "属性工作流系统已清理")
    }
}
