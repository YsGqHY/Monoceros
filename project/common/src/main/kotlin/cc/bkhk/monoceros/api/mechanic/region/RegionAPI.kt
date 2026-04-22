package cc.bkhk.monoceros.api.mechanic.region

import org.bukkit.Location
import org.bukkit.World
import java.util.UUID

/** 区域形状 */
sealed class RegionShape {
    abstract fun contains(location: Location): Boolean

    data class Cuboid(val world: String, val minX: Double, val minY: Double, val minZ: Double, val maxX: Double, val maxY: Double, val maxZ: Double) : RegionShape() {
        override fun contains(location: Location): Boolean {
            if (location.world?.name != world) return false
            return location.x in minX..maxX && location.y in minY..maxY && location.z in minZ..maxZ
        }
    }

    data class Sphere(val world: String, val centerX: Double, val centerY: Double, val centerZ: Double, val radius: Double) : RegionShape() {
        override fun contains(location: Location): Boolean {
            if (location.world?.name != world) return false
            val dx = location.x - centerX; val dy = location.y - centerY; val dz = location.z - centerZ
            return dx * dx + dy * dy + dz * dz <= radius * radius
        }
    }
}

/** 区域定义 */
data class RegionDefinition(
    val id: String,
    val shape: RegionShape,
    val onEnterScript: String? = null,
    val onLeaveScript: String? = null,
    val onStayScript: String? = null,
    val stayIntervalTicks: Long = 20,
    val effects: List<RegionEffect> = emptyList(),
    val variables: Map<String, Any?> = emptyMap(),
)

/** 区域效果 */
data class RegionEffect(
    val type: String,
    val amplifier: Int = 0,
    val ambient: Boolean = true,
)

/** 区域服务 */
interface RegionService {
    fun register(definition: RegionDefinition)
    fun unregister(id: String): RegionDefinition?
    fun get(id: String): RegionDefinition?
    fun all(): Collection<RegionDefinition>
    fun getRegionsAt(location: Location): List<RegionDefinition>
    fun isInRegion(playerId: UUID, regionId: String): Boolean
    fun getPlayersInRegion(regionId: String): Set<UUID>
}
