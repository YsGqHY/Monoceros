package cc.bkhk.monoceros.extension.property.common

import cc.bkhk.monoceros.Monoceros
import cc.bkhk.monoceros.api.extension.NativeExtension
import cc.bkhk.monoceros.api.workflow.PropertyAccessor
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.inventory.ItemStack
import org.bukkit.util.Vector
import kotlin.reflect.KClass

/** Location 属性访问器 */
object LocationPropertyAccessor : PropertyAccessor<Location> {
    override val targetType: KClass<Location> = Location::class

    override fun read(target: Location, key: String, context: Map<String, Any?>): Any? = when (key) {
        "world" -> target.world
        "x" -> target.x
        "y" -> target.y
        "z" -> target.z
        "blockX" -> target.blockX
        "blockY" -> target.blockY
        "blockZ" -> target.blockZ
        "yaw" -> target.yaw
        "pitch" -> target.pitch
        "block" -> target.block
        "chunk" -> target.chunk
        "direction" -> target.direction
        else -> null
    }

    override fun write(target: Location, key: String, value: Any?, context: Map<String, Any?>) {
        when (key) {
            "x" -> target.x = (value as? Number)?.toDouble() ?: error("Location.x 需要 Number 类型")
            "y" -> target.y = (value as? Number)?.toDouble() ?: error("Location.y 需要 Number 类型")
            "z" -> target.z = (value as? Number)?.toDouble() ?: error("Location.z 需要 Number 类型")
            "yaw" -> target.yaw = (value as? Number)?.toFloat() ?: error("Location.yaw 需要 Number 类型")
            "pitch" -> target.pitch = (value as? Number)?.toFloat() ?: error("Location.pitch 需要 Number 类型")
            "direction" -> if (value is Vector) target.direction = value else error("Location.direction 需要 Vector 类型")
            else -> error("Location 属性 '$key' 不可写")
        }
    }
}

/** Vector 属性访问器 */
object VectorPropertyAccessor : PropertyAccessor<Vector> {
    override val targetType: KClass<Vector> = Vector::class

    override fun read(target: Vector, key: String, context: Map<String, Any?>): Any? = when (key) {
        "x" -> target.x
        "y" -> target.y
        "z" -> target.z
        "length" -> target.length()
        "normalized" -> target.clone().normalize()
        else -> null
    }

    override fun write(target: Vector, key: String, value: Any?, context: Map<String, Any?>) {
        when (key) {
            "x" -> target.x = (value as? Number)?.toDouble() ?: error("Vector.x 需要 Number 类型")
            "y" -> target.y = (value as? Number)?.toDouble() ?: error("Vector.y 需要 Number 类型")
            "z" -> target.z = (value as? Number)?.toDouble() ?: error("Vector.z 需要 Number 类型")
            else -> error("Vector 属性 '$key' 不可写")
        }
    }
}

/** World 属性访问器 */
object WorldPropertyAccessor : PropertyAccessor<World> {
    override val targetType: KClass<World> = World::class

    override fun read(target: World, key: String, context: Map<String, Any?>): Any? = when (key) {
        "name" -> target.name
        "uid" -> target.uid
        "players" -> target.players
        "entities" -> target.entities
        "spawnLocation" -> target.spawnLocation
        "time" -> target.time
        "fullTime" -> target.fullTime
        "seed" -> target.seed
        else -> null
    }
}

/** ItemStack 属性访问器 */
object ItemStackPropertyAccessor : PropertyAccessor<ItemStack> {
    override val targetType: KClass<ItemStack> = ItemStack::class

    override fun read(target: ItemStack, key: String, context: Map<String, Any?>): Any? = when (key) {
        "type" -> target.type
        "amount" -> target.amount
        "meta" -> target.itemMeta
        "maxStackSize" -> target.maxStackSize
        "durability" -> target.durability
        else -> null
    }

    override fun write(target: ItemStack, key: String, value: Any?, context: Map<String, Any?>) {
        when (key) {
            "amount" -> target.amount = (value as? Number)?.toInt() ?: error("ItemStack.amount 需要 Number 类型")
            else -> error("ItemStack 属性 '$key' 不可写")
        }
    }
}

/** Block 属性访问器 */
object BlockPropertyAccessor : PropertyAccessor<Block> {
    override val targetType: KClass<Block> = Block::class

    override fun read(target: Block, key: String, context: Map<String, Any?>): Any? = when (key) {
        "type" -> target.type
        "location" -> target.location
        "world" -> target.world
        "x" -> target.x
        "y" -> target.y
        "z" -> target.z
        "blockData" -> target.blockData
        "lightLevel" -> target.lightLevel
        "biome" -> target.biome
        else -> null
    }
}

class CommonPropertyExtension : NativeExtension() {
    override val id = "property-common"
    override val name = "通用属性域扩展"
    override val version = "1.0.0"

    override fun onEnable() {
        val service = Monoceros.api().propertyWorkflow()
        service.register(LocationPropertyAccessor)
        service.register(VectorPropertyAccessor)
        service.register(WorldPropertyAccessor)
        service.register(ItemStackPropertyAccessor)
        service.register(BlockPropertyAccessor)
    }
}
