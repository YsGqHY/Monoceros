package cc.bkhk.monoceros.volatility

import org.bukkit.Location
import org.bukkit.block.data.BlockData
import org.bukkit.entity.Player

/**
 * 伪方块 NMS 实现
 *
 * 通过 Bukkit API 实现跨版本兼容的伪方块变更。
 * Bukkit 的 sendBlockChange 内部已处理 NMS 版本差异。
 */
class NmsVolatileBlockImpl : NmsVolatileBlock() {

    override fun sendBlockChange(viewer: Player, location: Location, data: BlockData) {
        viewer.sendBlockChange(location, data)
    }

    override fun sendBlockChanges(viewer: Player, changes: List<Pair<Location, BlockData>>) {
        if (changes.isEmpty()) return
        for ((loc, blockData) in changes) {
            viewer.sendBlockChange(loc, blockData)
        }
    }
}
