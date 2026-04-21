package cc.bkhk.monoceros.volatility

import cc.bkhk.monoceros.api.volatility.EntityFlag
import org.bukkit.entity.Entity
import org.bukkit.entity.Pose
import org.bukkit.entity.Player
import taboolib.module.nms.nmsProxy

/**
 * 实体元数据 NMS 代理接口
 *
 * 通过 TabooLib nmsProxy 自动选择版本实现。
 * 实现类命名约定：NmsVolatileEntityMetadataImpl
 */
abstract class NmsVolatileEntityMetadata {

    /** 设置实体标志位（发光、隐身等） */
    abstract fun setFlag(viewer: Player, entity: Entity, flag: EntityFlag, value: Boolean)

    /** 设置实体姿态 */
    abstract fun setPose(viewer: Player, entity: Entity, pose: Pose)

    /** 更新实体血量显示 */
    abstract fun updateHealth(viewer: Player, entity: Entity, health: Float)

    /** 骑乘挂载 */
    abstract fun mount(viewer: Player, entity: Entity)

    companion object {

        val INSTANCE by lazy {
            try {
                nmsProxy<NmsVolatileEntityMetadata>()
            } catch (_: Exception) {
                // 降级：空实现
                NoopVolatileEntityMetadata()
            }
        }
    }
}

/**
 * 空实现降级
 *
 * 实体元数据操作需要 NMS，降级时不做任何操作。
 */
class NoopVolatileEntityMetadata : NmsVolatileEntityMetadata() {

    override fun setFlag(viewer: Player, entity: Entity, flag: EntityFlag, value: Boolean) {}

    override fun setPose(viewer: Player, entity: Entity, pose: Pose) {}

    override fun updateHealth(viewer: Player, entity: Entity, health: Float) {}

    override fun mount(viewer: Player, entity: Entity) {}
}
