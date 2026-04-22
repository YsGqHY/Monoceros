package cc.bkhk.monoceros.dispatcher.event

import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.HandlerList
import org.bukkit.event.player.PlayerEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack

/**
 * 玩家护甲栏变化虚拟事件
 */
class PlayerArmorChangeEvent(
    player: Player,
    val slot: EquipmentSlot,
    val oldItem: ItemStack,
    var newItem: ItemStack,
) : PlayerEvent(player), Cancellable {

    private var cancelled: Boolean = false

    override fun getHandlers(): HandlerList = HANDLERS

    override fun isCancelled(): Boolean = cancelled

    override fun setCancelled(cancel: Boolean) {
        this.cancelled = cancel
    }

    companion object {
        @JvmStatic
        val HANDLERS = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList = HANDLERS
    }
}
