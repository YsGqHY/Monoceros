package cc.bkhk.monoceros.api.volatility

import org.bukkit.Location
import org.bukkit.World
import org.bukkit.block.data.BlockData
import org.bukkit.entity.Entity
import org.bukkit.entity.Player

/**
 * 实体标志位
 */
enum class EntityFlag {
    ON_FIRE,
    SNEAKING,
    SPRINTING,
    INVISIBLE,
    GLOWING,
}

/**
 * 世界边界静态状态
 */
data class WorldBorderState(
    val world: World,
    val size: Double? = null,
    val center: Location? = null,
    val warningTime: Int? = null,
    val warningDistance: Int? = null,
    val damageBuffer: Double? = null,
    val damageAmount: Double? = null,
)

/**
 * 世界边界动态状态
 */
data class DynamicWorldBorderState(
    val world: World,
    val oldSize: Double,
    val newSize: Double,
    val speedMs: Long,
    val center: Location? = null,
    val warningTime: Int? = null,
    val warningDistance: Int? = null,
    val damageBuffer: Double? = null,
    val damageAmount: Double? = null,
)

/**
 * 伪方块服务
 */
interface VolatileBlockService {
    fun sendBlockChange(viewer: Player, location: Location, data: BlockData)
    fun sendBlockChanges(viewer: Player, changes: List<Pair<Location, BlockData>>)
}

/**
 * 伪世界边界服务
 */
interface VolatileWorldBorderService {
    fun sendWorldBorder(viewer: Player, state: WorldBorderState)
    fun sendDynamicWorldBorder(viewer: Player, state: DynamicWorldBorderState)
}

/**
 * 实体元数据服务
 */
interface VolatileEntityMetadataService {
    fun setFlag(viewer: Player, entity: Entity, flag: EntityFlag, value: Boolean)
    fun setPose(viewer: Player, entity: Entity, pose: org.bukkit.entity.Pose)
    fun updateHealth(viewer: Player, entity: Entity, health: Float)
    fun mount(viewer: Player, entity: Entity)
}

/**
 * 挥发能力统一服务
 */
interface VolatilityService {
    fun blocks(): VolatileBlockService
    fun worldBorder(): VolatileWorldBorderService
    fun metadata(): VolatileEntityMetadataService
    fun illusions(): IllusionSessionService
}
