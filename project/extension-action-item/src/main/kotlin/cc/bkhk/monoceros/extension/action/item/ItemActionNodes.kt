package cc.bkhk.monoceros.extension.action.item

import cc.bkhk.monoceros.Monoceros
import cc.bkhk.monoceros.api.extension.NativeExtension
import cc.bkhk.monoceros.api.workflow.ActionContext
import cc.bkhk.monoceros.api.workflow.ActionNode
import cc.bkhk.monoceros.api.workflow.ActionNodeDefinition
import org.bukkit.Color
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable
import org.bukkit.inventory.meta.LeatherArmorMeta
import org.bukkit.inventory.meta.PotionMeta
import org.bukkit.persistence.PersistentDataType
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

/** 从上下文解析物品 */
private fun resolveItem(context: ActionContext): ItemStack? {
    return context.variables["item"] as? ItemStack
        ?: context.variables["targetItem"] as? ItemStack
        ?: context.variables["lastResult"] as? ItemStack
}

private fun parseColor(value: Any?): Color? {
    return when (value) {
        is Color -> value
        is String -> {
            val text = value.trim()
            if (text.startsWith("#") && text.length == 7) {
                runCatching { Color.fromRGB(text.substring(1).toInt(16)) }.getOrNull()
            } else {
                val parts = text.split(',', ' ').mapNotNull { it.trim().toIntOrNull() }
                if (parts.size == 3) Color.fromRGB(parts[0], parts[1], parts[2]) else null
            }
        }
        is List<*> -> {
            val rgb = value.mapNotNull { (it as? Number)?.toInt() }
            if (rgb.size == 3) Color.fromRGB(rgb[0], rgb[1], rgb[2]) else null
        }
        else -> null
    }
}

private fun namespacedKey(key: String): NamespacedKey? {
    return NamespacedKey.fromString("monoceros:${key.lowercase().replace(' ', '_')}")
}

class ItemBuildNode : ActionNode {
    override val type = "item.build"
    override fun execute(context: ActionContext, definition: ActionNodeDefinition): Any? {
        val materialName = definition.config["material"] as? String ?: "STONE"
        val material = Material.matchMaterial(materialName) ?: Material.STONE
        val amount = (definition.config["amount"] as? Number)?.toInt() ?: 1
        val item = ItemStack(material, amount)
        context.variables["item"] = item
        return item
    }
}

class ItemAmountNode : ActionNode {
    override val type = "item.amount"
    override fun execute(context: ActionContext, definition: ActionNodeDefinition): Any? {
        val item = resolveItem(context) ?: return null
        val amount = (definition.config["value"] as? Number)?.toInt() ?: return item.amount
        item.amount = amount.coerceIn(1, item.maxStackSize)
        return item.amount
    }
}

class ItemNameNode : ActionNode {
    override val type = "item.name"
    override fun execute(context: ActionContext, definition: ActionNodeDefinition): Any? {
        val item = resolveItem(context) ?: return null
        val name = (definition.config["value"] as? String)?.replace(Regex("&([0-9a-fk-orA-FK-OR])"), "\u00a7$1") ?: return null
        val meta = item.itemMeta ?: return null
        meta.setDisplayName(name)
        item.itemMeta = meta
        return name
    }
}

class ItemLoreNode : ActionNode {
    override val type = "item.lore"
    override fun execute(context: ActionContext, definition: ActionNodeDefinition): Any? {
        val item = resolveItem(context) ?: return null
        val meta = item.itemMeta ?: return null
        @Suppress("UNCHECKED_CAST")
        val lines = (definition.config["value"] as? List<*>)?.map { it.toString().replace(Regex("&([0-9a-fk-orA-FK-OR])"), "\u00a7$1") }
            ?: return null
        val mode = definition.config["mode"] as? String ?: "set"
        when (mode.lowercase()) {
            "set" -> meta.lore = lines
            "append" -> meta.lore = (meta.lore ?: mutableListOf()) + lines
        }
        item.itemMeta = meta
        return lines
    }
}

class ItemEnchantNode : ActionNode {
    override val type = "item.enchant"
    override fun execute(context: ActionContext, definition: ActionNodeDefinition): Any? {
        val item = resolveItem(context) ?: return null
        val enchantName = definition.config["enchantment"] as? String ?: return null
        val level = (definition.config["level"] as? Number)?.toInt() ?: 1
        val enchant = Enchantment.getByName(enchantName.uppercase()) ?: return null
        item.addUnsafeEnchantment(enchant, level)
        return level
    }
}

