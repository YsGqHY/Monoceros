package cc.bkhk.monoceros.extension.action.item

import cc.bkhk.monoceros.Monoceros
import cc.bkhk.monoceros.api.extension.NativeExtension
import cc.bkhk.monoceros.api.workflow.ActionContext
import cc.bkhk.monoceros.api.workflow.ActionNode
import cc.bkhk.monoceros.api.workflow.ActionNodeDefinition
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack

/** 从上下文解析物品 */
private fun resolveItem(context: ActionContext): ItemStack? {
    return context.variables["item"] as? ItemStack
        ?: context.variables["targetItem"] as? ItemStack
        ?: context.variables["lastResult"] as? ItemStack
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
    }
}
