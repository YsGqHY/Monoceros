package cc.bkhk.monoceros.volatility

import org.bukkit.Location
import org.bukkit.block.data.BlockData
import org.bukkit.entity.Player
import taboolib.module.nms.nmsProxy

/**
 * 伪方块 NMS 代理接口
 *
 * 通过 TabooLib nmsProxy 自动选择版本实现。
 * 实现类命名约定：NmsVolatileBlockImpl
 */
abstract class NmsVolatileBlock {

    /** 向指定玩家发送单个伪方块变更 */
    abstract fun sendBlockChange(viewer: Player, location: Location, data: BlockData)

    /** 向指定玩家批量发送伪方块变更 */
    abstract fun sendBlockChanges(viewer: Player, changes: List<Pair<Location, BlockData>>)

    companion object {

        val INSTANCE by lazy {
            try {
                nmsProxy<NmsVolatileBlock>()
            } catch (_: Exception) {
                // 降级：使用 Bukkit API
                BukkitFallbackVolatileBlock()
            }
        }
    }
}

/**
 * Bukkit API 降级实现
 *
 * 当 NMS 代理不可用时，使用 Bukkit 原生 API 作为降级方案。
 */
class BukkitFallbackVolatileBlock : NmsVolatileBlock() {

    override fun sendBlockChange(viewer: Player, location: Location, data: BlockData) {
        viewer.sendBlockChange(location, data)
    }

    override fun sendBlockChanges(viewer: Player, changes: List<Pair<Location, BlockData>>) {
        for ((loc, blockData) in changes) {
            viewer.sendBlockChange(loc, blockData)
        }
    }
}
