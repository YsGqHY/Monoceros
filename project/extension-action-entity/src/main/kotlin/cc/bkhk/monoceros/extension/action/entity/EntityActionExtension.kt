package cc.bkhk.monoceros.extension.action.entity

import cc.bkhk.monoceros.Monoceros
import cc.bkhk.monoceros.api.extension.NativeExtension

class EntityActionExtension : NativeExtension() {
    override val id = "action-entity"
    override val name = "实体域动作扩展"
    override val version = "1.0.0"

    override fun onEnable() {
        val service = Monoceros.api().actionWorkflow()
        service.registerNode(EntityDamageNode())
        service.registerNode(EntityHealNode())
        service.registerNode(EntityTeleportNode())
        service.registerNode(EntityPotionAddNode())
        service.registerNode(EntityPotionRemoveNode())
        service.registerNode(EntitySwitchNode())
        service.registerNode(EntityEquipmentSetNode())
        // 向量域动作节点
        service.registerNode(VectorBuildNode())
        service.registerNode(VectorCloneNode())
        service.registerNode(VectorModifyNode())
        service.registerNode(VectorAddNode())
        service.registerNode(VectorSubtractNode())
        service.registerNode(VectorMultiplyNode())
        service.registerNode(VectorDivideNode())
        service.registerNode(VectorNormalizeNode())
        service.registerNode(VectorLengthNode())
        service.registerNode(VectorLengthSquaredNode())
        service.registerNode(VectorDotNode())
        service.registerNode(VectorCrossNode())
        service.registerNode(VectorAngleNode())
        service.registerNode(VectorDistanceNode())
        service.registerNode(VectorDistanceSquaredNode())
        service.registerNode(VectorMidpointNode())
        service.registerNode(VectorRotateXNode())
        service.registerNode(VectorRotateYNode())
        service.registerNode(VectorRotateZNode())
        service.registerNode(VectorRandomNode())
        // 位置域动作节点
        service.registerNode(LocationBuildNode())
        service.registerNode(LocationCloneNode())
        service.registerNode(LocationModifyNode())
        service.registerNode(LocationAddNode())
        service.registerNode(LocationSubtractNode())
        service.registerNode(LocationMultiplyNode())
        service.registerNode(LocationDivideNode())
        service.registerNode(LocationDistanceNode())
        service.registerNode(LocationDistanceSquaredNode())
    }
}