class ItemFlagNode : ActionNode {
    override val type = "item.flag"
    override fun execute(context: ActionContext, definition: ActionNodeDefinition): Any? {
        val item = resolveItem(context) ?: return null
        val flagName = definition.config["flag"] as? String ?: return null
        val flag = try { ItemFlag.valueOf(flagName.uppercase()) } catch (_: Exception) { return null }
        val meta = item.itemMeta ?: return null
        meta.addItemFlags(flag)
        item.itemMeta = meta
        return flag.name
    }
}

class ItemUnbreakableNode : ActionNode {
    override val type = "item.unbreakable"
    override fun execute(context: ActionContext, definition: ActionNodeDefinition): Any? {
        val item = resolveItem(context) ?: return null
        val value = definition.config["value"] as? Boolean ?: true
        val meta = item.itemMeta ?: return null
        meta.isUnbreakable = value
        item.itemMeta = meta
        return value
    }
}

class ItemColorNode : ActionNode {
    override val type = "item.color"
    override fun execute(context: ActionContext, definition: ActionNodeDefinition): Any? {
        val item = resolveItem(context) ?: return null
        val meta = item.itemMeta as? LeatherArmorMeta ?: return null
        val color = parseColor(definition.config["value"]) ?: return null
        meta.setColor(color)
        item.itemMeta = meta
        return color
    }
}

class ItemDamageNode : ActionNode {
    override val type = "item.damage"
    override fun execute(context: ActionContext, definition: ActionNodeDefinition): Any? {
        val item = resolveItem(context) ?: return null
        val value = (definition.config["value"] as? Number)?.toInt() ?: return null
        val meta = item.itemMeta as? Damageable ?: return null
        meta.damage = value.coerceAtLeast(0)
        item.itemMeta = meta as org.bukkit.inventory.meta.ItemMeta
        return meta.damage
    }
}

class ItemDurabilityNode : ActionNode {
    override val type = "item.durability"
    override fun execute(context: ActionContext, definition: ActionNodeDefinition): Any? {
        val item = resolveItem(context) ?: return null
        val value = (definition.config["value"] as? Number)?.toInt() ?: return null
        val max = item.type.maxDurability.toInt()
        if (max <= 0) return null
        val meta = item.itemMeta as? Damageable ?: return null
        meta.damage = (max - value.coerceIn(0, max)).coerceIn(0, max)
        item.itemMeta = meta as org.bukkit.inventory.meta.ItemMeta
        return value.coerceIn(0, max)
    }
}

class ItemTagNode : ActionNode {
    override val type = "item.tag"
    override fun execute(context: ActionContext, definition: ActionNodeDefinition): Any? {
        val item = resolveItem(context) ?: return null
        val key = definition.config["key"] as? String ?: return null
        val value = definition.config["value"]?.toString() ?: return null
        val meta = item.itemMeta ?: return null
        val namespacedKey = namespacedKey(key) ?: return null
        meta.persistentDataContainer.set(namespacedKey, PersistentDataType.STRING, value)
        item.itemMeta = meta
        return value
    }
}

/** 切换当前物品上下文 */
class ItemSwitchNode : ActionNode {
    override val type = "item.switch"
    override fun execute(context: ActionContext, definition: ActionNodeDefinition): Any? {
        val source = definition.config["source"] as? String ?: "player"
        val slot = (definition.config["slot"] as? Number)?.toInt()
        val player = context.variables["player"] as? org.bukkit.entity.Player
        val item: ItemStack? = when (source.lowercase()) {
            "player", "hand", "main-hand" -> player?.inventory?.itemInMainHand
            "off-hand" -> player?.inventory?.itemInOffHand
            "slot" -> slot?.let { player?.inventory?.getItem(it) }
            "cursor" -> player?.openInventory?.cursor
            "helmet" -> player?.inventory?.helmet
            "chestplate" -> player?.inventory?.chestplate
            "leggings" -> player?.inventory?.leggings
            "boots" -> player?.inventory?.boots
            "variable" -> {
                val varName = definition.config["variable"] as? String ?: "item"
                context.variables[varName] as? ItemStack
            }
            else -> null
        }
        if (item != null) {
            context.variables["item"] = item
        }
        return item
    }
}

/** 获取药水效果数量 */
class ItemPotionSizeNode : ActionNode {
    override val type = "item.potion.size"
    override fun execute(context: ActionContext, definition: ActionNodeDefinition): Any? {
        val item = resolveItem(context) ?: return 0
        val meta = item.itemMeta as? PotionMeta ?: return 0
        return meta.customEffects.size
    }
}

/** 检查是否存在指定药水效果 */
class ItemPotionHasNode : ActionNode {
    override val type = "item.potion.has"
    override fun execute(context: ActionContext, definition: ActionNodeDefinition): Any? {
        val item = resolveItem(context) ?: return false
        val meta = item.itemMeta as? PotionMeta ?: return false
        val effectName = definition.config["effect"] as? String ?: return false
        val effectType = resolvePotionEffectType(effectName) ?: return false
        return meta.hasCustomEffect(effectType)
    }
}

