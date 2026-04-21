package cc.bkhk.monoceros.api.volatility

import org.bukkit.Location
import org.bukkit.block.data.BlockData
import java.util.UUID

/**
 * 幻象键
 *
 * 用于精准标识一个幻象效果的归属，支持按 viewer + namespace + targetId 撤销。
 */
data class IllusionKey(
    val viewerId: UUID,
    val namespace: String,
    val targetId: String,
)

/**
 * 幻象会话服务
 *
 * 管理伪方块、伪世界边界等客户端幻象效果的生命周期。
 * 同一玩家可被多个机制同时写入幻象状态，通过 [IllusionKey] 精准回滚。
 */
interface IllusionSessionService {

    /** 设置幻象方块 */
    fun putBlock(key: IllusionKey, location: Location, data: BlockData)

    /** 移除幻象方块 */
    fun removeBlock(key: IllusionKey, location: Location)

    /** 应用幻象世界边界 */
    fun applyWorldBorder(key: IllusionKey, state: WorldBorderState)

    /** 清除指定 key 的所有幻象效果 */
    fun clear(key: IllusionKey)

    /** 清除指定玩家的所有幻象效果 */
    fun clearViewer(viewerId: UUID)
}
