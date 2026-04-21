package cc.bkhk.monoceros.wireshark

import cc.bkhk.monoceros.api.wireshark.PacketDirection
import cc.bkhk.monoceros.api.wireshark.PacketSession
import cc.bkhk.monoceros.api.wireshark.PacketTraceRecord
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedDeque

/**
 * 玩家级数据包会话默认实现
 *
 * 维护该玩家启用的 tap 集合与追踪记录。
 */
class DefaultPacketSession(
    override val playerId: UUID,
) : PacketSession {

    /** 已启用的 tap ID 集合 */
    internal val enabledTapIds: MutableSet<String> = ConcurrentHashMap.newKeySet()

    /** 追踪记录（有界队列） */
    private val traces = ConcurrentLinkedDeque<PacketTraceRecord>()

    /** 追踪记录最大容量 */
    private val maxTraceSize = 500

    override fun enableTap(id: String) {
        enabledTapIds.add(id)
    }

    override fun disableTap(id: String) {
        enabledTapIds.remove(id)
    }

    override fun trace(limit: Int): List<PacketTraceRecord> {
        return traces.take(limit)
    }

    /** 添加追踪记录 */
    internal fun addTrace(record: PacketTraceRecord) {
        traces.addFirst(record)
        // 超出容量时移除最旧的记录
        while (traces.size > maxTraceSize) {
            traces.pollLast()
        }
    }
}
