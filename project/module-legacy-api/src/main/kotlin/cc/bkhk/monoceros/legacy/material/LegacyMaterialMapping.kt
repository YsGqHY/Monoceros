package cc.bkhk.monoceros.legacy.material

import cc.bkhk.monoceros.api.version.VersionProfile
import cc.bkhk.monoceros.api.version.service.MaterialMappingService
import org.bukkit.Material

/**
 * 旧版 Material 映射实现
 *
 * 处理 1.12.2 等旧版本的 Material 名称映射。
 * 旧版 Material 名称（如 WOOD_SWORD）映射到新版名称（如 WOODEN_SWORD）。
 */
object LegacyMaterialMapping : MaterialMappingService {

    /** 旧版名称 -> 新版名称映射表 */
    private val legacyMapping = mapOf(
        "WOOD_SWORD" to "WOODEN_SWORD",
        "WOOD_AXE" to "WOODEN_AXE",
        "WOOD_PICKAXE" to "WOODEN_PICKAXE",
        "WOOD_SPADE" to "WOODEN_SHOVEL",
        "WOOD_HOE" to "WOODEN_HOE",
        "GOLD_SWORD" to "GOLDEN_SWORD",
        "GOLD_AXE" to "GOLDEN_AXE",
        "GOLD_PICKAXE" to "GOLDEN_PICKAXE",
        "GOLD_SPADE" to "GOLDEN_SHOVEL",
        "GOLD_HOE" to "GOLDEN_HOE",
        "GOLD_HELMET" to "GOLDEN_HELMET",
        "GOLD_CHESTPLATE" to "GOLDEN_CHESTPLATE",
        "GOLD_LEGGINGS" to "GOLDEN_LEGGINGS",
        "GOLD_BOOTS" to "GOLDEN_BOOTS",
        "DIAMOND_SPADE" to "DIAMOND_SHOVEL",
        "IRON_SPADE" to "IRON_SHOVEL",
        "STONE_SPADE" to "STONE_SHOVEL",
        "SULPHUR" to "GUNPOWDER",
        "WATCH" to "CLOCK",
        "EXP_BOTTLE" to "EXPERIENCE_BOTTLE",
        "SNOW_BALL" to "SNOWBALL",
        "FIREBALL" to "FIRE_CHARGE",
        "NETHER_STALK" to "NETHER_WART",
        "THIN_GLASS" to "GLASS_PANE",
        "IRON_FENCE" to "IRON_BARS",
        "WORKBENCH" to "CRAFTING_TABLE",
        "WOOD" to "OAK_PLANKS",
        "LOG" to "OAK_LOG",
        "SAPLING" to "OAK_SAPLING",
        "LEAVES" to "OAK_LEAVES",
        "WEB" to "COBWEB",
        "LONG_GRASS" to "GRASS",
        "YELLOW_FLOWER" to "DANDELION",
        "RED_ROSE" to "POPPY",
        "STEP" to "STONE_SLAB",
        "SMOOTH_BRICK" to "STONE_BRICKS",
        "SIGN_POST" to "OAK_SIGN",
        "WALL_SIGN" to "OAK_WALL_SIGN",
        "WOOD_DOOR" to "OAK_DOOR",
        "WOOD_BUTTON" to "OAK_BUTTON",
        "WOOD_PLATE" to "OAK_PRESSURE_PLATE",
        "FENCE" to "OAK_FENCE",
        "FENCE_GATE" to "OAK_FENCE_GATE",
        "WOOD_STAIRS" to "OAK_STAIRS",
        "BOAT" to "OAK_BOAT",
    )

    override fun supports(profile: VersionProfile): Boolean = profile.legacyMode

    override fun matchMaterial(name: String): Material? {
        val upper = name.uppercase().replace(' ', '_').replace('-', '_')
        // 先尝试直接匹配
        Material.matchMaterial(upper)?.let { return it }
        // 尝试旧版映射
        val mapped = legacyMapping[upper]
        if (mapped != null) {
            Material.matchMaterial(mapped)?.let { return it }
        }
        return null
    }

    override fun canonicalName(material: Material): String = material.name
}
