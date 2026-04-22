package cc.bkhk.monoceros.api.version.service

import cc.bkhk.monoceros.api.version.VersionProfile
import cc.bkhk.monoceros.api.version.VersionedService
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

/**
 * Material 映射服务
 *
 * 处理旧版本 Material 名称到新版本的映射。
 */
interface MaterialMappingService : VersionedService {

    /** 按名称查找 Material（兼容旧版名称） */
    fun matchMaterial(name: String): Material?

    /** 获取 Material 的规范名称 */
    fun canonicalName(material: Material): String
}

/**
 * 文本处理服务
 *
 * 处理不同版本间的文本格式差异。
 */
interface TextProcessingService : VersionedService {

    /** 将颜色代码文本转换为当前版本支持的格式 */
    fun colorize(text: String): String

    /** 去除文本中的颜色代码 */
    fun stripColor(text: String): String
}

/**
 * 物品元数据桥接服务
 *
 * 处理不同版本间的 ItemMeta 差异。
 */
interface ItemMetaBridgeService : VersionedService {

    /** 获取物品显示名称 */
    fun getDisplayName(item: ItemStack): String?

    /** 设置物品显示名称 */
    fun setDisplayName(item: ItemStack, name: String?)

    /** 获取物品 Lore */
    fun getLore(item: ItemStack): List<String>

    /** 设置物品 Lore */
    fun setLore(item: ItemStack, lore: List<String>)

    /** 获取物品自定义模型数据（1.14+，旧版返回 0） */
    fun getCustomModelData(item: ItemStack): Int

    /** 设置物品自定义模型数据（1.14+，旧版忽略） */
    fun setCustomModelData(item: ItemStack, data: Int)
}
