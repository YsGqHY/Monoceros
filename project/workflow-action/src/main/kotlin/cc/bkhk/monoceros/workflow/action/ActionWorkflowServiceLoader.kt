package cc.bkhk.monoceros.workflow.action

import cc.bkhk.monoceros.api.workflow.ActionWorkflowService
import cc.bkhk.monoceros.impl.util.DiagnosticLogger
import cc.bkhk.monoceros.workflow.action.node.BranchActionNode
import cc.bkhk.monoceros.workflow.action.node.LogActionNode
import cc.bkhk.monoceros.workflow.action.node.ScriptActionNode
import cc.bkhk.monoceros.workflow.action.node.SetActionNode
import cc.bkhk.monoceros.workflow.action.node.WaitActionNode
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.PlatformFactory

/**
 * 动作工作流系统生命周期注册器
 */
object ActionWorkflowServiceLoader {

    private const val MODULE = "ActionWorkflow"

    private lateinit var service: DefaultActionWorkflowService

    @Awake(LifeCycle.LOAD)
    private fun onLoad() {
        service = DefaultActionWorkflowService()

        // 注册内建节点
        service.registerNode(ScriptActionNode())
        service.registerNode(SetActionNode())
        service.registerNode(LogActionNode())
        service.registerNode(WaitActionNode())
        service.registerNode(BranchActionNode())

        PlatformFactory.registerAPI<ActionWorkflowService>(service)
        DiagnosticLogger.info(MODULE, "动作工作流服务已注册到 PlatformFactory")
    }

    @Awake(LifeCycle.ENABLE)
    private fun onEnable() {
        val count = service.reloadAll()
        DiagnosticLogger.info(MODULE, "动作工作流定义加载完成: $count 个")
        service.startWatcher(service.createWatcherCallback())
    }

    @Awake(LifeCycle.DISABLE)
    private fun onDisable() {
        service.stopWatcher()
        DiagnosticLogger.info(MODULE, "动作工作流系统已清理")
    }
}
