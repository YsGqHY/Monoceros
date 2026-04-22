package cc.bkhk.monoceros.extension.property.common

import cc.bkhk.monoceros.Monoceros
import cc.bkhk.monoceros.api.extension.NativeExtension
import cc.bkhk.monoceros.api.workflow.PropertyAccessor
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.OfflinePlayer
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
        "lengthSquared" -> try { target.lengthSquared() } catch (_: Exception) { target.toVector().lengthSquared() }
        "distance" -> null // 需要上下文中的第二个 Location
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
        "lengthSquared" -> target.lengthSquared()
        "isZero" -> target.x == 0.0 && target.y == 0.0 && target.z == 0.0
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
        "weather" -> if (target.hasStorm()) "STORM" else if (target.isThundering) "THUNDER" else "CLEAR"
        "difficulty" -> target.difficulty.name
        "playerCount" -> target.players.size
        else -> null
    }

    override fun write(target: World, key: String, value: Any?, context: Map<String, Any?>) {
        when (key) {
            "time" -> target.time = (value as? Number)?.toLong() ?: error("World.time 需要 Number 类型")
            "fullTime" -> target.fullTime = (value as? Number)?.toLong() ?: error("World.fullTime 需要 Number 类型")
            else -> error("World 属性 '$key' 不可写")
        }
    }
}

/** ItemStack 属性访问器 */
object ItemStackPropertyAccessor : PropertyAccessor<ItemStack> {
    override val targetType: KClass<ItemStack> = ItemStack::class

    @Suppress("DEPRECATION")
    override fun read(target: ItemStack, key: String, context: Map<String, Any?>): Any? = when (key) {
        "type" -> target.type
        "amount" -> target.amount
        "meta" -> target.itemMeta
        "maxStackSize" -> target.maxStackSize
        "durability" -> target.durability
        "data" -> target.data?.data
        "hasItemMeta" -> target.hasItemMeta()
        "enchantments" -> target.enchantments
        "itemFlags" -> target.itemMeta?.itemFlags?.map { it.name }
        else -> null
    }

    override fun write(target: ItemStack, key: String, value: Any?, context: Map<String, Any?>) {
        when (key) {
            "amount" -> target.amount = (value as? Number)?.toInt() ?: error("ItemStack.amount 需要 Number 类型")
            "type" -> {
                val material = when (value) {
                    is Material -> value
                    else -> Material.matchMaterial(value?.toString() ?: error("ItemStack.type 不能为 null"))
                        ?: error("无法匹配材质: $value")
                }
                target.type = material
            }
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
        "data" -> target.blockData.asString
        "isEmpty" -> target.isEmpty
        "isLiquid" -> target.isLiquid
        "isPassable" -> try { target.isPassable } catch (_: Exception) { null }
        else -> null
    }

    override fun write(target: Block, key: String, value: Any?, context: Map<String, Any?>) {
        when (key) {
            "type" -> {
                val material = when (value) {
                    is Material -> value
                    else -> Material.matchMaterial(value?.toString() ?: error("Block.type 不能为 null"))
                        ?: error("无法匹配材质: $value")
                }
                target.type = material
            }
            else -> error("Block 属性 '$key' 不可写")
        }
    }
}

/** Color 属性访问器 */
object ColorPropertyAccessor : PropertyAccessor<Color> {
    override val targetType: KClass<Color> = Color::class

    override fun read(target: Color, key: String, context: Map<String, Any?>): Any? = when (key) {
        "red" -> target.red
        "green" -> target.green
        "blue" -> target.blue
        "rgb" -> target.asRGB()
        "asRGB" -> "#%06x".format(target.asRGB())
        "asHex" -> "#%06x".format(target.asRGB())
        else -> null
    }
}

/** OfflinePlayer 属性访问器 */
object OfflinePlayerPropertyAccessor : PropertyAccessor<OfflinePlayer> {
    override val targetType: KClass<OfflinePlayer> = OfflinePlayer::class

    @Suppress("DEPRECATION")
    override fun read(target: OfflinePlayer, key: String, context: Map<String, Any?>): Any? = when (key) {
        "name" -> target.name
        "uniqueId" -> target.uniqueId
        "isOnline" -> target.isOnline
        "player" -> target.player
        "firstPlayed" -> target.firstPlayed
        "lastPlayed" -> target.lastPlayed
        "bedSpawnLocation" -> target.bedSpawnLocation
        "isBanned" -> target.isBanned
        "isWhitelisted" -> target.isWhitelisted
        "hasPlayedBefore" -> target.hasPlayedBefore()
        else -> null
    }
}

class CommonPropertyExtension : NativeExtension() {
    override val id = "property-common"
    override val name = "通用属性域扩展"
    override val version = "1.0.0"

    override fun onEnable() {
        val service = Monoceros.api().propertyWorkflow()
        // 通用属性访问器
        service.register(LocationPropertyAccessor)
        service.register(VectorPropertyAccessor)
        service.register(WorldPropertyAccessor)
        service.register(ItemStackPropertyAccessor)
        service.register(BlockPropertyAccessor)
        service.register(ColorPropertyAccessor)
        service.register(OfflinePlayerPropertyAccessor)
        // 事件属性访问器
        service.register(EventPropertyAccessor)
        service.register(PlayerMoveEventPropertyAccessor)
        service.register(PlayerJoinEventPropertyAccessor)
        service.register(PlayerQuitEventPropertyAccessor)
        service.register(PlayerInteractEventPropertyAccessor)
        service.register(PlayerItemHeldEventPropertyAccessor)
        service.register(PlayerRespawnEventPropertyAccessor)
        service.register(PlayerCommandPreprocessEventPropertyAccessor)
        service.register(AsyncPlayerChatEventPropertyAccessor)
        service.register(EntityDamageEventPropertyAccessor)
        service.register(EntityDeathEventPropertyAccessor)
        service.register(EntityShootBowEventPropertyAccessor)
        // Packet 事件属性访问器（依赖 BukkitNMS）
        try {
            service.register(PacketSendEventPropertyAccessor)
            service.register(PacketReceiveEventPropertyAccessor)
        } catch (_: NoClassDefFoundError) {
            // BukkitNMS 不可用时跳过 Packet 属性访问器
        }
    }
}
