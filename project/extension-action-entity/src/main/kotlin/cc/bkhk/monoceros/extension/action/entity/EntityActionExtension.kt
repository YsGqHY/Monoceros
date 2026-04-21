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
    }
}