/** 添加/覆盖药水效果 */
class ItemPotionSetNode : ActionNode {
    override val type = "item.potion.set"
    override fun execute(context: ActionContext, definition: ActionNodeDefinition): Any? {
        val item = resolveItem(context) ?: return null
        val meta = item.itemMeta as? PotionMeta ?: return null
        val effectName = definition.config["effect"] as? String ?: return null
        val effectType = resolvePotionEffectType(effectName) ?: return null
        val duration = (definition.config["duration"] as? Number)?.toInt() ?: 200
        val amplifier = (definition.config["amplifier"] as? Number)?.toInt() ?: 0
        val ambient = definition.config["ambient"] as? Boolean ?: false
        val particles = definition.config["particles"] as? Boolean ?: true
        val icon = definition.config["icon"] as? Boolean ?: true
        val effect = PotionEffect(effectType, duration, amplifier, ambient, particles, icon)
        meta.addCustomEffect(effect, true)
        item.itemMeta = meta
        return true
    }
}

/** 移除指定药水效果 */
class ItemPotionRemoveNode : ActionNode {
    override val type = "item.potion.remove"
    override fun execute(context: ActionContext, definition: ActionNodeDefinition): Any? {
        val item = resolveItem(context) ?: return false
        val meta = item.itemMeta as? PotionMeta ?: return false
        val effectName = definition.config["effect"] as? String ?: return false
        val effectType = resolvePotionEffectType(effectName) ?: return false
        val removed = meta.removeCustomEffect(effectType)
        item.itemMeta = meta
        return removed
    }
}

/** 清除所有药水效果 */
class ItemPotionClearNode : ActionNode {
    override val type = "item.potion.clear"
    override fun execute(context: ActionContext, definition: ActionNodeDefinition): Any? {
        val item = resolveItem(context) ?: return false
        val meta = item.itemMeta as? PotionMeta ?: return false
        meta.clearCustomEffects()
        item.itemMeta = meta
        return true
    }
}

/** 解析药水效果类型（兼容 1.12.2 的名称差异） */
private fun resolvePotionEffectType(name: String): PotionEffectType? {
    val upper = name.uppercase().replace(' ', '_').replace('-', '_')
    return PotionEffectType.getByName(upper)
}

/** 消耗物品（减少数量） */
class ItemConsumeNode : ActionNode {
    override val type = "item.consume"
    override fun execute(context: ActionContext, definition: ActionNodeDefinition): Any? {
        val item = resolveItem(context) ?: return null
        val amount = (definition.config["amount"] as? Number)?.toInt() ?: 1
        item.amount = (item.amount - amount).coerceAtLeast(0)
        return item.amount
    }
}

/** 销毁物品（设置数量为 0） */
class ItemDestroyNode : ActionNode {
    override val type = "item.destroy"
    override fun execute(context: ActionContext, definition: ActionNodeDefinition): Any? {
        val item = resolveItem(context) ?: return null
        item.amount = 0
        return true
    }
}

/** 掉落物品到世界 */
class ItemDropNode : ActionNode {
    override val type = "item.drop"
    override fun execute(context: ActionContext, definition: ActionNodeDefinition): Any? {
        val item = resolveItem(context) ?: return null
        val player = context.variables["player"] as? org.bukkit.entity.Player ?: return null
        val location = context.variables["location"] as? org.bukkit.Location ?: player.location
        val dropped = location.world?.dropItemNaturally(location, item.clone())
        return dropped != null
    }
}

/** 给予玩家物品 */
class ItemGiveNode : ActionNode {
    override val type = "item.give"
    override fun execute(context: ActionContext, definition: ActionNodeDefinition): Any? {
        val item = resolveItem(context) ?: return null
        val player = context.variables["player"] as? org.bukkit.entity.Player ?: return null
        val amount = (definition.config["amount"] as? Number)?.toInt() ?: item.amount
        val giveItem = item.clone().apply { this.amount = amount }
        val overflow = player.inventory.addItem(giveItem)
        return overflow.isEmpty()
    }
}

/** 匹配物品（检查类型/名称/lore） */
class ItemMatchNode : ActionNode {
    override val type = "item.match"
    override fun execute(context: ActionContext, definition: ActionNodeDefinition): Any? {
        val item = resolveItem(context) ?: return false
        val materialName = definition.config["material"] as? String
        val displayName = definition.config["name"] as? String
        if (materialName != null && !item.type.name.equals(materialName, ignoreCase = true)) return false
        if (displayName != null) {
            val meta = item.itemMeta ?: return false
            if (!meta.hasDisplayName() || !meta.displayName.contains(displayName)) return false
        }
        return true
    }
}

