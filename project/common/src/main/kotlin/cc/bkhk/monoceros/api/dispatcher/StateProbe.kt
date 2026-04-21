package cc.bkhk.monoceros.api.dispatcher

import org.bukkit.event.Event

/**
 * 状态补偿探针
 *
 * 用于 Bukkit 原生没有提供事件的状态变化（如玩家朝向变化、持有物品变化等）。
 * 通过调度轮询 + 快照比对产出虚拟事件，产出的事件仍走同一套 dispatcher 路由。
 */
interface StateProbe {
    val id: String
    fun poll(): Collection<Event>
}
