package cc.bkhk.monoceros.volatility

import cc.bkhk.monoceros.api.volatility.DynamicWorldBorderState
import cc.bkhk.monoceros.api.volatility.WorldBorderState
import org.bukkit.entity.Player
import taboolib.module.nms.nmsProxy

/**
 * 伪世界边界 NMS 代理接口
 *
 * 通过 TabooLib nmsProxy 自动选择版本实现。
 * 实现类命名约定：NmsVolatileWorldBorderImpl
 */
abstract class NmsVolatileWorldBorder {

    /** 向指定玩家发送静态世界边界 */
    abstract fun sendWorldBorder(viewer: Player, state: WorldBorderState)

    /** 向指定玩家发送动态世界边界（收缩/扩张） */
    abstract fun sendDynamicWorldBorder(viewer: Player, state: DynamicWorldBorderState)

    companion object {

        val INSTANCE by lazy {
            try {
                nmsProxy<NmsVolatileWorldBorder>()
            } catch (_: Exception) {
                // 降级：空实现
                NoopVolatileWorldBorder()
            }
        }
    }
}

/**
 * 空实现降级
 *
 * 世界边界无法通过 Bukkit API 实现玩家级发送，降级时不做任何操作。
 */
class NoopVolatileWorldBorder : NmsVolatileWorldBorder() {

    override fun sendWorldBorder(viewer: Player, state: WorldBorderState) {
        // 无法降级，需要 NMS 实现
    }

    override fun sendDynamicWorldBorder(viewer: Player, state: DynamicWorldBorderState) {
        // 无法降级，需要 NMS 实现
    }
}
