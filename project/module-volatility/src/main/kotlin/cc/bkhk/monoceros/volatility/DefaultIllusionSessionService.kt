package cc.bkhk.monoceros.volatility

import cc.bkhk.monoceros.api.volatility.EntityFlag
import cc.bkhk.monoceros.api.volatility.IllusionKey
import cc.bkhk.monoceros.api.volatility.IllusionSessionService
import cc.bkhk.monoceros.api.volatility.WorldBorderState
import cc.bkhk.monoceros.impl.util.DiagnosticLogger
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.block.data.BlockData
import org.bukkit.entity.Entity
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 幻象会话服务默认实现
 *
 * 管理伪方块、伪世界边界、实体标志位等客户端幻象效果的生命周期。
 * 通过 [IllusionKey] 精准标识和回滚幻象效果。
 */
class DefaultIllusionSessionService : IllusionSessionService {

    private companion object {
        const val MODULE = "Illusion"
    }

    /** 幻象方块记录：key -> (location -> blockData) */
    private val blockRecords = ConcurrentHashMap<IllusionKey, ConcurrentHashMap<Location, BlockData>>()

    /** 幻象世界边界记录：key -> state */
    private val borderRecords = ConcurrentHashMap<IllusionKey, WorldBorderState>()

    /** 实体标志位记录：key -> (entityId -> (flag -> value)) */
    private val flagRecords = ConcurrentHashMap<IllusionKey, ConcurrentHashMap<Int, ConcurrentHashMap<EntityFlag, Boolean>>>()

    override fun putBlock(key: IllusionKey, location: Location, data: BlockData) {
        val player = Bukkit.getPlayer(key.viewerId) ?: return
        blockRecords.computeIfAbsent(key) { ConcurrentHashMap() }[location] = data
        NmsVolatileBlock.INSTANCE.sendBlockChange(player, location, data)
    }

    override fun removeBlock(key: IllusionKey, location: Location) {
        val player = Bukkit.getPlayer(key.viewerId) ?: return
        blockRecords[key]?.remove(location)
        // 恢复真实方块状态
        val realData = location.block.blockData
        NmsVolatileBlock.INSTANCE.sendBlockChange(player, location, realData)
    }

    override fun applyWorldBorder(key: IllusionKey, state: WorldBorderState) {
        val player = Bukkit.getPlayer(key.viewerId) ?: return
        borderRecords[key] = state
        NmsVolatileWorldBorder.INSTANCE.sendWorldBorder(player, state)
    }

    override fun setEntityFlag(key: IllusionKey, entity: Entity, flag: EntityFlag, value: Boolean) {
        val player = Bukkit.getPlayer(key.viewerId) ?: return
        flagRecords.computeIfAbsent(key) { ConcurrentHashMap() }
            .computeIfAbsent(entity.entityId) { ConcurrentHashMap() }[flag] = value
        NmsVolatileEntityMetadata.INSTANCE.setFlag(player, entity, flag, value)
    }

    override fun clear(key: IllusionKey) {
        val player = Bukkit.getPlayer(key.viewerId)

        // 恢复幻象方块
        blockRecords.remove(key)?.let { blocks ->
            if (player != null) {
                for ((location, _) in blocks) {
                    val realData = location.block.blockData
                    NmsVolatileBlock.INSTANCE.sendBlockChange(player, location, realData)
                }
            }
        }

        // 恢复幻象世界边界（发送服务器真实世界边界状态）
        borderRecords.remove(key)?.let { state ->
            if (player != null) {
                val world = state.world
                val wb = world.worldBorder
                val realState = WorldBorderState(
                    world = world,
                    size = wb.size,
                    center = wb.center,
                    warningTime = wb.warningTime,
                    warningDistance = wb.warningDistance,
                    damageBuffer = wb.damageBuffer,
                    damageAmount = wb.damageAmount,
                )
                NmsVolatileWorldBorder.INSTANCE.sendWorldBorder(player, realState)
            }
        }

        // 恢复实体标志位（重置为 false）
        flagRecords.remove(key)?.let { entityFlags ->
            if (player != null) {
                for ((entityId, flags) in entityFlags) {
                    val entity = player.world.entities.firstOrNull { it.entityId == entityId } ?: continue
                    for ((flag, _) in flags) {
                        NmsVolatileEntityMetadata.INSTANCE.setFlag(player, entity, flag, false)
                    }
                }
            }
        }

        DiagnosticLogger.info(MODULE, "清除幻象: ${key.namespace}:${key.targetId} for ${key.viewerId}")
    }

    override fun clearViewer(viewerId: UUID) {
        val keysToRemove = blockRecords.keys.filter { it.viewerId == viewerId } +
            borderRecords.keys.filter { it.viewerId == viewerId } +
            flagRecords.keys.filter { it.viewerId == viewerId }

        keysToRemove.toSet().forEach { clear(it) }
    }

    /** 清空所有幻象记录（先恢复客户端状态再清记录） */
    fun clearAll() {
        val allKeys = (blockRecords.keys + borderRecords.keys + flagRecords.keys).toSet()
        allKeys.forEach { clear(it) }
    }
}
