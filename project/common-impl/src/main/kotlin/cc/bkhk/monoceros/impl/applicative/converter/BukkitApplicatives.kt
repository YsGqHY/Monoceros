package cc.bkhk.monoceros.impl.applicative.converter

import cc.bkhk.monoceros.impl.applicative.AbstractApplicative
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.util.Vector

/** Color 转换器 */
object ColorApplicative : AbstractApplicative<Color>(Color::class.java) {
    override fun convertOrThrow(instance: Any): Color = when (instance) {
        is Color -> instance
        is String -> {
            // 支持 "r,g,b" 或 "#RRGGBB" 格式
            val trimmed = instance.trim()
            if (trimmed.startsWith("#") && trimmed.length == 7) {
                val rgb = trimmed.substring(1).toIntOrNull(16)
                    ?: throw IllegalArgumentException("无效的颜色格式: $instance")
                Color.fromRGB(rgb)
            } else {
                val parts = trimmed.split(",").map { it.trim().toIntOrNull() }
                if (parts.size == 3 && parts.all { it != null }) {
                    Color.fromRGB(parts[0]!!, parts[1]!!, parts[2]!!)
                } else {
                    throw IllegalArgumentException("无效的颜色格式: $instance (期望 'r,g,b' 或 '#RRGGBB')")
                }
            }
        }
        is Number -> Color.fromRGB(instance.toInt())
        else -> throw IllegalArgumentException("无法将 ${instance.javaClass.simpleName} 转换为 Color")
    }
}

/** Vector 转换器 */
object VectorApplicative : AbstractApplicative<Vector>(Vector::class.java) {
    override val aliases = arrayOf("vec")
    override fun convertOrThrow(instance: Any): Vector = when (instance) {
        is Vector -> instance
        is Location -> instance.toVector()
        is String -> {
            val parts = instance.split(",").map { it.trim().toDoubleOrNull() }
            if (parts.size == 3 && parts.all { it != null }) {
                Vector(parts[0]!!, parts[1]!!, parts[2]!!)
            } else {
                throw IllegalArgumentException("无效的向量格式: $instance (期望 'x,y,z')")
            }
        }
        else -> throw IllegalArgumentException("无法将 ${instance.javaClass.simpleName} 转换为 Vector")
    }
}

/** Location 转换器 */
object LocationApplicative : AbstractApplicative<Location>(Location::class.java) {
    override val aliases = arrayOf("loc")
    override fun convertOrThrow(instance: Any): Location = when (instance) {
        is Location -> instance
        is Entity -> instance.location
        is Vector -> Location(null, instance.x, instance.y, instance.z)
        is String -> parseLocationString(instance)
        else -> throw IllegalArgumentException("无法将 ${instance.javaClass.simpleName} 转换为 Location")
    }

    private fun parseLocationString(str: String): Location {
        val parts = str.split(",").map { it.trim() }
        return when (parts.size) {
            // x,y,z
            3 -> {
                val (x, y, z) = parts.map { it.toDoubleOrNull() ?: throw IllegalArgumentException("无效的坐标值: $it") }
                Location(null, x, y, z)
            }
            // world,x,y,z
            4 -> {
                val world = Bukkit.getWorld(parts[0])
                val (x, y, z) = parts.subList(1, 4).map { it.toDoubleOrNull() ?: throw IllegalArgumentException("无效的坐标值: $it") }
                Location(world, x, y, z)
            }
            // world,x,y,z,yaw,pitch
            6 -> {
                val world = Bukkit.getWorld(parts[0])
                val (x, y, z) = parts.subList(1, 4).map { it.toDoubleOrNull() ?: throw IllegalArgumentException("无效的坐标值: $it") }
                val yaw = parts[4].toFloatOrNull() ?: 0f
                val pitch = parts[5].toFloatOrNull() ?: 0f
                Location(world, x, y, z, yaw, pitch)
            }
            else -> throw IllegalArgumentException("无效的位置格式: $str (期望 'x,y,z' 或 'world,x,y,z' 或 'world,x,y,z,yaw,pitch')")
        }
    }
}

/** ItemStack 转换器 */
object ItemStackApplicative : AbstractApplicative<ItemStack>(ItemStack::class.java) {
    override val aliases = arrayOf("item")
    override fun convertOrThrow(instance: Any): ItemStack = when (instance) {
        is ItemStack -> instance
        else -> throw IllegalArgumentException("无法将 ${instance.javaClass.simpleName} 转换为 ItemStack")
    }
}

/** Entity 转换器 */
object EntityApplicative : AbstractApplicative<Entity>(Entity::class.java) {
    override fun convertOrThrow(instance: Any): Entity = when (instance) {
        is Entity -> instance
        else -> throw IllegalArgumentException("无法将 ${instance.javaClass.simpleName} 转换为 Entity")
    }
}

/** Player 转换器 */
object PlayerApplicative : AbstractApplicative<Player>(Player::class.java) {
    override fun convertOrThrow(instance: Any): Player = when (instance) {
        is Player -> instance
        is String -> Bukkit.getPlayerExact(instance)
            ?: throw IllegalArgumentException("玩家不存在或不在线: $instance")
        else -> throw IllegalArgumentException("无法将 ${instance.javaClass.simpleName} 转换为 Player")
    }
}

/** Inventory 转换器 */
object InventoryApplicative : AbstractApplicative<Inventory>(Inventory::class.java) {
    override fun convertOrThrow(instance: Any): Inventory = when (instance) {
        is Inventory -> instance
        is Player -> instance.inventory
        else -> throw IllegalArgumentException("无法将 ${instance.javaClass.simpleName} 转换为 Inventory")
    }
}
