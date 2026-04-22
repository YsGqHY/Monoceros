package cc.bkhk.monoceros.dispatcher.probe

import cc.bkhk.monoceros.api.dispatcher.StateProbe
import cc.bkhk.monoceros.dispatcher.event.PlayerArmorChangeEvent
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.event.Event
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import java.util.EnumMap
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 玩家护甲栏变化探针
 */
class PlayerArmorChangeStateProbe : StateProbe {

    override val id: String = "player-armor-change"

    private val cache = ConcurrentHashMap<UUID, EnumMap<EquipmentSlot, ItemStack>>()
    private val armorSlots = listOf(
        EquipmentSlot.HEAD,
        EquipmentSlot.CHEST,
        EquipmentSlot.LEGS,
        EquipmentSlot.FEET,
    )

    override fun poll(): Collection<Event> {
        val events = mutableListOf<Event>()
        Bukkit.getOnlinePlayers().forEach { player ->
            val slots = cache.computeIfAbsent(player.uniqueId) { EnumMap(EquipmentSlot::class.java) }
            armorSlots.forEach { slot ->
                val oldItem = (slots[slot] ?: ItemStack(Material.AIR)).clone()
                val newItem = (player.equipment?.getItem(slot) ?: ItemStack(Material.AIR)).clone()
                if (oldItem.isSimilar(newItem) && oldItem.amount == newItem.amount) {
                    return@forEach
                }
                events += PlayerArmorChangeEvent(player, slot, oldItem, newItem)
            }
        }
        return events
    }

    fun updateSnapshot(event: PlayerArmorChangeEvent) {
        val slots = cache.computeIfAbsent(event.player.uniqueId) { EnumMap(EquipmentSlot::class.java) }
        slots[event.slot] = event.newItem.clone()
    }

    fun rollback(event: PlayerArmorChangeEvent) {
        val slots = cache.computeIfAbsent(event.player.uniqueId) { EnumMap(EquipmentSlot::class.java) }
        slots[event.slot] = event.oldItem.clone()
    }
}
