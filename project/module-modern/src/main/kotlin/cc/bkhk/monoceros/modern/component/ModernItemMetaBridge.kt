package cc.bkhk.monoceros.modern.component

import cc.bkhk.monoceros.api.version.VersionProfile
import cc.bkhk.monoceros.api.version.service.ItemMetaBridgeService
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta

/**
 * 现代版本物品元数据桥接实现
 *
 * 适配 1.13+ 的 ItemMeta API。
 * Data Components (1.20.5+) 的深度适配在后续版本中补充。
 */
object ModernItemMetaBridge : ItemMetaBridgeService {

    override fun supports(profile: VersionProfile): Boolean = profile.modernMode

    override fun getDisplayName(item: ItemStack): String? {
        val meta = item.itemMeta ?: return null
        return if (meta.hasDisplayName()) meta.displayName else null
    }

    override fun setDisplayName(item: ItemStack, name: String?) {
        val meta = item.itemMeta ?: return
        meta.setDisplayName(name)
        item.itemMeta = meta
    }

    override fun getLore(item: ItemStack): List<String> {
        val meta = item.itemMeta ?: return emptyList()
        return meta.lore ?: emptyList()
    }

    override fun setLore(item: ItemStack, lore: List<String>) {
        val meta = item.itemMeta ?: return
        meta.lore = lore
        item.itemMeta = meta
    }

    override fun getCustomModelData(item: ItemStack): Int {
        val meta = item.itemMeta ?: return 0
        return try {
            if (meta.hasCustomModelData()) meta.customModelData else 0
        } catch (_: NoSuchMethodError) {
            // 1.13 以下没有 customModelData
            0
        }
    }

    override fun setCustomModelData(item: ItemStack, data: Int) {
        val meta = item.itemMeta ?: return
        try {
            meta.setCustomModelData(data)
            item.itemMeta = meta
        } catch (_: NoSuchMethodError) {
            // 1.13 以下忽略
        }
    }
}