/** 修改物品（通用属性修改） */
class ItemModifyNode : ActionNode {
    override val type = "item.modify"
    override fun execute(context: ActionContext, definition: ActionNodeDefinition): Any? {
        val item = resolveItem(context) ?: return null
        (definition.config["amount"] as? Number)?.let { item.amount = it.toInt().coerceIn(1, item.maxStackSize) }
        (definition.config["material"] as? String)?.let { name ->
            Material.matchMaterial(name)?.let { item.type = it }
        }
        return item
    }
}

/** 检查背包中是否有指定物品 */
class InventoryCheckNode : ActionNode {
    override val type = "inventory.check"
    override fun execute(context: ActionContext, definition: ActionNodeDefinition): Any? {
        val player = context.variables["player"] as? org.bukkit.entity.Player ?: return false
        val materialName = definition.config["material"] as? String ?: return false
        val material = Material.matchMaterial(materialName) ?: return false
        val amount = (definition.config["amount"] as? Number)?.toInt() ?: 1
        return player.inventory.contains(material, amount)
    }
}

/** 统计背包中指定物品数量 */
class InventoryCountNode : ActionNode {
    override val type = "inventory.count"
    override fun execute(context: ActionContext, definition: ActionNodeDefinition): Any? {
        val player = context.variables["player"] as? org.bukkit.entity.Player ?: return 0
        val materialName = definition.config["material"] as? String ?: return 0
        val material = Material.matchMaterial(materialName) ?: return 0
        return player.inventory.contents.filterNotNull().filter { it.type == material }.sumOf { it.amount }
    }
}

/** 查找背包中指定物品的槽位 */
class InventoryFindNode : ActionNode {
    override val type = "inventory.find"
    override fun execute(context: ActionContext, definition: ActionNodeDefinition): Any? {
        val player = context.variables["player"] as? org.bukkit.entity.Player ?: return -1
        val materialName = definition.config["material"] as? String ?: return -1
        val material = Material.matchMaterial(materialName) ?: return -1
        return player.inventory.first(material)
    }
}

/** 切换背包上下文 */
class InventorySwitchNode : ActionNode {
    override val type = "inventory.switch"
    override fun execute(context: ActionContext, definition: ActionNodeDefinition): Any? {
        val player = context.variables["player"] as? org.bukkit.entity.Player ?: return null
        val slot = (definition.config["slot"] as? Number)?.toInt() ?: return null
        val item = player.inventory.getItem(slot)
        if (item != null) {
            context.variables["item"] = item
        }
        return item
    }
}

/** 从背包中移除指定物品 */
class InventoryTakeNode : ActionNode {
    override val type = "inventory.take"
    override fun execute(context: ActionContext, definition: ActionNodeDefinition): Any? {
        val player = context.variables["player"] as? org.bukkit.entity.Player ?: return false
        val materialName = definition.config["material"] as? String ?: return false
        val material = Material.matchMaterial(materialName) ?: return false
        val amount = (definition.config["amount"] as? Number)?.toInt() ?: 1
        val item = ItemStack(material, amount)
        val result = player.inventory.removeItem(item)
        return result.isEmpty()
    }
}

class ItemActionExtension : NativeExtension() {
    override val id = "action-item"
    override val name = "物品域动作扩展"
    override val version = "1.0.0"

    override fun onEnable() {
        val service = Monoceros.api().actionWorkflow()
        service.registerNode(ItemBuildNode())
        service.registerNode(ItemAmountNode())
        service.registerNode(ItemNameNode())
        service.registerNode(ItemLoreNode())
        service.registerNode(ItemEnchantNode())
        service.registerNode(ItemFlagNode())
        service.registerNode(ItemUnbreakableNode())
        service.registerNode(ItemColorNode())
        service.registerNode(ItemDamageNode())
        service.registerNode(ItemDurabilityNode())
        service.registerNode(ItemTagNode())
        // P4.1 新增节点
        service.registerNode(ItemSwitchNode())
        service.registerNode(ItemPotionSizeNode())
        service.registerNode(ItemPotionHasNode())
        service.registerNode(ItemPotionSetNode())
        service.registerNode(ItemPotionRemoveNode())
        service.registerNode(ItemPotionClearNode())
        // 物品增强节点
        service.registerNode(ItemConsumeNode())
        service.registerNode(ItemDestroyNode())
        service.registerNode(ItemDropNode())
        service.registerNode(ItemGiveNode())
        service.registerNode(ItemMatchNode())
        service.registerNode(ItemModifyNode())
        // 背包操作节点
        service.registerNode(InventoryCheckNode())
        service.registerNode(InventoryCountNode())
        service.registerNode(InventoryFindNode())
        service.registerNode(InventorySwitchNode())
        service.registerNode(InventoryTakeNode())
    }
}
