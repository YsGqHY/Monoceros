package cc.bkhk.monoceros.extension.action.target

import cc.bkhk.monoceros.Monoceros
import cc.bkhk.monoceros.api.extension.NativeExtension

/**
 * 目标域动作扩展注册器
 */
class TargetActionExtension : NativeExtension() {
    override val id = "action-target"
    override val name = "目标域动作扩展"
    override val version = "1.0.0"

    override fun onEnable() {
        val service = Monoceros.api().actionWorkflow()
        service.registerNode(TargetSelfNode())
        service.registerNode(TargetWorldNode())
        service.registerNode(TargetServerNode())
        service.registerNode(TargetRadiusNode())
        service.registerNode(TargetBoxNode())
        service.registerNode(TargetFilterNode())
        service.registerNode(TargetLineOfSightNode())
    }
}
