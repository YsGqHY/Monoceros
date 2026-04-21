package cc.bkhk.monoceros.volatility

import cc.bkhk.monoceros.api.volatility.DynamicWorldBorderState
import cc.bkhk.monoceros.api.volatility.EntityFlag
import cc.bkhk.monoceros.api.volatility.IllusionSessionService
import cc.bkhk.monoceros.api.volatility.VolatileBlockService
import cc.bkhk.monoceros.api.volatility.VolatileEntityMetadataService
import cc.bkhk.monoceros.api.volatility.VolatileWorldBorderService
import cc.bkhk.monoceros.api.volatility.VolatilityService
import cc.bkhk.monoceros.api.volatility.WorldBorderState
import org.bukkit.Location
import org.bukkit.block.data.BlockData
import org.bukkit.entity.Entity
import org.bukkit.entity.Pose
import org.bukkit.entity.Player

/**
 * 挥发能力服务默认实现
 *
 * 组合 block/worldBorder/metadata 三个子服务，委托给 NMS 代理实现。
 */
class DefaultVolatilityService(
    internal val illusionService: DefaultIllusionSessionService,
) : VolatilityService {

    private val blockService = object : VolatileBlockService {
        override fun sendBlockChange(viewer: Player, location: Location, data: BlockData) {
            NmsVolatileBlock.INSTANCE.sendBlockChange(viewer, location, data)
        }

        override fun sendBlockChanges(viewer: Player, changes: List<Pair<Location, BlockData>>) {
            NmsVolatileBlock.INSTANCE.sendBlockChanges(viewer, changes)
        }
    }

    private val worldBorderService = object : VolatileWorldBorderService {
        override fun sendWorldBorder(viewer: Player, state: WorldBorderState) {
            NmsVolatileWorldBorder.INSTANCE.sendWorldBorder(viewer, state)
        }

        override fun sendDynamicWorldBorder(viewer: Player, state: DynamicWorldBorderState) {
            NmsVolatileWorldBorder.INSTANCE.sendDynamicWorldBorder(viewer, state)
        }
    }

    private val metadataService = object : VolatileEntityMetadataService {
        override fun setFlag(viewer: Player, entity: Entity, flag: EntityFlag, value: Boolean) {
            NmsVolatileEntityMetadata.INSTANCE.setFlag(viewer, entity, flag, value)
        }

        override fun setPose(viewer: Player, entity: Entity, pose: Pose) {
            NmsVolatileEntityMetadata.INSTANCE.setPose(viewer, entity, pose)
        }

        override fun updateHealth(viewer: Player, entity: Entity, health: Float) {
            NmsVolatileEntityMetadata.INSTANCE.updateHealth(viewer, entity, health)
        }

        override fun mount(viewer: Player, entity: Entity) {
            NmsVolatileEntityMetadata.INSTANCE.mount(viewer, entity)
        }
    }

    override fun blocks(): VolatileBlockService = blockService

    override fun worldBorder(): VolatileWorldBorderService = worldBorderService

    override fun metadata(): VolatileEntityMetadataService = metadataService

    override fun illusions(): IllusionSessionService = illusionService
}
