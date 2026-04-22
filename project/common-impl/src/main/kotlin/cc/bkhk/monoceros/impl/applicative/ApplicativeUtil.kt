package cc.bkhk.monoceros.impl.applicative

import cc.bkhk.monoceros.impl.applicative.converter.*
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.util.Vector

/**
 * Applicative 便捷扩展函数集
 *
 * 为 [Any?] 提供类型安全的转换方法，转换失败返回默认值。
 */

fun Any?.applicativeBoolean(def: Boolean = false): Boolean =
    if (this == null) def else BooleanApplicative.convertOrNull(this) ?: def

fun Any?.applicativeInt(def: Int = 0): Int =
    if (this == null) def else IntApplicative.convertOrNull(this) ?: def

fun Any?.applicativeLong(def: Long = 0L): Long =
    if (this == null) def else LongApplicative.convertOrNull(this) ?: def

fun Any?.applicativeFloat(def: Float = 0.0f): Float =
    if (this == null) def else FloatApplicative.convertOrNull(this) ?: def

fun Any?.applicativeDouble(def: Double = 0.0): Double =
    if (this == null) def else DoubleApplicative.convertOrNull(this) ?: def

fun Any?.applicativeString(def: String = "null"): String =
    if (this == null) def else StringApplicative.convertOrNull(this) ?: def

fun Any?.applicativeColor(def: Color = Color.WHITE): Color =
    if (this == null) def else ColorApplicative.convertOrNull(this) ?: def

fun Any?.applicativeVector(def: Vector = Vector()): Vector =
    if (this == null) def else VectorApplicative.convertOrNull(this) ?: def

fun Any?.applicativeLocation(def: Location? = null): Location? =
    if (this == null) def else LocationApplicative.convertOrNull(this) ?: def

fun Any?.applicativeItemStack(def: ItemStack? = null): ItemStack? =
    if (this == null) def else ItemStackApplicative.convertOrNull(this) ?: def

fun Any?.applicativeEntity(def: Entity? = null): Entity? =
    if (this == null) def else EntityApplicative.convertOrNull(this) ?: def

fun Any?.applicativePlayer(def: Player? = null): Player? =
    if (this == null) def else PlayerApplicative.convertOrNull(this) ?: def

fun Any?.applicativeInventory(def: Inventory? = null): Inventory? =
    if (this == null) def else InventoryApplicative.convertOrNull(this) ?: def

@Suppress("UNCHECKED_CAST")
fun Any?.applicativeList(def: List<*> = emptyList<Any>()): List<*> =
    if (this == null) def else ListApplicative.convertOrNull(this) ?: def

fun Any?.applicativeIntList(def: List<Int> = emptyList()): List<Int> =
    applicativeList().mapNotNull { IntApplicative.convertOrNull(it ?: return@mapNotNull null) }.ifEmpty { def }

fun Any?.applicativeStringList(def: List<String> = emptyList()): List<String> =
    applicativeList().mapNotNull { StringApplicative.convertOrNull(it ?: return@mapNotNull null) }.ifEmpty { def }

@Suppress("UNCHECKED_CAST")
fun Any?.applicativeMap(def: Map<*, *> = emptyMap<Any, Any>()): Map<*, *> =
    if (this == null) def else MapApplicative.convertOrNull(this) ?: def

/**
 * 泛型便捷转换
 */
object Applicatives {
    inline fun <reified T : Any> convert(instance: Any?): T? {
        val applicative = DefaultApplicativeRegistry.get(T::class.java) ?: return null
        return applicative.convertOrNull(instance)
    }

    inline fun <reified T : Any> convertOrThrow(instance: Any?): T {
        val applicative = DefaultApplicativeRegistry.get(T::class.java)
            ?: throw IllegalStateException("未注册 ${T::class.java.simpleName} 的转换器")
        return applicative.convert(instance)
    }
}
