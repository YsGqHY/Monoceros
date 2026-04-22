package cc.bkhk.monoceros.workflow.action

import cc.bkhk.monoceros.api.service.ReloadableService
import cc.bkhk.monoceros.api.workflow.ActionWorkflowService
import cc.bkhk.monoceros.impl.service.ReloadService
import cc.bkhk.monoceros.impl.util.DiagnosticLogger
import cc.bkhk.monoceros.workflow.action.node.BranchActionNode
import cc.bkhk.monoceros.workflow.action.node.CoerceActionNode
import cc.bkhk.monoceros.workflow.action.node.DispatchActionNode
import cc.bkhk.monoceros.workflow.action.node.IfElseActionNode
import cc.bkhk.monoceros.workflow.action.node.InputActionNode
import cc.bkhk.monoceros.workflow.action.node.LogActionNode
import cc.bkhk.monoceros.workflow.action.node.LoopActionNode
import cc.bkhk.monoceros.workflow.action.node.MathActionNode
import cc.bkhk.monoceros.workflow.action.node.RegexActionNode
import cc.bkhk.monoceros.workflow.action.node.ScriptActionNode
import cc.bkhk.monoceros.workflow.action.node.SetActionNode
import cc.bkhk.monoceros.workflow.action.node.SoundActionNode
import cc.bkhk.monoceros.workflow.action.node.TellrawActionNode
import cc.bkhk.monoceros.workflow.action.node.TryCatchActionNode
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
    fun onLoad() {
        service = DefaultActionWorkflowService()

        // 注册内建节点
        service.registerNode(ScriptActionNode())
        service.registerNode(SetActionNode())
        service.registerNode(LogActionNode())
        service.registerNode(WaitActionNode())
        service.registerNode(BranchActionNode())
        service.registerNode(LoopActionNode())
        service.registerNode(SoundActionNode())
        service.registerNode(TellrawActionNode())
        service.registerNode(RegexActionNode())
        service.registerNode(TryCatchActionNode())
        service.registerNode(InputActionNode())
        service.registerNode(IfElseActionNode())
        service.registerNode(MathActionNode())
        service.registerNode(CoerceActionNode())
        service.registerNode(DispatchActionNode())

        PlatformFactory.registerAPI<ActionWorkflowService>(service)
        DiagnosticLogger.info(MODULE, "动作工作流服务已注册, 内建节点: 15 个")

        // 注册到统一重载服务
        ReloadService.register(object : ReloadableService {
            override val serviceId = "workflow-action"
            override val priority = 30
            override fun reload(): Int = service.reloadAll()
        })
    }

    @Awake(LifeCycle.ENABLE)
    fun onEnable() {
        val count = service.reloadAll()
        DiagnosticLogger.info(MODULE, "动作工作流定义加载完成: $count 个")
        service.startWatcher(service.createWatcherCallback())
    }

    @Awake(LifeCycle.DISABLE)
    fun onDisable() {
        service.stopWatcher()
        DiagnosticLogger.info(MODULE, "动作工作流系统已清理")
    }
}
